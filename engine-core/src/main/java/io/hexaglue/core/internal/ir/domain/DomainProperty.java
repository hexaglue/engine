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
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a domain property or field.
 *
 * <p>
 * A domain property represents a semantic field or accessor of a domain type. It captures
 * the property's name, type, immutability, and role within the domain model.
 * </p>
 *
 * <h2>Property Sources</h2>
 * <p>
 * A property may correspond to:
 * </p>
 * <ul>
 *   <li><strong>Record component:</strong> Field in a Java record</li>
 *   <li><strong>Field with accessor:</strong> Private field with getter/setter</li>
 *   <li><strong>Accessor method:</strong> Property-style getter (JavaBean convention)</li>
 *   <li><strong>Synthetic:</strong> Derived property inferred by analyzers</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Semantic Focus:</strong> Captures business intent, not implementation details</li>
 *   <li><strong>Immutability:</strong> Once built, the property is immutable</li>
 *   <li><strong>Simplicity:</strong> Minimal but complete information for generation</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Property extractors analyze source elements</li>
 *   <li>{@link DomainProperty} is built using the builder pattern</li>
 *   <li>Property is added to its parent {@link DomainType}</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.domain.DomainPropertyView}) are created as adapters</li>
 * </ol>
 *
 * <h2>Source Mapping</h2>
 * <p>
 * This model stores a stable {@link SourceRef} instead of raw JSR-269 elements.
 * This supports deep equality checks, stable snapshots, and future incremental compilation.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 */
@InternalMarker(
        reason = "Internal domain property representation; plugins use io.hexaglue.spi.ir.domain.DomainPropertyView")
public final class DomainProperty {

    private final String name;
    private final TypeRef type;
    private final boolean identity;
    private final boolean immutable;
    private final String declaringType;
    private final String description;
    private final SourceRef sourceRef;
    private final List<AnnotationModel> annotations;
    private final RelationshipMetadata relationshipMetadata;

    private DomainProperty(
            String name,
            TypeRef type,
            boolean identity,
            boolean immutable,
            String declaringType,
            String description,
            SourceRef sourceRef,
            List<AnnotationModel> annotations,
            RelationshipMetadata relationshipMetadata) {
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.identity = identity;
        this.immutable = immutable;
        this.declaringType = declaringType;
        this.description = description;
        this.sourceRef = sourceRef;
        this.annotations = annotations != null ? List.copyOf(annotations) : List.of();
        this.relationshipMetadata = relationshipMetadata;
    }

    public String name() {
        return name;
    }

    public TypeRef type() {
        return type;
    }

    public boolean isIdentity() {
        return identity;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public Optional<String> declaringType() {
        return Optional.ofNullable(declaringType);
    }

    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the stable {@link SourceRef} identifying where this property was found in source code.
     *
     * @return source reference if available
     */
    public Optional<SourceRef> sourceRef() {
        return Optional.ofNullable(sourceRef);
    }

    /**
     * Returns annotations present on this property.
     *
     * @return immutable list of annotations (never {@code null}, may be empty)
     */
    public List<AnnotationModel> annotations() {
        return annotations;
    }

    /**
     * Returns relationship metadata if this property represents a relationship to another domain type.
     *
     * <p>This metadata is populated during the semantic enrichment phase by analyzing
     * jMolecules annotations, YAML configuration, and heuristics.</p>
     *
     * @return relationship metadata if present
     * @since 0.4.0
     */
    public Optional<RelationshipMetadata> relationship() {
        return Optional.ofNullable(relationshipMetadata);
    }

    @Override
    public String toString() {
        return "DomainProperty{" + name + ": " + type + ", identity=" + identity + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private TypeRef type;
        private boolean identity;
        private boolean immutable;
        private String declaringType;
        private String description;
        private SourceRef sourceRef;
        private List<AnnotationModel> annotations;
        private RelationshipMetadata relationshipMetadata;

        private Builder() {
            // package-private
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(TypeRef type) {
            this.type = type;
            return this;
        }

        public Builder identity(boolean identity) {
            this.identity = identity;
            return this;
        }

        public Builder immutable(boolean immutable) {
            this.immutable = immutable;
            return this;
        }

        public Builder declaringType(String declaringType) {
            this.declaringType = declaringType;
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
         * Sets the annotations for this property.
         *
         * @param annotations annotations (nullable)
         * @return this builder
         */
        public Builder annotations(List<AnnotationModel> annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Sets the relationship metadata for this property.
         *
         * <p>This is populated during semantic enrichment phase.</p>
         *
         * @param relationshipMetadata relationship metadata (nullable)
         * @return this builder
         * @since 0.4.0
         */
        public Builder relationshipMetadata(RelationshipMetadata relationshipMetadata) {
            this.relationshipMetadata = relationshipMetadata;
            return this;
        }

        public DomainProperty build() {
            return new DomainProperty(
                    name,
                    type,
                    identity,
                    immutable,
                    declaringType,
                    description,
                    sourceRef,
                    annotations,
                    relationshipMetadata);
        }
    }
}
