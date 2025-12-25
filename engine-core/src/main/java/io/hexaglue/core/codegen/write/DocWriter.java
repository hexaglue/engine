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

import io.hexaglue.core.codegen.files.GeneratedHeaderEngine;
import io.hexaglue.core.codegen.files.MergePlanner;
import io.hexaglue.spi.codegen.CustomBlock;
import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.tools.StandardLocation;

/**
 * Writes documentation files with merge support and header generation.
 *
 * <p>
 * This writer handles documentation artifacts such as:
 * </p>
 * <ul>
 *   <li>API specifications (OpenAPI, GraphQL SDL)</li>
 *   <li>README files</li>
 *   <li>Integration guides</li>
 *   <li>Architecture documentation</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating documentation writing from resource writing enables:
 * </p>
 * <ul>
 *   <li>Documentation-specific merge strategies</li>
 *   <li>Custom block support for user-maintained sections</li>
 *   <li>Appropriate header rendering for documentation formats</li>
 *   <li>Future support for documentation generation tools</li>
 * </ul>
 *
 * <h2>Documentation Formats</h2>
 * <p>
 * Common documentation formats include:
 * </p>
 * <ul>
 *   <li><strong>Markdown:</strong> README.md, integration guides</li>
 *   <li><strong>YAML:</strong> OpenAPI specifications</li>
 *   <li><strong>GraphQL SDL:</strong> GraphQL schemas</li>
 *   <li><strong>AsciiDoc:</strong> Technical documentation</li>
 * </ul>
 *
 * <h2>Custom Blocks in Documentation</h2>
 * <p>
 * Documentation files commonly use custom blocks for:
 * </p>
 * <ul>
 *   <li>Example code snippets</li>
 *   <li>Configuration samples</li>
 *   <li>Deployment instructions</li>
 *   <li>Troubleshooting guides</li>
 *   <li>Integration notes</li>
 * </ul>
 * <p>
 * Custom blocks allow plugins to generate documentation structure while
 * preserving user-written content.
 * </p>
 *
 * <h2>Header Generation</h2>
 * <p>
 * Headers are rendered using comment styles appropriate to the format:
 * </p>
 * <ul>
 *   <li>Markdown: Blockquote style {@code > ...}</li>
 *   <li>YAML: Shell-style {@code # ...}</li>
 *   <li>HTML: XML-style {@code <!-- ... -->}</li>
 * </ul>
 *
 * <h2>Output Location</h2>
 * <p>
 * Documentation is written to {@link StandardLocation#CLASS_OUTPUT} by default,
 * making it available in the build output alongside other artifacts.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * DocFile readme = DocFile.builder()
 *     .path("README.md")
 *     .content("# Generated API\\n...")
 *     .header(GeneratedHeader.minimalHexaGlue())
 *     .mergeMode(MergeMode.MERGE_CUSTOM_BLOCKS)
 *     .customBlocks(List.of(
 *         CustomBlock.of("examples"),
 *         CustomBlock.of("configuration")
 *     ))
 *     .build();
 *
 * DocWriter writer = new DocWriter(filerWriter, diagnostics);
 * boolean success = writer.write(readme, Optional.empty());
 * }</pre>
 */
public final class DocWriter {

    private static final DiagnosticCode CODE_INTERNAL_GENERATION_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-200");
    private static final DiagnosticCode CODE_MERGE_FAILED = DiagnosticCode.of("HG-MERGE-200");

    private final FilerWriter filerWriter;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new documentation file writer.
     *
     * @param filerWriter low-level filer writer (not {@code null})
     * @param diagnostics diagnostic reporter (not {@code null})
     */
    public DocWriter(FilerWriter filerWriter, DiagnosticReporter diagnostics) {
        this.filerWriter = Objects.requireNonNull(filerWriter, "filerWriter");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Writes a documentation file.
     *
     * @param docFile documentation file to write (not {@code null})
     * @param existingContent existing file content if file exists (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean write(DocFile docFile, Optional<String> existingContent) {
        Objects.requireNonNull(docFile, "docFile");
        Objects.requireNonNull(existingContent, "existingContent");

        // Prepare content with header
        String contentWithHeader = prepareContent(docFile);

        // Extract custom block IDs
        List<String> customBlockIds =
                docFile.customBlocks().stream().map(CustomBlock::id).collect(Collectors.toList());

        // Plan merge operation
        MergePlanner.MergePlan plan = MergePlanner.plan(
                contentWithHeader, existingContent, docFile.mergeMode(), docFile.header(), customBlockIds);

        // Execute based on plan
        switch (plan.action()) {
            case WRITE:
                String finalContent = plan.finalContent().orElse(contentWithHeader);
                return writeToFiler(docFile.path(), finalContent, docFile.charset());

            case SKIP:
                return true;

            case ERROR:
                // Report merge error as diagnostic
                diagnostics.error(
                        CODE_MERGE_FAILED,
                        "Merge failed for documentation file '" + docFile.path() + "': " + plan.message());
                return false;

            default:
                // This should never happen if MergeAction enum is complete
                diagnostics.error(
                        CODE_INTERNAL_GENERATION_ERROR,
                        "Internal error: unexpected merge action '" + plan.action() + "' for documentation file '"
                                + docFile.path() + "'");
                return false;
        }
    }

    /**
     * Writes a documentation file without merge planning (forced overwrite).
     *
     * <p>
     * This method bypasses merge logic and always overwrites the target file.
     * Use this when you know the file doesn't exist or when merge is not needed.
     * </p>
     *
     * @param docFile documentation file to write (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean writeOverwrite(DocFile docFile) {
        Objects.requireNonNull(docFile, "docFile");

        String contentWithHeader = prepareContent(docFile);
        return writeToFiler(docFile.path(), contentWithHeader, docFile.charset());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Content Preparation
    // ─────────────────────────────────────────────────────────────────────────

    private String prepareContent(DocFile docFile) {
        String content = docFile.content();

        // Prepend header if present
        if (docFile.header().isPresent()) {
            String header = renderHeaderForDoc(docFile.header().get(), docFile.path());
            content = header + "\n" + content;
        }

        return content;
    }

    private String renderHeaderForDoc(io.hexaglue.spi.codegen.GeneratedHeader header, String path) {
        // Determine comment style based on file extension
        String extension = getFileExtension(path);

        switch (extension) {
            case ".md":
            case ".markdown":
                return GeneratedHeaderEngine.renderMarkdown(header);

            case ".yaml":
            case ".yml":
                return GeneratedHeaderEngine.renderShell(header);

            case ".html":
            case ".xml":
                return GeneratedHeaderEngine.renderXml(header);

            case ".adoc":
            case ".asciidoc":
                // AsciiDoc uses line comments
                return GeneratedHeaderEngine.renderJavaLine(header);

            default:
                // Default to Markdown for documentation
                return GeneratedHeaderEngine.renderMarkdown(header);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filer Delegation
    // ─────────────────────────────────────────────────────────────────────────

    private boolean writeToFiler(String path, String content, java.nio.charset.Charset charset) {
        // Documentation uses empty package and relative path
        return filerWriter.writeTextResource(StandardLocation.CLASS_OUTPUT, "", path, content, charset);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return (lastDot >= 0) ? path.substring(lastDot) : "";
    }
}
