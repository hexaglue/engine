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
package io.hexaglue.core.internal.ir.domain;

import io.hexaglue.core.frontend.AnnotationModel;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.SourceRef;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a domain type (entity, value object, aggregate, etc.).
 *
 * <p>
 * A domain type represents a semantic business concept discovered during source analysis.
 * It captures the structure, properties, identity, and behavioral intent of the type.
 * </p>
 *
 * <h2>Domain Type Categories</h2>
 * <ul>
 *   <li><strong>Entity:</strong> Type with stable identity (e.g., Customer, Order)</li>
 *   <li><strong>Value Object:</strong> Immutable type without identity (e.g., Money, Address)</li>
 *   <li><strong>Aggregate Root:</strong> Entity that defines consistency boundary</li>
 *   <li><strong>Identifier:</strong> Wrapper type for entity IDs (e.g., CustomerId)</li>
 *   <li><strong>Enum:</strong> Enumerated values</li>
 *   <li><strong>Record:</strong> Immutable data carrier (Java record)</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Richness:</strong> Capture all structural and semantic information</li>
 *   <li><strong>Immutability:</strong> Once built, the type is immutable</li>
 *   <li><strong>Completeness:</strong> Support all generation needs (mappers, DTOs, persistence)</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Source elements are analyzed by domain extractors</li>
 *   <li>{@link DomainType} is built using the builder pattern</li>
 *   <li>Type is added to {@link DomainModel}</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.domain.DomainTypeView}) are created as adapters</li>
 *   <li>Plugins query via SPI during generation</li>
 * </ol>
 *
 * <h2>Source Mapping</h2>
 * <p>
 * This model intentionally does not store JSR-269 {@code Element} instances directly because they
 * are not suitable for long-lived storage, deep equality, or incremental compilation checks.
 * Instead, it stores a stable {@link SourceRef} built by the analysis frontend.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainType customerType = DomainType.builder()
 *     .qualifiedName("com.example.Customer")
 *     .simpleName("Customer")
 *     .kind(DomainTypeKind.ENTITY)
 *     .type(customerTypeRef)
 *     .addProperty(nameProperty)
 *     .addProperty(emailProperty)
 *     .id(customerId)
 *     .immutable(false)
 *     .sourceRef(SourceRef.builder(SourceRef.Kind.TYPE, "com.example.Customer").origin("jsr269").build())
 *     .build();
 * }</pre>
 */
@InternalMarker(reason = "Internal domain type representation; plugins use io.hexaglue.spi.ir.domain.DomainTypeView")
public final class DomainType {

    private final String qualifiedName;
    private final String simpleName;
    private final DomainTypeKind kind;
    private final TypeRef type;
    private final List<DomainProperty> properties;
    private final DomainId id;
    private final boolean immutable;
    private final String description;

    /**
     * Stable source reference for diagnostics and debug.
     *
     * <p>
     * This replaces the previous {@code Object sourceElement} placeholder which could contain a
     * {@code javax.lang.model.element.Element}. Storing raw elements is intentionally avoided.
     * </p>
     */
    private final SourceRef sourceRef;

    /**
     * Annotations present on this domain type.
     */
    private final List<AnnotationModel> annotations;

    /**
     * Direct supertype (superclass), if any.
     * {@code null} for types without explicit superclass (java.lang.Object is excluded).
     */
    private final TypeRef superType;

    /**
     * Interfaces implemented/extended by this type.
     * Never {@code null}, but may be empty.
     */
    private final List<TypeRef> interfaces;

    /**
     * Permitted subtypes for sealed types.
     * {@code null} if this type is not sealed.
     */
    private final List<TypeRef> permittedSubtypes;

    /**
     * Enum constants, if this type is an enumeration.
     * {@code null} if this type is not an enum.
     */
    private final List<String> enumConstants;

    /**
     * Creates a domain type with the given attributes.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @param simpleName    simple name (not {@code null})
     * @param kind          domain type kind (not {@code null})
     * @param type          Java type reference (not {@code null})
     * @param properties    domain properties (not {@code null})
     * @param id            identity definition (nullable)
     * @param immutable     immutability flag
     * @param description   optional description (nullable)
     * @param sourceRef     stable source reference (nullable)
     * @param annotations   annotations (nullable)
     * @param superType     direct supertype (nullable)
     * @param interfaces    implemented interfaces (nullable)
     * @param permittedSubtypes permitted subtypes for sealed types (nullable)
     * @param enumConstants enum constants (nullable)
     */
    private DomainType(
            String qualifiedName,
            String simpleName,
            DomainTypeKind kind,
            TypeRef type,
            List<DomainProperty> properties,
            DomainId id,
            boolean immutable,
            String description,
            SourceRef sourceRef,
            List<AnnotationModel> annotations,
            TypeRef superType,
            List<TypeRef> interfaces,
            List<TypeRef> permittedSubtypes,
            List<String> enumConstants) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "qualifiedName");
        this.simpleName = Objects.requireNonNull(simpleName, "simpleName");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.type = Objects.requireNonNull(type, "type");
        this.properties = Collections.unmodifiableList(new ArrayList<>(properties));
        this.id = id;
        this.immutable = immutable;
        this.description = description;
        this.sourceRef = sourceRef;
        this.annotations = annotations != null ? List.copyOf(annotations) : List.of();
        this.superType = superType;
        this.interfaces = interfaces != null ? List.copyOf(interfaces) : List.of();
        this.permittedSubtypes = permittedSubtypes != null ? List.copyOf(permittedSubtypes) : null;
        this.enumConstants = enumConstants != null ? List.copyOf(enumConstants) : null;
    }

    public String qualifiedName() {
        return qualifiedName;
    }

    public String simpleName() {
        return simpleName;
    }

    public DomainTypeKind kind() {
        return kind;
    }

    public TypeRef type() {
        return type;
    }

    public List<DomainProperty> properties() {
        return properties;
    }

    public Optional<DomainId> id() {
        return Optional.ofNullable(id);
    }

    public boolean isImmutable() {
        return immutable;
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the stable {@link SourceRef} that identifies the analyzed source location.
     *
     * <p>
     * This reference is designed for long-lived storage, deep equality checks, and diagnostics.
     * It is best-effort and may omit path/line/column depending on the frontend capabilities.
     * </p>
     *
     * @return source reference if available
     */
    public Optional<SourceRef> sourceRef() {
        return Optional.ofNullable(sourceRef);
    }

    /**
     * Returns annotations present on this domain type.
     *
     * @return immutable list of annotations (never {@code null}, may be empty)
     */
    public List<AnnotationModel> annotations() {
        return annotations;
    }

    /**
     * Returns the direct supertype (superclass), if any.
     *
     * @return supertype or empty (java.lang.Object is excluded)
     */
    public Optional<TypeRef> superType() {
        return Optional.ofNullable(superType);
    }

    /**
     * Returns the interfaces implemented/extended by this type.
     *
     * @return immutable list of interfaces (never {@code null}, may be empty)
     */
    public List<TypeRef> interfaces() {
        return interfaces;
    }

    /**
     * If this type is sealed, returns the permitted subtypes.
     *
     * @return permitted subtypes or empty if not sealed
     */
    public Optional<List<TypeRef>> permittedSubtypes() {
        return Optional.ofNullable(permittedSubtypes);
    }

    /**
     * If this type is an enumeration, returns the enum constants.
     *
     * @return enum constants or empty if not an enum
     */
    public Optional<List<String>> enumConstants() {
        return Optional.ofNullable(enumConstants);
    }

    @Override
    public String toString() {
        return "DomainType{" + qualifiedName + ", kind=" + kind + ", properties=" + properties.size() + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String qualifiedName;
        private String simpleName;
        private DomainTypeKind kind;
        private TypeRef type;
        private final List<DomainProperty> properties = new ArrayList<>();
        private DomainId id;
        private boolean immutable;
        private String description;
        private SourceRef sourceRef;
        private List<AnnotationModel> annotations;
        private TypeRef superType;
        private List<TypeRef> interfaces;
        private List<TypeRef> permittedSubtypes;
        private List<String> enumConstants;

        private Builder() {
            // package-private
        }

        /**
         * Initializes this builder from an existing domain type.
         *
         * <p>All fields from the source type are copied. Individual fields can then
         * be overridden using the setter methods.</p>
         *
         * @param source source domain type (not {@code null})
         * @return this builder
         * @throws NullPointerException if source is null
         */
        public Builder from(DomainType source) {
            Objects.requireNonNull(source, "source");
            this.qualifiedName = source.qualifiedName;
            this.simpleName = source.simpleName;
            this.kind = source.kind;
            this.type = source.type;
            this.properties.clear();
            this.properties.addAll(source.properties);
            this.id = source.id;
            this.immutable = source.immutable;
            this.description = source.description;
            this.sourceRef = source.sourceRef;
            this.annotations = source.annotations;
            this.superType = source.superType;
            this.interfaces = source.interfaces;
            this.permittedSubtypes = source.permittedSubtypes;
            this.enumConstants = source.enumConstants;
            return this;
        }

        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder kind(DomainTypeKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder type(TypeRef type) {
            this.type = type;
            return this;
        }

        public Builder addProperty(DomainProperty property) {
            Objects.requireNonNull(property, "property");
            properties.add(property);
            return this;
        }

        public Builder addProperties(List<DomainProperty> properties) {
            Objects.requireNonNull(properties, "properties");
            for (DomainProperty property : properties) {
                addProperty(property);
            }
            return this;
        }

        public Builder id(DomainId id) {
            this.id = id;
            return this;
        }

        public Builder immutable(boolean immutable) {
            this.immutable = immutable;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Sets the stable source reference.
         *
         * @param sourceRef stable source reference (nullable)
         * @return this builder
         */
        public Builder sourceRef(SourceRef sourceRef) {
            this.sourceRef = sourceRef;
            return this;
        }

        /**
         * Sets the annotations for this domain type.
         *
         * @param annotations annotations (nullable)
         * @return this builder
         */
        public Builder annotations(List<AnnotationModel> annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Sets the direct supertype (superclass).
         *
         * @param superType direct supertype (nullable)
         * @return this builder
         */
        public Builder superType(TypeRef superType) {
            this.superType = superType;
            return this;
        }

        /**
         * Sets the interfaces implemented/extended by this type.
         *
         * @param interfaces interfaces (nullable)
         * @return this builder
         */
        public Builder interfaces(List<TypeRef> interfaces) {
            this.interfaces = interfaces;
            return this;
        }

        /**
         * Sets the permitted subtypes for sealed types.
         *
         * @param permittedSubtypes permitted subtypes (nullable)
         * @return this builder
         */
        public Builder permittedSubtypes(List<TypeRef> permittedSubtypes) {
            this.permittedSubtypes = permittedSubtypes;
            return this;
        }

        /**
         * Sets the enum constants for enumeration types.
         *
         * @param enumConstants enum constants (nullable)
         * @return this builder
         */
        public Builder enumConstants(List<String> enumConstants) {
            this.enumConstants = enumConstants;
            return this;
        }

        public DomainType build() {
            return new DomainType(
                    qualifiedName,
                    simpleName,
                    kind,
                    type,
                    properties,
                    id,
                    immutable,
                    description,
                    sourceRef,
                    annotations,
                    superType,
                    interfaces,
                    permittedSubtypes,
                    enumConstants);
        }
    }
}
