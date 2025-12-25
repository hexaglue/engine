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
package io.hexaglue.spi.naming;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a qualified name (typically a Java fully-qualified name).
 *
 * <p>This is a small value type used throughout the SPI to avoid spreading raw string logic
 * (trimming, validation, splitting) across plugins.</p>
 *
 * <p>This type is JDK-only and intentionally minimal.</p>
 */
public final class QualifiedName implements Comparable<QualifiedName> {

    private final String value;

    private QualifiedName(String value) {
        this.value = requireNonBlank(value, "value");
    }

    /**
     * Creates a qualified name.
     *
     * @param value fully-qualified name (non-blank)
     * @return qualified name
     */
    public static QualifiedName of(String value) {
        return new QualifiedName(value);
    }

    /**
     * Returns the raw qualified name string.
     *
     * @return qualified name string
     */
    public String value() {
        return value;
    }

    /**
     * Returns the package portion of this name, if any.
     *
     * <p>For {@code "com.example.Foo"} this returns {@code "com.example"}.</p>
     *
     * @return package name if present
     */
    public Optional<String> packageName() {
        int idx = value.lastIndexOf('.');
        if (idx <= 0) return Optional.empty();
        return Optional.of(value.substring(0, idx));
    }

    /**
     * Returns the simple name portion of this name.
     *
     * <p>For {@code "com.example.Foo"} this returns {@code "Foo"}.</p>
     *
     * @return simple name (never blank)
     */
    public String simpleName() {
        int idx = value.lastIndexOf('.');
        if (idx < 0) return value;
        return value.substring(idx + 1);
    }

    /**
     * Returns the enclosing qualified name (everything before the last dot), if any.
     *
     * <p>For {@code "com.example.Outer.Inner"} this returns {@code "com.example.Outer"}.</p>
     *
     * @return enclosing qualified name if present
     */
    public Optional<QualifiedName> enclosing() {
        int idx = value.lastIndexOf('.');
        if (idx <= 0) return Optional.empty();
        return Optional.of(QualifiedName.of(value.substring(0, idx)));
    }

    @Override
    public int compareTo(QualifiedName o) {
        return this.value.compareTo(o.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof QualifiedName other)) return false;
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
