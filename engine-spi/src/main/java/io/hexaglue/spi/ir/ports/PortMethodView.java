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
package io.hexaglue.spi.ir.ports;

import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of a method in a port interface.
 *
 * <p>This view captures signature-level information needed for generation and validation.</p>
 */
public interface PortMethodView {

    /**
     * Method name.
     *
     * @return method name (never blank)
     */
    String name();

    /**
     * Return type.
     *
     * @return return type (never {@code null})
     */
    TypeRef returnType();

    /**
     * Parameters of this method.
     *
     * @return immutable list of parameters (never {@code null})
     */
    List<PortParameterView> parameters();

    /**
     * Whether this method is a default method on the interface.
     *
     * <p>Default methods may be ignored by some plugins depending on generation rules.</p>
     *
     * @return {@code true} if default method
     */
    boolean isDefault();

    /**
     * Whether this method is a static method on the interface.
     *
     * @return {@code true} if static method
     */
    boolean isStatic();

    /**
     * Optional stable signature id.
     *
     * <p>When provided, this should be deterministic and stable, such as:
     * {@code "findById(com.example.CustomerId):java.util.Optional<com.example.Customer>"}.</p>
     *
     * @return signature id if available
     */
    default Optional<String> signatureId() {
        return Optional.empty();
    }

    /**
     * Optional user-facing description.
     *
     * @return description if available
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Creates a simple immutable {@link PortMethodView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param name method name
     * @param returnType return type
     * @param parameters parameters list
     * @param isDefault default flag
     * @param isStatic static flag
     * @param signatureId signature id (nullable)
     * @param description description (nullable)
     * @return method view
     */
    static PortMethodView of(
            String name,
            TypeRef returnType,
            List<PortParameterView> parameters,
            boolean isDefault,
            boolean isStatic,
            String signatureId,
            String description) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(returnType, "returnType");
        Objects.requireNonNull(parameters, "parameters");

        final String n = name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("name must not be blank");

        final List<PortParameterView> ps = List.copyOf(parameters);
        for (PortParameterView p : ps) Objects.requireNonNull(p, "parameters contains null");

        final String sid = (signatureId == null || signatureId.isBlank()) ? null : signatureId.trim();
        final String desc = (description == null || description.isBlank()) ? null : description.trim();

        return new PortMethodView() {
            @Override
            public String name() {
                return n;
            }

            @Override
            public TypeRef returnType() {
                return returnType;
            }

            @Override
            public List<PortParameterView> parameters() {
                return ps;
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
                return Optional.ofNullable(sid);
            }

            @Override
            public Optional<String> description() {
                return Optional.ofNullable(desc);
            }
        };
    }
}
