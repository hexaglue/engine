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
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a domain service.
 *
 * <p>
 * A domain service encapsulates pure business rules that don't naturally belong to a single
 * entity or value object. Domain services express complex domain logic involving multiple
 * entities, calculations, or cross-cutting concerns.
 * </p>
 *
 * <h2>Domain Service Characteristics</h2>
 * <ul>
 *   <li><strong>Stateless:</strong> No internal state beyond method parameters</li>
 *   <li><strong>Pure Business Logic:</strong> No technical concerns (no persistence, no I/O)</li>
 *   <li><strong>Domain Language:</strong> Operations named using ubiquitous language</li>
 *   <li><strong>Entity Collaborators:</strong> Works with entities and value objects</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li><strong>PricingService:</strong> Complex pricing calculations across products</li>
 *   <li><strong>EligibilityChecker:</strong> Multi-criteria business rules</li>
 *   <li><strong>AllocationEngine:</strong> Resource allocation logic</li>
 * </ul>
 *
 * <h2>HexaGlue's Role</h2>
 * <p>
 * HexaGlue <strong>analyzes but never generates</strong> domain services. They are discovered for:
 * </p>
 * <ul>
 *   <li>Diagnostics and validation</li>
 *   <li>Documentation generation</li>
 *   <li>Dependency analysis</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Minimal Representation:</strong> Capture only what's needed for analysis</li>
 *   <li><strong>Immutability:</strong> Once built, the service is immutable</li>
 *   <li><strong>Non-Invasive:</strong> No modification to domain service code</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Domain service extractors discover service types</li>
 *   <li>{@link DomainService} is built using the builder pattern</li>
 *   <li>Service is added to {@link DomainModel}</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.domain.DomainServiceView}) are created as adapters</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainService pricingService = DomainService.builder()
 *     .qualifiedName("com.example.pricing.PricingService")
 *     .simpleName("PricingService")
 *     .description("Calculates prices with discounts and taxes")
 *     .build();
 * }</pre>
 */
@InternalMarker(
        reason = "Internal domain service representation; plugins use io.hexaglue.spi.ir.domain.DomainServiceView")
public final class DomainService {

    private final String qualifiedName;
    private final String simpleName;
    private final String description;

    /**
     * Stable source reference.
     *
     * <p>
     * This replaces the previous {@code Object sourceElement} placeholder which could contain a
     * {@code javax.lang.model.element.Element}. Storing raw elements is intentionally avoided.
     * </p>
     */
    private final SourceRef sourceRef;

    /**
     * Creates a domain service with the given attributes.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @param simpleName    simple name (not {@code null})
     * @param description   optional description (nullable)
     * @param sourceRef     stable source reference (nullable)
     */
    private DomainService(String qualifiedName, String simpleName, String description, SourceRef sourceRef) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "qualifiedName");
        this.simpleName = Objects.requireNonNull(simpleName, "simpleName");
        this.description = description;
        this.sourceRef = sourceRef;
    }

    /**
     * Returns the qualified name of this domain service.
     *
     * @return qualified name (never {@code null})
     */
    public String qualifiedName() {
        return qualifiedName;
    }

    /**
     * Returns the simple name of this domain service.
     *
     * @return simple name (never {@code null})
     */
    public String simpleName() {
        return simpleName;
    }

    /**
     * Returns the optional user-facing description.
     *
     * @return description if present
     */
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the stable source reference.
     *
     * @return source reference if available
     */
    public Optional<SourceRef> sourceRef() {
        return Optional.ofNullable(sourceRef);
    }

    @Override
    public String toString() {
        return "DomainService{" + qualifiedName + "}";
    }

    /**
     * Creates a builder for constructing {@link DomainService} instances.
     *
     * @return builder (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link DomainService}.
     *
     * <p>
     * This builder is not thread-safe and should be used from a single thread only.
     * </p>
     */
    public static final class Builder {
        private String qualifiedName;
        private String simpleName;
        private String description;
        private SourceRef sourceRef;

        private Builder() {
            // package-private
        }

        /**
         * Sets the qualified name.
         *
         * @param qualifiedName qualified name (not {@code null})
         * @return this builder
         */
        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            return this;
        }

        /**
         * Sets the simple name.
         *
         * @param simpleName simple name (not {@code null})
         * @return this builder
         */
        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description description (nullable)
         * @return this builder
         */
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
         * Builds the domain service.
         *
         * @return immutable domain service (never {@code null})
         * @throws NullPointerException if required fields are null
         */
        public DomainService build() {
            return new DomainService(qualifiedName, simpleName, description, sourceRef);
        }
    }
}
