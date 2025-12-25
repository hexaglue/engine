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
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of a method parameter in a port contract.
 *
 * <p>This view provides only signature-level information required by code generation
 * and diagnostics, without exposing compiler internals.</p>
 */
public interface PortParameterView {

    /**
     * Stable parameter name.
     *
     * <p>Parameter names may not always be available depending on compilation settings.
     * When unavailable, implementations should provide a deterministic synthetic name
     * (e.g., {@code "arg0"}, {@code "arg1"}).</p>
     *
     * @return parameter name (never blank)
     */
    String name();

    /**
     * Parameter type.
     *
     * @return parameter type (never {@code null})
     */
    TypeRef type();

    /**
     * Whether this parameter is varargs.
     *
     * @return {@code true} if varargs
     */
    boolean isVarArgs();

    /**
     * Optional user-facing description.
     *
     * @return description if available
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Creates a simple immutable {@link PortParameterView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param name parameter name
     * @param type parameter type
     * @param varArgs whether varargs
     * @param description description (nullable)
     * @return parameter view
     */
    static PortParameterView of(String name, TypeRef type, boolean varArgs, String description) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        final String n = name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        final String desc = (description == null || description.isBlank()) ? null : description.trim();

        return new PortParameterView() {
            @Override
            public String name() {
                return n;
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
                return Optional.ofNullable(desc);
            }
        };
    }
}
