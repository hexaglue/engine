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
import io.hexaglue.spi.ir.ports.PortMethodView;
import io.hexaglue.spi.ir.ports.PortParameterView;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a method in a port interface.
 *
 * <p>
 * This is the internal implementation of {@link PortMethodView}, providing rich
 * metadata about port methods for analysis, validation, and code generation.
 * Port methods define the operations available on a port contract.
 * </p>
 *
 * <h2>Method Information</h2>
 * <p>
 * A port method captures:
 * </p>
 * <ul>
 *   <li><strong>Name:</strong> Method name</li>
 *   <li><strong>Return Type:</strong> Full return type reference including generics</li>
 *   <li><strong>Parameters:</strong> Ordered list of method parameters</li>
 *   <li><strong>Modifiers:</strong> Whether default or static method</li>
 *   <li><strong>Signature ID:</strong> Optional stable signature identifier</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> All instances are immutable</li>
 *   <li><strong>Completeness:</strong> Captures all metadata needed for adapter generation</li>
 *   <li><strong>SPI Compatibility:</strong> Implements PortMethodView for plugin access</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and safe for concurrent access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortMethod method = PortMethod.builder()
 *     .name("findById")
 *     .returnType(optionalCustomerTypeRef)
 *     .addParameter(idParameter)
 *     .isDefault(false)
 *     .isStatic(false)
 *     .signatureId("findById(CustomerId):Optional<Customer>")
 *     .description("Finds a customer by ID")
 *     .build();
 * }</pre>
 */
@InternalMarker(reason = "Internal port method; plugins use io.hexaglue.spi.ir.ports.PortMethodView")
public final class PortMethod implements PortMethodView {

    private final String name;
    private final TypeRef returnType;
    private final List<PortParameter> parameters;
    private final boolean isDefault;
    private final boolean isStatic;
    private final String signatureId;
    private final String description;

    /**
     * Creates a port method with the given properties.
     *
     * @param name        method name (not {@code null}, not blank)
     * @param returnType  return type (not {@code null})
     * @param parameters  parameters list (not {@code null})
     * @param isDefault   whether default method
     * @param isStatic    whether static method
     * @param signatureId optional signature id (nullable)
     * @param description optional description (nullable)
     * @throws NullPointerException     if required fields are null
     * @throws IllegalArgumentException if validation fails
     */
    private PortMethod(
            String name,
            TypeRef returnType,
            List<PortParameter> parameters,
            boolean isDefault,
            boolean isStatic,
            String signatureId,
            String description) {

        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(returnType, "returnType");
        Objects.requireNonNull(parameters, "parameters");

        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Method name must not be blank");
        }

        // Validate parameters list contains no nulls
        for (PortParameter param : parameters) {
            Objects.requireNonNull(param, "parameters list contains null");
        }

        this.name = trimmedName;
        this.returnType = returnType;
        this.parameters = Collections.unmodifiableList(new ArrayList<>(parameters));
        this.isDefault = isDefault;
        this.isStatic = isStatic;
        this.signatureId = (signatureId == null || signatureId.isBlank()) ? null : signatureId.trim();
        this.description = (description == null || description.isBlank()) ? null : description.trim();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public TypeRef returnType() {
        return returnType;
    }

    @Override
    public List<PortParameterView> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    /**
     * Returns the parameters as {@link PortParameter} instances.
     *
     * <p>
     * This is a convenience method for internal code that needs the concrete type.
     * </p>
     *
     * @return immutable list of port parameters (never {@code null})
     */
    public List<PortParameter> internalParameters() {
        return parameters;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isStatic() {
        return isStatic;
    }

    @Override
    public Optional<String> signatureId() {
        return Optional.ofNullable(signatureId);
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns a builder for creating port methods.
     *
     * @return new builder instance (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PortMethod other)) return false;
        return isDefault == other.isDefault
                && isStatic == other.isStatic
                && name.equals(other.name)
                && returnType.equals(other.returnType)
                && parameters.equals(other.parameters)
                && Objects.equals(signatureId, other.signatureId)
                && Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, parameters, isDefault, isStatic, signatureId, description);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PortMethod{");
        sb.append("name='").append(name).append('\'');
        sb.append(", returnType=").append(returnType.render());
        sb.append(", parameters=").append(parameters.size());
        if (isDefault) {
            sb.append(", default");
        }
        if (isStatic) {
            sb.append(", static");
        }
        if (signatureId != null) {
            sb.append(", signatureId='").append(signatureId).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Builder for creating {@link PortMethod} instances.
     *
     * <p>
     * This builder follows the standard builder pattern with validation on build.
     * </p>
     *
     * <h2>Example</h2>
     * <pre>{@code
     * PortMethod method = PortMethod.builder()
     *     .name("save")
     *     .returnType(voidTypeRef)
     *     .addParameter(entityParameter)
     *     .isDefault(false)
     *     .isStatic(false)
     *     .build();
     * }</pre>
     */
    public static final class Builder {
        private String name;
        private TypeRef returnType;
        private final List<PortParameter> parameters = new ArrayList<>();
        private boolean isDefault;
        private boolean isStatic;
        private String signatureId;
        private String description;

        private Builder() {}

        /**
         * Sets the method name.
         *
         * @param name method name (not {@code null}, not blank)
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the return type.
         *
         * @param returnType return type (not {@code null})
         * @return this builder
         */
        public Builder returnType(TypeRef returnType) {
            this.returnType = returnType;
            return this;
        }

        /**
         * Adds a parameter to the method.
         *
         * @param parameter parameter to add (not {@code null})
         * @return this builder
         */
        public Builder addParameter(PortParameter parameter) {
            Objects.requireNonNull(parameter, "parameter");
            this.parameters.add(parameter);
            return this;
        }

        /**
         * Adds multiple parameters to the method.
         *
         * @param parameters parameters to add (not {@code null})
         * @return this builder
         */
        public Builder addParameters(List<PortParameter> parameters) {
            Objects.requireNonNull(parameters, "parameters");
            for (PortParameter param : parameters) {
                addParameter(param);
            }
            return this;
        }

        /**
         * Sets the parameters list, replacing any previously added parameters.
         *
         * @param parameters parameters list (not {@code null})
         * @return this builder
         */
        public Builder parameters(List<PortParameter> parameters) {
            Objects.requireNonNull(parameters, "parameters");
            this.parameters.clear();
            return addParameters(parameters);
        }

        /**
         * Sets whether this is a default method.
         *
         * @param isDefault {@code true} if default method
         * @return this builder
         */
        public Builder isDefault(boolean isDefault) {
            this.isDefault = isDefault;
            return this;
        }

        /**
         * Sets whether this is a static method.
         *
         * @param isStatic {@code true} if static method
         * @return this builder
         */
        public Builder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        /**
         * Sets the signature ID.
         *
         * @param signatureId optional signature id (nullable)
         * @return this builder
         */
        public Builder signatureId(String signatureId) {
            this.signatureId = signatureId;
            return this;
        }

        /**
         * Sets the method description.
         *
         * @param description optional description (nullable)
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Builds the port method.
         *
         * @return port method instance (never {@code null})
         * @throws NullPointerException     if required fields are null
         * @throws IllegalArgumentException if validation fails
         */
        public PortMethod build() {
            return new PortMethod(name, returnType, parameters, isDefault, isStatic, signatureId, description);
        }
    }
}
