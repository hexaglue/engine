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
import io.hexaglue.spi.ir.ports.PortParameterView;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a method parameter in a port contract.
 *
 * <p>
 * This is the internal implementation of {@link PortParameterView}, providing rich
 * metadata about port method parameters for analysis, validation, and code generation.
 * </p>
 *
 * <h2>Parameter Information</h2>
 * <p>
 * A port parameter captures:
 * </p>
 * <ul>
 *   <li><strong>Name:</strong> Parameter name (or synthetic name if unavailable)</li>
 *   <li><strong>Type:</strong> Full type reference including generics</li>
 *   <li><strong>Varargs:</strong> Whether this is a varargs parameter</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> All instances are immutable</li>
 *   <li><strong>Completeness:</strong> Captures all metadata needed for generation</li>
 *   <li><strong>SPI Compatibility:</strong> Implements PortParameterView for plugin access</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and safe for concurrent access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortParameter param = PortParameter.builder()
 *     .name("customerId")
 *     .type(customerIdTypeRef)
 *     .varArgs(false)
 *     .description("The customer identifier")
 *     .build();
 * }</pre>
 */
@InternalMarker(reason = "Internal port parameter; plugins use io.hexaglue.spi.ir.ports.PortParameterView")
public final class PortParameter implements PortParameterView {

    private final String name;
    private final TypeRef type;
    private final boolean varArgs;
    private final String description;

    /**
     * Creates a port parameter with the given properties.
     *
     * @param name        parameter name (not {@code null}, not blank)
     * @param type        parameter type (not {@code null})
     * @param varArgs     whether varargs
     * @param description optional description (nullable)
     * @throws NullPointerException     if name or type is null
     * @throws IllegalArgumentException if name is blank
     */
    private PortParameter(String name, TypeRef type, boolean varArgs, String description) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");

        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Parameter name must not be blank");
        }

        this.name = trimmedName;
        this.type = type;
        this.varArgs = varArgs;
        this.description = (description == null || description.isBlank()) ? null : description.trim();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public TypeRef type() {
        return type;
    }

    @Override
    public boolean isVarArgs() {
        return varArgs;
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns a builder for creating port parameters.
     *
     * @return new builder instance (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PortParameter other)) return false;
        return varArgs == other.varArgs
                && name.equals(other.name)
                && type.equals(other.type)
                && Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, varArgs, description);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PortParameter{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type=").append(type.render());
        if (varArgs) {
            sb.append(", varArgs");
        }
        if (description != null) {
            sb.append(", description='").append(description).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Builder for creating {@link PortParameter} instances.
     *
     * <p>
     * This builder follows the standard builder pattern with validation on build.
     * </p>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * PortParameter param = PortParameter.builder()
     *     .name("customerId")
     *     .type(typeRef)
     *     .varArgs(false)
     *     .description("Customer identifier")
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private String name;
        private TypeRef type;
        private boolean varArgs;
        private String description;

        private Builder() {}

        /**
         * Sets the parameter name.
         *
         * @param name parameter name (not {@code null}, not blank)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the parameter type.
         *
         * @param type parameter type (not {@code null})
         * @return this builder
         */
        public Builder type(TypeRef type) {
            this.type = type;
            return this;
        }

        /**
         * Sets whether this parameter is varargs.
         *
         * @param varArgs {@code true} if varargs
         * @return this builder
         */
        public Builder varArgs(boolean varArgs) {
            this.varArgs = varArgs;
            return this;
        }

        /**
         * Sets the parameter description.
         *
         * @param description optional description (nullable)
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the port parameter.
         *
         * @return port parameter instance (never {@code null})
         * @throws NullPointerException     if required fields are null
         * @throws IllegalArgumentException if validation fails
         */
        public PortParameter build() {
            return new PortParameter(name, type, varArgs, description);
        }
    }
}
