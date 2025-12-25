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

import java.util.List;
import java.util.Objects;

/**
 * Type variable reference (e.g., {@code T}).
 *
 * <p>Bounds are optional and primarily used for documentation and advanced code generation.</p>
 *
 * @param name variable name (non-blank, typically a single identifier like {@code "T"})
 * @param bounds upper bounds (may be empty)
 * @param nullability nullability marker
 */
public record TypeVariableRef(TypeName name, List<TypeRef> bounds, Nullability nullability) implements TypeRef {

    public TypeVariableRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(nullability, "nullability");
        if (name.value().indexOf('.') >= 0) {
            throw new IllegalArgumentException("Type variable name must not be qualified: " + name.value());
        }
        bounds = List.copyOf(bounds);
        for (TypeRef b : bounds) {
            Objects.requireNonNull(b, "bounds contains null");
        }
    }

    /**
     * Creates a type variable with no bounds.
     *
     * @param name variable name (non-blank)
     * @return type variable ref with {@link Nullability#UNSPECIFIED}
     */
    public static TypeVariableRef of(String name) {
        Objects.requireNonNull(name, "name");
        return new TypeVariableRef(TypeName.of(name.trim()), List.of(), Nullability.UNSPECIFIED);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.TYPE_VARIABLE;
    }

    @Override
    public TypeVariableRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return new TypeVariableRef(name, bounds, nullability);
    }

    @Override
    public String render() {
        return name.value();
    }
}
