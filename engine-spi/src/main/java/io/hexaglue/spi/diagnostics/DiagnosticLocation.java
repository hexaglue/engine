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
import java.util.Optional;

/**
 * Location information for a diagnostic.
 *
 * <p>This SPI type intentionally avoids exposing compiler-specific element handles.
 * Implementations may still map these locations to real source positions (line/column)
 * internally.</p>
 *
 * <p>All fields are optional; absence means "unknown".</p>
 */
public final class DiagnosticLocation {

    private static final DiagnosticLocation UNKNOWN = new DiagnosticLocation(null, null, null, null);

    private final String qualifiedName;
    private final String path;
    private final Integer line;
    private final Integer column;

    private DiagnosticLocation(String qualifiedName, String path, Integer line, Integer column) {
        this.qualifiedName = normalizeBlankToNull(qualifiedName);
        this.path = normalizeBlankToNull(path);
        this.line = (line == null || line <= 0) ? null : line;
        this.column = (column == null || column <= 0) ? null : column;
    }

    /**
     * Returns an "unknown" location.
     *
     * @return unknown location singleton
     */
    public static DiagnosticLocation unknown() {
        return UNKNOWN;
    }

    /**
     * Creates a location that points to a program element by qualified name.
     *
     * @param qualifiedName qualified name (e.g., {@code "com.example.CustomerRepository"}) (nullable)
     * @return location
     */
    public static DiagnosticLocation ofQualifiedName(String qualifiedName) {
        return new DiagnosticLocation(qualifiedName, null, null, null);
    }

    /**
     * Creates a location that points to a source path and optional line/column.
     *
     * @param path source path (e.g., {@code "src/main/java/.../Foo.java"}) (nullable)
     * @param line 1-based line number (nullable)
     * @param column 1-based column number (nullable)
     * @return location
     */
    public static DiagnosticLocation ofPath(String path, Integer line, Integer column) {
        return new DiagnosticLocation(null, path, line, column);
    }

    /**
     * Creates a location with both qualified name and path (best-effort).
     *
     * @param qualifiedName qualified name (nullable)
     * @param path path (nullable)
     * @param line 1-based line number (nullable)
     * @param column 1-based column number (nullable)
     * @return location
     */
    public static DiagnosticLocation of(String qualifiedName, String path, Integer line, Integer column) {
        return new DiagnosticLocation(qualifiedName, path, line, column);
    }

    /** @return qualified name if known */
    public Optional<String> qualifiedName() {
        return Optional.ofNullable(qualifiedName);
    }

    /** @return source path if known */
    public Optional<String> path() {
        return Optional.ofNullable(path);
    }

    /** @return 1-based line number if known */
    public Optional<Integer> line() {
        return Optional.ofNullable(line);
    }

    /** @return 1-based column number if known */
    public Optional<Integer> column() {
        return Optional.ofNullable(column);
    }

    /**
     * Returns {@code true} if no location information is present.
     *
     * @return {@code true} if unknown
     */
    public boolean isUnknown() {
        return qualifiedName == null && path == null && line == null && column == null;
    }

    @Override
    public String toString() {
        if (isUnknown()) return "<unknown>";
        StringBuilder sb = new StringBuilder();
        if (qualifiedName != null) sb.append(qualifiedName);
        if (path != null) {
            if (sb.length() > 0) sb.append(" @ ");
            sb.append(path);
        }
        if (line != null) sb.append(":").append(line);
        if (column != null) sb.append(":").append(column);
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DiagnosticLocation other)) return false;
        return Objects.equals(qualifiedName, other.qualifiedName)
                && Objects.equals(path, other.path)
                && Objects.equals(line, other.line)
                && Objects.equals(column, other.column);
    }

    @Override
    public int hashCode() {
        int r = Objects.hashCode(qualifiedName);
        r = 31 * r + Objects.hashCode(path);
        r = 31 * r + Objects.hashCode(line);
        r = 31 * r + Objects.hashCode(column);
        return r;
    }

    private static String normalizeBlankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
