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

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.SourceRef;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a domain entity identity.
 *
 * <p>
 * A domain identity represents how an entity is uniquely identified within its aggregate.
 * This abstraction allows HexaGlue to support various identity patterns without assuming
 * a specific implementation strategy.
 * </p>
 *
 * <h2>Identity Patterns</h2>
 * <ul>
 *   <li><strong>Dedicated ID type:</strong> Recommended pattern using a type-safe wrapper (e.g., {@code CustomerId})</li>
 *   <li><strong>Scalar ID:</strong> Simple primitive or string ID (less type-safe but common)</li>
 *   <li><strong>Composite ID:</strong> Multi-part identity (e.g., {@code OrderId + LineNumber})</li>
 *   <li><strong>Natural ID:</strong> Business-meaningful identifier (e.g., email, ISBN)</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Flexibility:</strong> Support various identity strategies</li>
 *   <li><strong>Type Safety:</strong> Encourage dedicated ID types</li>
 *   <li><strong>Immutability:</strong> IDs should never change once assigned</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>ID extractors analyze entity definitions</li>
 *   <li>{@link DomainId} is built using the builder pattern</li>
 *   <li>ID is associated with its parent {@link DomainType}</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.domain.DomainIdView}) are created as adapters</li>
 * </ol>
 *
 * <h2>Source Mapping</h2>
 * <p>
 * This model stores a stable {@link SourceRef} instead of a raw JSR-269 {@code Element}.
 * Storing raw compiler elements is intentionally avoided because they are not designed for
 * long-lived storage, deep equality checks, or future incremental compilation.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainId customerId = DomainId.builder()
 *     .declaringEntity("com.example.Customer")
 *     .name("id")
 *     .type(customerIdTypeRef)
 *     .composite(false)
 *     .sourceRef(SourceRef.builder(SourceRef.Kind.FIELD, "com.example.Customer#id").origin("jsr269").build())
 *     .build();
 * }</pre>
 */
@InternalMarker(reason = "Internal domain ID representation; plugins use io.hexaglue.spi.ir.domain.DomainIdView")
public final class DomainId {

    private final String declaringEntity;
    private final String name;
    private final TypeRef type;
    private final boolean composite;
    private final SourceRef sourceRef;

    /**
     * Creates a domain ID with the given attributes.
     *
     * @param declaringEntity qualified name of declaring entity (nullable)
     * @param name            logical ID name (not {@code null})
     * @param type            ID type reference (not {@code null})
     * @param composite       whether composite ID
     * @param sourceRef       stable source reference (nullable)
     */
    private DomainId(String declaringEntity, String name, TypeRef type, boolean composite, SourceRef sourceRef) {
        this.declaringEntity = declaringEntity;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.composite = composite;
        this.sourceRef = sourceRef;
    }

    public Optional<String> declaringEntity() {
        return Optional.ofNullable(declaringEntity);
    }

    public String name() {
        return name;
    }

    public TypeRef type() {
        return type;
    }

    public boolean isComposite() {
        return composite;
    }

    /**
     * Returns the stable {@link SourceRef} that identifies where this ID was discovered.
     *
     * @return source reference if available
     */
    public Optional<SourceRef> sourceRef() {
        return Optional.ofNullable(sourceRef);
    }

    @Override
    public String toString() {
        return "DomainId{" + name + ": " + type + ", composite=" + composite + "}";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String declaringEntity;
        private String name;
        private TypeRef type;
        private boolean composite;
        private SourceRef sourceRef;

        private Builder() {
            // package-private
        }

        public Builder declaringEntity(String declaringEntity) {
            this.declaringEntity = declaringEntity;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(TypeRef type) {
            this.type = type;
            return this;
        }

        public Builder composite(boolean composite) {
            this.composite = composite;
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

        public DomainId build() {
            return new DomainId(declaringEntity, name, type, composite, sourceRef);
        }
    }
}
