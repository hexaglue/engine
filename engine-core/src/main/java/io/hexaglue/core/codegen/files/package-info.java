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
 * File processing utilities for merge planning, custom blocks, and header generation.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides internal utilities for processing generated files before emission.
 * It bridges the gap between the SPI artifact model ({@link io.hexaglue.spi.codegen.SourceFile},
 * {@link io.hexaglue.spi.codegen.ResourceFile}, {@link io.hexaglue.spi.codegen.DocFile})
 * and the actual file writing logic.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * The file processing architecture follows these principles:
 * </p>
 * <ul>
 *   <li><strong>SPI Separation:</strong> Internal processing state is kept separate from
 *       the SPI model to maintain a clean public API</li>
 *   <li><strong>Merge-Aware Generation:</strong> Full support for preserving user content
 *       during regeneration cycles</li>
 *   <li><strong>Header Consistency:</strong> Uniform header formatting across all file types</li>
 *   <li><strong>Path Resolution:</strong> Clean separation between logical artifact names
 *       and physical file system paths</li>
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>{@link io.hexaglue.core.codegen.files.MergePlanner}</h3>
 * <p>
 * Plans merge operations based on {@link io.hexaglue.spi.codegen.MergeMode}:
 * </p>
 * <ul>
 *   <li>Determines whether to overwrite, merge, skip, or error</li>
 *   <li>Coordinates custom block preservation</li>
 *   <li>Produces {@link io.hexaglue.core.codegen.files.MergePlanner.MergePlan}
 *       with final content and action</li>
 *   <li>Detects orphaned custom blocks</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.files.CustomBlockEngine}</h3>
 * <p>
 * Extracts and merges user-maintained custom block regions:
 * </p>
 * <ul>
 *   <li>Parses custom block markers in existing files</li>
 *   <li>Extracts preserved content between markers</li>
 *   <li>Injects preserved content into new templates</li>
 *   <li>Validates marker syntax and pairing</li>
 *   <li>Detects conflicts and orphaned blocks</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.codegen.files.GeneratedHeaderEngine}</h3>
 * <p>
 * Renders {@link io.hexaglue.spi.codegen.GeneratedHeader} into formatted comment text:
 * </p>
 * <ul>
 *   <li>Supports multiple comment styles (Java, shell, XML, Markdown)</li>
 *   <li>Includes tool attribution, timestamp, license, copyright</li>
 *   <li>Adds standard regeneration warnings</li>
 *   <li>Ensures consistent formatting across artifacts</li>
 * </ul>
 *
 * <h3>Internal File Representations</h3>
 * <p>
 * Wrappers for SPI file types with processing metadata:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.codegen.files.SourceFileImpl} - wraps {@link io.hexaglue.spi.codegen.SourceFile}</li>
 *   <li>{@link io.hexaglue.core.codegen.files.ResourceFileImpl} - wraps {@link io.hexaglue.spi.codegen.ResourceFile}</li>
 *   <li>{@link io.hexaglue.core.codegen.files.DocFileImpl} - wraps {@link io.hexaglue.spi.codegen.DocFile}</li>
 * </ul>
 * <p>
 * These wrappers add:
 * </p>
 * <ul>
 *   <li>Resolved file system paths</li>
 *   <li>Associated merge plans</li>
 *   <li>Processing state tracking</li>
 * </ul>
 *
 * <h2>Merge Workflow</h2>
 * <pre>
 *  1. Plugin emits artifact
 *       │
 *       ├─► SourceFile/ResourceFile/DocFile (SPI model)
 *       │
 *  2. Core wraps in internal representation
 *       │
 *       ├─► SourceFileImpl/ResourceFileImpl/DocFileImpl
 *       │
 *  3. Resolve file system path
 *       │
 *       ├─► withResolvedPath(Path)
 *       │
 *  4. Check if file exists
 *       │
 *       ├─── No existing file ──► MergePlanner.plan() ──► WRITE
 *       │
 *       └─── Existing file ──┬─► OVERWRITE mode ──► WRITE (replace)
 *                            │
 *                            ├─► MERGE_CUSTOM_BLOCKS mode
 *                            │     │
 *                            │     ├─► CustomBlockEngine.extractBlocks(existing)
 *                            │     ├─► CustomBlockEngine.mergeBlocks(new, preserved)
 *                            │     └─► WRITE (merged)
 *                            │
 *                            ├─► WRITE_ONCE mode ──► SKIP
 *                            │
 *                            └─► FAIL_IF_EXISTS mode ──► ERROR
 *  5. Execute action
 *       │
 *       ├─► WRITE ──► ArtifactEmitter ──► Filer
 *       ├─► SKIP ──► Log diagnostic
 *       └─► ERROR ──► Report error
 * </pre>
 *
 * <h2>Custom Block Markers</h2>
 * <p>
 * Standard marker format:
 * </p>
 * <pre>
 * // @hexaglue-custom-start: block-id
 * ... user content preserved here ...
 * // @hexaglue-custom-end: block-id
 * </pre>
 * <p>
 * Supported comment styles:
 * </p>
 * <ul>
 *   <li>Java/C/C++: {@code //} or {@code /* ... *&#47;}</li>
 *   <li>Shell/YAML/Properties: {@code #}</li>
 *   <li>XML/HTML: {@code <!-- ... -->}</li>
 *   <li>Markdown: {@code > ...}</li>
 * </ul>
 *
 * <h2>Header Rendering</h2>
 * <p>
 * Example Java block comment header:
 * </p>
 * <pre>
 * /*
 *  * Generated by HexaGlue
 *  * Date: 2025-01-15T10:30:00Z
 *  * License: MPL-2.0
 *  * Copyright (c) 2025 Scalastic
 *  *
 *  * DO NOT EDIT - This file is generated and will be overwritten
 *  *&#47;
 * </pre>
 *
 * <h2>Error Handling</h2>
 * <p>
 * This package detects and reports:
 * </p>
 * <ul>
 *   <li>Malformed custom block markers (unmatched start/end, nested blocks)</li>
 *   <li>Duplicate block IDs within a file</li>
 *   <li>Orphaned blocks (present in old file but not in new template)</li>
 *   <li>Block ID mismatches between start and end markers</li>
 *   <li>Unsupported comment styles</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package are stateless and thread-safe. The internal file
 * representation classes ({@code *Impl}) are immutable and use fluent APIs for
 * state updates.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // 1. Wrap SPI file in internal representation
 * SourceFile spiFile = SourceFile.builder()
 *     .qualifiedTypeName("com.example.Foo")
 *     .content("public class Foo {}")
 *     .header(GeneratedHeader.minimalHexaGlue())
 *     .customBlocks(List.of(CustomBlock.of("imports")))
 *     .build();
 *
 * SourceFileImpl impl = SourceFileImpl.of(spiFile);
 *
 * // 2. Resolve file system path
 * Path sourceRoot = Paths.get("target/generated-sources");
 * impl = impl.withResolvedPath(impl.resolveTargetPath(sourceRoot));
 *
 * // 3. Plan merge operation
 * Optional<String> existingContent = readIfExists(impl.resolvedPath().get());
 * MergePlanner.MergePlan plan = MergePlanner.plan(
 *     spiFile.content(),
 *     existingContent,
 *     spiFile.mergeMode(),
 *     spiFile.header(),
 *     spiFile.customBlocks().stream().map(CustomBlock::id).toList()
 * );
 *
 * impl = impl.withMergePlan(plan);
 *
 * // 4. Execute based on plan action
 * if (plan.action() == MergeAction.WRITE) {
 *     writeFile(impl.resolvedPath().get(), plan.finalContent().get());
 * }
 * }</pre>
 *
 * <h2>Integration Points</h2>
 * <ul>
 *   <li>Used by: {@link io.hexaglue.core.codegen.ArtifactEmitter}</li>
 *   <li>Uses: {@code io.hexaglue.spi.codegen} (SPI artifact model)</li>
 *   <li>Complements: {@code io.hexaglue.core.codegen.write} (file writers)</li>
 *   <li>Supports: {@code io.hexaglue.core.codegen.merge} (merge strategies)</li>
 * </ul>
 */
package io.hexaglue.core.codegen.files;
