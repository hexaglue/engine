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

import java.util.LinkedHashMap;
import java.util.Map;

@Deprecated(since = "0.2.0", forRemoval = true)
final class RawOptionsMerger {

    private RawOptionsMerger() {}

    @Deprecated(since = "0.2.0", forRemoval = true)
    static RawOptionsStore merge(RawOptionsStore base, RawOptionsStore override) {
        Map<String, RawOptionsStore.RawEntry> globals = new LinkedHashMap<>();
        Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> plugins = new LinkedHashMap<>();

        // base first
        for (String k : base.globalNames()) base.findGlobal(k).ifPresent(v -> globals.put(k, v));
        for (RawOptionsStore.PluginNameKey k : base.pluginNames())
            base.findPlugin(k.pluginId(), k.name()).ifPresent(v -> plugins.put(k, v));

        // override wins
        for (String k : override.globalNames()) override.findGlobal(k).ifPresent(v -> globals.put(k, v));
        for (RawOptionsStore.PluginNameKey k : override.pluginNames())
            override.findPlugin(k.pluginId(), k.name()).ifPresent(v -> plugins.put(k, v));

        return new RawOptionsStore(globals, plugins);
    }
}
