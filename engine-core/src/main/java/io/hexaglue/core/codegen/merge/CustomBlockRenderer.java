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
package io.hexaglue.core.codegen.merge;

import io.hexaglue.spi.codegen.CustomBlock;
import java.util.Objects;

/**
 * Renders custom block markers in generated file content.
 *
 * <p>
 * This renderer creates properly formatted custom block regions with start and end
 * markers, using comment styles appropriate to the target file format. It ensures
 * consistent marker formatting across all generated files.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating rendering from parsing and merging enables:
 * </p>
 * <ul>
 *   <li>Clean separation of concerns (parse, process, render)</li>
 *   <li>Consistent marker formatting across file types</li>
 *   <li>Support for multiple comment styles</li>
 *   <li>Testable rendering logic</li>
 * </ul>
 *
 * <h2>Comment Styles</h2>
 * <p>
 * The renderer supports multiple comment styles:
 * </p>
 * <ul>
 *   <li><strong>JAVA_LINE:</strong> {@code // ...} for Java, C, C++, JavaScript</li>
 *   <li><strong>SHELL:</strong> {@code # ...} for shell scripts, properties, YAML</li>
 *   <li><strong>XML:</strong> {@code <!-- ... -->} for XML, HTML</li>
 *   <li><strong>SQL:</strong> {@code -- ...} for SQL scripts</li>
 * </ul>
 *
 * <h2>Rendering Format</h2>
 * <p>
 * A typical rendered custom block:
 * </p>
 * <pre>
 * // @hexaglue-custom-start: block-id
 * // User-maintained content goes here
 * // @hexaglue-custom-end: block-id
 * </pre>
 * <p>
 * With optional description:
 * </p>
 * <pre>
 * // @hexaglue-custom-start: imports
 * // Add your custom imports here
 * // @hexaglue-custom-end: imports
 * </pre>
 *
 * <h2>Indentation</h2>
 * <p>
 * The renderer supports indentation for blocks nested within class or method
 * structures. Markers are indented to match the surrounding code context.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * CustomBlock block = CustomBlock.of("imports");
 *
 * // Render with Java line comments
 * String rendered = CustomBlockRenderer.render(
 *     block,
 *     CommentStyle.JAVA_LINE,
 *     ""  // no content yet
 * );
 *
 * // Result:
 * // // @hexaglue-custom-start: imports
 * // // @hexaglue-custom-end: imports
 *
 * // With indentation
 * String indented = CustomBlockRenderer.renderWithIndent(
 *     block,
 *     CommentStyle.JAVA_LINE,
 *     "    ",  // 4-space indent
 *     ""
 * );
 * }</pre>
 */
public final class CustomBlockRenderer {

    private static final String MARKER_PREFIX = "@hexaglue-custom-";
    private static final String START_MARKER = MARKER_PREFIX + "start";
    private static final String END_MARKER = MARKER_PREFIX + "end";

    private CustomBlockRenderer() {
        // Utility class, no instantiation
    }

    /**
     * Renders a custom block with the specified comment style.
     *
     * @param block custom block descriptor (not {@code null})
     * @param style comment style (not {@code null})
     * @param content block content to include (not {@code null}, possibly empty)
     * @return rendered block (never {@code null})
     */
    public static String render(CustomBlock block, CommentStyle style, String content) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(content, "content");

        return renderWithIndent(block, style, "", content);
    }

    /**
     * Renders a custom block with indentation.
     *
     * @param block custom block descriptor (not {@code null})
     * @param style comment style (not {@code null})
     * @param indent indentation prefix (not {@code null}, possibly empty)
     * @param content block content to include (not {@code null}, possibly empty)
     * @return rendered block (never {@code null})
     */
    public static String renderWithIndent(CustomBlock block, CommentStyle style, String indent, String content) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(style, "style");
        Objects.requireNonNull(indent, "indent");
        Objects.requireNonNull(content, "content");

        String commentPrefix = getCommentPrefix(style);
        StringBuilder sb = new StringBuilder();

        // Start marker
        sb.append(indent)
                .append(commentPrefix)
                .append(" ")
                .append(START_MARKER)
                .append(": ")
                .append(block.id())
                .append('\n');

        // Optional description
        if (block.description() != null && !block.description().isEmpty()) {
            sb.append(indent)
                    .append(commentPrefix)
                    .append(" ")
                    .append(block.description())
                    .append('\n');
        }

        // Content (if any)
        if (!content.isEmpty()) {
            String[] lines = content.split("\n", -1);
            for (String line : lines) {
                sb.append(indent).append(line).append('\n');
            }
        }

        // End marker
        sb.append(indent)
                .append(commentPrefix)
                .append(" ")
                .append(END_MARKER)
                .append(": ")
                .append(block.id())
                .append('\n');

        return sb.toString();
    }

    /**
     * Renders an empty custom block (markers only, no content).
     *
     * @param block custom block descriptor (not {@code null})
     * @param style comment style (not {@code null})
     * @return rendered block markers (never {@code null})
     */
    public static String renderEmpty(CustomBlock block, CommentStyle style) {
        return render(block, style, "");
    }

    /**
     * Renders just the start marker.
     *
     * @param blockId block identifier (not {@code null})
     * @param style comment style (not {@code null})
     * @return start marker (never {@code null})
     */
    public static String renderStartMarker(String blockId, CommentStyle style) {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(style, "style");

        String commentPrefix = getCommentPrefix(style);
        return commentPrefix + " " + START_MARKER + ": " + blockId;
    }

    /**
     * Renders just the end marker.
     *
     * @param blockId block identifier (not {@code null})
     * @param style comment style (not {@code null})
     * @return end marker (never {@code null})
     */
    public static String renderEndMarker(String blockId, CommentStyle style) {
        Objects.requireNonNull(blockId, "blockId");
        Objects.requireNonNull(style, "style");

        String commentPrefix = getCommentPrefix(style);
        return commentPrefix + " " + END_MARKER + ": " + blockId;
    }

    /**
     * Detects the appropriate comment style based on file extension.
     *
     * @param fileName file name or path (not {@code null})
     * @return detected comment style (never {@code null})
     */
    public static CommentStyle detectCommentStyle(String fileName) {
        Objects.requireNonNull(fileName, "fileName");

        String extension = getFileExtension(fileName);

        switch (extension) {
            case ".java":
            case ".js":
            case ".ts":
            case ".c":
            case ".cpp":
            case ".h":
            case ".hpp":
            case ".cs":
            case ".go":
                return CommentStyle.JAVA_LINE;

            case ".properties":
            case ".yaml":
            case ".yml":
            case ".sh":
            case ".bash":
            case ".conf":
            case ".ini":
            case ".py":
            case ".rb":
                return CommentStyle.SHELL;

            case ".xml":
            case ".html":
            case ".xhtml":
            case ".svg":
                return CommentStyle.XML;

            case ".sql":
                return CommentStyle.SQL;

            default:
                // Default to Java line style for unknown extensions
                return CommentStyle.JAVA_LINE;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilities
    // ─────────────────────────────────────────────────────────────────────────

    private static String getCommentPrefix(CommentStyle style) {
        switch (style) {
            case JAVA_LINE:
                return "//";
            case SHELL:
                return "#";
            case XML:
                return "<!--";
            case SQL:
                return "--";
            default:
                throw new IllegalArgumentException("Unsupported comment style: " + style);
        }
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot >= 0) ? fileName.substring(lastDot) : "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Comment Style
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Comment style for custom block rendering.
     */
    public enum CommentStyle {
        /**
         * Java/C/C++/JavaScript line comment style: {@code // ...}.
         */
        JAVA_LINE,

        /**
         * Shell/YAML/Python comment style: {@code # ...}.
         */
        SHELL,

        /**
         * XML/HTML comment style: {@code <!-- ... -->}.
         */
        XML,

        /**
         * SQL comment style: {@code -- ...}.
         */
        SQL
    }
}
