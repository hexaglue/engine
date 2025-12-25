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
package io.hexaglue.core.frontend.jsr269;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

/**
 * Utility methods for working with JSR-269 {@link TypeMirror} instances.
 *
 * <p>
 * This class provides convenient operations on type mirrors that complement
 * the standard {@link Types} utility with additional helpers and safe casting.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * The JSR-269 type system requires careful type checking and casting. This utility
 * provides:
 * </p>
 * <ul>
 *   <li>Safe type casting with {@link Optional} return types</li>
 *   <li>Common type kind checks</li>
 *   <li>Type hierarchy navigation</li>
 *   <li>Simplified access to type components</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe. However, {@link TypeMirror} instances
 * are only valid within the annotation processing round that created them.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * TypeMirror typeMirror = ...;
 *
 * // Safe type casting
 * Optional<DeclaredType> declaredType = Jsr269Types.asDeclaredType(typeMirror);
 *
 * // Check type properties
 * if (Jsr269Types.isPrimitive(typeMirror)) {
 *     PrimitiveType primitiveType = Jsr269Types.asPrimitiveType(typeMirror).get();
 * }
 *
 * // Access type components
 * if (Jsr269Types.isArray(typeMirror)) {
 *     TypeMirror componentType = Jsr269Types.getArrayComponentType(typeMirror).get();
 * }
 * }</pre>
 *
 * @see Types
 * @see TypeMirror
 */
public final class Jsr269Types {

    private Jsr269Types() {
        // utility class
    }

    /**
     * Returns the type mirror as a {@link DeclaredType} if applicable.
     *
     * @param typeMirror type mirror to cast (not {@code null})
     * @return declared type if applicable
     */
    public static Optional<DeclaredType> asDeclaredType(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror instanceof DeclaredType dt ? Optional.of(dt) : Optional.empty();
    }

    /**
     * Returns the type mirror as a {@link PrimitiveType} if applicable.
     *
     * @param typeMirror type mirror to cast (not {@code null})
     * @return primitive type if applicable
     */
    public static Optional<PrimitiveType> asPrimitiveType(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror instanceof PrimitiveType pt ? Optional.of(pt) : Optional.empty();
    }

    /**
     * Returns the type mirror as an {@link ArrayType} if applicable.
     *
     * @param typeMirror type mirror to cast (not {@code null})
     * @return array type if applicable
     */
    public static Optional<ArrayType> asArrayType(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror instanceof ArrayType at ? Optional.of(at) : Optional.empty();
    }

    /**
     * Returns the type mirror as a {@link TypeVariable} if applicable.
     *
     * @param typeMirror type mirror to cast (not {@code null})
     * @return type variable if applicable
     */
    public static Optional<TypeVariable> asTypeVariable(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror instanceof TypeVariable tv ? Optional.of(tv) : Optional.empty();
    }

    /**
     * Returns the type mirror as a {@link WildcardType} if applicable.
     *
     * @param typeMirror type mirror to cast (not {@code null})
     * @return wildcard type if applicable
     */
    public static Optional<WildcardType> asWildcardType(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror instanceof WildcardType wt ? Optional.of(wt) : Optional.empty();
    }

    /**
     * Returns whether the type mirror represents a primitive type.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if primitive
     */
    public static boolean isPrimitive(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror.getKind().isPrimitive();
    }

    /**
     * Returns whether the type mirror represents a declared type.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if declared type
     */
    public static boolean isDeclared(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror.getKind() == TypeKind.DECLARED;
    }

    /**
     * Returns whether the type mirror represents an array type.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if array
     */
    public static boolean isArray(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror.getKind() == TypeKind.ARRAY;
    }

    /**
     * Returns whether the type mirror represents a type variable.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if type variable
     */
    public static boolean isTypeVariable(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror.getKind() == TypeKind.TYPEVAR;
    }

    /**
     * Returns whether the type mirror represents a wildcard type.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if wildcard
     */
    public static boolean isWildcard(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror.getKind() == TypeKind.WILDCARD;
    }

    /**
     * Returns whether the type mirror represents void.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if void
     */
    public static boolean isVoid(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return typeMirror.getKind() == TypeKind.VOID;
    }

    /**
     * Returns the type element of a declared type.
     *
     * @param typeMirror type mirror to query (not {@code null})
     * @return type element if the type is a declared type
     */
    public static Optional<TypeElement> getTypeElement(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");

        return asDeclaredType(typeMirror).map(dt -> (TypeElement) dt.asElement());
    }

    /**
     * Returns the qualified name of a declared type.
     *
     * @param typeMirror type mirror to query (not {@code null})
     * @return qualified name if the type is a declared type
     */
    public static Optional<String> getQualifiedName(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");

        return getTypeElement(typeMirror).map(te -> te.getQualifiedName().toString());
    }

    /**
     * Returns the component type of an array.
     *
     * @param typeMirror type mirror to query (not {@code null})
     * @return component type if the type is an array
     */
    public static Optional<TypeMirror> getArrayComponentType(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");

        return asArrayType(typeMirror).map(ArrayType::getComponentType);
    }

    /**
     * Returns the type arguments of a parameterized type.
     *
     * @param typeMirror type mirror to query (not {@code null})
     * @return type arguments if the type is a parameterized declared type (never {@code null})
     */
    public static List<? extends TypeMirror> getTypeArguments(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");

        return asDeclaredType(typeMirror).map(DeclaredType::getTypeArguments).orElse(List.of());
    }

    /**
     * Returns whether a declared type has type arguments.
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @return {@code true} if the type has type arguments
     */
    public static boolean hasTypeArguments(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        return !getTypeArguments(typeMirror).isEmpty();
    }

    /**
     * Returns the erasure of a type (raw type without type parameters).
     *
     * @param typeMirror type mirror to erase (not {@code null})
     * @param types      type utilities (not {@code null})
     * @return erased type (never {@code null})
     */
    public static TypeMirror erasure(TypeMirror typeMirror, Types types) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        Objects.requireNonNull(types, "types");
        return types.erasure(typeMirror);
    }

    /**
     * Returns whether two types are the same.
     *
     * @param type1 first type (not {@code null})
     * @param type2 second type (not {@code null})
     * @param types type utilities (not {@code null})
     * @return {@code true} if types are the same
     */
    public static boolean isSameType(TypeMirror type1, TypeMirror type2, Types types) {
        Objects.requireNonNull(type1, "type1");
        Objects.requireNonNull(type2, "type2");
        Objects.requireNonNull(types, "types");
        return types.isSameType(type1, type2);
    }

    /**
     * Returns whether type1 is assignable to type2.
     *
     * @param type1 first type (not {@code null})
     * @param type2 second type (not {@code null})
     * @param types type utilities (not {@code null})
     * @return {@code true} if type1 is assignable to type2
     */
    public static boolean isAssignable(TypeMirror type1, TypeMirror type2, Types types) {
        Objects.requireNonNull(type1, "type1");
        Objects.requireNonNull(type2, "type2");
        Objects.requireNonNull(types, "types");
        return types.isAssignable(type1, type2);
    }

    /**
     * Returns whether the type is a subtype of another type.
     *
     * @param subtype   potential subtype (not {@code null})
     * @param supertype potential supertype (not {@code null})
     * @param types     type utilities (not {@code null})
     * @return {@code true} if subtype is a subtype of supertype
     */
    public static boolean isSubtype(TypeMirror subtype, TypeMirror supertype, Types types) {
        Objects.requireNonNull(subtype, "subtype");
        Objects.requireNonNull(supertype, "supertype");
        Objects.requireNonNull(types, "types");
        return types.isSubtype(subtype, supertype);
    }

    /**
     * Returns the boxed type for a primitive type.
     *
     * @param primitiveType primitive type (not {@code null})
     * @param types         type utilities (not {@code null})
     * @return boxed type (never {@code null})
     */
    public static TypeMirror boxedClass(PrimitiveType primitiveType, Types types) {
        Objects.requireNonNull(primitiveType, "primitiveType");
        Objects.requireNonNull(types, "types");
        return types.boxedClass(primitiveType).asType();
    }

    /**
     * Returns the unboxed primitive type for a wrapper class.
     *
     * @param wrapperType wrapper class type (not {@code null})
     * @param types       type utilities (not {@code null})
     * @return primitive type if the type is a wrapper class
     */
    public static Optional<PrimitiveType> unboxedType(TypeMirror wrapperType, Types types) {
        Objects.requireNonNull(wrapperType, "wrapperType");
        Objects.requireNonNull(types, "types");

        try {
            PrimitiveType primitiveType = types.unboxedType(wrapperType);
            return Optional.of(primitiveType);
        } catch (IllegalArgumentException e) {
            // Not a wrapper type
            return Optional.empty();
        }
    }

    /**
     * Returns whether the type is a wrapper class (boxed primitive).
     *
     * @param typeMirror type mirror to check (not {@code null})
     * @param types      type utilities (not {@code null})
     * @return {@code true} if wrapper class
     */
    public static boolean isWrapperType(TypeMirror typeMirror, Types types) {
        Objects.requireNonNull(typeMirror, "typeMirror");
        Objects.requireNonNull(types, "types");
        return unboxedType(typeMirror, types).isPresent();
    }
}
