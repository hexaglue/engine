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
package io.hexaglue.core;

/**
 * Provides HexaGlue core version information.
 *
 * <p>
 * This class is primarily intended for diagnostics, generated headers, and build tooling.
 * It is deliberately small, immutable, and JDK-only.
 * </p>
 *
 * <h2>Versioning</h2>
 * <p>
 * The value returned by {@link #coreVersion()} is the version of the HexaGlue core artifact
 * currently on the classpath. When built with Maven/Gradle, it should match the module's
 * published version.
 * </p>
 */
public final class HexaGlueCoreVersion {

    private static final String UNKNOWN = "UNKNOWN";

    private HexaGlueCoreVersion() {
        // static-only
    }

    /**
     * Returns the HexaGlue core version string.
     *
     * <p>
     * The default implementation reads the version from the package metadata (Implementation-Version).
     * If that metadata is not available (for example in IDE runs), {@code "UNKNOWN"} is returned.
     * </p>
     *
     * @return the core version, never {@code null}
     */
    public static String coreVersion() {
        Package p = HexaGlueCoreVersion.class.getPackage();
        if (p == null) {
            return UNKNOWN;
        }
        String v = p.getImplementationVersion();
        return (v == null || v.isBlank()) ? UNKNOWN : v;
    }

    /**
     * Returns a short, human-friendly label for the core artifact.
     *
     * @return the label, never {@code null}
     */
    public static String displayName() {
        return "HexaGlue Core";
    }
}
