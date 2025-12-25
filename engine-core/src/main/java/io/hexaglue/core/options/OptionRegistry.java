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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of known HexaGlue option keys and their typing metadata.
 *
 * <p>
 * The registry provides canonical {@link OptionKey} instances for parsing and lookups.
 * It is internal to core; plugins only consume the resolved {@code OptionsView}.
 * </p>
 */
@Deprecated(since = "0.2.0", forRemoval = true)
public final class OptionRegistry {

    private final Map<RegistryKey, OptionSpec> specs;

    /**
     * Creates an empty registry.
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public OptionRegistry() {
        this.specs = new LinkedHashMap<>();
    }

    /**
     * Registers an option specification.
     *
     * @param spec spec to register, not {@code null}
     * @return this registry (for chaining)
     * @throws IllegalArgumentException if a spec with the same scope/plugin/name is already registered
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public OptionRegistry register(OptionSpec spec) {
        Objects.requireNonNull(spec, "spec");
        RegistryKey rk = RegistryKey.of(spec.key(), spec.pluginId(), spec.name());
        if (specs.containsKey(rk)) {
            throw new IllegalArgumentException("Duplicate option key: " + rk);
        }
        specs.put(rk, spec);
        return this;
    }

    /**
     * Finds a registered spec by parsed key parts.
     *
     * @param scope scope, not {@code null}
     * @param pluginId plugin id (nullable; required for PLUGIN scope)
     * @param name option name, not {@code null}
     * @return spec or {@code null} if not registered
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public OptionSpec find(OptionScope scope, String pluginId, String name) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(name, "name");

        String n = name.trim();
        if (n.isEmpty()) {
            return null;
        }

        String pid = (pluginId == null) ? null : pluginId.trim();
        if (scope == OptionScope.PLUGIN && (pid == null || pid.isEmpty())) {
            return null;
        }
        if (pid != null && pid.isEmpty()) {
            pid = null;
        }

        return specs.get(RegistryKey.of(scope, pid, n));
    }

    /**
     * Returns all registered specs in registration order.
     *
     * @return immutable list of specs
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public List<OptionSpec> all() {
        return List.copyOf(specs.values());
    }

    /**
     * Option specification (canonical key + expected value kind).
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public static final class OptionSpec {

        private final OptionKey<?> key;
        private final OptionValueKind valueKind;

        // Cached parsed parts for lookup without depending on OptionKey internal structure.
        private final OptionScope scope;
        private final String pluginId;
        private final String name;

        /**
         * Creates an option specification.
         *
         * @param key canonical SPI key, not {@code null}
         * @param valueKind expected kind, not {@code null}
         * @param scope scope of this option, not {@code null}
         * @param pluginId plugin id when scope is PLUGIN (nullable otherwise)
         * @param name option name (non-blank), not {@code null}
         */
        @Deprecated(since = "0.2.0", forRemoval = true)
        public OptionSpec(
                OptionKey<?> key, OptionValueKind valueKind, OptionScope scope, String pluginId, String name) {
            this.key = Objects.requireNonNull(key, "key");
            this.valueKind = Objects.requireNonNull(valueKind, "valueKind");
            this.scope = Objects.requireNonNull(scope, "scope");

            String n = Objects.requireNonNull(name, "name").trim();
            if (n.isEmpty()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            this.name = n;

            String pid = (pluginId == null) ? null : pluginId.trim();
            if (scope == OptionScope.PLUGIN && (pid == null || pid.isEmpty())) {
                throw new IllegalArgumentException("pluginId must be provided for PLUGIN scope");
            }
            this.pluginId = (pid == null || pid.isEmpty()) ? null : pid;
        }

        /**
         * Returns the canonical SPI key for this option.
         *
         * @return key, never {@code null}
         */
        @Deprecated(since = "0.2.0", forRemoval = true)
        public OptionKey<?> key() {
            return key;
        }

        /**
         * Returns the expected value kind for coercion.
         *
         * @return kind, never {@code null}
         */
        @Deprecated(since = "0.2.0", forRemoval = true)
        public OptionValueKind valueKind() {
            return valueKind;
        }

        /**
         * Returns the scope of this option.
         *
         * @return scope, never {@code null}
         */
        @Deprecated(since = "0.2.0", forRemoval = true)
        public OptionScope scope() {
            return scope;
        }

        /**
         * Returns the plugin id for PLUGIN-scoped options.
         *
         * @return plugin id, or {@code null} when not applicable
         */
        @Deprecated(since = "0.2.0", forRemoval = true)
        public String pluginId() {
            return pluginId;
        }

        /**
         * Returns the option name (without prefixes).
         *
         * @return name, never {@code null}
         */
        @Deprecated(since = "0.2.0", forRemoval = true)
        public String name() {
            return name;
        }
    }

    /**
     * Supported value kinds for coercion (core-internal).
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    public enum OptionValueKind {
        BOOLEAN,
        INTEGER,
        STRING
    }

    /**
     * Internal registry key: (scope, pluginId?, name).
     */
    private static final class RegistryKey {

        private final OptionScope scope;
        private final String pluginId;
        private final String name;

        private RegistryKey(OptionScope scope, String pluginId, String name) {
            this.scope = scope;
            this.pluginId = pluginId;
            this.name = name;
        }

        static RegistryKey of(OptionScope scope, String pluginId, String name) {
            return new RegistryKey(scope, pluginId, name);
        }

        static RegistryKey of(OptionKey<?> key, String pluginId, String name) {
            // Derive scope from the provided parts rather than OptionKey internals.
            // The caller (OptionSpec constructor) supplies scope explicitly.
            throw new UnsupportedOperationException("Use RegistryKey.of(scope, pluginId, name)");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RegistryKey other)) return false;
            if (scope != other.scope) return false;
            if (!name.equals(other.name)) return false;
            return Objects.equals(pluginId, other.pluginId);
        }

        @Override
        public int hashCode() {
            int r = scope.hashCode();
            r = 31 * r + name.hashCode();
            r = 31 * r + Objects.hashCode(pluginId);
            return r;
        }

        @Override
        public String toString() {
            return scope + ":" + (pluginId == null ? "" : pluginId + ":") + name;
        }
    }
}
