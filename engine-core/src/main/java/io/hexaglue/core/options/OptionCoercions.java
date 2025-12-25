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
import java.util.Objects;

final class OptionCoercions {

    private OptionCoercions() {}

    @SuppressWarnings("unchecked")
    static <T> T coerce(Object raw, Class<T> type, String labelForErrors) {
        Objects.requireNonNull(type, "type");

        if (raw == null) return null;

        // Exact instance
        if (type.isInstance(raw)) {
            return (T) raw;
        }

        // Common coercions
        if (type == String.class) {
            return (T) raw.toString();
        }
        if (type == Boolean.class) {
            return (T) coerceBoolean(raw, labelForErrors);
        }
        if (type == Integer.class) {
            return (T) coerceInteger(raw, labelForErrors);
        }

        // Map coercion for nested property metadata structures
        if (type == Map.class && raw instanceof Map<?, ?> rawMap) {
            return (T) coerceMap(rawMap, labelForErrors);
        }

        // Unknown type: do not attempt to materialize complex objects in SPI
        // (keeps SPI stable + avoids leaking SnakeYAML internal structures)
        throw new IllegalArgumentException("Unsupported option type for " + labelForErrors + ": " + type.getName());
    }

    private static Boolean coerceBoolean(Object raw, String label) {
        if (raw instanceof Boolean b) return b;
        if (raw instanceof Number n) return n.intValue() != 0;
        String s = raw.toString().trim().toLowerCase();
        return switch (s) {
            case "true", "yes", "on", "1" -> true;
            case "false", "no", "off", "0" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean for " + label + ": " + raw);
        };
    }

    private static Integer coerceInteger(Object raw, String label) {
        if (raw instanceof Number n) return n.intValue();
        String s = raw.toString().trim();
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid integer for " + label + ": " + raw, ex);
        }
    }

    /**
     * Coerces a raw map to {@code Map<String, Object>}.
     *
     * <p>This is used to preserve nested YAML structures (e.g., property metadata)
     * without flattening them completely. Ensures all keys are strings and creates
     * an immutable copy.</p>
     *
     * @param rawMap raw map from YAML
     * @param label label for error messages
     * @return coerced map
     * @throws IllegalArgumentException if map contains non-string keys
     * @since 0.4.0
     */
    private static Map<String, Object> coerceMap(Map<?, ?> rawMap, String label) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("Non-string key in map for " + label + ": " + entry.getKey());
            }
            result.put(key, entry.getValue());
        }
        return result;
    }
}
