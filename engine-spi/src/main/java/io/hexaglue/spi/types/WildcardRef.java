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
package io.hexaglue.spi.types;

import java.util.Objects;
import java.util.Optional;

/**
 * Wildcard type argument.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code ?}</li>
 *   <li>{@code ? extends Number}</li>
 *   <li>{@code ? super T}</li>
 * </ul>
 *
 * @param upperBound upper bound if present (exclusive with lowerBound)
 * @param lowerBound lower bound if present (exclusive with upperBound)
 * @param nullability nullability marker (typically {@link Nullability#UNSPECIFIED})
 */
public record WildcardRef(TypeRef upperBound, TypeRef lowerBound, Nullability nullability) implements TypeRef {

    public WildcardRef {
        Objects.requireNonNull(nullability, "nullability");
        if (upperBound != null && lowerBound != null) {
            throw new IllegalArgumentException("A wildcard cannot have both upperBound and lowerBound.");
        }
    }

    /** @return unbounded wildcard {@code ?} */
    public static WildcardRef unbounded() {
        return new WildcardRef(null, null, Nullability.UNSPECIFIED);
    }

    /** @return wildcard {@code ? extends <upper>} */
    public static WildcardRef extendsBound(TypeRef upper) {
        Objects.requireNonNull(upper, "upper");
        return new WildcardRef(upper, null, Nullability.UNSPECIFIED);
    }

    /** @return wildcard {@code ? super <lower>} */
    public static WildcardRef superBound(TypeRef lower) {
        Objects.requireNonNull(lower, "lower");
        return new WildcardRef(null, lower, Nullability.UNSPECIFIED);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.WILDCARD;
    }

    @Override
    public TypeName name() {
        return TypeName.of("?");
    }

    /**
     * Optional view of {@link #upperBound()}.
     *
     * @return upper bound if present
     */
    public Optional<TypeRef> upperBoundOptional() {
        return Optional.ofNullable(upperBound);
    }

    /**
     * Optional view of {@link #lowerBound()}.
     *
     * @return lower bound if present
     */
    public Optional<TypeRef> lowerBoundOptional() {
        return Optional.ofNullable(lowerBound);
    }

    @Override
    public WildcardRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return new WildcardRef(upperBound, lowerBound, nullability);
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
}
