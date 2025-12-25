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
package io.hexaglue.core.types.model;

import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeName;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * Internal representation of a type variable reference.
 *
 * <p>
 * Represents type parameters such as:
 * <ul>
 *   <li>{@code T} (unbounded)</li>
 *   <li>{@code E} (element type in collections)</li>
 *   <li>{@code T extends Number} (bounded)</li>
 *   <li>{@code T extends Comparable<T>} (recursive bound)</li>
 * </ul>
 * </p>
 *
 * <h2>Type Variables in Java</h2>
 * <p>
 * Type variables are placeholders for actual types that will be provided when the
 * generic type is instantiated. They are declared on:
 * <ul>
 *   <li>Classes and interfaces: {@code class Box<T> { ... }}</li>
 *   <li>Methods: {@code <T> void process(T item) { ... }}</li>
 *   <li>Constructors: {@code <T> MyClass(T item) { ... }}</li>
 * </ul>
 * </p>
 *
 * <h2>Bounds</h2>
 * <p>
 * Type variables can have zero or more upper bounds. Multiple bounds are specified
 * using {@code &}:
 * <pre>{@code
 * <T extends Number & Comparable<T>>
 * }</pre>
 * The bounds restrict what types can be used as type arguments.
 * </p>
 *
 * <h2>Validation</h2>
 * <p>
 * The constructor validates that:
 * <ul>
 *   <li>The name is not qualified (type variables are simple identifiers)</li>
 *   <li>All bounds are non-null</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe. The bounds list is defensively copied.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Unbounded type variable: T
 * TypeVariableTypeRef tVar = TypeVariableTypeRef.of("T");
 *
 * // Bounded type variable: T extends Number
 * ClassTypeRef numberType = ClassTypeRef.of("java.lang.Number");
 * TypeVariableTypeRef boundedT = TypeVariableTypeRef.of("T", List.of(numberType));
 * }</pre>
 */
public final class TypeVariableTypeRef extends BaseTypeRef {

    private final List<BaseTypeRef> bounds;

    /**
     * Constructs a type variable reference.
     *
     * @param name        variable name (not {@code null}, must be a simple identifier)
     * @param bounds      upper bounds (may be empty, all elements non-null)
     * @param nullability nullability marker (not {@code null})
     * @param typeMirror  optional underlying type mirror (may be {@code null})
     * @throws IllegalArgumentException if the name is qualified
     */
    public TypeVariableTypeRef(
            TypeName name, List<BaseTypeRef> bounds, Nullability nullability, TypeMirror typeMirror) {
        super(name, nullability, typeMirror);
        validateTypeVariableName(name);
        Objects.requireNonNull(bounds, "bounds");

        this.bounds = List.copyOf(bounds);
        for (BaseTypeRef bound : this.bounds) {
            Objects.requireNonNull(bound, "bounds contains null");
        }
    }

    /**
     * Creates a type variable reference with no bounds.
     *
     * @param name variable name (not blank)
     * @return type variable reference with {@link Nullability#UNSPECIFIED}
     */
    public static TypeVariableTypeRef of(String name) {
        Objects.requireNonNull(name, "name");
        return new TypeVariableTypeRef(TypeName.of(name.trim()), List.of(), Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a type variable reference with bounds.
     *
     * @param name   variable name (not blank)
     * @param bounds upper bounds (may be empty)
     * @return type variable reference with {@link Nullability#UNSPECIFIED}
     */
    public static TypeVariableTypeRef of(String name, List<BaseTypeRef> bounds) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bounds, "bounds");
        return new TypeVariableTypeRef(TypeName.of(name.trim()), bounds, Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a type variable reference with a type mirror.
     *
     * @param name       variable name (not blank)
     * @param bounds     upper bounds (may be empty)
     * @param typeMirror underlying type mirror (may be {@code null})
     * @return type variable reference with {@link Nullability#UNSPECIFIED}
     */
    public static TypeVariableTypeRef of(String name, List<BaseTypeRef> bounds, TypeMirror typeMirror) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bounds, "bounds");
        return new TypeVariableTypeRef(TypeName.of(name.trim()), bounds, Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Creates a type variable reference with nullability.
     *
     * @param name        variable name (not blank)
     * @param bounds      upper bounds (may be empty)
     * @param nullability nullability marker (not {@code null})
     * @param typeMirror  underlying type mirror (may be {@code null})
     * @return type variable reference
     */
    public static TypeVariableTypeRef of(
            String name, List<BaseTypeRef> bounds, Nullability nullability, TypeMirror typeMirror) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(nullability, "nullability");
        return new TypeVariableTypeRef(TypeName.of(name.trim()), bounds, nullability, typeMirror);
    }

    /**
     * Returns the upper bounds of this type variable.
     *
     * @return bounds (may be empty, immutable)
     */
    public List<BaseTypeRef> bounds() {
        return bounds;
    }

    /**
     * Returns {@code true} if this type variable has no explicit bounds.
     *
     * <p>
     * Note that unbounded type variables implicitly have {@code Object} as their bound,
     * but this method returns {@code true} if no explicit bounds were declared.
     * </p>
     *
     * @return {@code true} if no bounds are present
     */
    public boolean isUnbounded() {
        return bounds.isEmpty();
    }

    @Override
    public TypeKind kind() {
        return TypeKind.TYPE_VARIABLE;
    }

    @Override
    public TypeVariableTypeRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        if (this.nullability().equals(nullability)) {
            return this;
        }
        return new TypeVariableTypeRef(name(), bounds, nullability, typeMirror().orElse(null));
    }

    @Override
    public String render() {
        return name().value();
    }

    @Override
    public TypeRef toSpiType() {
        List<TypeRef> spiBounds = bounds.stream().map(BaseTypeRef::toSpiType).toList();

        return new io.hexaglue.spi.types.TypeVariableRef(name(), spiBounds, nullability());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TypeVariableTypeRef other)) return false;
        return name().equals(other.name())
                && bounds.equals(other.bounds)
                && nullability().equals(other.nullability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), bounds, nullability());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private static void validateTypeVariableName(TypeName name) {
        String value = name.value();
        if (value.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Type variable name must not be qualified: " + value);
        }
    }
}
