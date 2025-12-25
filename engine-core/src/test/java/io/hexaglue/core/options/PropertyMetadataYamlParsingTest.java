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

import static com.google.common.truth.Truth.assertThat;

import java.io.BufferedReader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for property metadata YAML parsing.
 *
 * <p>Tests verify that the YAML parser correctly handles the hierarchical structure:
 * <pre>
 * hexaglue:
 *   plugins:
 *     plugin-id:
 *       types:
 *         com.example.Type:
 *           properties:
 *             propertyName:
 *               metadata: value
 * </pre>
 *
 * @since 0.4.0
 */
class PropertyMetadataYamlParsingTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Type-Level Metadata
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parseTypeLevelMetadata() {
        // Given: YAML with type-level metadata
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          tableName: customers
                          schema: public
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Type-level metadata is correctly flattened
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.tableName", "customers");
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.schema", "public");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property-Level Metadata - Simple Values
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parsePropertyMetadata_simpleValues() {
        // Given: YAML with property metadata (simple values)
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          properties:
                            email:
                              column:
                                length: 255
                                unique: true
                                nullable: false
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Property metadata is correctly flattened
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.length",
                255);
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.unique",
                true);
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.nullable",
                false);
    }

    @Test
    void parsePropertyMetadata_multipleProperties() {
        // Given: YAML with multiple properties
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          properties:
                            email:
                              column:
                                length: 255
                            name:
                              column:
                                length: 100
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Both properties are parsed
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.length",
                255);
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.name.column.length",
                100);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Property-Level Metadata - Nested Structures
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parsePropertyMetadata_nestedStructures() {
        // Given: YAML with deeply nested property metadata
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          properties:
                            email:
                              column:
                                length: 255
                                unique: true
                              validation:
                                pattern: "^[a-z]+@example\\\\.com$"
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Nested structures are correctly flattened
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.length",
                255);
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.unique",
                true);
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.validation.pattern",
                "^[a-z]+@example\\.com$");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multiple Types
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parseMultipleTypes() {
        // Given: YAML with multiple types
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          tableName: customers
                          properties:
                            email:
                              column:
                                length: 255
                        com.example.domain.Order:
                          tableName: orders
                          properties:
                            amount:
                              column:
                                nullable: false
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Both types are parsed correctly
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.tableName", "customers");
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.length",
                255);

        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Order.tableName", "orders");
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Order.properties.amount.column.nullable",
                false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mixed Configuration (Type-Level + Property-Level)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parseMixedConfiguration() {
        // Given: YAML with both type-level and property-level metadata
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      tableName: default_table
                      types:
                        com.example.domain.Customer:
                          tableName: customers
                          schema: public
                          properties:
                            email:
                              column:
                                length: 255
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Plugin-level, type-level, and property-level metadata are all parsed
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "tableName", "default_table");
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.tableName", "customers");
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.schema", "public");
        assertPluginOption(
                store,
                "io.hexaglue.plugin.jpa",
                "types.com.example.domain.Customer.properties.email.column.length",
                255);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void parseEmptyTypesSection() {
        // Given: YAML with empty types section
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                      basePackage: com.example
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Plugin-level option is still parsed
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "basePackage", "com.example");
    }

    @Test
    void parseEmptyPropertiesSection() {
        // Given: YAML with empty properties section
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          tableName: customers
                          properties:
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Type-level metadata is still parsed
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.tableName", "customers");
    }

    @Test
    void parseNoTypesSection() {
        // Given: YAML without types section
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      basePackage: com.example
                      enableAuditing: true
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Plugin-level options are parsed normally
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "basePackage", "com.example");
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "enableAuditing", true);
    }

    @Test
    void parseMultiplePlugins() {
        // Given: YAML with multiple plugins having type metadata
        String yaml = """
                hexaglue:
                  plugins:
                    io.hexaglue.plugin.jpa:
                      types:
                        com.example.domain.Customer:
                          tableName: customers
                    io.hexaglue.plugin.docs:
                      types:
                        com.example.domain.Customer:
                          description: "Main customer entity"
                """;

        // When: Parse
        RawOptionsStore store = parseYaml(yaml);

        // Then: Both plugins' type metadata are parsed
        assertPluginOption(store, "io.hexaglue.plugin.jpa", "types.com.example.domain.Customer.tableName", "customers");
        assertPluginOption(
                store,
                "io.hexaglue.plugin.docs",
                "types.com.example.domain.Customer.description",
                "Main customer entity");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private RawOptionsStore parseYaml(String yamlContent) {
        BufferedReader reader = new BufferedReader(new StringReader(yamlContent));
        return HexaGlueYamlOptionsParser.parse(reader, "test-config");
    }

    private void assertPluginOption(RawOptionsStore store, String pluginId, String optionName, Object expectedValue) {
        java.util.Optional<RawOptionsStore.RawEntry> entry = store.findPlugin(pluginId, optionName);

        assertThat(entry).isPresent();
        assertThat(entry.get().raw).isEqualTo(expectedValue);
    }
}
