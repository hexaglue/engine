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

import java.util.Objects;

/**
 * Summary of outputs produced by a compilation step (typically one processing round).
 *
 * <p>
 * The detailed artifact model and diagnostics model live in other internal packages; this class
 * provides a coarse, stable summary suitable for logging and tests.
 * </p>
 */
public final class CompilationOutputs {

    private final boolean success;
    private final int generatedSources;
    private final int generatedResources;
    private final int generatedDocs;
    private final int reportedDiagnostics;

    private CompilationOutputs(
            boolean success, int generatedSources, int generatedResources, int generatedDocs, int reportedDiagnostics) {
        this.success = success;
        this.generatedSources = nonNegative(generatedSources, "generatedSources");
        this.generatedResources = nonNegative(generatedResources, "generatedResources");
        this.generatedDocs = nonNegative(generatedDocs, "generatedDocs");
        this.reportedDiagnostics = nonNegative(reportedDiagnostics, "reportedDiagnostics");
    }

    /**
     * Creates an outputs summary.
     *
     * @param success whether the compilation step succeeded
     * @param generatedSources number of source files generated
     * @param generatedResources number of resource files generated
     * @param generatedDocs number of documentation files generated
     * @param reportedDiagnostics number of diagnostics reported during this step
     * @return outputs, never {@code null}
     */
    public static CompilationOutputs of(
            boolean success, int generatedSources, int generatedResources, int generatedDocs, int reportedDiagnostics) {
        return new CompilationOutputs(
                success, generatedSources, generatedResources, generatedDocs, reportedDiagnostics);
    }

    /**
     * Returns an empty successful outputs summary (no artifacts).
     *
     * @return outputs, never {@code null}
     */
    public static CompilationOutputs emptySuccess() {
        return new CompilationOutputs(true, 0, 0, 0, 0);
    }

    /**
     * Returns whether the compilation step succeeded.
     *
     * @return {@code true} if successful
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Returns the number of generated Java source files.
     *
     * @return source count
     */
    public int generatedSources() {
        return generatedSources;
    }

    /**
     * Returns the number of generated resource files.
     *
     * @return resource count
     */
    public int generatedResources() {
        return generatedResources;
    }

    /**
     * Returns the number of generated documentation files.
     *
     * @return doc count
     */
    public int generatedDocs() {
        return generatedDocs;
    }

    /**
     * Returns the number of diagnostics reported during the step.
     *
     * @return diagnostic count
     */
    public int reportedDiagnostics() {
        return reportedDiagnostics;
    }

    @Override
    public String toString() {
        return "CompilationOutputs{success=" + success
                + ", sources=" + generatedSources
                + ", resources=" + generatedResources
                + ", docs=" + generatedDocs
                + ", diagnostics=" + reportedDiagnostics
                + "}";
    }

    private static int nonNegative(int v, String name) {
        Objects.requireNonNull(name, "name");
        if (v < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return v;
    }
}
