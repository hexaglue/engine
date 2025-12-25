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
package io.hexaglue.core.discovery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Sorts discovered plugins into a stable execution order.
 *
 * <p>
 * Ordering rules:
 * </p>
 * <ol>
 *   <li>Primary: {@link io.hexaglue.core.discovery.DiscoveredPlugin#priority()} ascending (lower runs earlier)</li>
 *   <li>Secondary: {@link io.hexaglue.core.discovery.DiscoveredPlugin#implementationClassName()} ascending (stable tie-breaker)</li>
 * </ol>
 *
 * <p>
 * This guarantees deterministic execution across JVMs and build environments.
 * </p>
 */
public final class PluginSorter {

    private static final Comparator<DiscoveredPlugin> ORDERING = Comparator.comparingInt(DiscoveredPlugin::priority)
            .thenComparing(DiscoveredPlugin::implementationClassName);

    /**
     * Returns a new list sorted according to the core ordering rules.
     *
     * @param plugins the plugins to sort, not {@code null}
     * @return a new sorted list, never {@code null}
     */
    public List<DiscoveredPlugin> sort(List<DiscoveredPlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins");
        List<DiscoveredPlugin> copy = new ArrayList<>(plugins);
        copy.sort(ORDERING);
        return List.copyOf(copy);
    }
}
