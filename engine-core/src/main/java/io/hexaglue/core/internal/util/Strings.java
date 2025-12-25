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
package io.hexaglue.core.internal.util;

import io.hexaglue.core.internal.InternalMarker;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Extended string utilities for internal core use.
 *
 * <p>
 * This class wraps Apache Commons Lang {@link StringUtils} and extends the basic string
 * utilities available in {@code io.hexaglue.spi.util.Strings} with additional operations
 * needed by the core implementation. It provides:
 * </p>
 * <ul>
 *   <li>Advanced string manipulation</li>
 *   <li>Joining and splitting operations</li>
 *   <li>Padding and alignment</li>
 *   <li>Common string transformations</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While the SPI provides minimal string utilities to avoid dependencies, the core needs more
 * comprehensive string operations. This class wraps Apache Commons Lang functionality while
 * maintaining compatibility with SPI utilities.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * String joined = Strings.join(", ", "a", "b", "c");
 * String indented = Strings.indent("text", 4);
 * List<String> lines = Strings.lines("line1\nline2\nline3");
 * }</pre>
 */
@InternalMarker(reason = "Extended string utilities wrapping Apache Commons for core implementation only")
public final class Strings {

    private Strings() {
        // utility class
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegate to SPI utilities (keep compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the string is null or blank.
     *
     * <p>Delegates to {@link StringUtils#isBlank(CharSequence)}.</p>
     *
     * @param s string
     * @return {@code true} if null or blank
     */
    public static boolean isBlank(String s) {
        return StringUtils.isBlank(s);
    }

    /**
     * Returns {@code s.trim()} or {@code null} if the result would be empty.
     *
     * <p>Delegates to {@link StringUtils#trimToNull(String)}.</p>
     *
     * @param s string
     * @return trimmed string or null
     */
    public static String trimToNull(String s) {
        return StringUtils.trimToNull(s);
    }

    /**
     * Returns {@code s.trim()} or empty string if {@code s} is null.
     *
     * <p>Delegates to {@link StringUtils#trimToEmpty(String)}.</p>
     *
     * @param s string
     * @return trimmed string (never null)
     */
    public static String trimToEmpty(String s) {
        return StringUtils.trimToEmpty(s);
    }

    /**
     * Converts a string to lower camel case.
     *
     * <p>Delegates to SPI for consistency with naming conventions.</p>
     *
     * @param value input value (non-blank)
     * @return lowerCamelCase string
     */
    public static String toLowerCamel(String value) {
        return io.hexaglue.spi.util.Strings.toLowerCamel(value);
    }

    /**
     * Converts a string to PascalCase.
     *
     * <p>Delegates to SPI for consistency with naming conventions.</p>
     *
     * @param value input value (non-blank)
     * @return PascalCase string
     */
    public static String toPascalCase(String value) {
        return io.hexaglue.spi.util.Strings.toPascalCase(value);
    }

    /**
     * Converts the first character to upper case.
     *
     * <p>Delegates to {@link StringUtils#capitalize(String)}.</p>
     *
     * @param value input value (non-blank)
     * @return capitalized string
     */
    public static String capitalize(String value) {
        return StringUtils.capitalize(value);
    }

    /**
     * Converts the first character to lower case.
     *
     * <p>Delegates to {@link StringUtils#uncapitalize(String)}.</p>
     *
     * @param value input value (non-blank)
     * @return decapitalized string
     */
    public static String decapitalize(String value) {
        return StringUtils.uncapitalize(value);
    }

    /**
     * Ensures the given string starts with the given prefix.
     *
     * <p>Delegates to SPI for consistency.</p>
     *
     * @param value input value (non-null)
     * @param prefix prefix (non-null)
     * @return value with prefix
     */
    public static String ensureStartsWith(String value, String prefix) {
        return io.hexaglue.spi.util.Strings.ensureStartsWith(value, prefix);
    }

    /**
     * Ensures the given string ends with the given suffix.
     *
     * <p>Delegates to SPI for consistency.</p>
     *
     * @param value input value (non-null)
     * @param suffix suffix (non-null)
     * @return value with suffix
     */
    public static String ensureEndsWith(String value, String suffix) {
        return io.hexaglue.spi.util.Strings.ensureEndsWith(value, suffix);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates to Apache Commons Lang
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a string with the specified character repeated n times.
     *
     * <p>Delegates to {@link StringUtils#repeat(char, int)}.</p>
     *
     * @param c character to repeat
     * @param count number of repetitions (must be >= 0)
     * @return repeated string (never {@code null})
     * @throws IllegalArgumentException if count is negative
     */
    public static String repeat(char c, int count) {
        return StringUtils.repeat(c, count);
    }

    /**
     * Returns a string with the specified string repeated n times.
     *
     * <p>Delegates to {@link StringUtils#repeat(String, int)}.</p>
     *
     * @param s string to repeat (not {@code null})
     * @param count number of repetitions (must be >= 0)
     * @return repeated string (never {@code null})
     * @throws IllegalArgumentException if count is negative
     */
    public static String repeat(String s, int count) {
        return StringUtils.repeat(s, count);
    }

    /**
     * Joins strings with a delimiter.
     *
     * <p>Delegates to {@link StringUtils#join(Object[], String)}.</p>
     *
     * @param delimiter delimiter (not {@code null})
     * @param parts parts to join (not {@code null})
     * @return joined string (never {@code null})
     */
    public static String join(String delimiter, String... parts) {
        return StringUtils.join(parts, delimiter);
    }

    /**
     * Joins strings with a delimiter.
     *
     * <p>Delegates to {@link StringUtils#join(Iterable, String)}.</p>
     *
     * @param delimiter delimiter (not {@code null})
     * @param parts parts to join (not {@code null})
     * @return joined string (never {@code null})
     */
    public static String join(String delimiter, Iterable<String> parts) {
        return StringUtils.join(parts, delimiter);
    }

    /**
     * Joins objects with a delimiter using their string representation.
     *
     * @param delimiter delimiter (not {@code null})
     * @param parts parts to join (not {@code null})
     * @param <T> element type
     * @return joined string (never {@code null})
     */
    public static <T> String joinObjects(String delimiter, Iterable<T> parts) {
        Objects.requireNonNull(delimiter, "delimiter");
        Objects.requireNonNull(parts, "parts");
        return StringUtils.join(parts, delimiter);
    }

    /**
     * Joins objects with a delimiter using a custom mapper.
     *
     * @param delimiter delimiter (not {@code null})
     * @param parts parts to join (not {@code null})
     * @param mapper function to convert elements to strings (not {@code null})
     * @param <T> element type
     * @return joined string (never {@code null})
     */
    public static <T> String joinWith(String delimiter, Iterable<T> parts, Function<T, String> mapper) {
        Objects.requireNonNull(delimiter, "delimiter");
        Objects.requireNonNull(parts, "parts");
        Objects.requireNonNull(mapper, "mapper");

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (T part : parts) {
            if (!first) {
                sb.append(delimiter);
            }
            sb.append(mapper.apply(part));
            first = false;
        }
        return sb.toString();
    }

    /**
     * Pads a string on the left to reach the specified length.
     *
     * <p>Delegates to {@link StringUtils#leftPad(String, int, char)}.</p>
     *
     * @param s string to pad (not {@code null})
     * @param minLength minimum length
     * @param padChar padding character
     * @return padded string (never {@code null})
     */
    public static String padLeft(String s, int minLength, char padChar) {
        return StringUtils.leftPad(s, minLength, padChar);
    }

    /**
     * Pads a string on the right to reach the specified length.
     *
     * <p>Delegates to {@link StringUtils#rightPad(String, int, char)}.</p>
     *
     * @param s string to pad (not {@code null})
     * @param minLength minimum length
     * @param padChar padding character
     * @return padded string (never {@code null})
     */
    public static String padRight(String s, int minLength, char padChar) {
        return StringUtils.rightPad(s, minLength, padChar);
    }

    /**
     * Removes a prefix from a string if present.
     *
     * @param s string (not {@code null})
     * @param prefix prefix to remove (not {@code null})
     * @return string without prefix (never {@code null})
     */
    public static String removePrefix(String s, String prefix) {
        return org.apache.commons.lang3.Strings.CS.removeStart(s, prefix);
    }

    /**
     * Removes a suffix from a string if present.
     *
     * @param s string (not {@code null})
     * @param suffix suffix to remove (not {@code null})
     * @return string without suffix (never {@code null})
     */
    public static String removeSuffix(String s, String suffix) {
        return org.apache.commons.lang3.Strings.CS.removeEnd(s, suffix);
    }

    /**
     * Returns the common prefix of two strings.
     *
     * <p>Delegates to {@link StringUtils#getCommonPrefix(String...)}.</p>
     *
     * @param a first string (not {@code null})
     * @param b second string (not {@code null})
     * @return common prefix (never {@code null}, may be empty)
     */
    public static String commonPrefix(String a, String b) {
        return StringUtils.getCommonPrefix(a, b);
    }

    /**
     * Returns {@code true} if the string is null or empty.
     *
     * <p>Delegates to {@link StringUtils#isEmpty(CharSequence)}.</p>
     *
     * @param s string
     * @return {@code true} if null or empty
     */
    public static boolean isEmpty(String s) {
        return StringUtils.isEmpty(s);
    }

    /**
     * Returns a default value if the string is null or blank.
     *
     * <p>Delegates to {@link StringUtils#defaultIfBlank(CharSequence, CharSequence)}.</p>
     *
     * @param s string
     * @param defaultValue default value (not {@code null})
     * @return original string if not blank, otherwise default value
     */
    public static String defaultIfBlank(String s, String defaultValue) {
        return StringUtils.defaultIfBlank(s, defaultValue);
    }

    /**
     * Returns a default value if the string is null or empty.
     *
     * <p>Delegates to {@link StringUtils#defaultIfEmpty(CharSequence, CharSequence)}.</p>
     *
     * @param s string
     * @param defaultValue default value (not {@code null})
     * @return original string if not empty, otherwise default value
     */
    public static String defaultIfEmpty(String s, String defaultValue) {
        return StringUtils.defaultIfEmpty(s, defaultValue);
    }

    /**
     * Truncates a string to the specified maximum length.
     *
     * <p>Delegates to {@link StringUtils#abbreviate(String, int)}.</p>
     *
     * @param s string to truncate (not {@code null})
     * @param maxLength maximum length (must be >= 4)
     * @return truncated string (never {@code null})
     */
    public static String truncate(String s, int maxLength) {
        return StringUtils.abbreviate(s, maxLength);
    }

    /**
     * Abbreviates a string in the middle if it exceeds the maximum length.
     *
     * <p>Delegates to {@link StringUtils#abbreviateMiddle(String, String, int)}.</p>
     *
     * @param s string to abbreviate (not {@code null})
     * @param maxLength maximum length (must be >= 3)
     * @return abbreviated string (never {@code null})
     */
    public static String abbreviateMiddle(String s, int maxLength) {
        return StringUtils.abbreviateMiddle(s, "...", maxLength);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extended utilities for core (not in Apache Commons)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Indents a string by adding spaces at the beginning.
     *
     * @param s string to indent (not {@code null})
     * @param spaces number of spaces (must be >= 0)
     * @return indented string (never {@code null})
     */
    public static String indent(String s, int spaces) {
        Objects.requireNonNull(s, "s");
        Preconditions.checkNonNegative(spaces, "spaces");
        if (spaces == 0) {
            return s;
        }
        String indentation = repeat(' ', spaces);
        return s.lines().map(line -> indentation + line).collect(Collectors.joining("\n"));
    }

    /**
     * Splits a string into lines.
     *
     * @param s string to split (not {@code null})
     * @return list of lines (never {@code null})
     */
    public static List<String> lines(String s) {
        Objects.requireNonNull(s, "s");
        return s.lines().collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns the common suffix of two strings.
     *
     * @param a first string (not {@code null})
     * @param b second string (not {@code null})
     * @return common suffix (never {@code null}, may be empty)
     */
    public static String commonSuffix(String a, String b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        int aLength = a.length();
        int bLength = b.length();
        int minLength = Math.min(aLength, bLength);

        for (int i = 0; i < minLength; i++) {
            if (a.charAt(aLength - 1 - i) != b.charAt(bLength - 1 - i)) {
                return a.substring(aLength - i);
            }
        }
        return a.substring(aLength - minLength);
    }
}
