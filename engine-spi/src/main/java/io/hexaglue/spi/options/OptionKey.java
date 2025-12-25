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

import java.util.Objects;
import java.util.Optional;

/**
 * Typed key identifying an option.
 *
 * <p>Keys are stable identifiers. The type parameter indicates the expected decoded
 * value type. Decoding is performed by the compiler/SPI implementation.</p>
 *
 * <p>Keys should be treated as constants (static final fields) by plugin authors.</p>
 *
 * <p>Recommended naming conventions:
 * <ul>
 *   <li>Global: {@code "hexaglue.<name>"} (e.g., {@code "hexaglue.debug"})</li>
 *   <li>Plugin: {@code "<pluginId>.<name>"} (e.g., {@code "io.hexaglue.plugin.spring.jpa.ddl"})</li>
 * </ul>
 *
 * <p>This type is JDK-only and intentionally minimal.</p>
 *
 * @param <T> decoded option value type
 */
public final class OptionKey<T> {

    private final String name;
    private final Class<T> type;
    private final OptionScope scope;
    private final String pluginId; // only for PLUGIN scope

    private OptionKey(String name, Class<T> type, OptionScope scope, String pluginId) {
        this.name = requireNonBlank(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.scope = Objects.requireNonNull(scope, "scope");
        this.pluginId = (pluginId == null) ? null : pluginId.trim();
        if (scope == OptionScope.PLUGIN) {
            if (this.pluginId == null || this.pluginId.isEmpty()) {
                throw new IllegalArgumentException("pluginId must be provided for PLUGIN scoped keys.");
            }
        }
    }

    /**
     * Creates a global option key.
     *
     * @param name stable key name (non-blank)
     * @param type decoded value type
     * @return option key
     */
    public static <T> OptionKey<T> global(String name, Class<T> type) {
        return new OptionKey<>(name, type, OptionScope.GLOBAL, null);
    }

    /**
     * Creates a plugin-scoped option key.
     *
     * @param pluginId owning plugin id (non-blank)
     * @param name stable key name (non-blank)
     * @param type decoded value type
     * @return option key
     */
    public static <T> OptionKey<T> plugin(String pluginId, String name, Class<T> type) {
        return new OptionKey<>(name, type, OptionScope.PLUGIN, requireNonBlank(pluginId, "pluginId"));
    }

    /** @return the stable option key name */
    public String name() {
        return name;
    }

    /** @return decoded value type */
    public Class<T> type() {
        return type;
    }

    /** @return option scope */
    public OptionScope scope() {
        return scope;
    }

    /**
     * Returns the owning plugin id for {@link OptionScope#PLUGIN} keys.
     *
     * @return plugin id if scope is PLUGIN, otherwise empty
     */
    public Optional<String> pluginId() {
        return Optional.ofNullable(pluginId);
    }

    @Override
    public String toString() {
        return (scope == OptionScope.PLUGIN ? (pluginId + ":") : "") + name + "<" + type.getSimpleName() + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OptionKey<?> other)) return false;
        return name.equals(other.name)
                && type.equals(other.type)
                && scope == other.scope
                && Objects.equals(pluginId, other.pluginId);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + scope.hashCode();
        result = 31 * result + (pluginId == null ? 0 : pluginId.hashCode());
        return result;
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String t = value.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return t;
    }
}
