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
package io.hexaglue.core.internal.ir.app;

import io.hexaglue.core.internal.InternalMarker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of the complete application model.
 *
 * <p>
 * The application model is the container for all analyzed application services (use cases)
 * discovered in the application. It is built during the analysis phase and serves as the
 * foundation for documentation, dependency analysis, and architectural visualization.
 * </p>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li><strong>Application Services:</strong> Use cases and application-level orchestration</li>
 * </ul>
 *
 * <h2>Application Layer Concepts</h2>
 * <p>
 * The application layer in Hexagonal Architecture:
 * </p>
 * <ul>
 *   <li>Orchestrates domain operations</li>
 *   <li>Coordinates calls to outbound ports (repositories, gateways)</li>
 *   <li>Defines transactional boundaries</li>
 *   <li>Exposes use cases to driving adapters (REST controllers, etc.)</li>
 * </ul>
 *
 * <h2>HexaGlue's Role</h2>
 * <p>
 * HexaGlue <strong>analyzes but never generates</strong> application services. The model is used for:
 * </p>
 * <ul>
 *   <li>Architecture documentation and visualization</li>
 *   <li>Use case discovery and cataloging</li>
 *   <li>Dependency mapping (which ports are used by which use cases)</li>
 *   <li>Architectural validation and diagnostics</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Captures all application services discovered during analysis</li>
 *   <li><strong>Immutability:</strong> Once built, the model is immutable</li>
 *   <li><strong>Queryable:</strong> Provides efficient lookup by qualified name</li>
 *   <li><strong>Optional:</strong> Not all projects expose application services explicitly</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Application service analyzers discover service types</li>
 *   <li>{@link ApplicationModel} is built using the builder pattern</li>
 *   <li>Model is validated for consistency</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.app.ApplicationModelView}) are created as adapters</li>
 *   <li>Plugins query the model via SPI for documentation and analysis</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ApplicationModel model = ApplicationModel.builder()
 *     .addService(registerCustomerService)
 *     .addService(placeOrderService)
 *     .build();
 *
 * Optional<ApplicationService> registerService =
 *     model.findService("com.example.application.RegisterCustomer");
 * }</pre>
 */
@InternalMarker(reason = "Internal application model; plugins use io.hexaglue.spi.ir.app.ApplicationModelView")
public final class ApplicationModel {

    private final List<ApplicationService> services;

    /**
     * Creates an application model with the given services.
     *
     * @param services application services (not {@code null})
     */
    private ApplicationModel(List<ApplicationService> services) {
        this.services = Collections.unmodifiableList(new ArrayList<>(services));
    }

    /**
     * Returns all application services in this model.
     *
     * @return immutable list of application services (never {@code null})
     */
    public List<ApplicationService> services() {
        return services;
    }

    /**
     * Finds an application service by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return application service if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<ApplicationService> findService(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return services.stream()
                .filter(s -> qualifiedName.equals(s.qualifiedName()))
                .findFirst();
    }

    /**
     * Finds all application services in the given package.
     *
     * @param packageName package name (not {@code null})
     * @return immutable list of services in package (never {@code null})
     * @throws NullPointerException if packageName is null
     */
    public List<ApplicationService> findServicesByPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return services.stream()
                .filter(s -> packageName.equals(s.packageName()))
                .toList();
    }

    /**
     * Returns whether this model contains any application services.
     *
     * @return {@code true} if the model is empty
     */
    public boolean isEmpty() {
        return services.isEmpty();
    }

    /**
     * Returns the total number of application services.
     *
     * @return service count
     */
    public int serviceCount() {
        return services.size();
    }

    /**
     * Returns whether an application service with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if service exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean containsService(String qualifiedName) {
        return findService(qualifiedName).isPresent();
    }

    /**
     * Creates a new empty application model.
     *
     * @return empty application model (never {@code null})
     */
    public static ApplicationModel empty() {
        return new ApplicationModel(List.of());
    }

    /**
     * Returns a builder for creating application models.
     *
     * @return new builder instance (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ApplicationModel other)) return false;
        return services.equals(other.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(services);
    }

    @Override
    public String toString() {
        return "ApplicationModel{" + "services=" + services.size() + '}';
    }

    /**
     * Builder for creating {@link ApplicationModel} instances.
     *
     * <p>
     * This builder follows the standard builder pattern with validation on build.
     * </p>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * ApplicationModel model = ApplicationModel.builder()
     *     .addService(registerCustomerService)
     *     .addService(placeOrderService)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private final List<ApplicationService> services = new ArrayList<>();

        private Builder() {}

        /**
         * Adds an application service to the model.
         *
         * @param service service to add (not {@code null})
         * @return this builder
         * @throws NullPointerException if service is null
         */
        public Builder addService(ApplicationService service) {
            Objects.requireNonNull(service, "service");
            this.services.add(service);
            return this;
        }

        /**
         * Adds multiple application services to the model.
         *
         * @param services services to add (not {@code null})
         * @return this builder
         * @throws NullPointerException if services is null or contains null
         */
        public Builder addServices(List<ApplicationService> services) {
            Objects.requireNonNull(services, "services");
            for (ApplicationService service : services) {
                addService(service);
            }
            return this;
        }

        /**
         * Sets the services list, replacing any previously added services.
         *
         * @param services services list (not {@code null})
         * @return this builder
         * @throws NullPointerException if services is null or contains null
         */
        public Builder services(List<ApplicationService> services) {
            Objects.requireNonNull(services, "services");
            this.services.clear();
            return addServices(services);
        }

        /**
         * Builds the application model.
         *
         * @return application model instance (never {@code null})
         */
        public ApplicationModel build() {
            return new ApplicationModel(services);
        }
    }
}
