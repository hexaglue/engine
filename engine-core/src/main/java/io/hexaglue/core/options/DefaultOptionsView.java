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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default immutable {@link OptionsView} implementation.
 *
 * <p>
 * This class is a passive, read-only view over resolved options produced by the core option
 * resolution pipeline. It does not parse or validate options.
 * </p>
 *
 * <p>
 * The view is backed by a map of fully-typed {@link OptionKey} instances to {@link OptionValue}
 * objects. Key equality may include the expected value type; core should therefore use canonical
 * keys (typically from an {@link OptionRegistry}) when building the map.
 * </p>
 */
@Deprecated(since = "0.2.0", forRemoval = true)
public final class DefaultOptionsView implements OptionsView {

    private final Map<OptionKey<?>, OptionValue<?>> values;

    /**
     * Creates an options view backed by resolved values.
     *
     * @param values resolved values by key (nullable entries are ignored), not {@code null}
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public DefaultOptionsView(Map<OptionKey<?>, OptionValue<?>> values) {
        Objects.requireNonNull(values, "values");
        Map<OptionKey<?>, OptionValue<?>> copy = new LinkedHashMap<>();
        for (Map.Entry<OptionKey<?>, OptionValue<?>> e : values.entrySet()) {
            OptionKey<?> k = e.getKey();
            OptionValue<?> v = e.getValue();
            if (k != null && v != null) {
                copy.put(k, v);
            }
        }
        this.values = Collections.unmodifiableMap(copy);
    }

    @Override
    @Deprecated(since = "0.2.0", forRemoval = true)
    public boolean isPresent(OptionKey<?> key) {
        Objects.requireNonNull(key, "key");
        OptionValue<?> v = values.get(key);
        return v != null && v.present();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Deprecated(since = "0.2.0", forRemoval = true)
    public <T> OptionValue<T> get(OptionKey<T> key) {
        Objects.requireNonNull(key, "key");
        OptionValue<?> v = values.get(key);
        if (v == null) {
            return OptionValue.missing();
        }
        // Core produces the map; key equality includes type, so this cast is safe by construction.
        return (OptionValue<T>) v;
    }

    @Override
    @Deprecated(since = "0.2.0", forRemoval = true)
    public Set<OptionKey<?>> keys(OptionScope scope) {
        Objects.requireNonNull(scope, "scope");
        return values.keySet().stream()
                .filter(k -> k.scope() == scope)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    @Deprecated(since = "0.2.0", forRemoval = true)
    public PluginOptionsView forPlugin(String pluginId) {
        return PluginOptionsView.of(this, pluginId);
    }

    /**
     * Returns the underlying resolved map (immutable).
     *
     * <p>This is core-internal convenience; plugins should rely on the SPI only.</p>
     *
     * @return immutable map of resolved values
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public Map<OptionKey<?>, OptionValue<?>> asResolvedMap() {
        return values;
    }
}
