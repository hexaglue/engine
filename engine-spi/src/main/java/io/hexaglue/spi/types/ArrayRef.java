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
 * Array type reference.
 *
 * @param componentType component type
 * @param nullability nullability marker for the array reference (not the component)
 */
public record ArrayRef(TypeRef componentType, Nullability nullability) implements TypeRef {

    public ArrayRef {
        Objects.requireNonNull(componentType, "componentType");
        Objects.requireNonNull(nullability, "nullability");
    }

    /**
     * Creates an array reference.
     *
     * @param componentType component type
     * @return array ref with {@link Nullability#UNSPECIFIED}
     */
    public static ArrayRef of(TypeRef componentType) {
        return new ArrayRef(componentType, Nullability.UNSPECIFIED);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.ARRAY;
    }

    @Override
    public TypeName name() {
        // Arrays do not have a standalone name; render uses componentType.
        return TypeName.of(componentType.name().value() + "[]");
    }

    @Override
    public ArrayRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return new ArrayRef(componentType, nullability);
    }

    @Override
    public String render() {
        return componentType.render() + "[]";
    }
}
