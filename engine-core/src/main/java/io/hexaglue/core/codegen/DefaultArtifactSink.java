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

import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.codegen.ResourceFile;
import io.hexaglue.spi.codegen.SourceFile;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link ArtifactSink} that collects artifacts in memory.
 *
 * <p>
 * This class is a mutable collector that plugins interact with during the generation phase.
 * It accumulates all requested artifacts and validates their basic structure. After all
 * plugins have completed, it produces an immutable {@link ArtifactPlan} for emission.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating collection (this class) from emission ({@link ArtifactEmitter}) enables:
 * </p>
 * <ul>
 *   <li>Deferred validation until all plugins have contributed</li>
 *   <li>Centralized conflict detection before any I/O</li>
 *   <li>Testable artifact analysis without file system dependencies</li>
 *   <li>Transaction-like semantics (all-or-nothing emission)</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <pre>
 * 1. Construction:  new DefaultArtifactSink(diagnostics)
 * 2. Collection:    sink.write(sourceFile) - called by plugins
 * 3. Planning:      ArtifactPlan plan = sink.buildPlan()
 * 4. Emission:      emitter.emit(plan) - done by core
 * </pre>
 *
 * <h2>Validation</h2>
 * <p>
 * Basic validation is performed when artifacts are added:
 * </p>
 * <ul>
 *   <li>Non-null artifact instances</li>
 *   <li>Non-blank qualified names and paths</li>
 *   <li>Required fields (content, merge mode, etc.)</li>
 * </ul>
 * <p>
 * More sophisticated validation (conflict detection, merge strategy compatibility)
 * is deferred to {@link ArtifactPlan}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is <strong>not thread-safe</strong>. It must be used from the annotation
 * processor thread only. Concurrent plugin execution is not supported.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Core creates sink
 * DefaultArtifactSink sink = new DefaultArtifactSink(diagnostics);
 *
 * // Pass to plugin via GenerationContext
 * GenerationContextSpec context = new DefaultGenerationContext(..., sink, ...);
 * plugin.generate(context);
 *
 * // Plugin uses sink
 * SourceFile source = SourceFile.builder()
 *     .qualifiedTypeName("com.example.GeneratedClass")
 *     .content("public class GeneratedClass {}")
 *     .build();
 * context.output().write(source);
 *
 * // Core builds plan after all plugins complete
 * ArtifactPlan plan = sink.buildPlan();
 * }</pre>
 */
public final class DefaultArtifactSink implements ArtifactSink {

    // private final DiagnosticReporter diagnostics;
    private final List<SourceFile> sourceFiles = new ArrayList<>();
    private final List<ResourceFile> resourceFiles = new ArrayList<>();
    private final List<DocFile> docFiles = new ArrayList<>();
    private boolean planBuilt = false;

    /**
     * Creates a new artifact sink.
     *
     * @param diagnostics diagnostic reporter for validation errors (not {@code null})
     */
    public DefaultArtifactSink(DiagnosticReporter diagnostics) {
        // this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    @Override
    public void write(SourceFile file) {
        Objects.requireNonNull(file, "file");
        checkNotBuilt();
        validateSourceFile(file);
        sourceFiles.add(file);
    }

    @Override
    public void write(ResourceFile file) {
        Objects.requireNonNull(file, "file");
        checkNotBuilt();
        validateResourceFile(file);
        resourceFiles.add(file);
    }

    @Override
    public void write(DocFile file) {
        Objects.requireNonNull(file, "file");
        checkNotBuilt();
        validateDocFile(file);
        docFiles.add(file);
    }

    /**
     * Builds an immutable {@link ArtifactPlan} from all collected artifacts.
     *
     * <p>
     * This method can only be called once. After calling it, no further artifacts
     * can be added to this sink.
     * </p>
     *
     * @return artifact plan (never {@code null})
     * @throws IllegalStateException if called more than once
     */
    public ArtifactPlan buildPlan() {
        checkNotBuilt();
        planBuilt = true;

        return ArtifactPlan.builder()
                .addAllSources(sourceFiles)
                .addAllResources(resourceFiles)
                .addAllDocs(docFiles)
                .build();
    }

    /**
     * Returns the number of source files collected so far.
     *
     * @return source file count
     */
    public int sourceFileCount() {
        return sourceFiles.size();
    }

    /**
     * Returns the number of resource files collected so far.
     *
     * @return resource file count
     */
    public int resourceFileCount() {
        return resourceFiles.size();
    }

    /**
     * Returns the number of documentation files collected so far.
     *
     * @return documentation file count
     */
    public int docFileCount() {
        return docFiles.size();
    }

    /**
     * Returns the total number of artifacts collected so far.
     *
     * @return total artifact count
     */
    public int totalArtifactCount() {
        return sourceFiles.size() + resourceFiles.size() + docFiles.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private void validateSourceFile(SourceFile file) {
        // Basic structural validation
        // The SourceFile constructor already validates required fields
        // Additional semantic validation can be added here if needed
    }

    private void validateResourceFile(ResourceFile file) {
        // Basic structural validation
        // The ResourceFile constructor already validates required fields
        // Additional semantic validation can be added here if needed
    }

    private void validateDocFile(DocFile file) {
        // Basic structural validation
        // The DocFile constructor already validates required fields
        // Additional semantic validation can be added here if needed
    }

    private void checkNotBuilt() {
        if (planBuilt) {
            throw new IllegalStateException("Artifact plan has already been built. No further artifacts can be added.");
        }
    }
}
