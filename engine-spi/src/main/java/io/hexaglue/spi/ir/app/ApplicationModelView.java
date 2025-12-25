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
package io.hexaglue.spi.ir.app;

import io.hexaglue.spi.stability.Stable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of application services (use cases) discovered by the compiler.
 *
 * <p>This model is optional in HexaGlue: not all integrations or projects will expose
 * application services explicitly. Implementations should return an empty list when
 * not supported.</p>
 */
@Stable(since = "1.0.0")
public interface ApplicationModelView {

    /**
     * Returns all discovered application services.
     *
     * @return immutable list of services (never {@code null})
     * @since 0.2.0
     */
    List<ApplicationServiceView> allServices();

    /**
     * Attempts to find an application service by qualified name.
     *
     * @param qualifiedName qualified name (non-blank)
     * @return service if found
     */
    Optional<ApplicationServiceView> findService(String qualifiedName);

    /**
     * Returns whether this view is supported by the current compiler integration.
     *
     * <p>When {@code false}, {@link #allServices()} should typically be empty and
     * {@link #findService(String)} should return empty.</p>
     *
     * @return {@code true} if supported
     */
    default boolean isSupported() {
        return true;
    }

    /**
     * Returns an "empty/unsupported" application model view.
     *
     * @return unsupported view (never {@code null})
     */
    static ApplicationModelView unsupported() {
        return new ApplicationModelView() {
            @Override
            public List<ApplicationServiceView> allServices() {
                return List.of();
            }

            @Override
            public Optional<ApplicationServiceView> findService(String qualifiedName) {
                return Optional.empty();
            }

            @Override
            public boolean isSupported() {
                return false;
            }
        };
    }

    /**
     * Creates a simple immutable {@link ApplicationModelView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param services services list (nullable)
     * @return application model view
     */
    static ApplicationModelView of(List<ApplicationServiceView> services) {
        final List<ApplicationServiceView> ss = (services == null) ? List.of() : List.copyOf(services);
        for (ApplicationServiceView s : ss) Objects.requireNonNull(s, "services contains null");

        return new ApplicationModelView() {
            @Override
            public List<ApplicationServiceView> allServices() {
                return ss;
            }

            @Override
            public Optional<ApplicationServiceView> findService(String qualifiedName) {
                Objects.requireNonNull(qualifiedName, "qualifiedName");
                String qn = qualifiedName.trim();
                if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
                for (ApplicationServiceView s : ss) {
                    if (qn.equals(s.qualifiedName())) return Optional.of(s);
                }
                return Optional.empty();
            }
        };
    }
}
