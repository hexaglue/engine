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

/**
 * Code generation orchestration and artifact emission.
 *
 * <h2>Overview</h2>
 * <p>
 * This package coordinates the generation lifecycle: collecting artifacts from plugins,
 * planning file operations, resolving conflicts, and writing files through the JSR-269 Filer.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * The code generation architecture follows these principles:
 * </p>
 * <ul>
 *   <li><strong>SPI Abstraction:</strong> Plugins interact with {@link io.hexaglue.spi.codegen.ArtifactSink}
 *       and never see compiler internals</li>
 *   <li><strong>Two-Phase Generation:</strong> Collection phase (plugins emit) followed by
 *       emission phase (core writes)</li>
 *   <li><strong>Conflict Resolution:</strong> Centralized handling of merge strategies and duplicates</li>
 *   <li><strong>Filer Isolation:</strong> JSR-269 Filer usage is confined to {@link io.hexaglue.core.codegen.ArtifactEmitter}</li>
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>{@link io.hexaglue.core.codegen.GenerationOrchestrator}</h3>
 * <p>
 * High-level coordinator for the entire generation process:
 * </p>
 * <ul>
 *   <li>Invokes plugins in dependency order</li>
 *   <li>Collects all artifacts into a plan</li>
 *   <li>Resolves conflicts and merge requirements</li>
 *   <li>Delegates emission to {@link io.hexaglue.core.codegen.ArtifactEmitter}</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.ArtifactPlan}</h3>
 * <p>
 * Immutable plan representing all artifacts to be generated:
 * </p>
 * <ul>
 *   <li>Tracks source files, resources, and documentation</li>
 *   <li>Detects duplicates and conflicts</li>
 *   <li>Groups artifacts by merge strategy</li>
 *   <li>Provides diagnostic information</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.DefaultArtifactSink}</h3>
 * <p>
 * Mutable collector that implements {@link io.hexaglue.spi.codegen.ArtifactSink}:
 * </p>
 * <ul>
 *   <li>Accumulates artifacts during plugin execution</li>
 *   <li>Validates artifact requirements</li>
 *   <li>Builds an {@link io.hexaglue.core.codegen.ArtifactPlan} when collection is complete</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.ArtifactEmitter}</h3>
 * <p>
 * Low-level file writer that interfaces with JSR-269 Filer:
 * </p>
 * <ul>
 *   <li>Writes source files via {@link javax.annotation.processing.Filer#createSourceFile}</li>
 *   <li>Writes resource files via {@link javax.annotation.processing.Filer#createResource}</li>
 *   <li>Applies merge strategies for existing files</li>
 *   <li>Reports I/O errors as diagnostics</li>
 * </ul>
 *
 * <h2>Artifact Flow</h2>
 * <pre>
 *  Plugin                    Core                         JSR-269
 *  ──────                    ────                         ───────
 *     │                                                       │
 *     │  1. Request artifact emission                        │
 *     ├────► ArtifactSink.write(SourceFile)                  │
 *     │           │                                           │
 *     │           └─► DefaultArtifactSink                     │
 *     │                 (collect in memory)                   │
 *     │                                                       │
 *     │  2. After all plugins complete                       │
 *     │                 │                                     │
 *     │                 └─► ArtifactPlan                      │
 *     │                      (analyze, detect conflicts)      │
 *     │                           │                           │
 *     │                           └─► ArtifactEmitter         │
 *     │                                     │                 │
 *     │                                     └─────────────────► Filer
 *     │                                                       │
 *     │                                                (write files)
 * </pre>
 *
 * <h2>Merge Strategies</h2>
 * <p>
 * Artifacts declare a {@link io.hexaglue.spi.codegen.MergeMode}:
 * </p>
 * <ul>
 *   <li><strong>OVERWRITE:</strong> Replace existing file unconditionally</li>
 *   <li><strong>MERGE_CUSTOM_BLOCKS:</strong> Preserve custom blocks from existing file</li>
 *   <li><strong>KEEP_EXISTING:</strong> Only write if file doesn't exist</li>
 *   <li><strong>FAIL_IF_EXISTS:</strong> Error if file already exists</li>
 * </ul>
 * <p>
 * Merge logic is implemented in subpackages ({@code io.hexaglue.core.codegen.merge}).
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Generation errors are reported through {@link io.hexaglue.spi.diagnostics.DiagnosticReporter}:
 * </p>
 * <ul>
 *   <li>Invalid artifact configuration (missing required fields)</li>
 *   <li>Duplicate artifacts with conflicting content</li>
 *   <li>I/O errors during file writing</li>
 *   <li>Merge failures (unable to preserve custom blocks)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link io.hexaglue.core.codegen.DefaultArtifactSink} is not thread-safe and must be used
 * from the annotation processor thread only. {@link io.hexaglue.core.codegen.ArtifactPlan}
 * is immutable and thread-safe. {@link io.hexaglue.core.codegen.ArtifactEmitter} is stateless
 * but must not be used concurrently due to Filer constraints.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // 1. Create artifact sink for collection
 * DefaultArtifactSink sink = new DefaultArtifactSink(diagnostics);
 *
 * // 2. Pass to plugin via GenerationContext
 * GenerationContextSpec context = new DefaultGenerationContext(..., sink, ...);
 * plugin.generate(context);
 *
 * // 3. Build plan and check for errors
 * ArtifactPlan plan = sink.buildPlan();
 * if (plan.hasConflicts()) {
 *     // Report conflicts
 * }
 *
 * // 4. Emit artifacts
 * ArtifactEmitter emitter = new ArtifactEmitter(filer, diagnostics);
 * emitter.emit(plan);
 * }</pre>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li>Called by: {@link io.hexaglue.core.lifecycle.CompilationPipeline}</li>
 *   <li>Uses: {@link io.hexaglue.core.diagnostics.DiagnosticEngine}</li>
 *   <li>Exposes: {@link io.hexaglue.spi.codegen.ArtifactSink} to plugins</li>
 *   <li>Delegates to: {@code io.hexaglue.core.codegen.merge} for merge logic</li>
 * </ul>
 */
package io.hexaglue.core.codegen;
