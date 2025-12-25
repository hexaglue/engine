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
 * Advanced merge strategies and custom block processing for file regeneration.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the core merge infrastructure that enables HexaGlue to
 * regenerate files without losing user-maintained content. It implements parsing,
 * rendering, and merging of custom block regions, coordinated by a high-level
 * merge engine.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * The merge architecture follows these principles:
 * </p>
 * <ul>
 *   <li><strong>Parse-Process-Render:</strong> Clean separation between parsing existing
 *       content, processing merge logic, and rendering new content</li>
 *   <li><strong>Strategy Pattern:</strong> Different merge strategies for different
 *       {@link io.hexaglue.spi.codegen.MergeMode} values</li>
 *   <li><strong>Custom Block Preservation:</strong> Robust detection and preservation
 *       of user-maintained regions</li>
 *   <li><strong>Diagnostic Integration:</strong> Comprehensive error reporting for
 *       merge failures, parse errors, and orphaned blocks</li>
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>{@link io.hexaglue.core.codegen.merge.MergeEngine}</h3>
 * <p>
 * High-level coordinator for merge operations:
 * </p>
 * <ul>
 *   <li>Validates merge requests</li>
 *   <li>Selects appropriate merge strategy</li>
 *   <li>Executes merge workflow</li>
 *   <li>Analyzes custom block changes</li>
 *   <li>Reports diagnostics</li>
 *   <li>Returns merge response with action and content</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.merge.MergeStrategies}</h3>
 * <p>
 * Collection of concrete merge strategy implementations:
 * </p>
 * <ul>
 *   <li><strong>OverwriteStrategy:</strong> Unconditional replacement</li>
 *   <li><strong>CustomBlockMergeStrategy:</strong> Preserve custom block regions</li>
 *   <li><strong>WriteOnceStrategy:</strong> Skip if file exists</li>
 *   <li><strong>FailIfExistsStrategy:</strong> Error if file exists</li>
 * </ul>
 * <p>
 * Each strategy implements the {@code MergeStrategy} interface and returns a
 * {@code MergeResult} indicating the action to take.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.codegen.merge.CustomBlockParser}</h3>
 * <p>
 * Parses custom block markers from existing files:
 * </p>
 * <ul>
 *   <li>Detects start/end marker pairs</li>
 *   <li>Extracts block content</li>
 *   <li>Validates marker syntax and pairing</li>
 *   <li>Returns structured {@code ParsedBlock} objects</li>
 *   <li>Reports parse errors with line numbers</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.merge.CustomBlockRenderer}</h3>
 * <p>
 * Renders custom block markers in new content:
 * </p>
 * <ul>
 *   <li>Generates start/end markers with proper comment syntax</li>
 *   <li>Supports multiple comment styles (Java, shell, XML, SQL)</li>
 *   <li>Handles indentation for nested blocks</li>
 *   <li>Includes optional block descriptions</li>
 *   <li>Auto-detects comment style from file extension</li>
 * </ul>
 *
 * <h2>Merge Workflow</h2>
 * <pre>
 *  1. Receive merge request
 *       │
 *       ├─► Validate: newContent, mergeMode, etc.
 *       │
 *  2. Select merge strategy
 *       │
 *       ├─► Based on MergeMode
 *       │     ├─► OVERWRITE ──► OverwriteStrategy
 *       │     ├─► MERGE_CUSTOM_BLOCKS ──► CustomBlockMergeStrategy
 *       │     ├─► WRITE_ONCE ──► WriteOnceStrategy
 *       │     └─► FAIL_IF_EXISTS ──► FailIfExistsStrategy
 *       │
 *  3. Execute strategy
 *       │
 *       ├─── OverwriteStrategy
 *       │      └─► Return new content (ignore existing)
 *       │
 *       ├─── CustomBlockMergeStrategy
 *       │      ├─► Parse existing content (CustomBlockParser)
 *       │      ├─► Extract custom blocks
 *       │      ├─► Parse new content
 *       │      ├─► Merge preserved blocks into new template
 *       │      └─► Return merged content
 *       │
 *       ├─── WriteOnceStrategy
 *       │      └─► Skip if exists, else write
 *       │
 *       └─── FailIfExistsStrategy
 *              └─► Error if exists, else write
 *
 *  4. Analyze result (if MERGE_CUSTOM_BLOCKS)
 *       │
 *       ├─► Detect orphaned blocks
 *       ├─► Report warnings for orphaned blocks
 *       └─► Validate merge success
 *
 *  5. Return merge response
 *       │
 *       └─► Action (WRITE/SKIP/ERROR) + finalContent + message
 * </pre>
 *
 * <h2>Custom Block Format</h2>
 * <p>
 * Custom blocks use marker comments to delimit user-maintained regions:
 * </p>
 * <pre>
 * // @hexaglue-custom-start: block-id
 * ... user content preserved here ...
 * // @hexaglue-custom-end: block-id
 * </pre>
 * <p>
 * Supported comment styles:
 * </p>
 * <table>
 *   <tr>
 *     <th>Language</th>
 *     <th>Comment Syntax</th>
 *     <th>Example</th>
 *   </tr>
 *   <tr>
 *     <td>Java, C, C++, JavaScript</td>
 *     <td>Line comment</td>
 *     <td>{@code // @hexaglue-custom-start: imports}</td>
 *   </tr>
 *   <tr>
 *     <td>Shell, YAML, Python</td>
 *     <td>Hash comment</td>
 *     <td>{@code # @hexaglue-custom-start: config}</td>
 *   </tr>
 *   <tr>
 *     <td>XML, HTML</td>
 *     <td>XML comment</td>
 *     <td>{@code <!-- @hexaglue-custom-start: content -->}</td>
 *   </tr>
 *   <tr>
 *     <td>SQL</td>
 *     <td>Dash comment</td>
 *     <td>{@code -- @hexaglue-custom-start: queries}</td>
 *   </tr>
 * </table>
 *
 * <h2>Custom Block Merge Algorithm</h2>
 * <pre>
 * 1. Parse existing file
 *    └─► Extract all custom blocks {id → content}
 *
 * 2. Parse new template
 *    └─► Find all custom block placeholders
 *
 * 3. Merge content
 *    ├─► For each line in new template:
 *    │     ├─► If start marker: begin custom block region
 *    │     │     └─► Inject preserved content from existing file
 *    │     ├─► If end marker: end custom block region
 *    │     └─► Otherwise: copy template line
 *    └─► Result: merged content with preserved user edits
 *
 * 4. Detect orphaned blocks
 *    └─► Blocks in existing file but not in new template
 * </pre>
 *
 * <h2>Validation and Error Handling</h2>
 * <p>
 * The merge package validates:
 * </p>
 * <ul>
 *   <li><strong>Marker Pairing:</strong> Every start marker must have matching end</li>
 *   <li><strong>Block ID Consistency:</strong> Start and end IDs must match</li>
 *   <li><strong>No Nesting:</strong> Custom blocks cannot overlap or nest</li>
 *   <li><strong>No Duplicates:</strong> Block IDs must be unique within a file</li>
 *   <li><strong>Valid Syntax:</strong> Markers must follow expected format</li>
 * </ul>
 * <p>
 * Errors are reported through {@link io.hexaglue.spi.diagnostics.DiagnosticReporter}:
 * </p>
 * <ul>
 *   <li><code>HG-MERGE-200:</code> Parse error in custom blocks</li>
 *   <li><code>HG-MERGE-100:</code> Orphaned blocks detected (warning)</li>
 *   <li><code>HG-MERGE-201:</code> Merge operation failed</li>
 * </ul>
 *
 * <h2>Orphaned Block Detection</h2>
 * <p>
 * Orphaned blocks occur when:
 * </p>
 * <ul>
 *   <li>A custom block exists in the old file</li>
 *   <li>But the new template no longer includes that block</li>
 *   <li>Possibly due to refactoring or template changes</li>
 * </ul>
 * <p>
 * Orphaned blocks are reported as warnings, allowing users to manually review
 * and migrate content if needed.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package are stateless and thread-safe, except where noted.
 * Merge operations can be executed concurrently for different files.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Setup
 * MergeEngine engine = new MergeEngine(diagnostics);
 *
 * // Prepare merge request
 * MergeEngine.MergeRequest request = MergeEngine.MergeRequest.builder()
 *     .newContent(generatedJavaSource)
 *     .existingContent(readExistingFile("Foo.java"))
 *     .mergeMode(MergeMode.MERGE_CUSTOM_BLOCKS)
 *     .customBlocks(List.of(
 *         CustomBlock.of("imports"),
 *         CustomBlock.of("methods")
 *     ))
 *     .location(DiagnosticLocation.ofQualifiedName("com.example.Foo"))
 *     .build();
 *
 * // Execute merge
 * MergeEngine.MergeResponse response = engine.merge(request);
 *
 * // Handle result
 * switch (response.action()) {
 *     case WRITE:
 *         writeFile("Foo.java", response.finalContent());
 *         break;
 *     case SKIP:
 *         log.info("Skipped: " + response.message());
 *         break;
 *     case ERROR:
 *         log.error("Merge failed: " + response.message());
 *         break;
 * }
 * }</pre>
 *
 * <h2>Integration with Other Packages</h2>
 * <p>
 * The merge package integrates with:
 * </p>
 * <ul>
 *   <li><strong>{@code io.hexaglue.core.codegen.files}:</strong> Uses
 *       {@code MergePlanner} which delegates to merge strategies</li>
 *   <li><strong>{@code io.hexaglue.core.codegen.write}:</strong> Writers call
 *       merge engine before writing files</li>
 *   <li><strong>{@code io.hexaglue.spi.codegen}:</strong> Implements merge behavior
 *       for {@link io.hexaglue.spi.codegen.MergeMode}</li>
 *   <li><strong>{@code io.hexaglue.spi.diagnostics}:</strong> Reports parse errors,
 *       orphaned blocks, and merge failures</li>
 * </ul>
 *
 * <h2>Testing Considerations</h2>
 * <p>
 * The merge package is designed for testability:
 * </p>
 * <ul>
 *   <li>Parser can be tested with various marker formats</li>
 *   <li>Renderer output can be verified against expected syntax</li>
 *   <li>Strategies can be tested with different content scenarios</li>
 *   <li>Engine can be tested with mock diagnostics</li>
 *   <li>No file I/O dependencies in core logic</li>
 * </ul>
 *
 * @see io.hexaglue.spi.codegen.MergeMode
 * @see io.hexaglue.spi.codegen.CustomBlock
 */
package io.hexaglue.core.codegen.merge;
