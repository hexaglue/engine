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
package io.hexaglue.core.internal.ir;

import java.util.Objects;
import java.util.Optional;

/**
 * Stable reference to a source program location, independent from JSR-269 {@code Element}
 * instances which are not suitable for long-lived storage or deep equality comparisons.
 *
 * <p>This reference is intentionally best-effort: path/line/column may be absent depending
 * on the frontend capabilities.</p>
 */
public final class SourceRef {

    /**
     * The kind of referenced program element.
     */
    public enum Kind {
        TYPE,
        METHOD,
        FIELD,
        PARAMETER,
        PACKAGE,
        UNKNOWN
    }

    private final Kind kind;
    private final String qualifiedName;
    private final String path;
    private final Integer line;
    private final Integer column;
    private final String origin;
    private final String hint;

    private SourceRef(Builder b) {
        this.kind = Objects.requireNonNull(b.kind, "kind");
        this.qualifiedName = Objects.requireNonNull(b.qualifiedName, "qualifiedName");
        this.path = b.path;
        this.line = b.line;
        this.column = b.column;
        this.origin = b.origin == null ? "unknown" : b.origin;
        this.hint = b.hint;
    }

    /**
     * Creates a new builder.
     *
     * @param kind element kind
     * @param qualifiedName stable qualified name (e.g. {@code com.acme.Customer})
     * @return builder
     */
    public static Builder builder(Kind kind, String qualifiedName) {
        return new Builder(kind, qualifiedName);
    }

    /** @return referenced element kind */
    public Kind kind() {
        return kind;
    }

    /** @return stable qualified name (never blank) */
    public String qualifiedName() {
        return qualifiedName;
    }

    /** @return best-effort source path (may be absent) */
    public Optional<String> path() {
        return Optional.ofNullable(path);
    }

    /** @return best-effort 1-based line number (may be absent) */
    public Optional<Integer> line() {
        return Optional.ofNullable(line);
    }

    /** @return best-effort 1-based column number (may be absent) */
    public Optional<Integer> column() {
        return Optional.ofNullable(column);
    }

    /** @return origin tag, e.g. {@code jsr269} */
    public String origin() {
        return origin;
    }

    /** @return optional human hint to improve messages */
    public Optional<String> hint() {
        return Optional.ofNullable(hint);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceRef other)) return false;
        return kind == other.kind
                && qualifiedName.equals(other.qualifiedName)
                && Objects.equals(path, other.path)
                && Objects.equals(line, other.line)
                && Objects.equals(column, other.column)
                && origin.equals(other.origin)
                && Objects.equals(hint, other.hint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, qualifiedName, path, line, column, origin, hint);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(kind).append(':').append(qualifiedName);
        if (path != null) {
            sb.append(" @ ").append(path);
            if (line != null) {
                sb.append(':').append(line);
                if (column != null) sb.append(':').append(column);
            }
        }
        if (hint != null && !hint.isBlank()) {
            sb.append(" (").append(hint).append(')');
        }
        return sb.toString();
    }

    /**
     * Builder for {@link SourceRef}.
     */
    public static final class Builder {
        private final Kind kind;
        private final String qualifiedName;

        private String path;
        private Integer line;
        private Integer column;
        private String origin;
        private String hint;

        private Builder(Kind kind, String qualifiedName) {
            this.kind = Objects.requireNonNull(kind, "kind");
            this.qualifiedName = requireNonBlank(qualifiedName, "qualifiedName");
        }

        /** @param path best-effort path such as {@code com/acme/Customer.java} */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /** @param line 1-based line number */
        public Builder line(Integer line) {
            this.line = line;
            return this;
        }

        /** @param column 1-based column number */
        public Builder column(Integer column) {
            this.column = column;
            return this;
        }

        /** @param origin origin tag, e.g. {@code jsr269} */
        public Builder origin(String origin) {
            this.origin = origin;
            return this;
        }

        /** @param hint optional human hint */
        public Builder hint(String hint) {
            this.hint = hint;
            return this;
        }

        /** @return immutable {@link SourceRef} */
        public SourceRef build() {
            return new SourceRef(this);
        }

        private static String requireNonBlank(String value, String label) {
            Objects.requireNonNull(value, label);
            if (value.isBlank()) {
                throw new IllegalArgumentException(label + " must not be blank");
            }
            return value;
        }
    }
}
