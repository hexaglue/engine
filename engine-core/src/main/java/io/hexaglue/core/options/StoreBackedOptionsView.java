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

import io.hexaglue.spi.options.OptionKey;
import io.hexaglue.spi.options.OptionScope;
import io.hexaglue.spi.options.OptionValue;
import io.hexaglue.spi.options.OptionsView;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Core options view backed by a raw store, plugin-agnostic.
 *
 * <p>Type coercion is performed on demand using {@link OptionKey#type()}.</p>
 */
public final class StoreBackedOptionsView implements OptionsView {

    private final RawOptionsStore store;

    public StoreBackedOptionsView(RawOptionsStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    @Override
    public boolean isPresent(OptionKey<?> key) {
        Objects.requireNonNull(key, "key");
        return switch (key.scope()) {
            case GLOBAL -> store.findGlobal(key.name()).isPresent();
            case PLUGIN ->
                store.findPlugin(key.pluginId().orElseThrow(), key.name()).isPresent();
        };
    }

    @Override
    public <T> OptionValue<T> get(OptionKey<T> key) {
        Objects.requireNonNull(key, "key");
        RawOptionsStore.RawEntry e =
                switch (key.scope()) {
                    case GLOBAL -> store.findGlobal(key.name()).orElse(null);
                    case PLUGIN ->
                        store.findPlugin(key.pluginId().orElseThrow(), key.name())
                                .orElse(null);
                };
        if (e == null) return OptionValue.missing();

        T decoded = OptionCoercions.coerce(e.raw, key.type(), key.toString());
        return OptionValue.present(decoded, e.source);
    }

    @Override
    public Set<OptionKey<?>> keys(OptionScope scope) {
        Objects.requireNonNull(scope, "scope");

        // Best-effort: types are not known to core for plugin options.
        // We return keys with Object.class so tooling can introspect available names.
        Set<OptionKey<?>> out = new LinkedHashSet<>();
        if (scope == OptionScope.GLOBAL) {
            for (String n : store.globalNames()) {
                out.add(OptionKey.global(n, Object.class));
            }
        } else {
            for (RawOptionsStore.PluginNameKey k : store.pluginNames()) {
                out.add(OptionKey.plugin(k.pluginId(), k.name(), Object.class));
            }
        }
        return Set.copyOf(out);
    }

    @Override
    public PluginOptionsView forPlugin(String pluginId) {
        return PluginOptionsView.of(this, pluginId);
    }
}
