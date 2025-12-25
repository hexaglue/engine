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
package io.hexaglue.core.lifecycle;

/**
 * High-level phases of a HexaGlue compilation.
 *
 * <p>
 * These phases are conceptual and may span multiple annotation-processing rounds.
 * The core uses them to produce deterministic logs and to structure internal orchestration.
 * </p>
 */
public enum CompilationPhase {

    /**
     * Discover plugins on the compilation classpath.
     */
    DISCOVER_PLUGINS,

    /**
     * Analyze user sources and construct the internal IR.
     */
    ANALYZE,

    /**
     * Run validations and produce diagnostics.
     */
    VALIDATE,

    /**
     * Execute plugins to produce artifact plans (sources/resources/docs).
     */
    GENERATE,

    /**
     * Write artifacts to the compiler output (Filer/resources/docs).
     */
    WRITE,

    /**
     * Finalization (flush, summary, final checks).
     */
    FINISH
}
