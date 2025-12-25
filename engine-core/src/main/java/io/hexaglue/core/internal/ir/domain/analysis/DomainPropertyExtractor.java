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

import io.hexaglue.core.frontend.AnnotationIntrospector;
import io.hexaglue.core.frontend.AnnotationModel;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.SourceRef;
import io.hexaglue.core.internal.ir.SourceRefs;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.RecordComponentElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

/**
 * Extracts domain properties from analyzed type elements.
 *
 * <p>
 * This extractor identifies and constructs {@link DomainProperty} instances from various
 * source representations including record components, fields with accessors, and accessor
 * methods following JavaBean conventions.
 * </p>
 *
 * <h2>Extraction Strategy</h2>
 * <p>
 * The extractor recognizes properties from:
 * </p>
 * <ul>
 *   <li><strong>Record components:</strong> All components of a Java {@code record}</li>
 *   <li><strong>Fields with getters:</strong> Private fields with public getter methods</li>
 *   <li><strong>JavaBean properties:</strong> Getter/setter pairs without explicit fields</li>
 *   <li><strong>Public fields:</strong> Direct public fields (discouraged but supported)</li>
 * </ul>
 *
 * <h2>Property Characteristics</h2>
 * <p>
 * For each property, the extractor determines:
 * </p>
 * <ul>
 *   <li><strong>Name:</strong> Stable property name (camelCase)</li>
 *   <li><strong>Type:</strong> Java type reference</li>
 *   <li><strong>Immutability:</strong> Whether the property has a setter</li>
 *   <li><strong>Identity:</strong> Whether the property is part of entity identity</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Flexibility:</strong> Support various property declaration styles</li>
 *   <li><strong>Consistency:</strong> Normalize to a common property model</li>
 *   <li><strong>Completeness:</strong> Extract all semantically relevant properties</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 */
@InternalMarker(reason = "Internal domain analysis; not exposed to plugins")
public final class DomainPropertyExtractor {

    private final TypeResolver typeResolver;
    private final IdentityAnnotationDetector identityAnnotationDetector;

    public DomainPropertyExtractor(TypeResolver typeResolver) {
        this.typeResolver = Objects.requireNonNull(typeResolver, "typeResolver");
        this.identityAnnotationDetector = new IdentityAnnotationDetector();
    }

    public List<DomainProperty> extractProperties(Object typeElement, String declaringType, boolean isRecord) {
        Objects.requireNonNull(typeElement, "typeElement");
        Objects.requireNonNull(declaringType, "declaringType");

        if (!(typeElement instanceof TypeElement te)) {
            throw new IllegalArgumentException("typeElement must be a TypeElement");
        }

        if (isRecord) {
            return extractRecordProperties(te, declaringType);
        } else {
            return extractClassProperties(te, declaringType);
        }
    }

    private List<DomainProperty> extractRecordProperties(TypeElement typeElement, String declaringType) {
        List<DomainProperty> properties = new ArrayList<>();

        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement instanceof RecordComponentElement recordComponent) {
                String name = recordComponent.getSimpleName().toString();
                TypeRef type = typeResolver.resolve(recordComponent.asType());

                // Check @Identity annotation first, then naming convention
                boolean hasIdentityAnnotation = false;
                // Record components don't have @Identity directly, check the field
                Element accessor = recordComponent.getAccessor();
                if (accessor instanceof ExecutableElement) {
                    // For records, check if there's a field with @Identity
                    hasIdentityAnnotation = typeElement.getEnclosedElements().stream()
                            .filter(e -> e.getKind() == ElementKind.FIELD)
                            .filter(e -> e.getSimpleName().toString().equals(name))
                            .filter(e -> e instanceof VariableElement)
                            .map(e -> (VariableElement) e)
                            .anyMatch(identityAnnotationDetector::hasIdentityAnnotation);
                }
                boolean identity = hasIdentityAnnotation || looksLikeIdentity(name);

                SourceRef ref = sourceRefForProperty(declaringType, name, recordComponent, "record component");

                // Extract annotations from record component
                List<AnnotationModel> annotations = AnnotationIntrospector.getAnnotations(recordComponent);

                DomainProperty property = DomainProperty.builder()
                        .name(name)
                        .type(type)
                        .declaringType(declaringType)
                        .identity(identity)
                        .immutable(true)
                        .sourceRef(ref)
                        .annotations(annotations)
                        .build();

                properties.add(property);
            }
        }

        return properties;
    }

    private List<DomainProperty> extractClassProperties(TypeElement typeElement, String declaringType) {
        Map<String, PropertyDescriptor> descriptors = new LinkedHashMap<>();

        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) element;
                String fieldName = field.getSimpleName().toString();

                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                TypeRef fieldType = typeResolver.resolve(field.asType());
                descriptors.computeIfAbsent(fieldName, k -> new PropertyDescriptor()).field = field;
                descriptors.get(fieldName).type = fieldType;
            }
        }

        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;
                String methodName = method.getSimpleName().toString();

                if (method.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                if (isGetter(method)) {
                    String propertyName = normalizeAccessorName(methodName);
                    TypeRef returnType = typeResolver.resolve(method.getReturnType());

                    PropertyDescriptor descriptor =
                            descriptors.computeIfAbsent(propertyName, k -> new PropertyDescriptor());
                    descriptor.getter = method;
                    if (descriptor.type == null) {
                        descriptor.type = returnType;
                    }
                }

                if (isSetter(method)) {
                    String propertyName = normalizeAccessorName(methodName);
                    descriptors.computeIfAbsent(propertyName, k -> new PropertyDescriptor()).setter = method;
                }
            }
        }

        List<DomainProperty> properties = new ArrayList<>();
        for (Map.Entry<String, PropertyDescriptor> entry : descriptors.entrySet()) {
            String propertyName = entry.getKey();
            PropertyDescriptor descriptor = entry.getValue();

            if (descriptor.type != null) {
                // Check @Identity annotation first, then naming convention
                boolean hasIdentityAnnotation = descriptor.field != null
                        && descriptor.field instanceof VariableElement
                        && identityAnnotationDetector.hasIdentityAnnotation((VariableElement) descriptor.field);
                boolean identity = hasIdentityAnnotation || looksLikeIdentity(propertyName);
                boolean immutable = descriptor.setter == null;

                Element origin = descriptor.field != null ? descriptor.field : descriptor.getter;
                String hint = descriptor.field != null ? "field" : "getter";
                SourceRef ref = sourceRefForProperty(declaringType, propertyName, origin, hint);

                // Extract annotations from field or getter (prefer field if both exist)
                List<AnnotationModel> annotations;
                if (descriptor.field != null) {
                    annotations = AnnotationIntrospector.getAnnotations(descriptor.field);
                } else if (descriptor.getter != null) {
                    annotations = AnnotationIntrospector.getAnnotations(descriptor.getter);
                } else {
                    annotations = List.of();
                }

                DomainProperty property = DomainProperty.builder()
                        .name(propertyName)
                        .type(descriptor.type)
                        .declaringType(declaringType)
                        .identity(identity)
                        .immutable(immutable)
                        .sourceRef(ref)
                        .annotations(annotations)
                        .build();

                properties.add(property);
            }
        }

        return properties;
    }

    private boolean isGetter(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        boolean hasNoParams = method.getParameters().isEmpty();
        boolean hasReturnType = method.getReturnType().getKind() != javax.lang.model.type.TypeKind.VOID;

        return hasNoParams
                && hasReturnType
                && (name.startsWith("get") && name.length() > 3 || name.startsWith("is") && name.length() > 2);
    }

    private boolean isSetter(ExecutableElement method) {
        String name = method.getSimpleName().toString();
        boolean hasOneParam = method.getParameters().size() == 1;
        boolean returnsVoid = method.getReturnType().getKind() == javax.lang.model.type.TypeKind.VOID;

        return name.startsWith("set") && name.length() > 3 && hasOneParam && returnsVoid;
    }

    private static final class PropertyDescriptor {
        VariableElement field;
        ExecutableElement getter;
        ExecutableElement setter;
        TypeRef type;
    }

    public DomainProperty extractProperty(
            String name,
            TypeRef type,
            String declaringType,
            boolean identity,
            boolean immutable,
            String description,
            Object sourceElement) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(declaringType, "declaringType");

        SourceRef ref = SourceRefs.coerce(sourceElement, declaringType + "#" + name, SourceRef.Kind.FIELD);

        // Extract annotations if source element is an Element
        List<AnnotationModel> annotations = List.of();
        if (sourceElement instanceof Element element) {
            annotations = AnnotationIntrospector.getAnnotations(element);
        }

        return DomainProperty.builder()
                .name(name)
                .type(type)
                .declaringType(declaringType)
                .identity(identity)
                .immutable(immutable)
                .description(description)
                .sourceRef(ref)
                .annotations(annotations)
                .build();
    }

    public boolean looksLikeIdentity(String propertyName) {
        Objects.requireNonNull(propertyName, "propertyName");

        String lower = propertyName.toLowerCase();
        return "id".equals(lower) || "identifier".equals(lower) || lower.endsWith("id");
    }

    public String normalizeAccessorName(String methodName) {
        Objects.requireNonNull(methodName, "methodName");

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }
        if (methodName.startsWith("is") && methodName.length() > 2) {
            return decapitalize(methodName.substring(2));
        }
        if (methodName.startsWith("set") && methodName.length() > 3) {
            return decapitalize(methodName.substring(3));
        }

        throw new IllegalArgumentException("Not a valid accessor method name: " + methodName);
    }

    private String decapitalize(String name) {
        if (name.isEmpty()) {
            return name;
        }

        if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
            return name;
        }

        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private static SourceRef sourceRefForProperty(
            String declaringType, String propertyName, Element origin, String hint) {
        String qn = declaringType + "#" + propertyName;
        // NOTE: best-effort; no file/line/column yet (can be added later via ProcessingEnvironment).
        return SourceRef.builder(SourceRef.Kind.FIELD, qn)
                .origin("jsr269")
                .hint(hint)
                .build();
    }
}
