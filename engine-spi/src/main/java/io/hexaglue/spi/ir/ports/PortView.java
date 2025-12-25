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
 * Read-only view of a port contract (typically a Java interface).
 *
 * <p>Ports are the primary stable contracts of the hexagon. HexaGlue uses them as
 * entry points to generate adapters and infrastructure.</p>
 */
public interface PortView {

    /**
     * Qualified name of the port type (typically an interface).
     *
     * @return qualified name (never blank)
     */
    String qualifiedName();

    /**
     * Simple name of the port type.
     *
     * @return simple name (never blank)
     */
    String simpleName();

    /**
     * Direction of the port.
     *
     * @return direction (never {@code null})
     */
    PortDirection direction();

    /**
     * Canonical Java type reference for the port type.
     *
     * @return type reference (never {@code null})
     */
    TypeRef type();

    /**
     * Methods declared by this port.
     *
     * @return immutable list of methods (never {@code null})
     */
    List<PortMethodView> methods();

    /**
     * Optional stable port identifier.
     *
     * <p>This can be used to group generated artifacts (e.g., one adapter per port).</p>
     *
     * @return port id if available
     */
    default Optional<String> portId() {
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
     * Creates a simple immutable {@link PortView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param qualifiedName qualified name
     * @param simpleName simple name
     * @param direction direction
     * @param type port type reference
     * @param methods methods list
     * @param portId port id (nullable)
     * @param description description (nullable)
     * @return port view
     */
    static PortView of(
            String qualifiedName,
            String simpleName,
            PortDirection direction,
            TypeRef type,
            List<PortMethodView> methods,
            String portId,
            String description) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(methods, "methods");

        final String qn = qualifiedName.trim();
        final String sn = simpleName.trim();
        if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
        if (sn.isEmpty()) throw new IllegalArgumentException("simpleName must not be blank");

        final List<PortMethodView> ms = List.copyOf(methods);
        for (PortMethodView m : ms) Objects.requireNonNull(m, "methods contains null");

        final String pid = (portId == null || portId.isBlank()) ? null : portId.trim();
        final String desc = (description == null || description.isBlank()) ? null : description.trim();

        return new PortView() {
            @Override
            public String qualifiedName() {
                return qn;
            }

            @Override
            public String simpleName() {
                return sn;
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
                return ms;
            }

            @Override
            public Optional<String> portId() {
                return Optional.ofNullable(pid);
            }

            @Override
            public Optional<String> description() {
                return Optional.ofNullable(desc);
            }
        };
    }
}
