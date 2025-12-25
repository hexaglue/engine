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
package io.hexaglue.core.codegen.write;

import io.hexaglue.core.codegen.files.CustomBlockEngine;
import io.hexaglue.core.codegen.files.GeneratedHeaderEngine;
import io.hexaglue.core.codegen.files.MergePlanner;
import io.hexaglue.spi.codegen.CustomBlock;
import io.hexaglue.spi.codegen.SourceFile;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Writes Java source files with merge support and header generation.
 *
 * <p>
 * This writer handles the complete lifecycle of source file emission, including:
 * </p>
 * <ul>
 *   <li>Header generation and prepending</li>
 *   <li>Custom block preservation during regeneration</li>
 *   <li>Merge strategy execution based on {@link io.hexaglue.spi.codegen.MergeMode}</li>
 *   <li>Encoding handling</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating source writing from general file writing enables:
 * </p>
 * <ul>
 *   <li>Source-specific merge logic (Java comment markers, package structure)</li>
 *   <li>Specialized header rendering for Java files</li>
 *   <li>Type-safe qualified name handling</li>
 *   <li>Future support for formatting, import organization, etc.</li>
 * </ul>
 *
 * <h2>Merge Support</h2>
 * <p>
 * Source files commonly include custom blocks for:
 * </p>
 * <ul>
 *   <li>Custom imports</li>
 *   <li>Helper methods</li>
 *   <li>Field initializers</li>
 *   <li>Static initialization blocks</li>
 * </ul>
 * <p>
 * The writer uses {@link MergePlanner} to determine how to handle existing files
 * and {@link CustomBlockEngine} to preserve user content.
 * </p>
 *
 * <h2>Header Generation</h2>
 * <p>
 * If a {@link io.hexaglue.spi.codegen.GeneratedHeader} is present, it is rendered
 * as a Java block comment and prepended to the source content.
 * </p>
 *
 * <h2>Write Process</h2>
 * <pre>
 * 1. Prepare content
 *    ├─► Render header if present
 *    └─► Prepend to source content
 *
 * 2. Check merge mode
 *    ├─► OVERWRITE: Write directly
 *    ├─► MERGE_CUSTOM_BLOCKS: Extract and merge blocks
 *    ├─► WRITE_ONCE: Skip if exists
 *    └─► FAIL_IF_EXISTS: Error if exists
 *
 * 3. Execute write
 *    └─► Delegate to FilerWriter
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * SourceFile sourceFile = SourceFile.builder()
 *     .qualifiedTypeName("com.example.Foo")
 *     .content("public class Foo {}")
 *     .header(GeneratedHeader.minimalHexaGlue())
 *     .mergeMode(MergeMode.MERGE_CUSTOM_BLOCKS)
 *     .customBlocks(List.of(CustomBlock.of("imports")))
 *     .build();
 *
 * SourceWriter writer = new SourceWriter(filerWriter, diagnostics);
 * boolean success = writer.write(sourceFile, Optional.empty());
 * }</pre>
 */
public final class SourceWriter {

    private static final DiagnosticCode CODE_INTERNAL_GENERATION_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-200");
    private static final DiagnosticCode CODE_MERGE_FAILED = DiagnosticCode.of("HG-MERGE-200");

    private final FilerWriter filerWriter;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new source file writer.
     *
     * @param filerWriter low-level filer writer (not {@code null})
     * @param diagnostics diagnostic reporter (not {@code null})
     */
    public SourceWriter(FilerWriter filerWriter, DiagnosticReporter diagnostics) {
        this.filerWriter = Objects.requireNonNull(filerWriter, "filerWriter");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Writes a source file.
     *
     * @param sourceFile source file to write (not {@code null})
     * @param existingContent existing file content if file exists (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean write(SourceFile sourceFile, Optional<String> existingContent) {
        Objects.requireNonNull(sourceFile, "sourceFile");
        Objects.requireNonNull(existingContent, "existingContent");

        // Prepare content with header
        String contentWithHeader = prepareContent(sourceFile);

        // Extract custom block IDs
        List<String> customBlockIds =
                sourceFile.customBlocks().stream().map(CustomBlock::id).collect(Collectors.toList());

        // Plan merge operation
        MergePlanner.MergePlan plan = MergePlanner.plan(
                contentWithHeader, existingContent, sourceFile.mergeMode(), sourceFile.header(), customBlockIds);

        // Execute based on plan
        switch (plan.action()) {
            case WRITE:
                return filerWriter.writeSource(
                        sourceFile.qualifiedTypeName(),
                        plan.finalContent().orElse(contentWithHeader),
                        sourceFile.charset());

            case SKIP:
                // Log skip reason but don't report as error
                return true;

            case ERROR:
                // Report merge error as diagnostic
                diagnostics.error(
                        CODE_MERGE_FAILED,
                        "Merge failed for source file '" + sourceFile.qualifiedTypeName() + "': " + plan.message());
                return false;

            default:
                // This should never happen if MergeAction enum is complete
                diagnostics.error(
                        CODE_INTERNAL_GENERATION_ERROR,
                        "Internal error: unexpected merge action '" + plan.action() + "' for source file '"
                                + sourceFile.qualifiedTypeName() + "'");
                return false;
        }
    }

    /**
     * Writes a source file without merge planning (forced overwrite).
     *
     * <p>
     * This method bypasses merge logic and always overwrites the target file.
     * Use this when you know the file doesn't exist or when merge is not needed.
     * </p>
     *
     * @param sourceFile source file to write (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean writeOverwrite(SourceFile sourceFile) {
        Objects.requireNonNull(sourceFile, "sourceFile");

        String contentWithHeader = prepareContent(sourceFile);

        return filerWriter.writeSource(sourceFile.qualifiedTypeName(), contentWithHeader, sourceFile.charset());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Content Preparation
    // ─────────────────────────────────────────────────────────────────────────

    private String prepareContent(SourceFile sourceFile) {
        String content = sourceFile.content();

        // Prepend header if present
        if (sourceFile.header().isPresent()) {
            String header =
                    GeneratedHeaderEngine.renderJavaBlock(sourceFile.header().get());
            content = header + "\n" + content;
        }

        return content;
    }
}
