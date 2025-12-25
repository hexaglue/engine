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
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Thread-safe collector for diagnostics emitted during compilation.
 *
 * <p>
 * The sink accumulates diagnostics as they are reported and provides query methods to inspect
 * collected diagnostics by severity or predicate.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is thread-safe. Multiple threads may add diagnostics concurrently.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DiagnosticSink sink = DiagnosticSink.create();
 * sink.add(Diagnostic.error(code, "Invalid configuration"));
 *
 * if (sink.hasErrors()) {
 *   // Stop compilation
 * }
 *
 * List<Diagnostic> allDiagnostics = sink.all();
 * }</pre>
 */
public final class DiagnosticSink {

    private final List<Diagnostic> diagnostics;

    private DiagnosticSink() {
        this.diagnostics = new CopyOnWriteArrayList<>();
    }

    /**
     * Creates a new empty diagnostic sink.
     *
     * @return new sink (never {@code null})
     */
    public static DiagnosticSink create() {
        return new DiagnosticSink();
    }

    /**
     * Adds a diagnostic to the sink.
     *
     * @param diagnostic the diagnostic (not {@code null})
     */
    public void add(Diagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");
        diagnostics.add(diagnostic);
    }

    /**
     * Adds multiple diagnostics to the sink.
     *
     * @param diagnostics the diagnostics (not {@code null})
     */
    public void addAll(List<Diagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        for (Diagnostic d : diagnostics) {
            if (d != null) {
                add(d);
            }
        }
    }

    /**
     * Returns all collected diagnostics in insertion order.
     *
     * <p>
     * The returned list is an immutable snapshot taken at the time of this call.
     * </p>
     *
     * @return immutable list of diagnostics (never {@code null})
     */
    public List<Diagnostic> all() {
        return Collections.unmodifiableList(new ArrayList<>(diagnostics));
    }

    /**
     * Returns diagnostics matching the given predicate.
     *
     * <p>
     * The returned list is an immutable snapshot.
     * </p>
     *
     * @param predicate filter predicate (not {@code null})
     * @return immutable list of matching diagnostics (never {@code null})
     */
    public List<Diagnostic> filter(Predicate<Diagnostic> predicate) {
        Objects.requireNonNull(predicate, "predicate");
        List<Diagnostic> result = new ArrayList<>();
        for (Diagnostic d : diagnostics) {
            if (predicate.test(d)) {
                result.add(d);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns diagnostics of the given severity.
     *
     * @param severity the severity (not {@code null})
     * @return immutable list of diagnostics (never {@code null})
     */
    public List<Diagnostic> withSeverity(DiagnosticSeverity severity) {
        Objects.requireNonNull(severity, "severity");
        return filter(d -> d.severity() == severity);
    }

    /**
     * Returns all error diagnostics.
     *
     * @return immutable list of errors (never {@code null})
     */
    public List<Diagnostic> errors() {
        return withSeverity(DiagnosticSeverity.ERROR);
    }

    /**
     * Returns all warning diagnostics.
     *
     * @return immutable list of warnings (never {@code null})
     */
    public List<Diagnostic> warnings() {
        return withSeverity(DiagnosticSeverity.WARNING);
    }

    /**
     * Returns all info diagnostics.
     *
     * @return immutable list of info diagnostics (never {@code null})
     */
    public List<Diagnostic> infos() {
        return withSeverity(DiagnosticSeverity.INFO);
    }

    /**
     * Returns {@code true} if the sink contains at least one error.
     *
     * @return {@code true} if errors are present
     */
    public boolean hasErrors() {
        return diagnostics.stream().anyMatch(d -> d.severity() == DiagnosticSeverity.ERROR);
    }

    /**
     * Returns {@code true} if the sink contains at least one warning.
     *
     * @return {@code true} if warnings are present
     */
    public boolean hasWarnings() {
        return diagnostics.stream().anyMatch(d -> d.severity() == DiagnosticSeverity.WARNING);
    }

    /**
     * Returns {@code true} if the sink is empty.
     *
     * @return {@code true} if no diagnostics have been added
     */
    public boolean isEmpty() {
        return diagnostics.isEmpty();
    }

    /**
     * Returns the total number of diagnostics.
     *
     * @return diagnostic count
     */
    public int size() {
        return diagnostics.size();
    }

    /**
     * Returns the count of diagnostics of the given severity.
     *
     * @param severity the severity (not {@code null})
     * @return count
     */
    public int count(DiagnosticSeverity severity) {
        Objects.requireNonNull(severity, "severity");
        return (int) diagnostics.stream().filter(d -> d.severity() == severity).count();
    }

    /**
     * Clears all diagnostics from the sink.
     *
     * <p>
     * This method is rarely needed during normal compilation, but can be useful in tests or
     * multi-round scenarios.
     * </p>
     */
    public void clear() {
        diagnostics.clear();
    }

    @Override
    public String toString() {
        return "DiagnosticSink[total=" + size() + ", errors=" + count(DiagnosticSeverity.ERROR) + ", warnings="
                + count(DiagnosticSeverity.WARNING) + "]";
    }
}
