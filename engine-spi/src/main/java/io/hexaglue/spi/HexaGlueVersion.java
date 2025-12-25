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
package io.hexaglue.spi;

import java.util.Objects;

/**
 * Represents the HexaGlue core version that a plugin can declare compatibility against.
 *
 * <p>This class is intentionally small and dependency-free.</p>
 */
public final class HexaGlueVersion implements Comparable<HexaGlueVersion> {

    /**
     * The SPI version. Bump this when you make breaking changes in the SPI.
     *
     * <p>It can be used by plugins to detect incompatibilities at compile time or runtime.</p>
     */
    public static final int SPI_VERSION = 1;

    private final int major;
    private final int minor;
    private final int patch;

    private HexaGlueVersion(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version components must be >= 0.");
        }
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    /**
     * Creates a semantic version.
     *
     * @param major major version (>= 0)
     * @param minor minor version (>= 0)
     * @param patch patch version (>= 0)
     * @return a version instance
     */
    public static HexaGlueVersion of(int major, int minor, int patch) {
        return new HexaGlueVersion(major, minor, patch);
    }

    /**
     * Parses a version string formatted as {@code "MAJOR.MINOR.PATCH"}.
     *
     * @param value version string
     * @return parsed version
     * @throws IllegalArgumentException if the value cannot be parsed
     */
    public static HexaGlueVersion parse(String value) {
        Objects.requireNonNull(value, "value");
        String v = value.trim();
        String[] parts = v.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid version: '" + value + "'. Expected MAJOR.MINOR.PATCH.");
        }
        try {
            int maj = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            int pat = Integer.parseInt(parts[2]);
            return of(maj, min, pat);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version: '" + value + "'. Components must be integers.", e);
        }
    }

    /** @return major version component */
    public int major() {
        return major;
    }

    /** @return minor version component */
    public int minor() {
        return minor;
    }

    /** @return patch version component */
    public int patch() {
        return patch;
    }

    /**
     * Returns whether this version is greater than or equal to the provided version.
     *
     * @param other other version
     * @return {@code true} if this >= other
     */
    public boolean atLeast(HexaGlueVersion other) {
        return compareTo(Objects.requireNonNull(other, "other")) >= 0;
    }

    @Override
    public int compareTo(HexaGlueVersion o) {
        int c = Integer.compare(this.major, o.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, o.minor);
        if (c != 0) return c;
        return Integer.compare(this.patch, o.patch);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HexaGlueVersion other)) return false;
        return major == other.major && minor == other.minor && patch == other.patch;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(major);
        result = 31 * result + Integer.hashCode(minor);
        result = 31 * result + Integer.hashCode(patch);
        return result;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
