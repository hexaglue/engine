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
 * Internal representation of a primitive type reference.
 *
 * <p>
 * Represents primitive Java types including:
 * <ul>
 *   <li>{@code boolean}</li>
 *   <li>{@code byte}</li>
 *   <li>{@code short}</li>
 *   <li>{@code int}</li>
 *   <li>{@code long}</li>
 *   <li>{@code float}</li>
 *   <li>{@code double}</li>
 *   <li>{@code char}</li>
 *   <li>{@code void}</li>
 * </ul>
 * </p>
 *
 * <h2>Design Notes</h2>
 * <p>
 * While primitives don't typically carry nullability information, the nullability
 * marker is preserved for consistency with the type system API. It is typically
 * {@link Nullability#UNSPECIFIED} for primitives.
 * </p>
 *
 * <h2>Validation</h2>
 * <p>
 * The constructor validates that the type name does not contain a dot (i.e., it must
 * be a simple keyword, not a qualified name).
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * PrimitiveTypeRef intType = PrimitiveTypeRef.of("int");
 * PrimitiveTypeRef voidType = PrimitiveTypeRef.of("void");
 * }</pre>
 */
public final class PrimitiveTypeRef extends BaseTypeRef {

    /**
     * Constructs a primitive type reference.
     *
     * @param name        primitive keyword (not {@code null}, must not contain dots)
     * @param nullability nullability marker (not {@code null})
     * @param typeMirror  optional underlying type mirror (may be {@code null})
     * @throws IllegalArgumentException if the name contains a dot
     */
    public PrimitiveTypeRef(TypeName name, Nullability nullability, TypeMirror typeMirror) {
        super(name, nullability, typeMirror);
        validatePrimitiveName(name);
    }

    /**
     * Creates a primitive type reference from a keyword.
     *
     * @param keyword primitive keyword (not blank)
     * @return primitive type reference with {@link Nullability#UNSPECIFIED}
     */
    public static PrimitiveTypeRef of(String keyword) {
        Objects.requireNonNull(keyword, "keyword");
        return new PrimitiveTypeRef(TypeName.of(keyword.trim()), Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a primitive type reference with a type mirror.
     *
     * @param keyword    primitive keyword (not blank)
     * @param typeMirror underlying type mirror (may be {@code null})
     * @return primitive type reference with {@link Nullability#UNSPECIFIED}
     */
    public static PrimitiveTypeRef of(String keyword, TypeMirror typeMirror) {
        Objects.requireNonNull(keyword, "keyword");
        return new PrimitiveTypeRef(TypeName.of(keyword.trim()), Nullability.UNSPECIFIED, typeMirror);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.PRIMITIVE;
    }

    @Override
    public PrimitiveTypeRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        if (this.nullability().equals(nullability)) {
            return this;
        }
        return new PrimitiveTypeRef(name(), nullability, typeMirror().orElse(null));
    }

    @Override
    public TypeRef toSpiType() {
        return io.hexaglue.spi.types.PrimitiveRef.of(name().value()).withNullability(nullability());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PrimitiveTypeRef other)) return false;
        return name().equals(other.name()) && nullability().equals(other.nullability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), nullability());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private static void validatePrimitiveName(TypeName name) {
        String value = name.value();
        if (value.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Primitive type name must not be qualified: " + value);
        }
    }
}
