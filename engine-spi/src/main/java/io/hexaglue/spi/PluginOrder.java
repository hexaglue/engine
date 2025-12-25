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
package io.hexaglue.spi;

import io.hexaglue.spi.stability.Stable;

/**
 * Best-effort ordering group for plugin execution.
 *
 * <p>The compiler may use this to produce deterministic ordering when multiple plugins are present.
 * Plugins must not rely on ordering for correctness.</p>
 */
@Stable(since = "1.0.0")
public enum PluginOrder {

    /**
     * Plugins that should run early (e.g., foundational IR enrichers or cross-cutting validations).
     */
    EARLY(0),

    /**
     * Default group for most plugins.
     */
    NORMAL(100),

    /**
     * Plugins that should run late (e.g., documentation aggregators, packaging, finalization outputs).
     */
    LATE(200);

    private final int priority;

    PluginOrder(int priority) {
        this.priority = priority;
    }

    /**
     * Returns a numeric priority used for sorting.
     *
     * <p>Lower numbers run first.</p>
     *
     * @return priority
     */
    public int priority() {
        return priority;
    }
}
