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
 * File writing abstractions for JSR-269 Filer with merge and transaction support.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides specialized writers for different file types, coordinating
 * the actual I/O operations through the JSR-269 {@link javax.annotation.processing.Filer}.
 * It builds on the file processing utilities in {@code io.hexaglue.core.codegen.files}
 * to provide complete write workflows with merge support, header generation, and
 * error handling.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * The write architecture follows these principles:
 * </p>
 * <ul>
 *   <li><strong>Type-Safe Writers:</strong> Dedicated writers for each file type
 *       ({@link io.hexaglue.core.codegen.write.SourceWriter},
 *       {@link io.hexaglue.core.codegen.write.ResourceWriter},
 *       {@link io.hexaglue.core.codegen.write.DocWriter})</li>
 *   <li><strong>Filer Isolation:</strong> All JSR-269 Filer interactions centralized
 *       in {@link io.hexaglue.core.codegen.write.FilerWriter}</li>
 *   <li><strong>Merge Integration:</strong> Automatic coordination with merge planning
 *       and custom block preservation</li>
 *   <li><strong>Transaction Semantics:</strong> Error tracking and fail-fast behavior
 *       through {@link io.hexaglue.core.codegen.write.WriteTransactions}</li>
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>{@link io.hexaglue.core.codegen.write.FilerWriter}</h3>
 * <p>
 * Low-level abstraction over JSR-269 Filer:
 * </p>
 * <ul>
 *   <li>Creates source files via {@code Filer.createSourceFile()}</li>
 *   <li>Creates resource files via {@code Filer.createResource()}</li>
 *   <li>Handles text and binary content</li>
 *   <li>Reports I/O errors as diagnostics</li>
 *   <li>Manages encoding for text files</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.write.SourceWriter}</h3>
 * <p>
 * Specialized writer for Java source files:
 * </p>
 * <ul>
 *   <li>Prepends generated headers using Java block comment style</li>
 *   <li>Preserves custom blocks during regeneration</li>
 *   <li>Executes merge strategies based on {@link io.hexaglue.spi.codegen.MergeMode}</li>
 *   <li>Handles qualified type name resolution</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.write.ResourceWriter}</h3>
 * <p>
 * Specialized writer for resource files:
 * </p>
 * <ul>
 *   <li>Handles both text and binary resources</li>
 *   <li>Renders headers with appropriate comment styles (shell, XML, Markdown)</li>
 *   <li>Supports merge for text resources</li>
 *   <li>Writes to {@link javax.tools.StandardLocation#CLASS_OUTPUT}</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.write.DocWriter}</h3>
 * <p>
 * Specialized writer for documentation files:
 * </p>
 * <ul>
 *   <li>Renders headers using documentation-appropriate styles</li>
 *   <li>Preserves custom blocks for user-maintained sections</li>
 *   <li>Supports multiple documentation formats (Markdown, YAML, AsciiDoc)</li>
 *   <li>Enables collaborative documentation generation</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.write.WriteTransactions}</h3>
 * <p>
 * Transaction coordinator for file writes:
 * </p>
 * <ul>
 *   <li>Tracks write operations and outcomes</li>
 *   <li>Provides fail-fast behavior on errors</li>
 *   <li>Aggregates diagnostics across writes</li>
 *   <li>Returns comprehensive write results</li>
 * </ul>
 * <p>
 * Note: True rollback is not possible with JSR-269 Filer. Transactions provide
 * error tracking and fail-fast semantics, not atomicity.
 * </p>
 *
 * <h2>Write Workflow</h2>
 * <pre>
 *  1. Plugin emits artifact
 *       │
 *       ├─► SourceFile/ResourceFile/DocFile (SPI)
 *       │
 *  2. ArtifactSink collects
 *       │
 *       ├─► DefaultArtifactSink
 *       │
 *  3. Build artifact plan
 *       │
 *       ├─► ArtifactPlan (conflict detection)
 *       │
 *  4. Begin write transaction
 *       │
 *       ├─► WriteTransactions.begin()
 *       │
 *  5. Write each artifact
 *       │
 *       ├─── Source ──► SourceWriter
 *       │                  │
 *       │                  ├─► Prepare content + header
 *       │                  ├─► Plan merge (MergePlanner)
 *       │                  ├─► Preserve custom blocks (CustomBlockEngine)
 *       │                  └─► Write via FilerWriter
 *       │
 *       ├─── Resource ──► ResourceWriter
 *       │                    │
 *       │                    ├─► Determine text vs binary
 *       │                    ├─► Render header (format-specific)
 *       │                    ├─► Plan merge if text
 *       │                    └─► Write via FilerWriter
 *       │
 *       └─── Doc ──► DocWriter
 *                      │
 *                      ├─► Prepare content + header
 *                      ├─► Plan merge
 *                      ├─► Preserve custom blocks
 *                      └─► Write via FilerWriter
 *  6. Commit transaction
 *       │
 *       └─► WriteResult (success/failure counts)
 * </pre>
 *
 * <h2>Merge Integration</h2>
 * <p>
 * Writers coordinate with {@code io.hexaglue.core.codegen.files} for merge operations:
 * </p>
 * <ol>
 *   <li>Read existing file content if present</li>
 *   <li>Delegate to {@link io.hexaglue.core.codegen.files.MergePlanner} to determine action</li>
 *   <li>Use {@link io.hexaglue.core.codegen.files.CustomBlockEngine} to preserve user content</li>
 *   <li>Use {@link io.hexaglue.core.codegen.files.GeneratedHeaderEngine} for header rendering</li>
 *   <li>Execute write based on merge plan</li>
 * </ol>
 *
 * <h2>Header Rendering</h2>
 * <p>
 * Headers are rendered using format-appropriate comment styles:
 * </p>
 * <table>
 *   <tr>
 *     <th>File Type</th>
 *     <th>Comment Style</th>
 *     <th>Example</th>
 *   </tr>
 *   <tr>
 *     <td>Java source</td>
 *     <td>Block comment</td>
 *     <td>{@code /* ... *&#47;}</td>
 *   </tr>
 *   <tr>
 *     <td>Properties, YAML</td>
 *     <td>Shell comment</td>
 *     <td>{@code # ...}</td>
 *   </tr>
 *   <tr>
 *     <td>XML, HTML</td>
 *     <td>XML comment</td>
 *     <td>{@code <!-- ... -->}</td>
 *   </tr>
 *   <tr>
 *     <td>Markdown</td>
 *     <td>Blockquote</td>
 *     <td>{@code > ...}</td>
 *   </tr>
 * </table>
 *
 * <h2>Error Handling</h2>
 * <p>
 * All I/O errors are reported through {@link io.hexaglue.spi.diagnostics.DiagnosticReporter}:
 * </p>
 * <ul>
 *   <li>File creation failures (permissions, disk space)</li>
 *   <li>Write errors (encoding, stream failures)</li>
 *   <li>Merge failures (malformed custom blocks)</li>
 *   <li>Filer constraints (duplicate file names)</li>
 * </ul>
 * <p>
 * Writers return {@code boolean} success indicators, allowing callers to continue
 * or fail-fast as appropriate.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All writer classes are stateless and thread-safe. However, the underlying
 * JSR-269 {@link javax.annotation.processing.Filer} is not thread-safe.
 * Callers must ensure single-threaded access to Filer.
 * {@link io.hexaglue.core.codegen.write.WriteTransactions.WriteTransaction}
 * is not thread-safe and must be used from a single thread.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create writers
 * FilerWriter filerWriter = new FilerWriter(filer, diagnostics);
 * SourceWriter sourceWriter = new SourceWriter(filerWriter, diagnostics);
 * ResourceWriter resourceWriter = new ResourceWriter(filerWriter, diagnostics);
 * DocWriter docWriter = new DocWriter(filerWriter, diagnostics);
 *
 * // Begin transaction
 * WriteTransactions.WriteTransaction tx = WriteTransactions.begin(
 *     sourceWriter,
 *     resourceWriter,
 *     docWriter
 * ).withFailFast(true);
 *
 * // Write artifacts
 * for (SourceFile source : plan.sourceFiles()) {
 *     tx.writeSource(source);
 * }
 * for (ResourceFile resource : plan.resourceFiles()) {
 *     tx.writeResource(resource);
 * }
 * for (DocFile doc : plan.docFiles()) {
 *     tx.writeDoc(doc);
 * }
 *
 * // Commit and check result
 * WriteTransactions.WriteResult result = tx.commit();
 * if (!result.isSuccess()) {
 *     diagnostics.error("Write failed: " + result.failureCount() + " errors");
 * }
 * }</pre>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li>Used by: {@link io.hexaglue.core.codegen.ArtifactEmitter}</li>
 *   <li>Uses: {@code io.hexaglue.core.codegen.files} (merge planning, headers, custom blocks)</li>
 *   <li>Uses: {@code io.hexaglue.spi.codegen} (artifact models)</li>
 *   <li>Reports to: {@link io.hexaglue.spi.diagnostics.DiagnosticReporter}</li>
 * </ul>
 *
 * @see javax.annotation.processing.Filer
 */
package io.hexaglue.core.codegen.write;
