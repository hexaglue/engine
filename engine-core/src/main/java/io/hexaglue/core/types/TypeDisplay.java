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
package io.hexaglue.core.types;

import io.hexaglue.spi.types.ArrayRef;
import io.hexaglue.spi.types.ClassRef;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.ParameterizedRef;
import io.hexaglue.spi.types.PrimitiveRef;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.TypeVariableRef;
import io.hexaglue.spi.types.WildcardRef;
import java.util.List;
import java.util.Objects;

/**
 * Utilities for rendering and displaying {@link TypeRef} instances.
 *
 * <p>
 * This class provides methods to:
 * <ul>
 *   <li>Render types as Java source code</li>
 *   <li>Render simple names (without packages)</li>
 *   <li>Format type lists</li>
 *   <li>Create human-readable descriptions</li>
 * </ul>
 * </p>
 *
 * <h2>Rendering Modes</h2>
 *
 * <h3>Qualified Rendering</h3>
 * <p>
 * Uses fully-qualified names where available. This is the default mode and ensures
 * no naming conflicts.
 * </p>
 * <pre>
 * java.util.List&lt;java.lang.String&gt;
 * </pre>
 *
 * <h3>Simple Rendering</h3>
 * <p>
 * Uses simple names only. Useful for display purposes but may be ambiguous.
 * </p>
 * <pre>
 * List&lt;String&gt;
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class TypeDisplay {

    private TypeDisplay() {
        // utility class
    }

    /**
     * Renders a type reference as Java source code (fully qualified).
     *
     * <p>
     * This method delegates to {@link TypeRef#render()} by default.
     * </p>
     *
     * @param type type to render (not {@code null})
     * @return Java source representation (never blank)
     */
    public static String render(TypeRef type) {
        Objects.requireNonNull(type, "type");
        return type.render();
    }

    /**
     * Renders a type reference using simple names only.
     *
     * <p>
     * Example: {@code java.util.List<String>} becomes {@code List<String>}.
     * </p>
     *
     * @param type type to render (not {@code null})
     * @return simple name representation (never blank)
     */
    public static String renderSimple(TypeRef type) {
        Objects.requireNonNull(type, "type");

        return switch (type.kind()) {
            case PRIMITIVE -> ((PrimitiveRef) type).name().value();
            case CLASS -> simpleName(((ClassRef) type).name().value());
            case ARRAY -> renderSimple(((ArrayRef) type).componentType()) + "[]";
            case PARAMETERIZED -> renderSimpleParameterized((ParameterizedRef) type);
            case WILDCARD -> renderSimpleWildcard((WildcardRef) type);
            case TYPE_VARIABLE -> ((TypeVariableRef) type).name().value();
        };
    }

    /**
     * Renders a type list as comma-separated Java source.
     *
     * <p>
     * Example: {@code [String, Integer]} becomes {@code "String, Integer"}.
     * </p>
     *
     * @param types types to render (not {@code null})
     * @return comma-separated type list (never {@code null})
     */
    public static String renderList(List<? extends TypeRef> types) {
        Objects.requireNonNull(types, "types");
        if (types.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(render(types.get(i)));
        }
        return sb.toString();
    }

    /**
     * Renders a type list as comma-separated simple names.
     *
     * @param types types to render (not {@code null})
     * @return comma-separated simple name list (never {@code null})
     */
    public static String renderListSimple(List<? extends TypeRef> types) {
        Objects.requireNonNull(types, "types");
        if (types.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < types.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(renderSimple(types.get(i)));
        }
        return sb.toString();
    }

    /**
     * Creates a human-readable description of a type.
     *
     * <p>
     * This method provides additional context beyond simple rendering, such as
     * nullability markers if present.
     * </p>
     *
     * @param type type to describe (not {@code null})
     * @return description (never blank)
     */
    public static String describe(TypeRef type) {
        Objects.requireNonNull(type, "type");

        String base = renderSimple(type);
        if (type.nullability() == Nullability.NULLABLE) {
            return base + " (nullable)";
        }
        if (type.nullability() == Nullability.NONNULL) {
            return base + " (non-null)";
        }
        return base;
    }

    /**
     * Creates a concise description suitable for error messages.
     *
     * <p>
     * Example: {@code "List<String>"} or {@code "int"}.
     * </p>
     *
     * @param type type to describe (not {@code null})
     * @return concise description (never blank)
     */
    public static String describeConcise(TypeRef type) {
        Objects.requireNonNull(type, "type");
        return renderSimple(type);
    }

    /**
     * Extracts the simple name from a possibly qualified name.
     *
     * <p>
     * Example: {@code "java.util.List"} becomes {@code "List"}.
     * </p>
     *
     * @param qualifiedOrSimple qualified or simple name (not {@code null})
     * @return simple name (never blank)
     */
    public static String simpleName(String qualifiedOrSimple) {
        Objects.requireNonNull(qualifiedOrSimple, "qualifiedOrSimple");
        int lastDot = qualifiedOrSimple.lastIndexOf('.');
        if (lastDot < 0) {
            return qualifiedOrSimple;
        }
        return qualifiedOrSimple.substring(lastDot + 1);
    }

    /**
     * Extracts the package name from a qualified name.
     *
     * <p>
     * Example: {@code "java.util.List"} becomes {@code "java.util"}.
     * </p>
     *
     * @param qualified qualified name (not {@code null})
     * @return package name, or empty string if not qualified
     */
    public static String packageName(String qualified) {
        Objects.requireNonNull(qualified, "qualified");
        int lastDot = qualified.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return qualified.substring(0, lastDot);
    }

    /**
     * Returns {@code true} if the name appears to be qualified.
     *
     * @param name name to check (not {@code null})
     * @return {@code true} if contains a dot
     */
    public static boolean isQualified(String name) {
        Objects.requireNonNull(name, "name");
        return name.indexOf('.') >= 0;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String renderSimpleParameterized(ParameterizedRef type) {
        StringBuilder sb = new StringBuilder();
        sb.append(simpleName(type.rawType().name().value()));
        sb.append("<");
        List<TypeRef> args = type.typeArguments();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(renderSimple(args.get(i)));
        }
        sb.append(">");
        return sb.toString();
    }

    private static String renderSimpleWildcard(WildcardRef type) {
        if (type.upperBoundOptional().isEmpty() && type.lowerBoundOptional().isEmpty()) {
            return "?";
        }
        if (type.upperBoundOptional().isPresent()) {
            return "? extends " + renderSimple(type.upperBoundOptional().orElseThrow());
        }
        return "? super " + renderSimple(type.lowerBoundOptional().orElseThrow());
    }
}
