/**
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2025 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */
package io.hexaglue.core.internal.ir.domain.analysis;

import io.hexaglue.core.diagnostics.DiagnosticFactory;
import io.hexaglue.core.frontend.AnnotationIntrospector;
import io.hexaglue.core.frontend.AnnotationModel;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.SourceRef;
import io.hexaglue.core.internal.ir.SourceRefs;
import io.hexaglue.core.internal.ir.domain.DomainId;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;

/**
 * Extracts domain types from analyzed source elements.
 *
 * <p>
 * This extractor is responsible for building complete {@link DomainType}
 * instances from
 * source elements (typically {@code javax.lang.model.element.TypeElement}
 * instances).
 * It coordinates with {@link DomainPropertyExtractor} and
 * {@link DomainTypeKindResolver}
 * to produce a comprehensive domain type representation.
 * </p>
 *
 * <h2>Extraction Process</h2>
 * <p>
 * For each domain type, the extractor:
 * </p>
 * <ol>
 * <li>Determines the type's qualified and simple names</li>
 * <li>Resolves the domain type kind (entity, value object, etc.)</li>
 * <li>Extracts all properties using {@link DomainPropertyExtractor}</li>
 * <li>Identifies the identity property if applicable</li>
 * <li>Determines immutability characteristics</li>
 * <li>Captures documentation and descriptions</li>
 * <li>Builds the final {@link DomainType} instance</li>
 * </ol>
 *
 * <h2>Dependencies</h2>
 * <p>
 * This extractor collaborates with:
 * </p>
 * <ul>
 * <li>{@link DomainTypeKindResolver} - for type kind classification</li>
 * <li>{@link DomainPropertyExtractor} - for property extraction</li>
 * <li>{@link DomainRules} - for validation during extraction</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is safe for concurrent use if constructed with thread-safe
 * dependencies.
 * </p>
 */
@InternalMarker(reason = "Internal domain analysis; not exposed to plugins")
public final class DomainTypeExtractor {

    private final DomainTypeKindResolver kindResolver;
    private final DomainPropertyExtractor propertyExtractor;
    private final DomainRules rules;
    private final Elements elementUtils;
    private final TypeResolver typeResolver;
    private final AggregateRootAnnotationDetector aggregateRootAnnotationDetector;
    private final EntityAnnotationDetector entityAnnotationDetector;
    private final ValueObjectAnnotationDetector valueObjectAnnotationDetector;
    private final DomainEventAnnotationDetector domainEventAnnotationDetector;
    private final DiagnosticReporter diagnostics;

    public DomainTypeExtractor(
            DomainTypeKindResolver kindResolver,
            DomainPropertyExtractor propertyExtractor,
            DomainRules rules,
            TypeResolver typeResolver,
            Elements elementUtils,
            DiagnosticReporter diagnostics) {
        this.kindResolver = Objects.requireNonNull(kindResolver, "kindResolver");
        this.propertyExtractor = Objects.requireNonNull(propertyExtractor, "propertyExtractor");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.typeResolver = Objects.requireNonNull(typeResolver, "typeResolver");
        this.elementUtils = Objects.requireNonNull(elementUtils, "elementUtils");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.aggregateRootAnnotationDetector = new AggregateRootAnnotationDetector();
        this.entityAnnotationDetector = new EntityAnnotationDetector();
        this.valueObjectAnnotationDetector = new ValueObjectAnnotationDetector();
        this.domainEventAnnotationDetector = new DomainEventAnnotationDetector();
    }

    public Optional<DomainType> extract(Object typeElement, TypeRef typeRef) {
        Objects.requireNonNull(typeElement, "typeElement");
        Objects.requireNonNull(typeRef, "typeRef");

        if (!(typeElement instanceof TypeElement te)) {
            throw new IllegalArgumentException("typeElement must be a TypeElement");
        }

        String qualifiedName = te.getQualifiedName().toString();
        String simpleName = te.getSimpleName().toString();
        boolean isEnum = te.getKind() == ElementKind.ENUM;
        boolean isRecord = te.getKind() == ElementKind.RECORD;

        List<DomainProperty> properties = propertyExtractor.extractProperties(te, qualifiedName, isRecord);

        Optional<DomainProperty> identityProperty = findIdentityProperty(properties);
        DomainId id =
                identityProperty.map(prop -> buildDomainId(prop, qualifiedName)).orElse(null);

        boolean immutable = determineImmutability(te, properties, isRecord);
        boolean hasValueObjectAnnotation = valueObjectAnnotationDetector.hasValueObjectAnnotation(te);
        boolean hasDomainEventAnnotation = domainEventAnnotationDetector.hasDomainEventAnnotation(te);

        // Emit diagnostics for detected jMolecules annotations
        if (hasDomainEventAnnotation) {
            diagnostics.report(DiagnosticFactory.info(
                    DomainAnalyzer.CODE_JMOLECULES_DOMAIN_EVENT,
                    "jMolecules @DomainEvent detected on type '" + simpleName + "'",
                    te,
                    "io.hexaglue.core"));
        }

        if (hasValueObjectAnnotation) {
            diagnostics.report(DiagnosticFactory.info(
                    DomainAnalyzer.CODE_JMOLECULES_VALUE_OBJECT,
                    "jMolecules @ValueObject detected on type '" + simpleName + "'",
                    te,
                    "io.hexaglue.core"));
        }

        boolean hasEntityAnnotation = entityAnnotationDetector.hasEntityAnnotation(te);
        if (hasEntityAnnotation) {
            diagnostics.report(DiagnosticFactory.info(
                    DomainAnalyzer.CODE_JMOLECULES_ENTITY,
                    "jMolecules @Entity detected on type '" + simpleName + "'",
                    te,
                    "io.hexaglue.core"));
        }

        boolean hasAggregateRootAnnotation = aggregateRootAnnotationDetector.hasAggregateRootAnnotation(te);
        if (hasAggregateRootAnnotation) {
            diagnostics.report(DiagnosticFactory.info(
                    DomainAnalyzer.CODE_JMOLECULES_AGGREGATE_ROOT,
                    "jMolecules @AggregateRoot detected on type '" + simpleName + "'",
                    te,
                    "io.hexaglue.core"));
        }

        DomainTypeKind kind = kindResolver.resolve(
                simpleName,
                isEnum,
                isRecord,
                identityProperty.isPresent(),
                immutable,
                hasDomainEventAnnotation,
                hasValueObjectAnnotation,
                hasEntityAnnotation);

        // Upgrade ENTITY to AGGREGATE_ROOT if marked with aggregate root annotations
        if (kind == DomainTypeKind.ENTITY && hasAggregateRootAnnotation) {
            kind = DomainTypeKind.AGGREGATE_ROOT;
        }

        String description = extractDocumentation(te);

        // Extract annotations
        List<AnnotationModel> annotations = AnnotationIntrospector.getAnnotations(te);

        // Extract type hierarchy information
        TypeRef superType = extractSuperType(te).orElse(null);
        List<TypeRef> interfaces = extractInterfaces(te);
        List<TypeRef> permittedSubtypes = extractPermittedSubtypes(te).orElse(null);

        // Extract enum constants if this is an enumeration
        List<String> enumConstants = extractEnumConstants(te).orElse(null);

        // SourceRef ref = SourceRef.builder(SourceRef.Kind.TYPE, qualifiedName)
        //         .origin("jsr269")
        //         .hint("domain type")
        //         .build();

        SourceRef ref = SourceRefs.coerce(te, qualifiedName, SourceRef.Kind.TYPE);

        DomainType domainType = DomainType.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .kind(kind)
                .type(typeRef)
                .addProperties(properties)
                .id(id)
                .immutable(immutable)
                .description(description)
                .sourceRef(ref)
                .annotations(annotations)
                .superType(superType)
                .interfaces(interfaces)
                .permittedSubtypes(permittedSubtypes)
                .enumConstants(enumConstants)
                .build();

        return Optional.of(domainType);
    }

    private boolean determineImmutability(TypeElement typeElement, List<DomainProperty> properties, boolean isRecord) {
        if (isRecord) {
            return true;
        }

        boolean isFinal = typeElement.getModifiers().contains(Modifier.FINAL);
        boolean allPropertiesImmutable = properties.stream().allMatch(DomainProperty::isImmutable);

        return isFinal && allPropertiesImmutable;
    }

    private String extractDocumentation(TypeElement typeElement) {
        String docComment = elementUtils.getDocComment(typeElement);
        if (docComment != null && !docComment.isBlank()) {
            int firstPeriod = docComment.indexOf('.');
            if (firstPeriod > 0) {
                return docComment.substring(0, firstPeriod + 1).trim();
            }
            return docComment.trim();
        }
        return null;
    }

    public DomainType extractType(
            String qualifiedName,
            String simpleName,
            DomainTypeKind kind,
            TypeRef typeRef,
            List<DomainProperty> properties,
            DomainId id,
            boolean immutable,
            String description,
            Object sourceElement) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(typeRef, "typeRef");
        Objects.requireNonNull(properties, "properties");

        SourceRef ref = SourceRefs.coerce(sourceElement, qualifiedName, SourceRef.Kind.TYPE);

        return DomainType.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .kind(kind)
                .type(typeRef)
                .addProperties(properties)
                .id(id)
                .immutable(immutable)
                .description(description)
                .sourceRef(ref)
                .build();
    }

    /**
     * Determines if the given type element represents a domain type.
     *
     * <p>
     * This method applies heuristics to determine if a type should be analyzed as part
     * of the domain model. It checks:
     * </p>
     * <ul>
     *   <li>Element kind (must be class, enum, or record - not interface)</li>
     *   <li>Package name patterns (e.g., *.domain.*, *.model.*)</li>
     *   <li>Exclusion of technical types (controllers, repositories, etc.)</li>
     *   <li>Exclusion of JDK and common library types</li>
     * </ul>
     *
     * @param typeElement source element to check (not {@code null})
     * @return {@code true} if the element is likely a domain type
     * @throws NullPointerException if typeElement is null
     */
    public boolean isDomainType(Object typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        if (!(typeElement instanceof TypeElement te)) {
            return false;
        }

        // Check element kind - must be class, enum, or record (not interface)
        ElementKind kind = te.getKind();
        boolean isInterface = (kind == ElementKind.INTERFACE);

        // Extract qualified name and package name
        String qualifiedName = te.getQualifiedName().toString();
        String packageName = extractPackageName(qualifiedName);

        // Delegate to domain rules for heuristic-based determination
        return rules.isLikelyDomainType(qualifiedName, packageName, isInterface);
    }

    /**
     * Extracts the direct supertype (superclass) from a type element.
     *
     * <p>Returns empty if:
     * <ul>
     *   <li>The type is {@code java.lang.Object}</li>
     *   <li>The type doesn't explicitly extend another class</li>
     *   <li>The supertype is {@code java.lang.Object}</li>
     *   <li>The type is an interface, enum, or record</li>
     * </ul>
     *
     * @param typeElement type element (not {@code null})
     * @return supertype or empty
     */
    private Optional<TypeRef> extractSuperType(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        // Interfaces, enums don't have meaningful superclasses for our purposes
        ElementKind kind = typeElement.getKind();
        if (kind == ElementKind.INTERFACE || kind == ElementKind.ENUM) {
            return Optional.empty();
        }

        // Records implicitly extend java.lang.Record, but we don't expose that
        if (kind == ElementKind.RECORD) {
            return Optional.empty();
        }

        javax.lang.model.type.TypeMirror superclass = typeElement.getSuperclass();
        if (superclass == null || superclass.getKind() == TypeKind.NONE) {
            return Optional.empty();
        }

        // Convert to TypeRef
        TypeRef superTypeRef = typeResolver.resolve(superclass);

        // Exclude java.lang.Object
        if ("java.lang.Object".equals(superTypeRef.name().value())) {
            return Optional.empty();
        }

        return Optional.of(superTypeRef);
    }

    /**
     * Extracts the interfaces implemented/extended by a type element.
     *
     * @param typeElement type element (not {@code null})
     * @return list of interfaces (never {@code null}, may be empty)
     */
    private List<TypeRef> extractInterfaces(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        List<? extends javax.lang.model.type.TypeMirror> interfaceMirrors = typeElement.getInterfaces();
        if (interfaceMirrors.isEmpty()) {
            return List.of();
        }

        List<TypeRef> interfaces = new ArrayList<>(interfaceMirrors.size());
        for (javax.lang.model.type.TypeMirror interfaceMirror : interfaceMirrors) {
            TypeRef interfaceRef = typeResolver.resolve(interfaceMirror);
            interfaces.add(interfaceRef);
        }

        return List.copyOf(interfaces);
    }

    /**
     * Extracts permitted subtypes for sealed types (Java 17+).
     *
     * <p>Returns empty if:
     * <ul>
     *   <li>The type is not sealed</li>
     *   <li>The JDK version doesn't support sealed types</li>
     * </ul>
     *
     * @param typeElement type element (not {@code null})
     * @return permitted subtypes or empty if not sealed
     */
    private Optional<List<TypeRef>> extractPermittedSubtypes(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        // Use reflection to check for sealed types (Java 17+)
        // This is safe because if the method doesn't exist, we just return empty
        try {
            // Check if TypeElement has getPermittedSubclasses() method (Java 17+)
            java.lang.reflect.Method method = TypeElement.class.getMethod("getPermittedSubclasses");
            @SuppressWarnings("unchecked")
            List<? extends javax.lang.model.type.TypeMirror> permittedMirrors =
                    (List<? extends javax.lang.model.type.TypeMirror>) method.invoke(typeElement);

            if (permittedMirrors == null || permittedMirrors.isEmpty()) {
                return Optional.empty();
            }

            List<TypeRef> permittedRefs = new ArrayList<>(permittedMirrors.size());
            for (javax.lang.model.type.TypeMirror permittedMirror : permittedMirrors) {
                TypeRef permittedRef = typeResolver.resolve(permittedMirror);
                permittedRefs.add(permittedRef);
            }

            return Optional.of(List.copyOf(permittedRefs));
        } catch (NoSuchMethodException | IllegalAccessException | java.lang.reflect.InvocationTargetException e) {
            // Java < 17 or method not available, return empty
            return Optional.empty();
        }
    }

    /**
     * Extracts enum constants from an enumeration type.
     *
     * <p>Returns empty if the type is not an enum. Enum constants are returned
     * in the order they are declared in the source code.</p>
     *
     * @param typeElement type element (not {@code null})
     * @return enum constants or empty if not an enum
     */
    private Optional<List<String>> extractEnumConstants(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        // Only extract constants if this is an enum
        if (typeElement.getKind() != ElementKind.ENUM) {
            return Optional.empty();
        }

        // Get all enclosed elements and filter for enum constants
        List<String> constants = typeElement.getEnclosedElements().stream()
                .filter(element -> element.getKind() == ElementKind.ENUM_CONSTANT)
                .map(element -> element.getSimpleName().toString())
                .toList();

        if (constants.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(List.copyOf(constants));
    }

    /**
     * Extracts package name from qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return package name, or empty string if in default package
     */
    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? "" : qualifiedName.substring(0, lastDot);
    }

    public Optional<DomainProperty> findIdentityProperty(List<DomainProperty> properties) {
        Objects.requireNonNull(properties, "properties");
        return properties.stream().filter(DomainProperty::isIdentity).findFirst();
    }

    public DomainId buildDomainId(DomainProperty identityProperty, String declaringEntity) {
        Objects.requireNonNull(identityProperty, "identityProperty");
        Objects.requireNonNull(declaringEntity, "declaringEntity");

        // SourceRef is always present in modern code (set during property extraction)
        // If absent, create a synthetic SourceRef for robustness
        SourceRef idRef = identityProperty.sourceRef().orElseGet(() -> {
            String stableId = declaringEntity + "#" + identityProperty.name();
            return SourceRef.builder(SourceRef.Kind.FIELD, stableId)
                    .origin("synthetic-property")
                    .build();
        });

        return DomainId.builder()
                .declaringEntity(declaringEntity)
                .name(identityProperty.name())
                .type(identityProperty.type())
                .composite(false)
                .sourceRef(idRef)
                .build();
    }
}
