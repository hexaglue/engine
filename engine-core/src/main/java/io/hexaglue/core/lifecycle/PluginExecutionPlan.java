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
package io.hexaglue.core.lifecycle;

import io.hexaglue.core.discovery.DiscoveredPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic execution plan for discovered plugins.
 *
 * <p>
 * The plan is immutable and provides the ordered plugin list that should be used during generation.
 * The core may rebuild the plan if the classpath changes across rounds (rare in practice).
 * </p>
 */
public final class PluginExecutionPlan {

    private final List<DiscoveredPlugin> plugins;

    /**
     * Creates a new execution plan from an ordered list.
     *
     * @param plugins ordered plugins, not {@code null}
     */
    public PluginExecutionPlan(List<DiscoveredPlugin> plugins) {
        Objects.requireNonNull(plugins, "plugins");
        this.plugins = Collections.unmodifiableList(new ArrayList<>(plugins));
    }

    /**
     * Returns the ordered plugins.
     *
     * @return ordered plugin list, never {@code null}
     */
    public List<DiscoveredPlugin> plugins() {
        return plugins;
    }

    /**
     * Returns whether no plugin is present.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return plugins.isEmpty();
    }

    @Override
    public String toString() {
        return "PluginExecutionPlan{plugins=" + plugins.size() + "}";
    }
}
