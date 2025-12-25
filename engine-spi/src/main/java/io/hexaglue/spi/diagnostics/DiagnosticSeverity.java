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

/**
 * Severity of a diagnostic emitted by HexaGlue core or plugins.
 *
 * <p>Severity is used for reporting and for deciding whether a build should fail.
 * The decision policy (e.g., "warnings as errors") belongs to the compiler integration.</p>
 */
public enum DiagnosticSeverity {

    /**
     * Informational message. Does not indicate a problem.
     */
    INFO,

    /**
     * Warning about a potentially problematic configuration or design.
     */
    WARNING,

    /**
     * Error that prevents correct generation or indicates an invalid model.
     */
    ERROR
}
