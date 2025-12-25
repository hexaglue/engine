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
package io.hexaglue.spi.diagnostics;

import java.util.Objects;

/**
 * Stable identifier for a diagnostic category.
 *
 * <p>Codes are intended to be stable across versions to allow:
 * <ul>
 *   <li>documentation references</li>
 *   <li>IDE filtering/suppression</li>
 *   <li>build tooling rules</li>
 * </ul>
 *
 * <p>Recommended format:
 * <ul>
 *   <li>Core: {@code "HG-CORE-xxxx"}</li>
 *   <li>Plugins: {@code "HG-<PLUGIN>-xxxx"} or {@code "<pluginId>-xxxx"}</li>
 * </ul>
 */
public final class DiagnosticCode {

    private final String value;

    private DiagnosticCode(String value) {
        this.value = requireNonBlank(value, "value");
    }

    /**
     * Creates a diagnostic code from a stable string.
     *
     * @param value code string (non-blank)
     * @return diagnostic code
     */
    public static DiagnosticCode of(String value) {
        return new DiagnosticCode(value);
    }

    /** @return the code string */
    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DiagnosticCode other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    private static String requireNonBlank(String v, String label) {
        Objects.requireNonNull(v, label);
        String t = v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return t;
    }
}
