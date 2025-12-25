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

import io.hexaglue.spi.naming.NameRole;
import java.util.Objects;

/**
 * Naming rules and conventions for generated code.
 *
 * <p>
 * This class encapsulates common naming patterns used throughout HexaGlue:
 * <ul>
 *   <li>Type name suffixes and prefixes</li>
 *   <li>Package segment conventions</li>
 *   <li>Member naming patterns</li>
 *   <li>Resource path conventions</li>
 * </ul>
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Centralizing naming rules in one place:
 * <ul>
 *   <li>Ensures consistency across all generated artifacts</li>
 *   <li>Makes conventions discoverable and documentable</li>
 *   <li>Allows future customization without scattered changes</li>
 * </ul>
 * </p>
 *
 * <h2>Conventions</h2>
 * <p>
 * Default conventions follow common Java patterns:
 * <ul>
 *   <li>Types: PascalCase</li>
 *   <li>Fields: camelCase</li>
 *   <li>Methods: camelCase</li>
 *   <li>Constants: CONSTANT_CASE</li>
 *   <li>Packages: lowercase.dot.separated</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class NameRules {

    /**
     * Common type suffixes used in infrastructure code.
     */
    public static final class Suffixes {
        public static final String ENTITY = "Entity";
        public static final String DTO = "Dto";
        public static final String MAPPER = "Mapper";
        public static final String REPOSITORY = "Repository";
        public static final String CONTROLLER = "Controller";
        public static final String SERVICE = "Service";
        public static final String ADAPTER = "Adapter";
        public static final String CONFIG = "Config";
        public static final String FACTORY = "Factory";
        public static final String BUILDER = "Builder";
        public static final String IMPL = "Impl";
        public static final String TEST = "Test";

        private Suffixes() {}
    }

    /**
     * Common type prefixes used in infrastructure code.
     */
    public static final class Prefixes {
        public static final String DEFAULT = "Default";
        public static final String BASE = "Base";
        public static final String ABSTRACT = "Abstract";
        public static final String GENERATED = "Generated";

        private Prefixes() {}
    }

    /**
     * Common package segments used in generated code.
     */
    public static final class PackageSegments {
        public static final String DOMAIN = "domain";
        public static final String PORTS = "ports";
        public static final String ADAPTERS = "adapters";
        public static final String INFRASTRUCTURE = "infrastructure";
        public static final String PERSISTENCE = "persistence";
        public static final String REST = "rest";
        public static final String GRAPHQL = "graphql";
        public static final String MESSAGING = "messaging";
        public static final String CONFIG = "config";
        public static final String MAPPERS = "mappers";

        private PackageSegments() {}
    }

    /**
     * Common resource path prefixes.
     */
    public static final class ResourcePaths {
        public static final String META_INF = "META-INF";
        public static final String META_INF_SERVICES = "META-INF/services";
        public static final String META_INF_SPRING = "META-INF/spring";

        private ResourcePaths() {}
    }

    private NameRules() {
        // utility class
    }

    /**
     * Returns a conventional suffix for a given role and hint.
     *
     * <p>
     * This method provides default suffixes based on common patterns. Plugins may choose
     * to use different suffixes if needed.
     * </p>
     *
     * @param role role (not {@code null})
     * @param hint optional hint (nullable)
     * @return suggested suffix (never {@code null}, may be empty)
     */
    public static String suggestSuffix(NameRole role, String hint) {
        Objects.requireNonNull(role, "role");

        if (hint != null && !hint.isBlank()) {
            // If a hint is provided, use it as-is (caller should sanitize)
            return hint;
        }

        // Default suffixes based on role
        return switch (role) {
            case TYPE -> "";
            case FIELD, METHOD, PARAMETER -> "";
            case CONSTANT -> "";
            default -> "";
        };
    }

    /**
     * Returns a conventional prefix for a given role and hint.
     *
     * @param role role (not {@code null})
     * @param hint optional hint (nullable)
     * @return suggested prefix (never {@code null}, may be empty)
     */
    public static String suggestPrefix(NameRole role, String hint) {
        Objects.requireNonNull(role, "role");

        if (hint != null && !hint.isBlank()) {
            return hint;
        }

        // Most generated code doesn't use prefixes by default
        return "";
    }

    /**
     * Returns a conventional package segment for a given role and hint.
     *
     * @param role role (not {@code null})
     * @param hint optional hint (nullable)
     * @return suggested package segment (never {@code null})
     */
    public static String suggestPackageSegment(NameRole role, String hint) {
        Objects.requireNonNull(role, "role");

        if (hint != null && !hint.isBlank()) {
            return NameSanitizer.sanitize(hint.toLowerCase());
        }

        if (role == NameRole.PACKAGE) {
            return PackageSegments.INFRASTRUCTURE;
        }

        return "generated";
    }

    /**
     * Applies standard naming conventions based on role.
     *
     * <p>
     * This method transforms a base name according to role-specific conventions:
     * <ul>
     *   <li>TYPE → PascalCase</li>
     *   <li>FIELD, METHOD, PARAMETER → camelCase</li>
     *   <li>CONSTANT → CONSTANT_CASE</li>
     *   <li>PACKAGE → lowercase</li>
     * </ul>
     * </p>
     *
     * @param baseName base name (not {@code null})
     * @param role     role (not {@code null})
     * @return conventionally formatted name (never blank)
     */
    public static String applyConvention(String baseName, NameRole role) {
        Objects.requireNonNull(baseName, "baseName");
        Objects.requireNonNull(role, "role");

        return switch (role) {
            case TYPE -> NameSanitizer.toPascalCase(baseName);
            case FIELD, METHOD, PARAMETER -> NameSanitizer.toCamelCase(baseName);
            case CONSTANT -> NameSanitizer.toConstantCase(baseName);
            case PACKAGE -> NameSanitizer.sanitizePackage(baseName.toLowerCase());
            case RESOURCE_PATH, DOC_PATH -> baseName.trim();
        };
    }

    /**
     * Returns {@code true} if the given name follows Java class naming conventions.
     *
     * <p>
     * A conventional class name:
     * <ul>
     *   <li>Starts with uppercase</li>
     *   <li>Uses PascalCase</li>
     *   <li>Does not contain underscores (except for inner classes)</li>
     * </ul>
     * </p>
     *
     * @param name name to check (not {@code null})
     * @return {@code true} if conventional
     */
    public static boolean isConventionalClassName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return false;
        }
        if (!Character.isUpperCase(name.charAt(0))) {
            return false;
        }
        // Simple heuristic: no underscores except potentially for inner classes
        return NameSanitizer.isValidJavaIdentifier(name);
    }

    /**
     * Returns {@code true} if the given name follows Java field/method naming conventions.
     *
     * <p>
     * A conventional field/method name:
     * <ul>
     *   <li>Starts with lowercase</li>
     *   <li>Uses camelCase</li>
     *   <li>Does not contain underscores (except constants)</li>
     * </ul>
     * </p>
     *
     * @param name name to check (not {@code null})
     * @return {@code true} if conventional
     */
    public static boolean isConventionalMemberName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return false;
        }
        if (!Character.isLowerCase(name.charAt(0))) {
            return false;
        }
        return NameSanitizer.isValidJavaIdentifier(name);
    }

    /**
     * Returns {@code true} if the given name follows Java constant naming conventions.
     *
     * <p>
     * A conventional constant name:
     * <ul>
     *   <li>All uppercase</li>
     *   <li>Words separated by underscores</li>
     * </ul>
     * </p>
     *
     * @param name name to check (not {@code null})
     * @return {@code true} if conventional
     */
    public static boolean isConventionalConstantName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isUpperCase(c) && !Character.isDigit(c) && c != '_') {
                return false;
            }
        }
        return NameSanitizer.isValidJavaIdentifier(name);
    }

    /**
     * Returns {@code true} if the given name follows Java package naming conventions.
     *
     * <p>
     * A conventional package name:
     * <ul>
     *   <li>All lowercase</li>
     *   <li>Dot-separated segments</li>
     *   <li>No underscores or hyphens</li>
     * </ul>
     * </p>
     *
     * @param name name to check (not {@code null})
     * @return {@code true} if conventional
     */
    public static boolean isConventionalPackageName(String name) {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            return false;
        }

        String[] segments = name.split("\\.");
        for (String segment : segments) {
            if (segment.isEmpty()) {
                return false;
            }
            for (int i = 0; i < segment.length(); i++) {
                char c = segment.charAt(i);
                if (!Character.isLowerCase(c) && !Character.isDigit(c)) {
                    return false;
                }
            }
            if (!NameSanitizer.isValidJavaIdentifier(segment)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Suggests a getter method name for a field.
     *
     * <p>
     * Example: {@code suggestGetter("active", "boolean")} returns {@code "isActive"}.
     * Example: {@code suggestGetter("name", "String")} returns {@code "getName"}.
     * </p>
     *
     * @param fieldName field name (not {@code null})
     * @param fieldType field type simple name (not {@code null})
     * @return getter method name (never blank)
     */
    public static String suggestGetter(String fieldName, String fieldType) {
        Objects.requireNonNull(fieldName, "fieldName");
        Objects.requireNonNull(fieldType, "fieldType");

        String name = NameSanitizer.toCamelCase(fieldName);
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        if ("boolean".equals(fieldType) || "Boolean".equals(fieldType)) {
            // Use "is" prefix for booleans, unless the name already starts with "is"
            if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
                return name;
            }
            return "is" + capitalized;
        }

        return "get" + capitalized;
    }

    /**
     * Suggests a setter method name for a field.
     *
     * <p>
     * Example: {@code suggestSetter("name")} returns {@code "setName"}.
     * </p>
     *
     * @param fieldName field name (not {@code null})
     * @return setter method name (never blank)
     */
    public static String suggestSetter(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");

        String name = NameSanitizer.toCamelCase(fieldName);
        String capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);

        return "set" + capitalized;
    }

    /**
     * Suggests a builder method name for a field.
     *
     * <p>
     * Example: {@code suggestBuilder("name")} returns {@code "name"} (fluent style).
     * </p>
     *
     * @param fieldName field name (not {@code null})
     * @return builder method name (never blank)
     */
    public static String suggestBuilder(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        return NameSanitizer.toCamelCase(fieldName);
    }
}
