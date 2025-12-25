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
package io.hexaglue.spi.options;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a resolved option value.
 *
 * <p>Options may originate from multiple places (annotation attributes, compiler arguments,
 * configuration files, etc.). This SPI type provides a minimal and stable way to surface
 * both the value and its provenance.</p>
 *
 * <p>Decoding and validation are responsibilities of the compiler/SPI implementation.</p>
 *
 * @param <T> decoded value type
 * @param value decoded value (nullable when not present)
 * @param present whether the option was explicitly present
 * @param source a stable source description (e.g., "compiler-arg", "annotation", "config-file") (nullable)
 */
public record OptionValue<T>(T value, boolean present, String source) {

    /**
     * Creates a missing value.
     *
     * @param <T> value type
     * @return missing option value
     */
    public static <T> OptionValue<T> missing() {
        return new OptionValue<>(null, false, null);
    }

    /**
     * Creates a present value with an optional source description.
     *
     * @param value decoded value (may be null if the option explicitly sets null/empty semantics)
     * @param source stable source description (nullable)
     * @param <T> value type
     * @return present option value
     */
    public static <T> OptionValue<T> present(T value, String source) {
        String src = (source == null) ? null : source.trim();
        return new OptionValue<>(value, true, (src == null || src.isEmpty()) ? null : src);
    }

    /**
     * Convenience view for code that wants to treat missing as {@link Optional#empty()}.
     *
     * @return optional value (empty if not present)
     */
    public Optional<T> asOptional() {
        return present ? Optional.ofNullable(value) : Optional.empty();
    }

    public OptionValue {
        if (!present) {
            // Normalize: if not present, ignore any value/source
            value = null;
            source = null;
        } else {
            if (source != null) {
                String t = source.trim();
                source = t.isEmpty() ? null : t;
            }
        }
    }

    /**
     * Returns a human-friendly representation intended for logs and diagnostics.
     *
     * <p>This method avoids leaking implementation specifics. It does not serialize complex objects.</p>
     *
     * @return debug string
     */
    public String debugString() {
        if (!present) return "<missing>";
        String v = (value == null) ? "<null>" : value.toString();
        if (source == null) return v;
        return v + " (source=" + source + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OptionValue<?> other)) return false;
        return present == other.present && Objects.equals(value, other.value) && Objects.equals(source, other.source);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(present);
        result = 31 * result + Objects.hashCode(value);
        result = 31 * result + Objects.hashCode(source);
        return result;
    }
}
