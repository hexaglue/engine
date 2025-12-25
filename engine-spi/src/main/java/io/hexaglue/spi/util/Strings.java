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
package io.hexaglue.spi.util;

import java.util.Locale;
import java.util.Objects;

/**
 * Small, dependency-free string utilities.
 *
 * <p>This utility exists to avoid each plugin re-implementing common string operations
 * while keeping the SPI JDK-only.</p>
 */
public final class Strings {

    private Strings() {
        // utility class
    }

    /**
     * Returns {@code true} if the string is null or blank.
     *
     * @param s string
     * @return {@code true} if null or blank
     */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /**
     * Returns {@code s.trim()} or {@code null} if the result would be empty.
     *
     * @param s string
     * @return trimmed string or null
     */
    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /**
     * Returns {@code s.trim()} or empty string if {@code s} is null.
     *
     * @param s string
     * @return trimmed string (never null)
     */
    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Converts a string to lower camel case in a conservative way.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "CustomerId"} -> {@code "customerId"}</li>
     *   <li>{@code "customer_id"} -> {@code "customerId"}</li>
     *   <li>{@code "customer-id"} -> {@code "customerId"}</li>
     * </ul>
     *
     * <p>This is best-effort and intended for generated names.</p>
     *
     * @param value input value (non-blank)
     * @return lowerCamelCase string
     */
    public static String toLowerCamel(String value) {
        String v = Preconditions.requireNonBlank(value, "value");
        String pascal = toPascalCase(v);
        if (pascal.isEmpty()) return pascal;
        return Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

    /**
     * Converts a string to PascalCase in a conservative way.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "customer_id"} -> {@code "CustomerId"}</li>
     *   <li>{@code "customer-id"} -> {@code "CustomerId"}</li>
     *   <li>{@code "Customer id"} -> {@code "CustomerId"}</li>
     * </ul>
     *
     * @param value input value (non-blank)
     * @return PascalCase string
     */
    public static String toPascalCase(String value) {
        String v = Preconditions.requireNonBlank(value, "value");
        StringBuilder sb = new StringBuilder();
        boolean upperNext = true;
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (upperNext) {
                    sb.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    sb.append(c);
                }
            } else {
                upperNext = true;
            }
        }
        return sb.toString();
    }

    /**
     * Converts the first character to upper case (best-effort).
     *
     * @param value input value (non-blank)
     * @return value with upper-cased first character
     */
    public static String capitalize(String value) {
        String v = Preconditions.requireNonBlank(value, "value");
        if (v.length() == 1) return v.toUpperCase(Locale.ROOT);
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }

    /**
     * Converts the first character to lower case (best-effort).
     *
     * @param value input value (non-blank)
     * @return value with lower-cased first character
     */
    public static String decapitalize(String value) {
        String v = Preconditions.requireNonBlank(value, "value");
        if (v.length() == 1) return v.toLowerCase(Locale.ROOT);
        return Character.toLowerCase(v.charAt(0)) + v.substring(1);
    }

    /**
     * Ensures the given string starts with the given prefix.
     *
     * @param value input value (non-null)
     * @param prefix prefix (non-null)
     * @return value if it already starts with prefix, otherwise prefix + value
     */
    public static String ensureStartsWith(String value, String prefix) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(prefix, "prefix");
        return value.startsWith(prefix) ? value : prefix + value;
    }

    /**
     * Ensures the given string ends with the given suffix.
     *
     * @param value input value (non-null)
     * @param suffix suffix (non-null)
     * @return value if it already ends with suffix, otherwise value + suffix
     */
    public static String ensureEndsWith(String value, String suffix) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(suffix, "suffix");
        return value.endsWith(suffix) ? value : value + suffix;
    }
}
