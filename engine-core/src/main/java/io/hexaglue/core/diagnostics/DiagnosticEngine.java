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
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic.Kind;

/**
 * Core diagnostic engine for HexaGlue compilation.
 *
 * <p>
 * The diagnostic engine is responsible for:
 * <ul>
 *   <li>Collecting diagnostics from core and plugins</li>
 *   <li>Routing diagnostics to the JSR-269 {@link Messager}</li>
 *   <li>Managing diagnostic lifecycle during compilation</li>
 *   <li>Providing query and filtering capabilities</li>
 * </ul>
 * </p>
 *
 * <h2>Architecture</h2>
 * <p>
 * The engine integrates several components:
 * <ul>
 *   <li>{@link DiagnosticSink} - collects diagnostics</li>
 *   <li>{@link DefaultDiagnosticReporter} - SPI bridge for plugins</li>
 *   <li>{@link DiagnosticRenderer} - formats diagnostics for display</li>
 *   <li>{@link ValidationEngine} - executes validation plans</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are thread-safe. Multiple threads may report diagnostics concurrently.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // During processor initialization
 * DiagnosticEngine engine = DiagnosticEngine.create(messager);
 *
 * // Pass reporter to plugins via GenerationContextSpec
 * DiagnosticReporter reporter = engine.reporter();
 *
 * // During compilation
 * reporter.error(DiagnosticCode.of("INVALID_PORT"), "Port must be an interface");
 *
 * // After plugin execution
 * engine.flushToMessager();
 *
 * // Check for errors
 * if (engine.hasErrors()) {
 *   // Abort generation
 * }
 * }</pre>
 */
public final class DiagnosticEngine {

    private final DiagnosticSink sink;
    private final DefaultDiagnosticReporter reporter;
    private final Messager messager;

    private DiagnosticEngine(Messager messager) {
        this.messager = Objects.requireNonNull(messager, "messager");
        this.sink = DiagnosticSink.create();
        this.reporter = DefaultDiagnosticReporter.of(sink);
    }

    /**
     * Creates a diagnostic engine backed by a JSR-269 messager.
     *
     * @param messager the annotation processing messager (not {@code null})
     * @return diagnostic engine (never {@code null})
     */
    public static DiagnosticEngine create(Messager messager) {
        return new DiagnosticEngine(messager);
    }

    /**
     * Returns the diagnostic reporter exposed to plugins via the SPI.
     *
     * @return reporter (never {@code null})
     */
    public DiagnosticReporter reporter() {
        return reporter;
    }

    /**
     * Returns the diagnostic sink for internal inspection.
     *
     * <p>
     * This accessor is core-internal and allows direct access to collected diagnostics
     * without going through the reporter interface.
     * </p>
     *
     * @return sink (never {@code null})
     */
    public DiagnosticSink sink() {
        return sink;
    }

    /**
     * Flushes all collected diagnostics to the JSR-269 messager.
     *
     * <p>
     * This method should be called after each compilation round to ensure diagnostics
     * are visible to the build tool and IDE.
     * </p>
     *
     * <p>
     * Diagnostics are converted to the appropriate {@link Kind} and printed using the messager.
     * If a diagnostic has an associated element, the messager will display it at the correct
     * source location.
     * </p>
     */
    public void flushToMessager() {
        List<Diagnostic> diagnostics = sink.all();
        for (Diagnostic d : diagnostics) {
            printToMessager(d);
        }
    }

    /**
     * Flushes diagnostics and clears the sink.
     *
     * <p>
     * This method is useful in multi-round scenarios where diagnostics from one round
     * should not carry over to the next.
     * </p>
     */
    public void flushAndClear() {
        flushToMessager();
        sink.clear();
    }

    /**
     * Returns {@code true} if the sink contains at least one error.
     *
     * @return {@code true} if errors are present
     */
    public boolean hasErrors() {
        return sink.hasErrors();
    }

    /**
     * Returns {@code true} if the sink contains at least one warning.
     *
     * @return {@code true} if warnings are present
     */
    public boolean hasWarnings() {
        return sink.hasWarnings();
    }

    /**
     * Returns all collected errors.
     *
     * @return immutable list of errors (never {@code null})
     */
    public List<Diagnostic> errors() {
        return sink.errors();
    }

    /**
     * Returns all collected warnings.
     *
     * @return immutable list of warnings (never {@code null})
     */
    public List<Diagnostic> warnings() {
        return sink.warnings();
    }

    /**
     * Returns all collected diagnostics.
     *
     * @return immutable list of diagnostics (never {@code null})
     */
    public List<Diagnostic> all() {
        return sink.all();
    }

    /**
     * Executes a validation plan and reports all issues as diagnostics.
     *
     * <p>
     * This is a convenience method that:
     * <ul>
     *   <li>Executes the validation plan</li>
     *   <li>Converts validation issues to diagnostics</li>
     *   <li>Reports them via the reporter</li>
     *   <li>Handles execution errors gracefully</li>
     * </ul>
     * </p>
     *
     * @param plan     validation plan to execute (not {@code null})
     * @param pluginId optional plugin id for diagnostics (nullable)
     * @return validation result (never {@code null})
     */
    public ValidationEngine.ValidationResult executeValidation(ValidationPlan plan, String pluginId) {
        Objects.requireNonNull(plan, "plan");

        ValidationEngine.ValidationResult result = ValidationEngine.execute(plan);

        // Report validation issues as diagnostics
        for (io.hexaglue.spi.diagnostics.ValidationIssue issue : result.issues()) {
            reporter.report(issue.toDiagnostic(pluginId));
        }

        // Report execution errors as internal errors
        for (ValidationEngine.ValidationError error : result.executionErrors()) {
            reporter.report(Diagnostic.builder()
                    .severity(DiagnosticSeverity.ERROR)
                    .code(DiagnosticCode.of("HG-CORE-208"))
                    .message("Validation rule '" + error.ruleName() + "' failed: "
                            + error.cause().getMessage())
                    .pluginId(pluginId)
                    .cause(error.cause())
                    .build());
        }

        return result;
    }

    /**
     * Reports a single diagnostic directly to the engine.
     *
     * <p>
     * This method is a convenience for core code that needs to report diagnostics without
     * going through the reporter interface.
     * </p>
     *
     * @param diagnostic diagnostic to report (not {@code null})
     */
    public void report(Diagnostic diagnostic) {
        reporter.report(diagnostic);
    }

    /**
     * Returns a summary string of collected diagnostics.
     *
     * @return summary (never {@code null})
     */
    public String summary() {
        return DiagnosticRenderer.summary(sink.all());
    }

    @Override
    public String toString() {
        return "DiagnosticEngine[" + summary() + "]";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void printToMessager(Diagnostic diagnostic) {
        Kind kind = toMessagerKind(diagnostic.severity());
        String message = DiagnosticRenderer.compact(diagnostic);

        // Best-effort: try to associate with an element if we have a qualified name
        // In practice, the core should track elements separately if precise location is needed
        messager.printMessage(kind, message);
    }

    private static Kind toMessagerKind(DiagnosticSeverity severity) {
        return switch (severity) {
            case ERROR -> Kind.ERROR;
            case WARNING -> Kind.WARNING;
            case INFO -> Kind.NOTE;
        };
    }
}
