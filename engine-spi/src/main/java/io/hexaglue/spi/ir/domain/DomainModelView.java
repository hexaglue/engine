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
package io.hexaglue.spi.ir.domain;

import io.hexaglue.spi.stability.Stable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of the domain model known to the compiler.
 *
 * <p>This is a high-level entry point for plugins that need to discover domain types,
 * their properties, and domain services.</p>
 *
 * <p>Implementations must be deterministic for a given compilation run.</p>
 */
@Stable(since = "1.0.0")
public interface DomainModelView {

    /**
     * Returns all discovered domain types.
     *
     * @return immutable list of domain types (never {@code null})
     * @since 0.2.0
     */
    List<DomainTypeView> allTypes();

    /**
     * Attempts to find a domain type by its qualified name.
     *
     * @param qualifiedName qualified name (non-blank)
     * @return domain type if present
     */
    Optional<DomainTypeView> findType(String qualifiedName);

    /**
     * Returns all discovered domain services (if modeled).
     *
     * <p>Not all compilers will model domain services explicitly. Implementations should return
     * an empty list instead of {@code null}.</p>
     *
     * @return immutable list of domain services (never {@code null})
     * @since 0.2.0
     */
    List<DomainServiceView> allServices();

    /**
     * Attempts to find a domain service by qualified name.
     *
     * @param qualifiedName qualified name (non-blank)
     * @return domain service if present
     */
    Optional<DomainServiceView> findService(String qualifiedName);

    /**
     * Creates a simple immutable {@link DomainModelView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param types domain types (nullable)
     * @param services domain services (nullable)
     * @return domain model view
     */
    static DomainModelView of(List<DomainTypeView> types, List<DomainServiceView> services) {
        final List<DomainTypeView> ts = (types == null) ? List.of() : List.copyOf(types);
        final List<DomainServiceView> ss = (services == null) ? List.of() : List.copyOf(services);

        for (DomainTypeView t : ts) Objects.requireNonNull(t, "types contains null");
        for (DomainServiceView s : ss) Objects.requireNonNull(s, "services contains null");

        return new DomainModelView() {
            @Override
            public List<DomainTypeView> allTypes() {
                return ts;
            }

            @Override
            public Optional<DomainTypeView> findType(String qualifiedName) {
                Objects.requireNonNull(qualifiedName, "qualifiedName");
                String qn = qualifiedName.trim();
                if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
                for (DomainTypeView t : ts) {
                    if (qn.equals(t.qualifiedName())) return Optional.of(t);
                }
                return Optional.empty();
            }

            @Override
            public List<DomainServiceView> allServices() {
                return ss;
            }

            @Override
            public Optional<DomainServiceView> findService(String qualifiedName) {
                Objects.requireNonNull(qualifiedName, "qualifiedName");
                String qn = qualifiedName.trim();
                if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
                for (DomainServiceView s : ss) {
                    if (qn.equals(s.qualifiedName())) return Optional.of(s);
                }
                return Optional.empty();
            }
        };
    }
}
