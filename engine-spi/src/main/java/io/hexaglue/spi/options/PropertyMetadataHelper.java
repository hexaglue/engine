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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Helper utilities for accessing property-level metadata from plugin options.
 *
 * <p>This class provides convenient methods for plugins to query per-property
 * configuration without manually constructing deeply nested option keys.</p>
 *
 * <h2>YAML Schema Support</h2>
 * <p>Supports hierarchical property metadata configuration:
 * <pre>{@code
 * hexaglue:
 *   plugins:
 *     io.hexaglue.plugin.jpa:
 *       types:
 *         com.example.domain.Customer:
 *           tableName: customers
 *           properties:
 *             email:
 *               column:
 *                 length: 255
 *                 unique: true
 * }</pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In plugin code:
 * PluginOptionsView options = context.options().forPlugin("io.hexaglue.plugin.jpa");
 *
 * // Query specific property metadata
 * Optional<Integer> columnLength = PropertyMetadataHelper.getPropertyMetadata(
 *     options,
 *     "com.example.domain.Customer",
 *     "email",
 *     "column.length",
 *     Integer.class
 * );
 *
 * // Get entire property config section as map
 * Map<String, Object> columnConfig = PropertyMetadataHelper.getPropertyConfig(
 *     options,
 *     "com.example.domain.Customer",
 *     "email",
 *     "column"
 * );
 *
 * // Access nested values
 * Boolean unique = (Boolean) columnConfig.get("unique");
 * }</pre>
 *
 * <h2>Resolution Strategy</h2>
 * <p>This helper only accesses YAML configuration. Plugins should implement their own
 * resolution strategy combining:
 * <ol>
 *   <li>Annotations (highest priority)</li>
 *   <li>YAML configuration (via this helper)</li>
 *   <li>Heuristics (type/name-based)</li>
 *   <li>Defaults (lowest priority)</li>
 * </ol>
 *
 * @since 0.4.0
 */
public final class PropertyMetadataHelper {

    private PropertyMetadataHelper() {}

    /**
     * Gets a specific property metadata value.
     *
     * <p>Constructs the option key {@code types.<typeFqn>.properties.<propertyName>.<metadataPath>}
     * and retrieves the value if present in the configuration.</p>
     *
     * @param options plugin options view
     * @param typeFqn fully qualified domain type name
     * @param propertyName property name
     * @param metadataPath dot-separated path (e.g., "column.length", "validation.pattern")
     * @param type expected value type
     * @param <T> value type
     * @return metadata value if present
     * @throws NullPointerException if any parameter is null
     */
    public static <T> Optional<T> getPropertyMetadata(
            OptionsView.PluginOptionsView options,
            String typeFqn,
            String propertyName,
            String metadataPath,
            Class<T> type) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(typeFqn, "typeFqn");
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(metadataPath, "metadataPath");
        Objects.requireNonNull(type, "type");

        String key = buildPropertyMetadataKey(typeFqn, propertyName, metadataPath);
        return Optional.ofNullable(options.getOrDefault(key, type, null));
    }

    /**
     * Gets entire property configuration section as a map.
     *
     * <p>Useful for accessing multiple related metadata values without separate calls.
     * Returns an empty map if the section is not configured.</p>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * Map<String, Object> column = getPropertyConfig(options, "Customer", "email", "column");
     * // column = {length=255, unique=true, nullable=false}
     * }</pre>
     *
     * @param options plugin options view
     * @param typeFqn fully qualified domain type name
     * @param propertyName property name
     * @param section section name (e.g., "column", "validation")
     * @return configuration map (empty if not present)
     * @throws NullPointerException if any parameter is null
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> getPropertyConfig(
            OptionsView.PluginOptionsView options, String typeFqn, String propertyName, String section) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(typeFqn, "typeFqn");
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(section, "section");

        String key = buildPropertyMetadataKey(typeFqn, propertyName, section);
        return options.getOrDefault(key, Map.class, Map.of());
    }

    /**
     * Gets type-level metadata (not property-specific).
     *
     * <p>Accesses configuration at {@code types.<typeFqn>.<metadataPath>} level,
     * such as table name, schema, or other type-wide settings.</p>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * Optional<String> tableName = getTypeMetadata(
     *     options,
     *     "com.example.domain.Customer",
     *     "tableName",
     *     String.class
     * );
     * }</pre>
     *
     * @param options plugin options view
     * @param typeFqn fully qualified domain type name
     * @param metadataPath dot-separated path (e.g., "tableName", "schema")
     * @param type expected value type
     * @param <T> value type
     * @return metadata value if present
     * @throws NullPointerException if any parameter is null
     */
    public static <T> Optional<T> getTypeMetadata(
            OptionsView.PluginOptionsView options, String typeFqn, String metadataPath, Class<T> type) {
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(typeFqn, "typeFqn");
        Objects.requireNonNull(metadataPath, "metadataPath");
        Objects.requireNonNull(type, "type");

        String key = "types." + typeFqn + "." + metadataPath;
        return Optional.ofNullable(options.getOrDefault(key, type, null));
    }

    /**
     * Builds option key for property metadata.
     *
     * <p>Format: {@code types.<typeFqn>.properties.<propertyName>.<metadataPath>}</p>
     *
     * @param typeFqn type qualified name
     * @param propertyName property name
     * @param metadataPath metadata path
     * @return option key string
     */
    private static String buildPropertyMetadataKey(String typeFqn, String propertyName, String metadataPath) {
        return "types." + typeFqn + ".properties." + propertyName + "." + metadataPath;
    }
}
