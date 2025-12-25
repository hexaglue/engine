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

import io.hexaglue.spi.ir.domain.RelationshipKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Helper for extracting relationship metadata from YAML configuration.
 *
 * <p>This utility provides type-safe access to relationship metadata configured in hexaglue.yaml,
 * typically under {@code plugins.<plugin-id>.types.<type-name>.properties.<property-name>.relationship}.
 *
 * <h2>Example YAML Configuration</h2>
 * <pre>{@code
 * plugins:
 *   jpa-repository:
 *     types:
 *       Order:
 *         properties:
 *           customerId:
 *             relationship:
 *               kind: MANY_TO_ONE
 *               target: com.example.domain.Customer
 *               interAggregate: true
 *               bidirectional: false
 *               mappedBy: null
 * }</pre>
 *
 * <h2>Usage in Plugins</h2>
 * <pre>{@code
 * OptionsView options = context.options();
 * Map<String, Object> propertyConfig = PropertyMetadataHelper.getPropertyConfig(
 *     options, "jpa-repository", "com.example.Order", "customerId"
 * );
 *
 * Optional<RelationshipMetadata> relationship =
 *     RelationshipMetadataHelper.getRelationshipMetadata(propertyConfig);
 *
 * if (relationship.isPresent()) {
 *     RelationshipMetadata meta = relationship.get();
 *     // Use metadata for generation
 * }
 * }</pre>
 *
 * @since 0.4.0
 */
public final class RelationshipMetadataHelper {

    private RelationshipMetadataHelper() {
        // Utility class
    }

    /**
     * Extracts relationship metadata from property configuration.
     *
     * <p>Looks for a {@code relationship} key in the property configuration map and parses
     * it into a {@link RelationshipMetadata} instance.
     *
     * @param propertyConfig property configuration map (nullable)
     * @return relationship metadata if configured, empty otherwise
     */
    public static Optional<RelationshipMetadata> getRelationshipMetadata(Map<String, Object> propertyConfig) {
        if (propertyConfig == null || propertyConfig.isEmpty()) {
            return Optional.empty();
        }

        Object relationshipObj = propertyConfig.get("relationship");
        if (relationshipObj == null) {
            return Optional.empty();
        }

        if (!(relationshipObj instanceof Map)) {
            return Optional.empty();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> relationshipMap = (Map<String, Object>) relationshipObj;

        return parseRelationshipMetadata(relationshipMap);
    }

    /**
     * Parses a relationship configuration map into metadata.
     *
     * @param relationshipMap relationship configuration (not null)
     * @return parsed metadata if valid, empty otherwise
     */
    private static Optional<RelationshipMetadata> parseRelationshipMetadata(Map<String, Object> relationshipMap) {
        Objects.requireNonNull(relationshipMap, "relationshipMap");

        // Extract kind (required)
        String kindStr = getStringValue(relationshipMap, "kind");
        if (kindStr == null) {
            return Optional.empty();
        }

        RelationshipKind kind;
        try {
            kind = RelationshipKind.valueOf(kindStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }

        // Extract target (required)
        String target = getStringValue(relationshipMap, "target");
        if (target == null) {
            return Optional.empty();
        }

        // Extract interAggregate (required)
        Boolean interAggregate = getBooleanValue(relationshipMap, "interAggregate");
        if (interAggregate == null) {
            return Optional.empty();
        }

        // Extract optional fields
        Boolean bidirectional = getBooleanValue(relationshipMap, "bidirectional");
        String mappedBy = getStringValue(relationshipMap, "mappedBy");

        // Build metadata
        if (bidirectional != null && bidirectional && mappedBy != null) {
            return Optional.of(RelationshipMetadata.bidirectional(kind, target, interAggregate, mappedBy));
        }

        return Optional.of(RelationshipMetadata.of(kind, target, interAggregate));
    }

    /**
     * Extracts a string value from a map.
     *
     * @param map source map
     * @param key key to lookup
     * @return string value, or null if not found or not a string
     */
    private static String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return null;
    }

    /**
     * Extracts a boolean value from a map.
     *
     * @param map source map
     * @param key key to lookup
     * @return boolean value, or null if not found or not a boolean
     */
    private static Boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
}
