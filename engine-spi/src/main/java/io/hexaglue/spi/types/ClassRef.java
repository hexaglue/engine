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
 * Non-parameterized class or interface type reference.
 *
 * <p>The name should typically be qualified (e.g., {@code "java.util.List"}), but the SPI
 * does not mandate that: some contexts may only provide a simple name.</p>
 *
 * @param name type name (qualified or simple)
 * @param nullability nullability marker
 */
public record ClassRef(TypeName name, Nullability nullability) implements TypeRef {

    public ClassRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(nullability, "nullability");
    }

    /**
     * Creates a class reference from a type name string.
     *
     * @param qualifiedOrSimpleName class name (non-blank)
     * @return class ref with {@link Nullability#UNSPECIFIED}
     */
    public static ClassRef of(String qualifiedOrSimpleName) {
        Objects.requireNonNull(qualifiedOrSimpleName, "qualifiedOrSimpleName");
        return new ClassRef(TypeName.of(qualifiedOrSimpleName.trim()), Nullability.UNSPECIFIED);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.CLASS;
    }

    @Override
    public ClassRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return new ClassRef(name, nullability);
    }

    @Override
    public String render() {
        return name.value();
    }

    /**
     * Returns the qualified name if this name is qualified.
     *
     * @return qualified name if available
     */
    public Optional<String> qualifiedName() {
        return name.isQualified() ? Optional.of(name.value()) : Optional.empty();
    }
}
