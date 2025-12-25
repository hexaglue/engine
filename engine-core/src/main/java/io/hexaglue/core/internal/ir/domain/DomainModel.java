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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of the complete domain model.
 *
 * <p>
 * The domain model is the central container for all analyzed domain types, domain services,
 * and their relationships. It is built during the analysis phase and serves as the foundation
 * for validation, code generation, and SPI view construction.
 * </p>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li><strong>Domain Types:</strong> Entities, value objects, aggregates, enums, records</li>
 *   <li><strong>Domain Services:</strong> Pure business rules that don't belong to a single entity</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Captures all domain knowledge discovered during analysis</li>
 *   <li><strong>Immutability:</strong> Once built, the model is immutable</li>
 *   <li><strong>Queryable:</strong> Provides efficient lookup by qualified name</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Domain analyzers discover and extract domain types from source code</li>
 *   <li>{@link DomainModel} is built using the builder pattern</li>
 *   <li>Model is validated for consistency</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.domain.DomainModelView}) are created as adapters</li>
 *   <li>Plugins query the model via SPI during generation</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainModel model = DomainModel.builder()
 *     .addType(customerType)
 *     .addType(orderType)
 *     .addService(pricingService)
 *     .build();
 *
 * Optional<DomainType> customer = model.findType("com.example.Customer");
 * }</pre>
 */
@InternalMarker(reason = "Internal domain model; plugins use io.hexaglue.spi.ir.domain.DomainModelView")
public final class DomainModel {

    private final List<DomainType> types;
    private final List<DomainService> services;

    /**
     * Creates a domain model with the given types and services.
     *
     * @param types    domain types (not {@code null})
     * @param services domain services (not {@code null})
     */
    private DomainModel(List<DomainType> types, List<DomainService> services) {
        this.types = Collections.unmodifiableList(new ArrayList<>(types));
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
    }

    /**
     * Returns all domain types in this model.
     *
     * @return immutable list of domain types (never {@code null})
     */
    public List<DomainType> types() {
        return types;
    }

    /**
     * Returns all domain services in this model.
     *
     * @return immutable list of domain services (never {@code null})
     */
    public List<DomainService> services() {
        return services;
    }

    /**
     * Finds a domain type by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return domain type if found
     */
    public Optional<DomainType> findType(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return types.stream()
                .filter(t -> qualifiedName.equals(t.qualifiedName()))
                .findFirst();
    }

    /**
     * Finds a domain service by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return domain service if found
     */
    public Optional<DomainService> findService(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return services.stream()
                .filter(s -> qualifiedName.equals(s.qualifiedName()))
                .findFirst();
    }

    /**
     * Returns whether this model contains any domain types or services.
     *
     * @return {@code true} if the model is empty
     */
    public boolean isEmpty() {
        return types.isEmpty() && services.isEmpty();
    }

    /**
     * Returns the total number of domain types.
     *
     * @return type count
     */
    public int typeCount() {
        return types.size();
    }

    /**
     * Returns the total number of domain services.
     *
     * @return service count
     */
    public int serviceCount() {
        return services.size();
    }

    @Override
    public String toString() {
        return "DomainModel{types=" + typeCount() + ", services=" + serviceCount() + "}";
    }

    /**
     * Creates a builder for constructing {@link DomainModel} instances.
     *
     * @return builder (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty domain model.
     *
     * @return empty model (never {@code null})
     */
    public static DomainModel empty() {
        return new DomainModel(List.of(), List.of());
    }

    /**
     * Builder for {@link DomainModel}.
     *
     * <p>
     * This builder is not thread-safe and should be used from a single thread only.
     * </p>
     */
    public static final class Builder {
        private final List<DomainType> types = new ArrayList<>();
        private final List<DomainService> services = new ArrayList<>();

        private Builder() {
            // package-private
        }

        /**
         * Adds a domain type to the model.
         *
         * @param type domain type (not {@code null})
         * @return this builder
         */
        public Builder addType(DomainType type) {
            Objects.requireNonNull(type, "type");
            types.add(type);
            return this;
        }

        /**
         * Adds multiple domain types to the model.
         *
         * @param types domain types (not {@code null})
         * @return this builder
         */
        public Builder addTypes(List<DomainType> types) {
            Objects.requireNonNull(types, "types");
            for (DomainType type : types) {
                addType(type);
            }
            return this;
        }

        /**
         * Adds a domain service to the model.
         *
         * @param service domain service (not {@code null})
         * @return this builder
         */
        public Builder addService(DomainService service) {
            Objects.requireNonNull(service, "service");
            services.add(service);
            return this;
        }

        /**
         * Adds multiple domain services to the model.
         *
         * @param services domain services (not {@code null})
         * @return this builder
         */
        public Builder addServices(List<DomainService> services) {
            Objects.requireNonNull(services, "services");
            for (DomainService service : services) {
                addService(service);
            }
            return this;
        }

        /**
         * Builds the domain model.
         *
         * @return immutable domain model (never {@code null})
         */
        public DomainModel build() {
            return new DomainModel(types, services);
        }
    }
}
