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
package io.hexaglue.core.internal.ir.ports;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.spi.ir.ports.PortDirection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of the complete port model.
 *
 * <p>
 * The port model is the central container for all analyzed ports discovered in the application.
 * It is built during the analysis phase and serves as the foundation for adapter generation,
 * validation, and SPI view construction.
 * </p>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li><strong>Driving Ports (Inbound):</strong> Use cases, APIs the application offers</li>
 *   <li><strong>Driven Ports (Outbound):</strong> Repositories, gateways the application requires</li>
 * </ul>
 *
 * <h2>Port Categories</h2>
 * <p>
 * Ports are categorized by direction in Hexagonal Architecture:
 * </p>
 * <ul>
 *   <li><strong>DRIVING:</strong> Inbound ports expressing what the application offers</li>
 *   <li><strong>DRIVEN:</strong> Outbound ports expressing what the application depends on</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Captures all ports discovered during analysis</li>
 *   <li><strong>Immutability:</strong> Once built, the model is immutable</li>
 *   <li><strong>Queryable:</strong> Provides efficient lookup by qualified name and direction</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Port analyzers discover and extract port interfaces from source code</li>
 *   <li>{@link PortModel} is built using the builder pattern</li>
 *   <li>Model is validated for consistency</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.ports.PortModelView}) are created as adapters</li>
 *   <li>Plugins query the model via SPI during adapter generation</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortModel model = PortModel.builder()
 *     .addPort(customerRepositoryPort)
 *     .addPort(orderRepositoryPort)
 *     .addPort(orderUseCasePort)
 *     .build();
 *
 * Optional<Port> repository = model.findPort("com.example.CustomerRepository");
 * List<Port> drivenPorts = model.portsByDirection(PortDirection.DRIVEN);
 * }</pre>
 */
@InternalMarker(reason = "Internal port model; plugins use io.hexaglue.spi.ir.ports.PortModelView")
public final class PortModel {

    private final List<Port> ports;

    /**
     * Creates a port model with the given ports.
     *
     * @param ports ports list (not {@code null})
     */
    private PortModel(List<Port> ports) {
        this.ports = Collections.unmodifiableList(new ArrayList<>(ports));
    }

    /**
     * Returns all ports in this model.
     *
     * @return immutable list of ports (never {@code null})
     */
    public List<Port> ports() {
        return ports;
    }

    /**
     * Returns all ports matching the given direction.
     *
     * @param direction port direction (not {@code null})
     * @return immutable list of ports (never {@code null})
     * @throws NullPointerException if direction is null
     */
    public List<Port> portsByDirection(PortDirection direction) {
        Objects.requireNonNull(direction, "direction");
        return ports.stream().filter(p -> p.direction() == direction).toList();
    }

    /**
     * Returns all driving ports (inbound).
     *
     * @return immutable list of driving ports (never {@code null})
     */
    public List<Port> drivingPorts() {
        return portsByDirection(PortDirection.DRIVING);
    }

    /**
     * Returns all driven ports (outbound).
     *
     * @return immutable list of driven ports (never {@code null})
     */
    public List<Port> drivenPorts() {
        return portsByDirection(PortDirection.DRIVEN);
    }

    /**
     * Finds a port by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return port if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<Port> findPort(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return ports.stream()
                .filter(p -> qualifiedName.equals(p.qualifiedName()))
                .findFirst();
    }

    /**
     * Finds all ports in the given package.
     *
     * @param packageName package name (not {@code null})
     * @return immutable list of ports in package (never {@code null})
     * @throws NullPointerException if packageName is null
     */
    public List<Port> findPortsByPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return ports.stream().filter(p -> packageName.equals(p.packageName())).toList();
    }

    /**
     * Returns whether this model contains any ports.
     *
     * @return {@code true} if the model is empty
     */
    public boolean isEmpty() {
        return ports.isEmpty();
    }

    /**
     * Returns the total number of ports.
     *
     * @return port count
     */
    public int portCount() {
        return ports.size();
    }

    /**
     * Returns the number of driving ports.
     *
     * @return driving port count
     */
    public int drivingPortCount() {
        return (int) ports.stream().filter(Port::isDriving).count();
    }

    /**
     * Returns the number of driven ports.
     *
     * @return driven port count
     */
    public int drivenPortCount() {
        return (int) ports.stream().filter(Port::isDriven).count();
    }

    /**
     * Returns whether a port with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if port exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean containsPort(String qualifiedName) {
        return findPort(qualifiedName).isPresent();
    }

    /**
     * Creates a new empty port model.
     *
     * @return empty port model (never {@code null})
     */
    public static PortModel empty() {
        return new PortModel(List.of());
    }

    /**
     * Returns a builder for creating port models.
     *
     * @return new builder instance (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PortModel other)) return false;
        return ports.equals(other.ports);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ports);
    }

    @Override
    public String toString() {
        return "PortModel{" + "ports="
                + ports.size() + ", driving="
                + drivingPortCount() + ", driven="
                + drivenPortCount() + '}';
    }

    /**
     * Builder for creating {@link PortModel} instances.
     *
     * <p>
     * This builder follows the standard builder pattern with validation on build.
     * </p>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * PortModel model = PortModel.builder()
     *     .addPort(repositoryPort)
     *     .addPort(useCasePort)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private final List<Port> ports = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a port to the model.
         *
         * @param port port to add (not {@code null})
         * @return this builder
         * @throws NullPointerException if port is null
         */
        public Builder addPort(Port port) {
            Objects.requireNonNull(port, "port");
            this.ports.add(port);
            return this;
        }

        /**
         * Adds multiple ports to the model.
         *
         * @param ports ports to add (not {@code null})
         * @return this builder
         * @throws NullPointerException if ports is null or contains null
         */
        public Builder addPorts(List<Port> ports) {
            Objects.requireNonNull(ports, "ports");
            for (Port port : ports) {
                addPort(port);
            }
            return this;
        }

        /**
         * Sets the ports list, replacing any previously added ports.
         *
         * @param ports ports list (not {@code null})
         * @return this builder
         * @throws NullPointerException if ports is null or contains null
         */
        public Builder ports(List<Port> ports) {
            Objects.requireNonNull(ports, "ports");
            this.ports.clear();
            return addPorts(ports);
        }

        /**
         * Builds the port model.
         *
         * @return port model instance (never {@code null})
         */
        public PortModel build() {
            return new PortModel(ports);
        }
    }
}
