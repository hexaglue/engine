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
package io.hexaglue.core.codegen;

import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.codegen.MergeMode;
import io.hexaglue.spi.codegen.ResourceFile;
import io.hexaglue.spi.codegen.SourceFile;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.io.IOException;
import java.io.Writer;
import java.util.Objects;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Emits artifacts to the file system using the JSR-269 {@link Filer}.
 *
 * <p>
 * This class is the low-level file writer that bridges HexaGlue's artifact model
 * ({@link SourceFile}, {@link ResourceFile}, {@link DocFile}) with the JSR-269
 * annotation processing API. It handles:
 * </p>
 * <ul>
 *   <li>File creation via {@link Filer}</li>
 *   <li>Encoding and charset handling</li>
 *   <li>Error reporting for I/O failures</li>
 *   <li>Merge mode interpretation (delegated to merge subpackage)</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Isolating {@link Filer} usage to this class enables:
 * </p>
 * <ul>
 *   <li>Testability of artifact collection without file system access</li>
 *   <li>Centralized error handling for I/O operations</li>
 *   <li>Clear separation between artifact modeling and file writing</li>
 *   <li>Future support for alternative output mechanisms (testing, preview, etc.)</li>
 * </ul>
 *
 * <h2>Merge Strategies</h2>
 * <p>
 * Each artifact declares a {@link MergeMode}:
 * </p>
 * <ul>
 *   <li><strong>OVERWRITE:</strong> Replace file unconditionally</li>
 *   <li><strong>MERGE_CUSTOM_BLOCKS:</strong> Preserve custom blocks (requires merge engine)</li>
 *   <li><strong>KEEP_EXISTING:</strong> Only write if file doesn't exist</li>
 *   <li><strong>FAIL_IF_EXISTS:</strong> Error if file exists</li>
 * </ul>
 * <p>
 * Currently, only {@code OVERWRITE} is fully implemented. Other modes will be
 * implemented in {@code io.hexaglue.core.codegen.merge}.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * I/O errors are reported via {@link DiagnosticReporter} with appropriate locations.
 * Emission continues for remaining artifacts even if some fail.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and could be thread-safe, but {@link Filer} itself is not
 * thread-safe. Therefore, this emitter must not be used concurrently.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ArtifactPlan plan = sink.buildPlan();
 * ArtifactEmitter emitter = new ArtifactEmitter(filer, diagnostics);
 * emitter.emit(plan);
 * }</pre>
 */
public final class ArtifactEmitter {

    private static final DiagnosticCode CODE_MERGE_NOT_SUPPORTED = DiagnosticCode.of("HG-CORE-CODEGEN-101");
    private static final DiagnosticCode CODE_IO_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-203");
    private static final DiagnosticCode CODE_INVALID_RESOURCE = DiagnosticCode.of("HG-CORE-CODEGEN-204");

    private final Filer filer;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new artifact emitter.
     *
     * @param filer JSR-269 filer for file creation (not {@code null})
     * @param diagnostics diagnostic reporter for errors (not {@code null})
     */
    public ArtifactEmitter(Filer filer, DiagnosticReporter diagnostics) {
        this.filer = Objects.requireNonNull(filer, "filer");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Emits all artifacts in the given plan.
     *
     * <p>
     * Artifacts are emitted in this order:
     * </p>
     * <ol>
     *   <li>Source files</li>
     *   <li>Resource files</li>
     *   <li>Documentation files</li>
     * </ol>
     * <p>
     * If any artifact fails to emit, an error is reported and emission continues
     * for remaining artifacts.
     * </p>
     *
     * @param plan artifact plan to emit (not {@code null})
     */
    public void emit(ArtifactPlan plan) {
        Objects.requireNonNull(plan, "plan");

        for (SourceFile file : plan.sourceFiles()) {
            emitSource(file);
        }

        for (ResourceFile file : plan.resourceFiles()) {
            emitResource(file);
        }

        for (DocFile file : plan.docFiles()) {
            emitDoc(file);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Source file emission
    // ─────────────────────────────────────────────────────────────────────────

    private void emitSource(SourceFile file) {
        try {
            // Check merge mode
            if (file.mergeMode() != MergeMode.OVERWRITE) {
                diagnostics.report(Diagnostic.builder()
                        .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.WARNING)
                        .code(CODE_MERGE_NOT_SUPPORTED)
                        .message("Merge mode '" + file.mergeMode()
                                + "' not yet fully supported for source files. Using OVERWRITE.")
                        .location(locationForSource(file))
                        .build());
            }

            JavaFileObject jfo = filer.createSourceFile(file.qualifiedTypeName());
            try (Writer writer = jfo.openWriter()) {
                writer.write(file.content());
            }

        } catch (IOException e) {
            diagnostics.report(Diagnostic.builder()
                    .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                    .code(CODE_IO_ERROR)
                    .message("Failed to write source file '" + file.qualifiedTypeName() + "': " + e.getMessage())
                    .location(locationForSource(file))
                    .cause(e)
                    .build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resource file emission
    // ─────────────────────────────────────────────────────────────────────────

    private void emitResource(ResourceFile file) {
        try {
            // Check merge mode
            if (file.mergeMode() != MergeMode.OVERWRITE) {
                diagnostics.report(Diagnostic.builder()
                        .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.WARNING)
                        .code(CODE_MERGE_NOT_SUPPORTED)
                        .message("Merge mode '" + file.mergeMode()
                                + "' not yet fully supported for resource files. Using OVERWRITE.")
                        .location(locationForResource(file))
                        .build());
            }

            // Determine package and relative name from path
            // For simplicity, treat the entire path as relative name with empty package
            String pkg = "";
            String relativeName = file.path();

            FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, pkg, relativeName);

            if (file.text().isPresent()) {
                // Text resource
                try (Writer writer = fo.openWriter()) {
                    writer.write(file.text().get());
                }
            } else if (file.bytes().isPresent()) {
                // Binary resource
                try (var outputStream = fo.openOutputStream()) {
                    outputStream.write(file.bytes().get());
                }
            } else {
                diagnostics.report(Diagnostic.builder()
                        .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                        .code(CODE_INVALID_RESOURCE)
                        .message("Resource file '" + file.path() + "' has neither text nor binary content.")
                        .location(locationForResource(file))
                        .build());
            }

        } catch (IOException e) {
            diagnostics.report(Diagnostic.builder()
                    .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                    .code(CODE_IO_ERROR)
                    .message("Failed to write resource file '" + file.path() + "': " + e.getMessage())
                    .location(locationForResource(file))
                    .cause(e)
                    .build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Documentation file emission
    // ─────────────────────────────────────────────────────────────────────────

    private void emitDoc(DocFile file) {
        try {
            // Check merge mode
            if (file.mergeMode() != MergeMode.OVERWRITE) {
                diagnostics.report(Diagnostic.builder()
                        .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.WARNING)
                        .code(CODE_MERGE_NOT_SUPPORTED)
                        .message("Merge mode '" + file.mergeMode()
                                + "' not yet fully supported for documentation files. Using OVERWRITE.")
                        .location(locationForDoc(file))
                        .build());
            }

            // Documentation goes to SOURCE_OUTPUT or CLASS_OUTPUT depending on use case
            // For now, use CLASS_OUTPUT
            String pkg = "";
            String relativeName = file.path();

            FileObject fo = filer.createResource(StandardLocation.CLASS_OUTPUT, pkg, relativeName);

            try (Writer writer = fo.openWriter()) {
                writer.write(file.content());
            }

        } catch (IOException e) {
            diagnostics.report(Diagnostic.builder()
                    .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                    .code(CODE_IO_ERROR)
                    .message("Failed to write documentation file '" + file.path() + "': " + e.getMessage())
                    .location(locationForDoc(file))
                    .cause(e)
                    .build());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diagnostic location creation
    // ─────────────────────────────────────────────────────────────────────────

    private DiagnosticLocation locationForSource(SourceFile file) {
        return DiagnosticLocation.ofQualifiedName(file.qualifiedTypeName());
    }

    private DiagnosticLocation locationForResource(ResourceFile file) {
        return DiagnosticLocation.ofPath(file.path(), null, null);
    }

    private DiagnosticLocation locationForDoc(DocFile file) {
        return DiagnosticLocation.ofPath(file.path(), null, null);
    }
}
