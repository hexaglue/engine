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
package io.hexaglue.core.options;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

final class RawOptionsStore {

    static final class RawEntry {
        final Object raw;
        final String source; // stable, e.g. "config-file:/hexaglue.yaml"

        RawEntry(Object raw, String source) {
            this.raw = raw;
            this.source = source;
        }
    }

    private final Map<String, RawEntry> global;
    private final Map<PluginNameKey, RawEntry> plugin;

    RawOptionsStore(Map<String, RawEntry> global, Map<PluginNameKey, RawEntry> plugin) {
        this.global = Map.copyOf(Objects.requireNonNull(global, "global"));
        this.plugin = Map.copyOf(Objects.requireNonNull(plugin, "plugin"));
    }

    Optional<RawEntry> findGlobal(String name) {
        return Optional.ofNullable(global.get(name));
    }

    Optional<RawEntry> findPlugin(String pluginId, String name) {
        return Optional.ofNullable(plugin.get(new PluginNameKey(pluginId, name)));
    }

    Set<String> globalNames() {
        return global.keySet();
    }

    Set<PluginNameKey> pluginNames() {
        return plugin.keySet();
    }

    record PluginNameKey(String pluginId, String name) {
        PluginNameKey {
            pluginId = pluginId.trim();
            name = name.trim();
        }
    }
}
