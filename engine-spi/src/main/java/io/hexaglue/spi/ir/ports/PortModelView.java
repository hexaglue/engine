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

import io.hexaglue.spi.stability.Stable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of all ports discovered by the compiler.
 *
 * <p>This model groups both driving and driven ports. Implementations should ensure
 * determinism and stable ordering for a given compilation.</p>
 */
@Stable(since = "1.0.0")
public interface PortModelView {

    /**
     * Returns all ports.
     *
     * @return immutable list of ports (never {@code null})
     * @since 0.2.0
     */
    List<PortView> allPorts();

    /**
     * Returns only ports matching a given direction.
     *
     * @param direction direction (never {@code null})
     * @return immutable list of ports (never {@code null})
     * @since 0.2.0
     */
    default List<PortView> allPorts(PortDirection direction) {
        Objects.requireNonNull(direction, "direction");
        return allPorts().stream().filter(p -> p.direction() == direction).toList();
    }

    /**
     * Attempts to find a port by qualified name.
     *
     * @param qualifiedName qualified name (non-blank)
     * @return port if found
     */
    Optional<PortView> findPort(String qualifiedName);

    /**
     * Creates a simple immutable {@link PortModelView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param ports ports list (nullable)
     * @return port model view
     */
    static PortModelView of(List<PortView> ports) {
        final List<PortView> ps = (ports == null) ? List.of() : List.copyOf(ports);
        for (PortView p : ps) Objects.requireNonNull(p, "ports contains null");

        return new PortModelView() {
            @Override
            public List<PortView> allPorts() {
                return ps;
            }

            @Override
            public Optional<PortView> findPort(String qualifiedName) {
                Objects.requireNonNull(qualifiedName, "qualifiedName");
                String qn = qualifiedName.trim();
                if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
                for (PortView p : ps) {
                    if (qn.equals(p.qualifiedName())) return Optional.of(p);
                }
                return Optional.empty();
            }
        };
    }
}
