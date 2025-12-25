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
 * Represents a type name.
 *
 * <p>This is a small value type used by the SPI to avoid spreading string parsing and
 * validation across plugins.</p>
 *
 * <p>Type names may be:
 * <ul>
 *   <li>qualified (e.g., {@code "java.util.List"})</li>
 *   <li>simple (e.g., {@code "List"})</li>
 *   <li>primitive keywords (e.g., {@code "int"}, {@code "void"})</li>
 * </ul>
 */
public final class TypeName implements Comparable<TypeName> {

    private final String value;

    private TypeName(String value) {
        this.value = requireNonBlank(value, "value");
    }

    /**
     * Creates a type name.
     *
     * @param value name (non-blank)
     * @return type name
     */
    public static TypeName of(String value) {
        return new TypeName(value);
    }

    /** @return raw name value */
    public String value() {
        return value;
    }

    /**
     * Returns the package name if this looks like a qualified name.
     *
     * @return package name if present
     */
    public Optional<String> packageName() {
        int idx = value.lastIndexOf('.');
        if (idx <= 0) return Optional.empty();
        return Optional.of(value.substring(0, idx));
    }

    /**
     * Returns the simple name portion of this type name.
     *
     * @return simple name
     */
    public String simpleName() {
        int idx = value.lastIndexOf('.');
        if (idx < 0) return value;
        return value.substring(idx + 1);
    }

    /**
     * Returns whether this type name looks qualified (contains a dot).
     *
     * @return {@code true} if qualified
     */
    public boolean isQualified() {
        return value.indexOf('.') >= 0;
    }

    @Override
    public int compareTo(TypeName o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TypeName other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    private static String requireNonBlank(String v, String label) {
        Objects.requireNonNull(v, label);
        String t = v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return t;
    }
}
