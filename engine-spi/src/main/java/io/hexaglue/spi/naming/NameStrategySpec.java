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
package io.hexaglue.spi.naming;

import io.hexaglue.spi.stability.Evolvable;
import java.util.Objects;
import java.util.Optional;

/**
 * Naming conventions shared across plugins.
 *
 * <p>This SPI is designed so that:
 * <ul>
 *   <li>Plugins can remain convention-driven without hardcoding conventions everywhere.</li>
 *   <li>Core can offer consistent defaults and optional customization points.</li>
 * </ul>
 *
 * <p>The naming strategy must be deterministic and stable for a given input.</p>
 *
 * <h2>Stability rule</h2>
 * <p>This interface should evolve by adding new methods with default implementations,
 * or by extending input data in backward-compatible ways.</p>
 */
@Evolvable(since = "1.0.0")
public interface NameStrategySpec {

    /**
     * Returns the base package (root) used for generated infrastructure artifacts.
     *
     * <p>Example: {@code "com.example.infrastructure"}.</p>
     *
     * @return base generated package (never {@code null})
     */
    String basePackage();

    /**
     * Resolves a generated package name for a given domain/package input and logical role.
     *
     * <p>Implementations may choose to:
     * <ul>
     *   <li>mirror the domain package</li>
     *   <li>collapse packages</li>
     *   <li>append role-based segments (e.g., ".persistence", ".rest")</li>
     * </ul>
     *
     * @param domainPackage domain package name (nullable if unknown)
     * @param role name role (never {@code null})
     * @param hint optional stable hint (e.g., plugin id, feature name) (nullable)
     * @return generated package name (never {@code null})
     */
    String packageName(String domainPackage, NameRole role, String hint);

    /**
     * Resolves a generated simple type name for a given input and role.
     *
     * <p>Example: for a domain type {@code Customer} and role {@code TYPE} with hint {@code "JpaEntity"},
     * a strategy could return {@code "CustomerEntity"}.</p>
     *
     * @param baseSimpleName base simple name (non-blank)
     * @param role role (never {@code null})
     * @param hint optional hint (nullable)
     * @return generated simple name (never {@code null})
     */
    String simpleName(String baseSimpleName, NameRole role, String hint);

    /**
     * Resolves a generated member name (field/method/parameter/constant).
     *
     * @param base base name (non-blank)
     * @param role role (never {@code null})
     * @param hint optional hint (nullable)
     * @return generated member name (never {@code null})
     */
    String memberName(String base, NameRole role, String hint);

    /**
     * Resolves a generated resource path.
     *
     * <p>Example: {@code "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports"}.</p>
     *
     * @param base base path or name (non-blank)
     * @param role role (typically {@link NameRole#RESOURCE_PATH}) (never {@code null})
     * @param hint optional hint (nullable)
     * @return generated resource path (never {@code null})
     */
    String resourcePath(String base, NameRole role, String hint);

    /**
     * Resolves a generated documentation path.
     *
     * <p>Example: {@code "hexaglue/docs/spring-jpa/README.md"}.</p>
     *
     * @param base base name/path (non-blank)
     * @param role role (typically {@link NameRole#DOC_PATH}) (never {@code null})
     * @param hint optional hint (nullable)
     * @return generated documentation path (never {@code null})
     */
    String docPath(String base, NameRole role, String hint);

    /**
     * Convenience method: creates a fully-qualified name for a generated type.
     *
     * @param pkg generated package name (non-blank)
     * @param simpleName generated simple name (non-blank)
     * @return qualified name
     */
    default QualifiedName qualifiedTypeName(String pkg, String simpleName) {
        Objects.requireNonNull(pkg, "pkg");
        Objects.requireNonNull(simpleName, "simpleName");
        String p = pkg.trim();
        String s = simpleName.trim();
        if (p.isEmpty()) throw new IllegalArgumentException("pkg must not be blank");
        if (s.isEmpty()) throw new IllegalArgumentException("simpleName must not be blank");
        return QualifiedName.of(p + "." + s);
    }

    /**
     * Optional hint: returns a stable "component key" that can be used to group artifacts.
     *
     * <p>This can be used by tooling or documentation generators to create deterministic sections.</p>
     *
     * @return component key if supported
     */
    default Optional<String> componentKey() {
        return Optional.empty();
    }

    /**
     * Creates a simple conventional naming strategy.
     *
     * <p>This factory is useful for tests and tooling. It implements:
     * <ul>
     *   <li>generated package = basePackage + optional role segment</li>
     *   <li>type names = base + optional hint suffix</li>
     *   <li>member names = base (unchanged)</li>
     * </ul>
     *
     * @param basePackage base generated package (non-blank)
     * @return naming strategy
     */
    static NameStrategySpec simple(String basePackage) {
        Objects.requireNonNull(basePackage, "basePackage");
        String bp = basePackage.trim();
        if (bp.isEmpty()) throw new IllegalArgumentException("basePackage must not be blank");

        return new NameStrategySpec() {
            @Override
            public String basePackage() {
                return bp;
            }

            @Override
            public String packageName(String domainPackage, NameRole role, String hint) {
                Objects.requireNonNull(role, "role");
                // Keep it conservative: only append role when it is PACKAGE.
                if (role == NameRole.PACKAGE && hint != null && !hint.isBlank()) {
                    return bp + "." + sanitizeSegment(hint);
                }
                return bp;
            }

            @Override
            public String simpleName(String baseSimpleName, NameRole role, String hint) {
                Objects.requireNonNull(role, "role");
                String base = requireNonBlank(baseSimpleName, "baseSimpleName");
                if (hint == null || hint.isBlank()) return base;
                // Append hint as suffix to keep names readable and deterministic.
                return base + sanitizeTypeSuffix(hint);
            }

            @Override
            public String memberName(String base, NameRole role, String hint) {
                Objects.requireNonNull(role, "role");
                return requireNonBlank(base, "base");
            }

            @Override
            public String resourcePath(String base, NameRole role, String hint) {
                Objects.requireNonNull(role, "role");
                return requireNonBlank(base, "base");
            }

            @Override
            public String docPath(String base, NameRole role, String hint) {
                Objects.requireNonNull(role, "role");
                return requireNonBlank(base, "base");
            }
        };
    }

    private static String requireNonBlank(String v, String label) {
        Objects.requireNonNull(v, label);
        String t = v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return t;
    }

    private static String sanitizeSegment(String raw) {
        String t = raw.trim().toLowerCase();
        // conservative: keep alphanumerics and dashes/underscores, convert others to underscore.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        String s = sb.toString();
        // avoid empty segments
        return s.isEmpty() ? "generated" : s;
    }

    private static String sanitizeTypeSuffix(String raw) {
        String t = raw.trim();
        // turn "jpa-entity" into "JpaEntity"
        StringBuilder sb = new StringBuilder();
        boolean upperNext = true;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(upperNext ? Character.toUpperCase(c) : c);
                upperNext = false;
            } else {
                upperNext = true;
            }
        }
        String s = sb.toString();
        return s.isEmpty() ? "" : s;
    }
}
