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
import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortView;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a port contract.
 *
 * <p>
 * This is the internal implementation of {@link PortView}, providing rich
 * metadata about ports for analysis, validation, and code generation.
 * Ports are the stable contracts of the hexagon in Hexagonal Architecture.
 * </p>
 *
 * <h2>Port Concepts</h2>
 * <p>
 * In Hexagonal Architecture:
 * </p>
 * <ul>
 *   <li><strong>Driving Ports (Inbound):</strong> Express what the application offers (use cases, APIs)</li>
 *   <li><strong>Driven Ports (Outbound):</strong> Express what the application requires (repositories, gateways)</li>
 * </ul>
 *
 * <h2>Port Information</h2>
 * <p>
 * A port captures:
 * </p>
 * <ul>
 *   <li><strong>Qualified Name:</strong> Full interface name</li>
 *   <li><strong>Simple Name:</strong> Interface simple name</li>
 *   <li><strong>Direction:</strong> Driving or driven</li>
 *   <li><strong>Type:</strong> Full type reference for the port interface</li>
 *   <li><strong>Methods:</strong> All methods declared by the port</li>
 *   <li><strong>Port ID:</strong> Optional stable identifier for grouping artifacts</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> All instances are immutable</li>
 *   <li><strong>Completeness:</strong> Captures all metadata needed for adapter generation</li>
 *   <li><strong>SPI Compatibility:</strong> Implements PortView for plugin access</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and safe for concurrent access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * Port port = Port.builder()
 *     .qualifiedName("com.example.CustomerRepository")
 *     .simpleName("CustomerRepository")
 *     .direction(PortDirection.DRIVEN)
 *     .type(repositoryTypeRef)
 *     .addMethod(findByIdMethod)
 *     .addMethod(saveMethod)
 *     .portId("customer-repository")
 *     .description("Repository for customer persistence")
 *     .build();
 * }</pre>
 */
@InternalMarker(reason = "Internal port representation; plugins use io.hexaglue.spi.ir.ports.PortView")
public final class Port implements PortView {

    private final String qualifiedName;
    private final String simpleName;
    private final PortDirection direction;
    private final TypeRef type;
    private final List<PortMethod> methods;
    private final String portId;
    private final String description;

    /**
     * Creates a port with the given properties.
     *
     * @param qualifiedName qualified name (not {@code null}, not blank)
     * @param simpleName    simple name (not {@code null}, not blank)
     * @param direction     port direction (not {@code null})
     * @param type          port type reference (not {@code null})
     * @param methods       methods list (not {@code null})
     * @param portId        optional port id (nullable)
     * @param description   optional description (nullable)
     * @throws NullPointerException     if required fields are null
     * @throws IllegalArgumentException if validation fails
     */
    private Port(
            String qualifiedName,
            String simpleName,
            PortDirection direction,
            TypeRef type,
            List<PortMethod> methods,
            String portId,
            String description) {

        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(methods, "methods");

        String trimmedQualifiedName = qualifiedName.trim();
        String trimmedSimpleName = simpleName.trim();

        if (trimmedQualifiedName.isEmpty()) {
            throw new IllegalArgumentException("Qualified name must not be blank");
        }
        if (trimmedSimpleName.isEmpty()) {
            throw new IllegalArgumentException("Simple name must not be blank");
        }

        // Validate methods list contains no nulls
        for (PortMethod method : methods) {
            Objects.requireNonNull(method, "methods list contains null");
        }

        this.qualifiedName = trimmedQualifiedName;
        this.simpleName = trimmedSimpleName;
        this.direction = direction;
        this.type = type;
        this.methods = Collections.unmodifiableList(new ArrayList<>(methods));
        this.portId = (portId == null || portId.isBlank()) ? null : portId.trim();
        this.description = (description == null || description.isBlank()) ? null : description.trim();
    }

    @Override
    public String qualifiedName() {
        return qualifiedName;
    }

    @Override
    public String simpleName() {
        return simpleName;
    }

    @Override
    public PortDirection direction() {
        return direction;
    }

    @Override
    public TypeRef type() {
        return type;
    }

    @Override
    public List<PortMethodView> methods() {
        return Collections.unmodifiableList(methods);
    }

    /**
     * Returns the methods as {@link PortMethod} instances.
     *
     * <p>
     * This is a convenience method for internal code that needs the concrete type.
     * </p>
     *
     * @return immutable list of port methods (never {@code null})
     */
    public List<PortMethod> internalMethods() {
        return methods;
    }

    @Override
    public Optional<String> portId() {
        return Optional.ofNullable(portId);
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the package name of this port.
     *
     * @return package name, or empty string if in default package
     */
    public String packageName() {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? "" : qualifiedName.substring(0, lastDot);
    }

    /**
     * Returns whether this port is a driving port (inbound).
     *
     * @return {@code true} if driving port
     */
    public boolean isDriving() {
        return direction == PortDirection.DRIVING;
    }

    /**
     * Returns whether this port is a driven port (outbound).
     *
     * @return {@code true} if driven port
     */
    public boolean isDriven() {
        return direction == PortDirection.DRIVEN;
    }

    /**
     * Returns a builder for creating ports.
     *
     * @return new builder instance (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Port other)) return false;
        return qualifiedName.equals(other.qualifiedName)
                && simpleName.equals(other.simpleName)
                && direction == other.direction
                && type.equals(other.type)
                && methods.equals(other.methods)
                && Objects.equals(portId, other.portId)
                && Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName, simpleName, direction, type, methods, portId, description);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Port{");
        sb.append("qualifiedName='").append(qualifiedName).append('\'');
        sb.append(", direction=").append(direction);
        sb.append(", methods=").append(methods.size());
        if (portId != null) {
            sb.append(", portId='").append(portId).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Builder for creating {@link Port} instances.
     *
     * <p>
     * This builder follows the standard builder pattern with validation on build.
     * </p>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * Port port = Port.builder()
     *     .qualifiedName("com.example.CustomerRepository")
     *     .simpleName("CustomerRepository")
     *     .direction(PortDirection.DRIVEN)
     *     .type(typeRef)
     *     .addMethod(method1)
     *     .addMethod(method2)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private String qualifiedName;
        private String simpleName;
        private PortDirection direction;
        private TypeRef type;
        private final List<PortMethod> methods = new ArrayList<>();
        private String portId;
        private String description;

        private Builder() {}

        /**
         * Sets the qualified name.
         *
         * @param qualifiedName qualified name (not {@code null}, not blank)
         * @return this builder
         */
        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            return this;
        }

        /**
         * Sets the simple name.
         *
         * @param simpleName simple name (not {@code null}, not blank)
         * @return this builder
         */
        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        /**
         * Sets the port direction.
         *
         * @param direction direction (not {@code null})
         * @return this builder
         */
        public Builder direction(PortDirection direction) {
            this.direction = direction;
            return this;
        }

        /**
         * Sets the port type reference.
         *
         * @param type type reference (not {@code null})
         * @return this builder
         */
        public Builder type(TypeRef type) {
            this.type = type;
            return this;
        }

        /**
         * Adds a method to the port.
         *
         * @param method method to add (not {@code null})
         * @return this builder
         */
        public Builder addMethod(PortMethod method) {
            Objects.requireNonNull(method, "method");
            this.methods.add(method);
            return this;
        }

        /**
         * Adds multiple methods to the port.
         *
         * @param methods methods to add (not {@code null})
         * @return this builder
         */
        public Builder addMethods(List<PortMethod> methods) {
            Objects.requireNonNull(methods, "methods");
            for (PortMethod method : methods) {
                addMethod(method);
            }
            return this;
        }

        /**
         * Sets the methods list, replacing any previously added methods.
         *
         * @param methods methods list (not {@code null})
         * @return this builder
         */
        public Builder methods(List<PortMethod> methods) {
            Objects.requireNonNull(methods, "methods");
            this.methods.clear();
            return addMethods(methods);
        }

        /**
         * Sets the port ID.
         *
         * @param portId optional port id (nullable)
         * @return this builder
         */
        public Builder portId(String portId) {
            this.portId = portId;
            return this;
        }

        /**
         * Sets the port description.
         *
         * @param description optional description (nullable)
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the port.
         *
         * @return port instance (never {@code null})
         * @throws NullPointerException     if required fields are null
         * @throws IllegalArgumentException if validation fails
         */
        public Port build() {
            return new Port(qualifiedName, simpleName, direction, type, methods, portId, description);
        }
    }
}
