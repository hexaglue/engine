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
import java.util.Objects;
import javax.lang.model.type.TypeMirror;

/**
 * Internal representation of an array type reference.
 *
 * <p>
 * Represents array types such as:
 * <ul>
 *   <li>{@code String[]}</li>
 *   <li>{@code int[]}</li>
 *   <li>{@code List<String>[]}</li>
 *   <li>{@code int[][]} (multidimensional arrays)</li>
 * </ul>
 * </p>
 *
 * <h2>Component Type</h2>
 * <p>
 * The component type is the element type of the array. For multidimensional arrays,
 * the component type is itself an array type.
 * </p>
 * <pre>{@code
 * // For String[]
 * componentType = ClassTypeRef("String")
 *
 * // For int[][]
 * componentType = ArrayTypeRef(PrimitiveTypeRef("int"))
 * }</pre>
 *
 * <h2>Nullability</h2>
 * <p>
 * The nullability marker applies to the array reference itself, not to the component type.
 * For example, {@code String[] nullable} means the array can be null, but individual
 * elements may or may not be null based on the component type's nullability.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * BaseTypeRef stringType = ClassTypeRef.of("java.lang.String");
 * ArrayTypeRef stringArray = ArrayTypeRef.of(stringType);
 * ArrayTypeRef stringMatrix = ArrayTypeRef.of(stringArray); // String[][]
 * }</pre>
 */
public final class ArrayTypeRef extends BaseTypeRef {

    private final BaseTypeRef componentType;

    /**
     * Constructs an array type reference.
     *
     * @param componentType component type (not {@code null})
     * @param nullability   nullability marker for the array reference (not {@code null})
     * @param typeMirror    optional underlying type mirror (may be {@code null})
     */
    public ArrayTypeRef(BaseTypeRef componentType, Nullability nullability, TypeMirror typeMirror) {
        super(computeName(componentType), nullability, typeMirror);
        this.componentType = Objects.requireNonNull(componentType, "componentType");
    }

    /**
     * Creates an array type reference.
     *
     * @param componentType component type (not {@code null})
     * @return array type reference with {@link Nullability#UNSPECIFIED}
     */
    public static ArrayTypeRef of(BaseTypeRef componentType) {
        Objects.requireNonNull(componentType, "componentType");
        return new ArrayTypeRef(componentType, Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates an array type reference with a type mirror.
     *
     * @param componentType component type (not {@code null})
     * @param typeMirror    underlying type mirror (may be {@code null})
     * @return array type reference with {@link Nullability#UNSPECIFIED}
     */
    public static ArrayTypeRef of(BaseTypeRef componentType, TypeMirror typeMirror) {
        Objects.requireNonNull(componentType, "componentType");
        return new ArrayTypeRef(componentType, Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Creates an array type reference with nullability.
     *
     * @param componentType component type (not {@code null})
     * @param nullability   nullability marker (not {@code null})
     * @param typeMirror    underlying type mirror (may be {@code null})
     * @return array type reference
     */
    public static ArrayTypeRef of(BaseTypeRef componentType, Nullability nullability, TypeMirror typeMirror) {
        Objects.requireNonNull(componentType, "componentType");
        Objects.requireNonNull(nullability, "nullability");
        return new ArrayTypeRef(componentType, nullability, typeMirror);
    }

    /**
     * Returns the component type of this array.
     *
     * @return component type (never {@code null})
     */
    public BaseTypeRef componentType() {
        return componentType;
    }

    @Override
    public TypeKind kind() {
        return TypeKind.ARRAY;
    }

    @Override
    public ArrayTypeRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        if (this.nullability().equals(nullability)) {
            return this;
        }
        return new ArrayTypeRef(componentType, nullability, typeMirror().orElse(null));
    }

    @Override
    public String render() {
        return componentType.render() + "[]";
    }

    @Override
    public TypeRef toSpiType() {
        return io.hexaglue.spi.types.ArrayRef.of(componentType.toSpiType()).withNullability(nullability());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ArrayTypeRef other)) return false;
        return componentType.equals(other.componentType) && nullability().equals(other.nullability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentType, nullability());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static TypeName computeName(BaseTypeRef componentType) {
        return TypeName.of(componentType.name().value() + "[]");
    }
}
