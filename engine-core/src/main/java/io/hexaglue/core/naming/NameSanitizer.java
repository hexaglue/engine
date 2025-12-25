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
package io.hexaglue.core.naming;

import java.util.Objects;
import java.util.Set;

/**
 * Utilities for sanitizing and normalizing identifiers to ensure they are valid Java names.
 *
 * <p>
 * This class provides methods to:
 * <ul>
 *   <li>Validate Java identifiers</li>
 *   <li>Sanitize invalid characters</li>
 *   <li>Handle reserved keywords</li>
 *   <li>Convert naming conventions (camelCase, snake_case, etc.)</li>
 * </ul>
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Generated code must be valid Java. Names derived from:
 * <ul>
 *   <li>Domain types (which may use unconventional naming)</li>
 *   <li>External systems (databases, APIs)</li>
 *   <li>User configuration</li>
 * </ul>
 * must be sanitized to produce legal Java identifiers.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class NameSanitizer {

    /**
     * Java reserved keywords that cannot be used as identifiers.
     */
    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            // Contextual keywords (modules)
            "exports",
            "module",
            "open",
            "opens",
            "provides",
            "requires",
            "to",
            "transitive",
            "uses",
            "with",
            // Reserved literals
            "true",
            "false",
            "null");

    private NameSanitizer() {
        // utility class
    }

    /**
     * Returns {@code true} if the given string is a valid Java identifier.
     *
     * <p>
     * A valid identifier:
     * <ul>
     *   <li>Is not blank</li>
     *   <li>Starts with a Java letter or underscore</li>
     *   <li>Contains only Java letters, digits, or underscores</li>
     *   <li>Is not a Java keyword</li>
     * </ul>
     * </p>
     *
     * @param name name to validate (not {@code null})
     * @return {@code true} if valid
     */
    public static boolean isValidJavaIdentifier(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return false;
        }
        if (JAVA_KEYWORDS.contains(name)) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!Character.isJavaIdentifierPart(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sanitizes a string to produce a valid Java identifier.
     *
     * <p>
     * This method:
     * <ul>
     *   <li>Replaces invalid characters with underscores</li>
     *   <li>Ensures the result starts with a valid character</li>
     *   <li>Appends an underscore if the result is a keyword</li>
     *   <li>Returns "unnamed" if the input cannot be sanitized</li>
     * </ul>
     * </p>
     *
     * @param raw raw input (not {@code null})
     * @return valid Java identifier (never blank)
     */
    public static String sanitize(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "unnamed";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (i == 0) {
                if (Character.isJavaIdentifierStart(c)) {
                    sb.append(c);
                } else if (Character.isJavaIdentifierPart(c)) {
                    sb.append('_').append(c);
                } else {
                    sb.append('_');
                }
            } else {
                if (Character.isJavaIdentifierPart(c)) {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            }
        }

        String result = sb.toString();
        if (result.isEmpty()) {
            return "unnamed";
        }

        // Avoid keywords
        if (JAVA_KEYWORDS.contains(result)) {
            return result + "_";
        }

        return result;
    }

    /**
     * Sanitizes a package name.
     *
     * <p>
     * Package names are a sequence of identifiers separated by dots. This method:
     * <ul>
     *   <li>Splits on dots</li>
     *   <li>Sanitizes each segment</li>
     *   <li>Joins with dots</li>
     * </ul>
     * </p>
     *
     * @param raw raw package name (not {@code null})
     * @return valid package name (never blank)
     */
    public static String sanitizePackage(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "unnamed";
        }

        String[] segments = trimmed.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (!segment.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(sanitize(segment));
            }
        }

        String result = sb.toString();
        return result.isEmpty() ? "unnamed" : result;
    }

    /**
     * Converts a name to camelCase.
     *
     * <p>
     * This method handles:
     * <ul>
     *   <li>snake_case → camelCase</li>
     *   <li>kebab-case → camelCase</li>
     *   <li>space-separated → camelCase</li>
     * </ul>
     * </p>
     *
     * @param raw raw input (not {@code null})
     * @return camelCase identifier (never blank)
     */
    public static String toCamelCase(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "unnamed";
        }

        StringBuilder sb = new StringBuilder();
        boolean upperNext = false;

        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (upperNext) {
                    sb.append(Character.toUpperCase(c));
                    upperNext = false;
                } else {
                    sb.append(i == 0 ? Character.toLowerCase(c) : c);
                }
            } else {
                // Treat non-alphanumeric as word separator
                upperNext = (sb.length() > 0); // Only uppercase if not at start
            }
        }

        String result = sb.toString();
        return result.isEmpty() ? "unnamed" : sanitize(result);
    }

    /**
     * Converts a name to PascalCase (upper camel case).
     *
     * <p>
     * Similar to {@link #toCamelCase(String)}, but starts with uppercase.
     * </p>
     *
     * @param raw raw input (not {@code null})
     * @return PascalCase identifier (never blank)
     */
    public static String toPascalCase(String raw) {
        Objects.requireNonNull(raw, "raw");
        String camel = toCamelCase(raw);
        if (camel.isEmpty()) {
            return "Unnamed";
        }
        return Character.toUpperCase(camel.charAt(0)) + camel.substring(1);
    }

    /**
     * Converts a name to CONSTANT_CASE.
     *
     * <p>
     * This method:
     * <ul>
     *   <li>Inserts underscores before uppercase letters</li>
     *   <li>Converts all letters to uppercase</li>
     *   <li>Replaces non-alphanumeric characters with underscores</li>
     * </ul>
     * </p>
     *
     * @param raw raw input (not {@code null})
     * @return CONSTANT_CASE identifier (never blank)
     */
    public static String toConstantCase(String raw) {
        Objects.requireNonNull(raw, "raw");
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "UNNAMED";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                if (Character.isUpperCase(c)
                        && i > 0
                        && sb.length() > 0
                        && Character.isLowerCase(trimmed.charAt(i - 1))) {
                    sb.append('_');
                }
                sb.append(Character.toUpperCase(c));
            } else {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '_') {
                    sb.append('_');
                }
            }
        }

        String result = sb.toString();
        // Clean up consecutive underscores
        result = result.replaceAll("_+", "_");
        // Remove trailing underscores
        while (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }

        return result.isEmpty() ? "UNNAMED" : result;
    }

    /**
     * Returns {@code true} if the name is a Java reserved keyword.
     *
     * @param name name to check (not {@code null})
     * @return {@code true} if keyword
     */
    public static boolean isKeyword(String name) {
        Objects.requireNonNull(name, "name");
        return JAVA_KEYWORDS.contains(name);
    }

    /**
     * Escapes a keyword by appending an underscore.
     *
     * <p>
     * If the name is not a keyword, returns it unchanged.
     * </p>
     *
     * @param name name to escape (not {@code null})
     * @return escaped name (never blank)
     */
    public static String escapeKeyword(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return "unnamed";
        }
        return isKeyword(name) ? name + "_" : name;
    }
}
