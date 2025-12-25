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
import io.hexaglue.spi.codegen.ResourceFile;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import javax.tools.StandardLocation;

/**
 * Writes resource files with merge support and header generation.
 *
 * <p>
 * This writer handles both text and binary resource files, including:
 * </p>
 * <ul>
 *   <li>Configuration files (properties, YAML, JSON)</li>
 *   <li>Binary resources (images, fonts, data files)</li>
 *   <li>Template files</li>
 *   <li>Static content</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating resource writing from source writing enables:
 * </p>
 * <ul>
 *   <li>Dual-mode handling (text vs binary)</li>
 *   <li>Resource-specific merge strategies</li>
 *   <li>Path-based organization (not package-based)</li>
 *   <li>Appropriate header rendering for different file formats</li>
 * </ul>
 *
 * <h2>Text vs Binary Resources</h2>
 * <p>
 * Resources can be either text-based or binary:
 * </p>
 * <ul>
 *   <li><strong>Text:</strong> Supports encoding, headers, potential merge</li>
 *   <li><strong>Binary:</strong> Raw bytes, no encoding, typically overwrite-only</li>
 * </ul>
 *
 * <h2>Resource Locations</h2>
 * <p>
 * Resources are typically written to {@link StandardLocation#CLASS_OUTPUT},
 * which corresponds to {@code target/classes} or equivalent. The resource path
 * is relative to this root.
 * </p>
 *
 * <h2>Header Generation</h2>
 * <p>
 * For text resources, headers are rendered using comment styles appropriate
 * to the file format:
 * </p>
 * <ul>
 *   <li>.properties, .yaml: Shell-style {@code #} comments</li>
 *   <li>.xml, .html: XML-style {@code <!-- -->} comments</li>
 *   <li>.md: Markdown blockquotes</li>
 * </ul>
 *
 * <h2>Merge Support</h2>
 * <p>
 * Text resources support the same merge modes as source files, though custom
 * blocks are less common. Binary resources typically use {@code OVERWRITE} mode.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Text resource
 * ResourceFile propsFile = ResourceFile.builder()
 *     .path("config/application.properties")
 *     .text("app.name=MyApp")
 *     .header(GeneratedHeader.minimalHexaGlue())
 *     .build();
 *
 * ResourceWriter writer = new ResourceWriter(filerWriter, diagnostics);
 * boolean success = writer.write(propsFile, Optional.empty());
 *
 * // Binary resource
 * ResourceFile imageFile = ResourceFile.builder()
 *     .path("images/logo.png")
 *     .bytes(pngBytes)
 *     .mergeMode(MergeMode.OVERWRITE)
 *     .build();
 *
 * success = writer.write(imageFile, Optional.empty());
 * }</pre>
 */
public final class ResourceWriter {

    private static final DiagnosticCode CODE_INVALID_RESOURCE = DiagnosticCode.of("HG-WRITE-100");
    private static final DiagnosticCode CODE_INTERNAL_GENERATION_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-200");
    private static final DiagnosticCode CODE_MERGE_FAILED = DiagnosticCode.of("HG-MERGE-200");

    private final FilerWriter filerWriter;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new resource file writer.
     *
     * @param filerWriter low-level filer writer (not {@code null})
     * @param diagnostics diagnostic reporter (not {@code null})
     */
    public ResourceWriter(FilerWriter filerWriter, DiagnosticReporter diagnostics) {
        this.filerWriter = Objects.requireNonNull(filerWriter, "filerWriter");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Writes a resource file.
     *
     * @param resourceFile resource file to write (not {@code null})
     * @param existingContent existing file content if file exists (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean write(ResourceFile resourceFile, Optional<String> existingContent) {
        Objects.requireNonNull(resourceFile, "resourceFile");
        Objects.requireNonNull(existingContent, "existingContent");

        if (resourceFile.text().isPresent()) {
            return writeTextResource(resourceFile, existingContent);
        } else if (resourceFile.bytes().isPresent()) {
            return writeBinaryResource(resourceFile);
        } else {
            reportInvalidResource(resourceFile.path());
            return false;
        }
    }

    /**
     * Writes a resource file without merge planning (forced overwrite).
     *
     * <p>
     * This method bypasses merge logic and always overwrites the target file.
     * Use this when you know the file doesn't exist or when merge is not needed.
     * </p>
     *
     * @param resourceFile resource file to write (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean writeOverwrite(ResourceFile resourceFile) {
        Objects.requireNonNull(resourceFile, "resourceFile");

        if (resourceFile.text().isPresent()) {
            String content = prepareTextContent(resourceFile);
            return writeToFiler(resourceFile.path(), content, resourceFile.charset());
        } else if (resourceFile.bytes().isPresent()) {
            return writeToFiler(resourceFile.path(), resourceFile.bytes().get());
        } else {
            reportInvalidResource(resourceFile.path());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Text Resource Writing
    // ─────────────────────────────────────────────────────────────────────────

    private boolean writeTextResource(ResourceFile resourceFile, Optional<String> existingContent) {
        String contentWithHeader = prepareTextContent(resourceFile);

        // Plan merge operation
        MergePlanner.MergePlan plan = MergePlanner.plan(
                contentWithHeader,
                existingContent,
                resourceFile.mergeMode(),
                resourceFile.header(),
                Collections.emptyList() // Resources rarely use custom blocks
                );

        // Execute based on plan
        switch (plan.action()) {
            case WRITE:
                String finalContent = plan.finalContent().orElse(contentWithHeader);
                return writeToFiler(resourceFile.path(), finalContent, resourceFile.charset());

            case SKIP:
                return true;

            case ERROR:
                // Report merge error as diagnostic
                diagnostics.error(
                        CODE_MERGE_FAILED,
                        "Merge failed for resource file '" + resourceFile.path() + "': " + plan.message());
                return false;

            default:
                // This should never happen if MergeAction enum is complete
                diagnostics.error(
                        CODE_INTERNAL_GENERATION_ERROR,
                        "Internal error: unexpected merge action '" + plan.action() + "' for resource file '"
                                + resourceFile.path() + "'");
                return false;
        }
    }

    private String prepareTextContent(ResourceFile resourceFile) {
        String content = resourceFile.text().orElse("");

        // Prepend header if present
        if (resourceFile.header().isPresent()) {
            String header = renderHeaderForResource(resourceFile.header().get(), resourceFile.path());
            content = header + "\n" + content;
        }

        return content;
    }

    private String renderHeaderForResource(io.hexaglue.spi.codegen.GeneratedHeader header, String path) {
        // Determine comment style based on file extension
        String extension = getFileExtension(path);

        switch (extension) {
            case ".properties":
            case ".yaml":
            case ".yml":
            case ".conf":
                return GeneratedHeaderEngine.renderShell(header);

            case ".xml":
            case ".html":
            case ".svg":
                return GeneratedHeaderEngine.renderXml(header);

            case ".md":
            case ".markdown":
                return GeneratedHeaderEngine.renderMarkdown(header);

            default:
                // Default to shell-style for unknown text formats
                return GeneratedHeaderEngine.renderShell(header);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Binary Resource Writing
    // ─────────────────────────────────────────────────────────────────────────

    private boolean writeBinaryResource(ResourceFile resourceFile) {
        return writeToFiler(resourceFile.path(), resourceFile.bytes().get());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filer Delegation
    // ─────────────────────────────────────────────────────────────────────────

    private boolean writeToFiler(String path, String content, java.nio.charset.Charset charset) {
        // Resources use empty package and relative path
        return filerWriter.writeTextResource(StandardLocation.CLASS_OUTPUT, "", path, content, charset);
    }

    private boolean writeToFiler(String path, byte[] bytes) {
        return filerWriter.writeBinaryResource(StandardLocation.CLASS_OUTPUT, "", path, bytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        return (lastDot >= 0) ? path.substring(lastDot) : "";
    }

    private void reportInvalidResource(String path) {
        diagnostics.report(Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(CODE_INVALID_RESOURCE)
                .message("Resource file '" + path + "' has neither text nor binary content")
                .location(DiagnosticLocation.ofPath(path, null, null))
                .build());
    }
}
