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
package io.hexaglue.core.diagnostics;

import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.Objects;

/**
 * Default implementation of {@link DiagnosticReporter} that routes diagnostics to a {@link DiagnosticSink}.
 *
 * <p>
 * This reporter acts as a bridge between the SPI interface (used by plugins) and the internal
 * diagnostic collection mechanism. All reported diagnostics are added to the sink for later
 * processing by the {@link DiagnosticEngine}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This reporter is thread-safe when backed by a thread-safe sink (which {@link DiagnosticSink} is).
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DiagnosticSink sink = DiagnosticSink.create();
 * DiagnosticReporter reporter = DefaultDiagnosticReporter.of(sink);
 *
 * // Pass reporter to plugins via GenerationContextSpec
 * reporter.error(DiagnosticCode.of("INVALID_PORT"), "Port must be an interface");
 * }</pre>
 */
public final class DefaultDiagnosticReporter implements DiagnosticReporter {

    private final DiagnosticSink sink;

    private DefaultDiagnosticReporter(DiagnosticSink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * Creates a reporter that routes diagnostics to the given sink.
     *
     * @param sink the target sink (not {@code null})
     * @return diagnostic reporter (never {@code null})
     */
    public static DefaultDiagnosticReporter of(DiagnosticSink sink) {
        return new DefaultDiagnosticReporter(sink);
    }

    @Override
    public void report(Diagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        sink.add(diagnostic);
    }

    /**
     * Returns the backing sink.
     *
     * <p>
     * This accessor is core-internal and allows the engine to inspect collected diagnostics.
     * Plugins do not have access to this method.
     * </p>
     *
     * @return sink (never {@code null})
     */
    public DiagnosticSink sink() {
        return sink;
    }

    @Override
    public String toString() {
        return "DefaultDiagnosticReporter[sink=" + sink + "]";
    }
}
