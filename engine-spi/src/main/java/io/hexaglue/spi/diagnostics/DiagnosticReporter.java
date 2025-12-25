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

import io.hexaglue.spi.stability.Stable;
import java.util.List;
import java.util.Objects;

/**
 * Reporting interface for diagnostics.
 *
 * <p>Plugins should use this interface instead of throwing exceptions for user-caused errors.
 * The compiler is responsible for routing diagnostics to the appropriate backend
 * (JSR-269 Messager, logs, test harness, etc.).</p>
 *
 * <p>This interface is intentionally minimal and stable.</p>
 */
@Stable(since = "1.0.0")
public interface DiagnosticReporter {

    /**
     * Reports a diagnostic.
     *
     * @param diagnostic diagnostic to report (never {@code null})
     */
    void report(Diagnostic diagnostic);

    /**
     * Reports multiple diagnostics.
     *
     * @param diagnostics list of diagnostics (never {@code null})
     */
    default void reportAll(List<Diagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        for (Diagnostic d : diagnostics) {
            if (d != null) report(d);
        }
    }

    /**
     * Convenience method to report an error.
     *
     * @param code diagnostic code
     * @param message message
     */
    default void error(DiagnosticCode code, String message) {
        report(Diagnostic.error(code, message));
    }

    /**
     * Convenience method to report a warning.
     *
     * @param code diagnostic code
     * @param message message
     */
    default void warning(DiagnosticCode code, String message) {
        report(Diagnostic.warning(code, message));
    }

    /**
     * Convenience method to report an info.
     *
     * @param code diagnostic code
     * @param message message
     */
    default void info(DiagnosticCode code, String message) {
        report(Diagnostic.info(code, message));
    }

    /**
     * Creates a simple reporter that accumulates diagnostics into a list.
     *
     * <p>This is useful for tests and tooling.</p>
     *
     * @param sink mutable list sink (never {@code null})
     * @return reporter
     */
    static DiagnosticReporter accumulating(List<Diagnostic> sink) {
        Objects.requireNonNull(sink, "sink");
        return sink::add;
    }
}
