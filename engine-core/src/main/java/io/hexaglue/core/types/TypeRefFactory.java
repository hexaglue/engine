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
 * Factory for creating {@link TypeRef} instances.
 *
 * <p>
 * This factory provides convenient methods to construct type references without
 * directly instantiating SPI types. It ensures proper validation and defaults.
 * </p>
 *
 * <h2>Common Types</h2>
 * <p>
 * The factory provides constants for frequently used types:
 * <ul>
 *   <li>{@link #OBJECT} - java.lang.Object</li>
 *   <li>{@link #STRING} - java.lang.String</li>
 *   <li>{@link #INT} - int primitive</li>
 *   <li>{@link #BOOLEAN} - boolean primitive</li>
 *   <li>{@link #VOID} - void primitive</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe. Factory constants are immutable.
 * </p>
 */
public final class TypeRefFactory {

    // Common primitive types
    public static final PrimitiveRef VOID = PrimitiveRef.of("void");
    public static final PrimitiveRef BOOLEAN = PrimitiveRef.of("boolean");
    public static final PrimitiveRef BYTE = PrimitiveRef.of("byte");
    public static final PrimitiveRef SHORT = PrimitiveRef.of("short");
    public static final PrimitiveRef INT = PrimitiveRef.of("int");
    public static final PrimitiveRef LONG = PrimitiveRef.of("long");
    public static final PrimitiveRef FLOAT = PrimitiveRef.of("float");
    public static final PrimitiveRef DOUBLE = PrimitiveRef.of("double");
    public static final PrimitiveRef CHAR = PrimitiveRef.of("char");

    // Common class types
    public static final ClassRef OBJECT = ClassRef.of("java.lang.Object");
    public static final ClassRef STRING = ClassRef.of("java.lang.String");
    public static final ClassRef INTEGER = ClassRef.of("java.lang.Integer");
    public static final ClassRef LONG_WRAPPER = ClassRef.of("java.lang.Long");
    public static final ClassRef BOOLEAN_WRAPPER = ClassRef.of("java.lang.Boolean");
    public static final ClassRef DOUBLE_WRAPPER = ClassRef.of("java.lang.Double");

    private TypeRefFactory() {
        // utility class
    }

    /**
     * Creates a primitive type reference.
     *
     * @param keyword primitive keyword (not blank)
     * @return primitive reference (never {@code null})
     */
    public static PrimitiveRef primitive(String keyword) {
        Objects.requireNonNull(keyword, "keyword");
        return PrimitiveRef.of(keyword);
    }

    /**
     * Creates a class type reference.
     *
     * @param qualifiedName qualified name (not blank)
     * @return class reference (never {@code null})
     */
    public static ClassRef classRef(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return ClassRef.of(qualifiedName);
    }

    /**
     * Creates a class type reference with nullability.
     *
     * @param qualifiedName qualified name (not blank)
     * @param nullability   nullability (not {@code null})
     * @return class reference (never {@code null})
     */
    public static ClassRef classRef(String qualifiedName, Nullability nullability) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(nullability, "nullability");
        return ClassRef.of(qualifiedName).withNullability(nullability);
    }

    /**
     * Creates an array type reference.
     *
     * @param component component type (not {@code null})
     * @return array reference (never {@code null})
     */
    public static ArrayRef arrayOf(TypeRef component) {
        Objects.requireNonNull(component, "component");
        return ArrayRef.of(component);
    }

    /**
     * Creates an array type reference with nullability.
     *
     * @param component   component type (not {@code null})
     * @param nullability nullability (not {@code null})
     * @return array reference (never {@code null})
     */
    public static ArrayRef arrayOf(TypeRef component, Nullability nullability) {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(nullability, "nullability");
        return ArrayRef.of(component).withNullability(nullability);
    }

    /**
     * Creates a parameterized type reference.
     *
     * @param rawType       raw class type (not {@code null})
     * @param typeArguments type arguments (not empty)
     * @return parameterized reference (never {@code null})
     */
    public static ParameterizedRef parameterized(ClassRef rawType, List<TypeRef> typeArguments) {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(typeArguments, "typeArguments");
        return ParameterizedRef.of(rawType, typeArguments);
    }

    /**
     * Creates a parameterized type reference.
     *
     * @param rawType raw class type (not {@code null})
     * @param args    type arguments (varargs, not empty)
     * @return parameterized reference (never {@code null})
     */
    public static ParameterizedRef parameterized(ClassRef rawType, TypeRef... args) {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(args, "args");
        return ParameterizedRef.of(rawType, List.of(args));
    }

    /**
     * Creates an unbounded wildcard {@code ?}.
     *
     * @return wildcard reference (never {@code null})
     */
    public static WildcardRef wildcard() {
        return WildcardRef.unbounded();
    }

    /**
     * Creates a wildcard {@code ? extends <upper>}.
     *
     * @param upper upper bound (not {@code null})
     * @return wildcard reference (never {@code null})
     */
    public static WildcardRef wildcardExtends(TypeRef upper) {
        Objects.requireNonNull(upper, "upper");
        return WildcardRef.extendsBound(upper);
    }

    /**
     * Creates a wildcard {@code ? super <lower>}.
     *
     * @param lower lower bound (not {@code null})
     * @return wildcard reference (never {@code null})
     */
    public static WildcardRef wildcardSuper(TypeRef lower) {
        Objects.requireNonNull(lower, "lower");
        return WildcardRef.superBound(lower);
    }

    /**
     * Creates a type variable reference.
     *
     * @param name   variable name (not blank)
     * @param bounds bounds (nullable, defaults to empty)
     * @return type variable reference (never {@code null})
     */
    public static TypeVariableRef typeVariable(String name, List<TypeRef> bounds) {
        Objects.requireNonNull(name, "name");
        List<TypeRef> b = (bounds == null) ? List.of() : bounds;
        return new TypeVariableRef(io.hexaglue.spi.types.TypeName.of(name.trim()), b, Nullability.UNSPECIFIED);
    }

    /**
     * Returns the boxed (wrapper) type for a primitive.
     *
     * @param primitive primitive type (not {@code null})
     * @return boxed class reference (never {@code null})
     */
    public static ClassRef box(PrimitiveRef primitive) {
        Objects.requireNonNull(primitive, "primitive");

        String keyword = primitive.name().value();
        return switch (keyword) {
            case "void" -> classRef("java.lang.Void");
            case "boolean" -> classRef("java.lang.Boolean");
            case "byte" -> classRef("java.lang.Byte");
            case "short" -> classRef("java.lang.Short");
            case "int" -> classRef("java.lang.Integer");
            case "long" -> classRef("java.lang.Long");
            case "float" -> classRef("java.lang.Float");
            case "double" -> classRef("java.lang.Double");
            case "char" -> classRef("java.lang.Character");
            default -> throw new IllegalArgumentException("Unknown primitive: " + keyword);
        };
    }

    /**
     * Returns the unboxed (primitive) type for a wrapper class.
     *
     * @param wrapper wrapper class reference (not {@code null})
     * @return primitive reference, or null if not a wrapper
     */
    public static PrimitiveRef unbox(ClassRef wrapper) {
        Objects.requireNonNull(wrapper, "wrapper");

        String name = wrapper.name().value();
        return switch (name) {
            case "java.lang.Void", "Void" -> VOID;
            case "java.lang.Boolean", "Boolean" -> BOOLEAN;
            case "java.lang.Byte", "Byte" -> BYTE;
            case "java.lang.Short", "Short" -> SHORT;
            case "java.lang.Integer", "Integer" -> INT;
            case "java.lang.Long", "Long" -> LONG;
            case "java.lang.Float", "Float" -> FLOAT;
            case "java.lang.Double", "Double" -> DOUBLE;
            case "java.lang.Character", "Character" -> CHAR;
            default -> null;
        };
    }

    /**
     * Returns {@code true} if the class reference is a primitive wrapper.
     *
     * @param classRef class reference (not {@code null})
     * @return {@code true} if wrapper
     */
    public static boolean isWrapper(ClassRef classRef) {
        Objects.requireNonNull(classRef, "classRef");
        return unbox(classRef) != null;
    }
}
