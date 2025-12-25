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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses custom block markers from generated file content.
 *
 * <p>
 * This parser identifies and extracts custom block regions defined by start and end
 * markers in generated files. It provides structured access to block metadata and
 * content, enabling merge operations to preserve user-maintained sections.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating parsing from rendering and merging enables:
 * </p>
 * <ul>
 *   <li>Clean separation of concerns (parse, process, render)</li>
 *   <li>Testable parsing logic without merge dependencies</li>
 *   <li>Support for multiple marker formats and styles</li>
 *   <li>Robust error detection and reporting</li>
 * </ul>
 *
 * <h2>Marker Format</h2>
 * <p>
 * Custom blocks are delimited by start and end markers:
 * </p>
 * <pre>
 * // @hexaglue-custom-start: block-id
 * ... preserved content ...
 * // @hexaglue-custom-end: block-id
 * </pre>
 * <p>
 * The parser supports multiple comment styles:
 * </p>
 * <ul>
 *   <li>Line comments: {@code //}, {@code #}</li>
 *   <li>Block comments: {@code /* ... *&#47;}, {@code <!-- ... -->}</li>
 * </ul>
 *
 * <h2>Parsing Algorithm</h2>
 * <pre>
 * 1. Scan file line by line
 * 2. Detect start marker ──► Begin block accumulation
 * 3. Accumulate lines     ──► Add to current block
 * 4. Detect end marker    ──► Complete block
 * 5. Validate pairing     ──► Check ID consistency
 * 6. Return blocks        ──► Structured block list
 * </pre>
 *
 * <h2>Validation</h2>
 * <p>
 * The parser validates:
 * </p>
 * <ul>
 *   <li>Start/end marker pairing (no unmatched markers)</li>
 *   <li>Block ID consistency (start and end must match)</li>
 *   <li>No nested blocks (blocks cannot overlap)</li>
 *   <li>No duplicate block IDs within a file</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * String fileContent = """
 *     public class Foo {
 *         // @hexaglue-custom-start: imports
 *         import com.example.MyClass;
 *         // @hexaglue-custom-end: imports
 *
 *         // generated code
 *     }
 *     """;
 *
 * List<ParsedBlock> blocks = CustomBlockParser.parse(fileContent);
 * for (ParsedBlock block : blocks) {
 *     System.out.println("Block: " + block.id());
 *     System.out.println("Content: " + block.content());
 * }
 * }</pre>
 */
public final class CustomBlockParser {

    private static final String MARKER_PREFIX = "@hexaglue-custom-";
    private static final String START_MARKER = MARKER_PREFIX + "start";
    private static final String END_MARKER = MARKER_PREFIX + "end";

    // Matches start marker with optional comment syntax
    private static final Pattern START_PATTERN = Pattern.compile(
            "^\\s*(?://|#|/\\*|<!--)\\s*" + Pattern.quote(START_MARKER) + ":\\s*([a-zA-Z0-9_-]+)\\s*(?:\\*/|-->)?\\s*$",
            Pattern.MULTILINE);

    // Matches end marker with optional comment syntax
    private static final Pattern END_PATTERN = Pattern.compile(
            "^\\s*(?://|#|/\\*|<!--)\\s*" + Pattern.quote(END_MARKER) + ":\\s*([a-zA-Z0-9_-]+)\\s*(?:\\*/|-->)?\\s*$",
            Pattern.MULTILINE);

    private CustomBlockParser() {
        // Utility class, no instantiation
    }

    /**
     * Parses all custom blocks from the given file content.
     *
     * @param fileContent file content to parse (not {@code null})
     * @return list of parsed blocks (never {@code null}, possibly empty)
     * @throws ParseException if markers are malformed or validation fails
     */
    public static List<ParsedBlock> parse(String fileContent) throws ParseException {
        Objects.requireNonNull(fileContent, "fileContent");

        List<ParsedBlock> blocks = new ArrayList<>();
        String[] lines = fileContent.split("\n", -1);

        String currentBlockId = null;
        int startLine = -1;
        List<String> blockLines = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            Matcher startMatcher = START_PATTERN.matcher(line);
            Matcher endMatcher = END_PATTERN.matcher(line);

            if (startMatcher.find()) {
                if (currentBlockId != null) {
                    throw new ParseException(
                            "Nested custom blocks not allowed. Found start of block '" + startMatcher.group(1)
                                    + "' at line " + (i + 1) + " while block '"
                                    + currentBlockId + "' is still open.",
                            i + 1);
                }
                currentBlockId = startMatcher.group(1);
                startLine = i + 1; // 1-indexed for user-facing messages
                blockLines.clear();

            } else if (endMatcher.find()) {
                String endBlockId = endMatcher.group(1);
                if (currentBlockId == null) {
                    throw new ParseException(
                            "Found end marker for block '" + endBlockId + "' at line " + (i + 1)
                                    + " without matching start marker.",
                            i + 1);
                }
                if (!currentBlockId.equals(endBlockId)) {
                    throw new ParseException(
                            "Block ID mismatch: started with '" + currentBlockId + "' at line "
                                    + startLine + " but ended with '" + endBlockId
                                    + "' at line " + (i + 1) + ".",
                            i + 1);
                }

                // Create parsed block
                String content = String.join("\n", blockLines);
                blocks.add(new ParsedBlock(currentBlockId, content, startLine, i + 1));

                currentBlockId = null;
                startLine = -1;
                blockLines.clear();

            } else if (currentBlockId != null) {
                // Inside a block, accumulate content
                blockLines.add(line);
            }
        }

        if (currentBlockId != null) {
            throw new ParseException(
                    "Unclosed custom block '" + currentBlockId + "' started at line " + startLine + ".", startLine);
        }

        // Check for duplicate IDs
        validateNoDuplicates(blocks);

        return Collections.unmodifiableList(blocks);
    }

    /**
     * Checks if the given file content contains any custom blocks.
     *
     * @param fileContent file content to check (not {@code null})
     * @return {@code true} if custom blocks are present
     */
    public static boolean hasCustomBlocks(String fileContent) {
        Objects.requireNonNull(fileContent, "fileContent");
        return START_PATTERN.matcher(fileContent).find();
    }

    /**
     * Counts the number of custom blocks in the given file content.
     *
     * @param fileContent file content to analyze (not {@code null})
     * @return number of custom blocks
     * @throws ParseException if parsing fails
     */
    public static int countBlocks(String fileContent) throws ParseException {
        Objects.requireNonNull(fileContent, "fileContent");
        return parse(fileContent).size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private static void validateNoDuplicates(List<ParsedBlock> blocks) throws ParseException {
        List<String> ids = new ArrayList<>();
        for (ParsedBlock block : blocks) {
            if (ids.contains(block.id())) {
                throw new ParseException(
                        "Duplicate custom block ID '" + block.id() + "' found in file.", block.startLine());
            }
            ids.add(block.id());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parsed Block
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Represents a parsed custom block with metadata.
     */
    public static final class ParsedBlock {
        private final String id;
        private final String content;
        private final int startLine;
        private final int endLine;

        private ParsedBlock(String id, String content, int startLine, int endLine) {
            this.id = Objects.requireNonNull(id, "id");
            this.content = Objects.requireNonNull(content, "content");
            this.startLine = startLine;
            this.endLine = endLine;
        }

        /**
         * Returns the block ID.
         *
         * @return block ID (never {@code null})
         */
        public String id() {
            return id;
        }

        /**
         * Returns the block content (lines between start and end markers).
         *
         * @return content (never {@code null}, possibly empty)
         */
        public String content() {
            return content;
        }

        /**
         * Returns the start line number (1-indexed).
         *
         * @return start line number
         */
        public int startLine() {
            return startLine;
        }

        /**
         * Returns the end line number (1-indexed).
         *
         * @return end line number
         */
        public int endLine() {
            return endLine;
        }

        /**
         * Returns whether this block is empty (no content between markers).
         *
         * @return {@code true} if content is empty or whitespace-only
         */
        public boolean isEmpty() {
            return content.trim().isEmpty();
        }

        /**
         * Returns the number of lines in this block.
         *
         * @return line count
         */
        public int lineCount() {
            return content.isEmpty() ? 0 : content.split("\n", -1).length;
        }

        @Override
        public String toString() {
            return "ParsedBlock{" + "id='"
                    + id + '\'' + ", lines="
                    + startLine + "-" + endLine + ", contentLength="
                    + content.length() + '}';
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parse Exception
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exception thrown when custom block parsing fails.
     */
    public static final class ParseException extends Exception {
        private final int lineNumber;

        /**
         * Creates a parse exception.
         *
         * @param message error message (not {@code null})
         * @param lineNumber line number where error occurred (1-indexed)
         */
        public ParseException(String message, int lineNumber) {
            super(message);
            this.lineNumber = lineNumber;
        }

        /**
         * Returns the line number where the error occurred.
         *
         * @return line number (1-indexed)
         */
        public int lineNumber() {
            return lineNumber;
        }

        @Override
        public String toString() {
            return "ParseException at line " + lineNumber + ": " + getMessage();
        }
    }
}
