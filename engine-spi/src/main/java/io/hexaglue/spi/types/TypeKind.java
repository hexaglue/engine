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

/**
 * Structural kind of a {@link TypeRef}.
 *
 * <p>This enumeration is intentionally small and stable. It is not meant to model all
 * language features; it models the minimal set HexaGlue plugins commonly need.</p>
 */
public enum TypeKind {

    /** Primitive type, including {@code void}. */
    PRIMITIVE,

    /** A non-parameterized class/interface type. */
    CLASS,

    /** An array type. */
    ARRAY,

    /** A parameterized class/interface type. */
    PARAMETERIZED,

    /** A wildcard type argument (e.g., {@code ?}, {@code ? extends Number}, {@code ? super T}). */
    WILDCARD,

    /** A type variable (e.g., {@code T}, {@code E}). */
    TYPE_VARIABLE
}
