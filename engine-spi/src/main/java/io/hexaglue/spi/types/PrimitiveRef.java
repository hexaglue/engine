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

/**
 * Primitive type reference.
 *
 * <p>Includes {@code void} as a primitive keyword for simplicity.</p>
 *
 * @param name primitive keyword (e.g., {@code "int"}, {@code "boolean"}, {@code "void"})
 * @param nullability nullability marker (typically {@link Nullability#UNSPECIFIED})
 */
public record PrimitiveRef(TypeName name, Nullability nullability) implements TypeRef {

    public PrimitiveRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(nullability, "nullability");
        String v = name.value();
        // Keep validation minimal but strict enough to avoid accidental class names.
        if (v.indexOf('.') >= 0) {
            throw new IllegalArgumentException("Primitive type name must not be qualified: " + v);
        }
    }

    /**
     * Creates a primitive reference.
     *
     * @param keyword primitive keyword (non-blank)
     * @return primitive ref with {@link Nullability#UNSPECIFIED}
     */
    public static PrimitiveRef of(String keyword) {
        Objects.requireNonNull(keyword, "keyword");
        return new PrimitiveRef(TypeName.of(keyword.trim()), Nullability.UNSPECIFIED);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.PRIMITIVE;
    }

    @Override
    public PrimitiveRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return new PrimitiveRef(name, nullability);
    }

    @Override
    public String render() {
        return name.value();
    }
}
