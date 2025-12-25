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
import io.hexaglue.spi.types.ParameterizedRef;
import io.hexaglue.spi.types.PrimitiveRef;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.TypeVariableRef;
import io.hexaglue.spi.types.WildcardRef;
import java.util.Comparator;
import java.util.Objects;

/**
 * Comparators and equality utilities for {@link TypeRef} instances.
 *
 * <p>
 * This class provides:
 * <ul>
 *   <li>Structural equality checking (ignoring nullability)</li>
 *   <li>Deep equality checking (including nullability)</li>
 *   <li>Name-based comparators for sorting</li>
 *   <li>Type hierarchy checks</li>
 * </ul>
 * </p>
 *
 * <h2>Equality Semantics</h2>
 * <p>
 * Two type references are considered <strong>structurally equal</strong> if they represent
 * the same type structure, ignoring nullability markers. This is useful for type matching
 * where nullability is not semantically significant.
 * </p>
 *
 * <p>
 * Two type references are considered <strong>deeply equal</strong> if they are structurally
 * equal and have the same nullability.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class TypeComparators {

    private TypeComparators() {
        // utility class
    }

    /**
     * Returns {@code true} if two type references are structurally equal (ignoring nullability).
     *
     * @param a first type (not {@code null})
     * @param b second type (not {@code null})
     * @return {@code true} if structurally equal
     */
    public static boolean equalIgnoringNullability(TypeRef a, TypeRef b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        if (a.kind() != b.kind()) {
            return false;
        }

        return switch (a.kind()) {
            case PRIMITIVE -> equalPrimitive((PrimitiveRef) a, (PrimitiveRef) b);
            case CLASS -> equalClass((ClassRef) a, (ClassRef) b);
            case ARRAY -> equalArray((ArrayRef) a, (ArrayRef) b);
            case PARAMETERIZED -> equalParameterized((ParameterizedRef) a, (ParameterizedRef) b);
            case WILDCARD -> equalWildcard((WildcardRef) a, (WildcardRef) b);
            case TYPE_VARIABLE -> equalTypeVariable((TypeVariableRef) a, (TypeVariableRef) b);
        };
    }

    /**
     * Returns {@code true} if two type references are deeply equal (including nullability).
     *
     * @param a first type (not {@code null})
     * @param b second type (not {@code null})
     * @return {@code true} if deeply equal
     */
    public static boolean deepEquals(TypeRef a, TypeRef b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        return equalIgnoringNullability(a, b) && a.nullability() == b.nullability();
    }

    /**
     * Returns a comparator that sorts types by their rendered name.
     *
     * @return name-based comparator (never {@code null})
     */
    public static Comparator<TypeRef> byRenderedName() {
        return Comparator.comparing(TypeRef::render);
    }

    /**
     * Returns a comparator that sorts types by kind, then by name.
     *
     * @return kind-and-name comparator (never {@code null})
     */
    public static Comparator<TypeRef> byKindThenName() {
        return Comparator.comparing((TypeRef t) -> t.kind()).thenComparing(TypeRef::render);
    }

    /**
     * Returns {@code true} if the type is a primitive (excluding void).
     *
     * @param type type to check (not {@code null})
     * @return {@code true} if primitive (not void)
     */
    public static boolean isPrimitive(TypeRef type) {
        Objects.requireNonNull(type, "type");
        if (type.kind() != TypeKind.PRIMITIVE) {
            return false;
        }
        String name = type.name().value();
        return !"void".equals(name);
    }

    /**
     * Returns {@code true} if the type is void.
     *
     * @param type type to check (not {@code null})
     * @return {@code true} if void
     */
    public static boolean isVoid(TypeRef type) {
        Objects.requireNonNull(type, "type");
        return type.kind() == TypeKind.PRIMITIVE && "void".equals(type.name().value());
    }

    /**
     * Returns {@code true} if the type is a boolean primitive.
     *
     * @param type type to check (not {@code null})
     * @return {@code true} if boolean
     */
    public static boolean isBoolean(TypeRef type) {
        Objects.requireNonNull(type, "type");
        return type.kind() == TypeKind.PRIMITIVE && "boolean".equals(type.name().value());
    }

    /**
     * Returns {@code true} if the type is a numeric primitive (int, long, etc.).
     *
     * @param type type to check (not {@code null})
     * @return {@code true} if numeric primitive
     */
    public static boolean isNumericPrimitive(TypeRef type) {
        Objects.requireNonNull(type, "type");
        if (type.kind() != TypeKind.PRIMITIVE) {
            return false;
        }
        String name = type.name().value();
        return "byte".equals(name)
                || "short".equals(name)
                || "int".equals(name)
                || "long".equals(name)
                || "float".equals(name)
                || "double".equals(name)
                || "char".equals(name);
    }

    /**
     * Returns {@code true} if the type is {@code java.lang.String}.
     *
     * @param type type to check (not {@code null})
     * @return {@code true} if String
     */
    public static boolean isString(TypeRef type) {
        Objects.requireNonNull(type, "type");
        if (type.kind() != TypeKind.CLASS) {
            return false;
        }
        String name = type.name().value();
        return "java.lang.String".equals(name) || "String".equals(name);
    }

    /**
     * Returns {@code true} if the type is {@code java.lang.Object}.
     *
     * @param type type to check (not {@code null})
     * @return {@code true} if Object
     */
    public static boolean isObject(TypeRef type) {
        Objects.requireNonNull(type, "type");
        if (type.kind() != TypeKind.CLASS) {
            return false;
        }
        String name = type.name().value();
        return "java.lang.Object".equals(name) || "Object".equals(name);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal equality helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean equalPrimitive(PrimitiveRef a, PrimitiveRef b) {
        return a.name().value().equals(b.name().value());
    }

    private static boolean equalClass(ClassRef a, ClassRef b) {
        return a.name().value().equals(b.name().value());
    }

    private static boolean equalArray(ArrayRef a, ArrayRef b) {
        return equalIgnoringNullability(a.componentType(), b.componentType());
    }

    private static boolean equalParameterized(ParameterizedRef a, ParameterizedRef b) {
        if (!equalClass(a.rawType(), b.rawType())) {
            return false;
        }
        if (a.typeArguments().size() != b.typeArguments().size()) {
            return false;
        }
        for (int i = 0; i < a.typeArguments().size(); i++) {
            if (!equalIgnoringNullability(
                    a.typeArguments().get(i), b.typeArguments().get(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean equalWildcard(WildcardRef a, WildcardRef b) {
        if (a.upperBoundOptional().isPresent() != b.upperBoundOptional().isPresent()) {
            return false;
        }
        if (a.lowerBoundOptional().isPresent() != b.lowerBoundOptional().isPresent()) {
            return false;
        }
        if (a.upperBoundOptional().isPresent()) {
            return equalIgnoringNullability(
                    a.upperBoundOptional().orElseThrow(), b.upperBoundOptional().orElseThrow());
        }
        if (a.lowerBoundOptional().isPresent()) {
            return equalIgnoringNullability(
                    a.lowerBoundOptional().orElseThrow(), b.lowerBoundOptional().orElseThrow());
        }
        return true; // Both unbounded
    }

    private static boolean equalTypeVariable(TypeVariableRef a, TypeVariableRef b) {
        if (!a.name().value().equals(b.name().value())) {
            return false;
        }
        if (a.bounds().size() != b.bounds().size()) {
            return false;
        }
        for (int i = 0; i < a.bounds().size(); i++) {
            if (!equalIgnoringNullability(a.bounds().get(i), b.bounds().get(i))) {
                return false;
            }
        }
        return true;
    }
}
