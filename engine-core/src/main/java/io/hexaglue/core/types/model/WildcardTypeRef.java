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
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

/**
 * Internal representation of a wildcard type argument.
 *
 * <p>
 * Represents wildcard types such as:
 * <ul>
 *   <li>{@code ?} (unbounded)</li>
 *   <li>{@code ? extends Number} (upper bound)</li>
 *   <li>{@code ? super Integer} (lower bound)</li>
 * </ul>
 * </p>
 *
 * <h2>Bounds</h2>
 * <p>
 * A wildcard can have at most one bound, which is either:
 * <ul>
 *   <li>An upper bound (extends): the wildcard represents an unknown subtype of the bound</li>
 *   <li>A lower bound (super): the wildcard represents an unknown supertype of the bound</li>
 *   <li>No bound: the wildcard represents any type</li>
 * </ul>
 * </p>
 *
 * <h2>Validation</h2>
 * <p>
 * The constructor validates that a wildcard cannot have both an upper and a lower bound
 * simultaneously, as this is not valid in Java.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Unbounded wildcard: ?
 * WildcardTypeRef unbounded = WildcardTypeRef.unbounded();
 *
 * // Upper bound: ? extends Number
 * ClassTypeRef numberType = ClassTypeRef.of("java.lang.Number");
 * WildcardTypeRef extendsNumber = WildcardTypeRef.extendsBound(numberType);
 *
 * // Lower bound: ? super Integer
 * ClassTypeRef integerType = ClassTypeRef.of("java.lang.Integer");
 * WildcardTypeRef superInteger = WildcardTypeRef.superBound(integerType);
 * }</pre>
 */
public final class WildcardTypeRef extends BaseTypeRef {

    private final BaseTypeRef upperBound;
    private final BaseTypeRef lowerBound;

    /**
     * Constructs a wildcard type reference.
     *
     * @param upperBound  upper bound if present (may be {@code null}, exclusive with lowerBound)
     * @param lowerBound  lower bound if present (may be {@code null}, exclusive with upperBound)
     * @param nullability nullability marker (not {@code null})
     * @param typeMirror  optional underlying type mirror (may be {@code null})
     * @throws IllegalArgumentException if both upperBound and lowerBound are non-null
     */
    public WildcardTypeRef(
            BaseTypeRef upperBound, BaseTypeRef lowerBound, Nullability nullability, TypeMirror typeMirror) {
        super(TypeName.of("?"), nullability, typeMirror);
        if (upperBound != null && lowerBound != null) {
            throw new IllegalArgumentException("A wildcard cannot have both upperBound and lowerBound");
        }
        this.upperBound = upperBound;
        this.lowerBound = lowerBound;
    }

    /**
     * Creates an unbounded wildcard {@code ?}.
     *
     * @return wildcard reference with {@link Nullability#UNSPECIFIED}
     */
    public static WildcardTypeRef unbounded() {
        return new WildcardTypeRef(null, null, Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates an unbounded wildcard with a type mirror.
     *
     * @param typeMirror underlying type mirror (may be {@code null})
     * @return wildcard reference with {@link Nullability#UNSPECIFIED}
     */
    public static WildcardTypeRef unbounded(TypeMirror typeMirror) {
        return new WildcardTypeRef(null, null, Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Creates a wildcard {@code ? extends <upper>}.
     *
     * @param upper upper bound (not {@code null})
     * @return wildcard reference with {@link Nullability#UNSPECIFIED}
     */
    public static WildcardTypeRef extendsBound(BaseTypeRef upper) {
        Objects.requireNonNull(upper, "upper");
        return new WildcardTypeRef(upper, null, Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a wildcard {@code ? extends <upper>} with a type mirror.
     *
     * @param upper      upper bound (not {@code null})
     * @param typeMirror underlying type mirror (may be {@code null})
     * @return wildcard reference with {@link Nullability#UNSPECIFIED}
     */
    public static WildcardTypeRef extendsBound(BaseTypeRef upper, TypeMirror typeMirror) {
        Objects.requireNonNull(upper, "upper");
        return new WildcardTypeRef(upper, null, Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Creates a wildcard {@code ? super <lower>}.
     *
     * @param lower lower bound (not {@code null})
     * @return wildcard reference with {@link Nullability#UNSPECIFIED}
     */
    public static WildcardTypeRef superBound(BaseTypeRef lower) {
        Objects.requireNonNull(lower, "lower");
        return new WildcardTypeRef(null, lower, Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a wildcard {@code ? super <lower>} with a type mirror.
     *
     * @param lower      lower bound (not {@code null})
     * @param typeMirror underlying type mirror (may be {@code null})
     * @return wildcard reference with {@link Nullability#UNSPECIFIED}
     */
    public static WildcardTypeRef superBound(BaseTypeRef lower, TypeMirror typeMirror) {
        Objects.requireNonNull(lower, "lower");
        return new WildcardTypeRef(null, lower, Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Returns the upper bound if present.
     *
     * @return upper bound
     */
    public Optional<BaseTypeRef> upperBound() {
        return Optional.ofNullable(upperBound);
    }

    /**
     * Returns the lower bound if present.
     *
     * @return lower bound
     */
    public Optional<BaseTypeRef> lowerBound() {
        return Optional.ofNullable(lowerBound);
    }

    /**
     * Returns {@code true} if this wildcard is unbounded.
     *
     * @return {@code true} if no bounds are present
     */
    public boolean isUnbounded() {
        return upperBound == null && lowerBound == null;
    }

    @Override
    public TypeKind kind() {
        return TypeKind.WILDCARD;
    }

    @Override
    public WildcardTypeRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        if (this.nullability().equals(nullability)) {
            return this;
        }
        return new WildcardTypeRef(
                upperBound, lowerBound, nullability, typeMirror().orElse(null));
    }

    @Override
    public String render() {
        if (upperBound != null) {
            return "? extends " + upperBound.render();
        }
        if (lowerBound != null) {
            return "? super " + lowerBound.render();
        }
        return "?";
    }

    @Override
    public TypeRef toSpiType() {
        TypeRef spiUpper = upperBound != null ? upperBound.toSpiType() : null;
        TypeRef spiLower = lowerBound != null ? lowerBound.toSpiType() : null;

        return new io.hexaglue.spi.types.WildcardRef(spiUpper, spiLower, nullability());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WildcardTypeRef other)) return false;
        return Objects.equals(upperBound, other.upperBound)
                && Objects.equals(lowerBound, other.lowerBound)
                && nullability().equals(other.nullability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(upperBound, lowerBound, nullability());
    }
}
