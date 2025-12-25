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
package io.hexaglue.spi.options;

import io.hexaglue.spi.stability.Stable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Read-only access to resolved configuration options.
 *
 * <p>This SPI abstracts how options are defined and resolved (compiler args, annotations,
 * config files, presets, etc.). Plugins must only rely on this stable surface.</p>
 *
 * <p>Implementations should:
 * <ul>
 *   <li>Provide deterministic resolution rules.</li>
 *   <li>Expose safe provenance information for diagnostics.</li>
 * </ul>
 */
@Stable(since = "1.0.0")
public interface OptionsView {

    /**
     * Returns whether an option is present (explicitly defined).
     *
     * @param key option key
     * @return {@code true} if the option was explicitly present
     */
    boolean isPresent(OptionKey<?> key);

    /**
     * Returns the resolved option value object (value + provenance).
     *
     * @param key option key
     * @param <T> value type
     * @return resolved option value (never {@code null})
     */
    <T> OptionValue<T> get(OptionKey<T> key);

    /**
     * Returns the resolved option value or a default value if missing.
     *
     * <p>If the option is present but its decoded value is {@code null}, this method returns {@code null}.</p>
     *
     * @param key option key
     * @param defaultValue default value to return when missing
     * @param <T> value type
     * @return resolved value or defaultValue if missing
     */
    default <T> T getOrDefault(OptionKey<T> key, T defaultValue) {
        OptionValue<T> v = get(key);
        return v.present() ? v.value() : defaultValue;
    }

    /**
     * Returns the resolved option value as an {@link Optional}.
     *
     * @param key option key
     * @param <T> value type
     * @return optional value (empty if missing)
     */
    default <T> Optional<T> getOptional(OptionKey<T> key) {
        return get(key).asOptional();
    }

    /**
     * Returns all known option keys for a given scope.
     *
     * <p>This is primarily intended for tooling and diagnostics. Plugins should not
     * assume this set is exhaustive in all integrations.</p>
     *
     * @param scope scope
     * @return set of keys (never {@code null})
     */
    Set<OptionKey<?>> keys(OptionScope scope);

    /**
     * Returns a view bound to a specific plugin id.
     *
     * <p>This allows plugin code to resolve plugin-scoped keys without repeatedly supplying
     * the plugin id.</p>
     *
     * @param pluginId plugin id (non-blank)
     * @return plugin-bound view (never {@code null})
     */
    PluginOptionsView forPlugin(String pluginId);

    /**
     * Creates a simple immutable {@link OptionsView} backed by a map.
     *
     * <p>This factory is intended for tests and lightweight tooling. Implementations
     * can ignore provenance and treat all entries as present.</p>
     *
     * @param values resolved values by key (nullable)
     * @return options view
     */
    static OptionsView of(Map<OptionKey<?>, OptionValue<?>> values) {
        final Map<OptionKey<?>, OptionValue<?>> map = (values == null) ? Map.of() : Map.copyOf(values);

        return new OptionsView() {
            @Override
            public boolean isPresent(OptionKey<?> key) {
                Objects.requireNonNull(key, "key");
                OptionValue<?> v = map.get(key);
                return v != null && v.present();
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> OptionValue<T> get(OptionKey<T> key) {
                Objects.requireNonNull(key, "key");
                OptionValue<?> v = map.get(key);
                if (v == null) return OptionValue.missing();
                // Trust the producer of the map; this is a test/tooling helper.
                return (OptionValue<T>) v;
            }

            @Override
            public Set<OptionKey<?>> keys(OptionScope scope) {
                Objects.requireNonNull(scope, "scope");
                return map.keySet().stream()
                        .filter(k -> k.scope() == scope)
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
            }

            @Override
            public PluginOptionsView forPlugin(String pluginId) {
                return PluginOptionsView.of(this, pluginId);
            }
        };
    }

    /**
     * Plugin-bound view of options.
     *
     * <p>This is a small convenience wrapper that keeps the SPI stable while improving ergonomics.</p>
     */
    @Stable(since = "1.0.0")
    interface PluginOptionsView {

        /**
         * The plugin id this view is bound to.
         *
         * @return plugin id (never {@code null})
         */
        String pluginId();

        /**
         * Returns whether an option is present for this plugin.
         *
         * @param name key name
         * @return {@code true} if present
         */
        boolean isPresent(String name);

        /**
         * Resolves an option by key name and expected type.
         *
         * @param name key name (non-blank)
         * @param type expected decoded type
         * @param <T> value type
         * @return option value (never {@code null})
         */
        <T> OptionValue<T> get(String name, Class<T> type);

        /**
         * Convenience: returns resolved value or default when missing.
         *
         * @param name key name
         * @param type expected type
         * @param defaultValue default when missing
         * @param <T> value type
         * @return resolved value or default
         */
        default <T> T getOrDefault(String name, Class<T> type, T defaultValue) {
            OptionValue<T> v = get(name, type);
            return v.present() ? v.value() : defaultValue;
        }

        /**
         * Creates a plugin-bound view backed by a parent {@link OptionsView}.
         *
         * @param parent parent options view
         * @param pluginId plugin id
         * @return plugin options view
         */
        static PluginOptionsView of(OptionsView parent, String pluginId) {
            Objects.requireNonNull(parent, "parent");
            Objects.requireNonNull(pluginId, "pluginId");
            final String pid = pluginId.trim();
            if (pid.isEmpty()) {
                throw new IllegalArgumentException("pluginId must not be blank");
            }

            return new PluginOptionsView() {
                @Override
                public String pluginId() {
                    return pid;
                }

                @Override
                public boolean isPresent(String name) {
                    return parent.isPresent(OptionKey.plugin(pid, name, Object.class));
                }

                // @SuppressWarnings("unchecked")
                @Override
                public <T> OptionValue<T> get(String name, Class<T> type) {
                    Objects.requireNonNull(name, "name");
                    Objects.requireNonNull(type, "type");
                    String n = name.trim();
                    if (n.isEmpty()) throw new IllegalArgumentException("name must not be blank");
                    // Key equality includes type; resolve using the provided expected type.
                    return parent.get(OptionKey.plugin(pid, n, type));
                }
            };
        }

        /**
         * Creates an empty plugin options view for testing purposes.
         *
         * <p>This view returns missing values for all option queries.</p>
         *
         * @return empty plugin options view
         * @since 0.4.0
         */
        static PluginOptionsView empty() {
            return new PluginOptionsView() {
                @Override
                public String pluginId() {
                    return "test-plugin";
                }

                @Override
                public boolean isPresent(String name) {
                    return false;
                }

                @Override
                public <T> OptionValue<T> get(String name, Class<T> type) {
                    return OptionValue.missing();
                }
            };
        }
    }
}
