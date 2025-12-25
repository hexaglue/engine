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
package io.hexaglue.core.codegen.files;

import io.hexaglue.spi.codegen.CustomBlock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts, parses, and merges custom block regions in generated text files.
 *
 * <p>
 * Custom blocks are user-maintained sections within generated files that are preserved
 * across regeneration cycles. This engine handles the detection, extraction, and reinsertion
 * of these blocks during merge operations.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Custom blocks enable HexaGlue to regenerate infrastructure code without losing
 * user customizations. This engine provides:
 * </p>
 * <ul>
 *   <li>Robust parsing of custom block markers</li>
 *   <li>Extraction of block content from existing files</li>
 *   <li>Injection of preserved content into newly generated files</li>
 *   <li>Conflict detection when block IDs change</li>
 * </ul>
 *
 * <h2>Marker Syntax</h2>
 * <p>
 * Custom blocks are delimited by start and end markers:
 * </p>
 * <pre>
 * // @hexaglue-custom-start: block-id
 * ... user content ...
 * // @hexaglue-custom-end: block-id
 * </pre>
 * <p>
 * The marker format is intentionally simple and comment-style agnostic. The engine
 * supports line comments ({@code //}, {@code #}) and block comments ({@code /* ... *&#47;}).
 * </p>
 *
 * <h2>Merge Algorithm</h2>
 * <p>
 * When merging a newly generated file with an existing file:
 * </p>
 * <ol>
 *   <li>Parse existing file to extract all custom block contents</li>
 *   <li>Parse new file to find all custom block placeholders</li>
 *   <li>Match blocks by ID</li>
 *   <li>Replace placeholder content with preserved content</li>
 *   <li>Preserve formatting and indentation</li>
 * </ol>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The engine detects and reports:
 * </p>
 * <ul>
 *   <li>Unmatched start/end markers</li>
 *   <li>Duplicate block IDs within a file</li>
 *   <li>Orphaned blocks (present in old file but not in new template)</li>
 *   <li>New blocks (declared in template but not in old file)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * String existingContent = """
 *     // @hexaglue-custom-start: imports
 *     import com.example.MyCustomClass;
 *     // @hexaglue-custom-end: imports
 *     """;
 *
 * String newTemplate = """
 *     // @hexaglue-custom-start: imports
 *     // @hexaglue-custom-end: imports
 *     """;
 *
 * Map<String, String> blocks = CustomBlockEngine.extractBlocks(existingContent);
 * String merged = CustomBlockEngine.mergeBlocks(newTemplate, blocks);
 * // Result preserves the custom import
 * }</pre>
 */
public final class CustomBlockEngine {

    private static final String MARKER_PREFIX = "@hexaglue-custom-";
    private static final String START_MARKER = MARKER_PREFIX + "start";
    private static final String END_MARKER = MARKER_PREFIX + "end";

    // Matches: // @hexaglue-custom-start: block-id or # @hexaglue-custom-start: block-id, etc.
    private static final Pattern START_PATTERN = Pattern.compile(
            "^\\s*(?://|#|/\\*|<!--)\\s*" + Pattern.quote(START_MARKER) + ":\\s*([a-zA-Z0-9_-]+)\\s*(?:\\*/|-->)?\\s*$",
            Pattern.MULTILINE);

    // Matches: // @hexaglue-custom-end: block-id
    private static final Pattern END_PATTERN = Pattern.compile(
            "^\\s*(?://|#|/\\*|<!--)\\s*" + Pattern.quote(END_MARKER) + ":\\s*([a-zA-Z0-9_-]+)\\s*(?:\\*/|-->)?\\s*$",
            Pattern.MULTILINE);

    private CustomBlockEngine() {
        // Utility class, no instantiation
    }

    /**
     * Extracts all custom blocks from the given file content.
     *
     * <p>
     * Returns a map from block ID to the content between start and end markers.
     * The content includes leading and trailing newlines as they appear in the source.
     * </p>
     *
     * @param fileContent file content to parse (not {@code null})
     * @return map of block ID to preserved content (never {@code null}, possibly empty)
     * @throws IllegalArgumentException if markers are malformed or unmatched
     */
    public static Map<String, String> extractBlocks(String fileContent) {
        Objects.requireNonNull(fileContent, "fileContent");

        Map<String, String> blocks = new HashMap<>();
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
                    throw new IllegalArgumentException(
                            "Nested custom blocks not allowed. Found start of block '" + startMatcher.group(1)
                                    + "' at line " + (i + 1) + " while block '"
                                    + currentBlockId + "' is still open.");
                }
                currentBlockId = startMatcher.group(1);
                startLine = i;
                blockLines.clear();

            } else if (endMatcher.find()) {
                String endBlockId = endMatcher.group(1);
                if (currentBlockId == null) {
                    throw new IllegalArgumentException("Found end marker for block '" + endBlockId + "' at line "
                            + (i + 1) + " without matching start marker.");
                }
                if (!currentBlockId.equals(endBlockId)) {
                    throw new IllegalArgumentException(
                            "Block ID mismatch: started with '" + currentBlockId + "' at line "
                                    + (startLine + 1) + " but ended with '" + endBlockId
                                    + "' at line " + (i + 1) + ".");
                }

                // Store block content (everything between start and end markers)
                String blockContent = String.join("\n", blockLines);
                if (blocks.containsKey(currentBlockId)) {
                    throw new IllegalArgumentException(
                            "Duplicate custom block ID '" + currentBlockId + "' found in file.");
                }
                blocks.put(currentBlockId, blockContent);

                currentBlockId = null;
                startLine = -1;
                blockLines.clear();

            } else if (currentBlockId != null) {
                // Inside a block, accumulate content
                blockLines.add(line);
            }
        }

        if (currentBlockId != null) {
            throw new IllegalArgumentException(
                    "Unclosed custom block '" + currentBlockId + "' started at line " + (startLine + 1) + ".");
        }

        return Collections.unmodifiableMap(blocks);
    }

    /**
     * Merges preserved block content into a new file template.
     *
     * <p>
     * This method replaces the content between custom block markers in {@code newTemplate}
     * with the corresponding content from {@code preservedBlocks}. If a block exists in
     * the template but not in the preserved map, it remains empty.
     * </p>
     *
     * @param newTemplate new file template with custom block markers (not {@code null})
     * @param preservedBlocks map of block ID to preserved content (not {@code null})
     * @return merged content (never {@code null})
     */
    public static String mergeBlocks(String newTemplate, Map<String, String> preservedBlocks) {
        Objects.requireNonNull(newTemplate, "newTemplate");
        Objects.requireNonNull(preservedBlocks, "preservedBlocks");

        String[] lines = newTemplate.split("\n", -1);
        StringBuilder result = new StringBuilder();

        String currentBlockId = null;
        boolean skippingTemplateContent = false;

        for (String line : lines) {
            Matcher startMatcher = START_PATTERN.matcher(line);
            Matcher endMatcher = END_PATTERN.matcher(line);

            if (startMatcher.find()) {
                currentBlockId = startMatcher.group(1);
                skippingTemplateContent = true;

                // Write start marker
                result.append(line).append('\n');

                // Inject preserved content if available
                if (preservedBlocks.containsKey(currentBlockId)) {
                    String preserved = preservedBlocks.get(currentBlockId);
                    if (!preserved.isEmpty()) {
                        result.append(preserved);
                        if (!preserved.endsWith("\n")) {
                            result.append('\n');
                        }
                    }
                }

            } else if (endMatcher.find()) {
                skippingTemplateContent = false;
                currentBlockId = null;

                // Write end marker
                result.append(line).append('\n');

            } else if (!skippingTemplateContent) {
                // Outside custom block, preserve template line
                result.append(line).append('\n');
            }
            // If skippingTemplateContent, we discard template placeholder lines
        }

        // Remove final newline if original didn't have one
        String merged = result.toString();
        if (!newTemplate.endsWith("\n") && merged.endsWith("\n")) {
            merged = merged.substring(0, merged.length() - 1);
        }

        return merged;
    }

    /**
     * Generates standard custom block markers for a given block ID.
     *
     * <p>
     * This method generates start and end markers using Java line comment style.
     * The generated content includes placeholder text and formatting.
     * </p>
     *
     * @param block custom block descriptor (not {@code null})
     * @param commentStyle comment style to use (not {@code null})
     * @return formatted custom block region (never {@code null})
     */
    public static String generateBlockMarkers(CustomBlock block, String commentStyle) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(commentStyle, "commentStyle");

        String startMarker = commentStyle + " " + START_MARKER + ": " + block.id();
        String endMarker = commentStyle + " " + END_MARKER + ": " + block.id();

        StringBuilder sb = new StringBuilder();
        sb.append(startMarker).append('\n');

        if (block.description() != null) {
            sb.append(commentStyle).append(" ").append(block.description()).append('\n');
        }

        sb.append(endMarker).append('\n');

        return sb.toString();
    }

    /**
     * Validates that all declared custom blocks in the template are well-formed.
     *
     * <p>
     * This method checks that:
     * </p>
     * <ul>
     *   <li>All start markers have matching end markers</li>
     *   <li>Block IDs match between start and end</li>
     *   <li>No blocks are nested</li>
     *   <li>No duplicate block IDs exist</li>
     * </ul>
     *
     * @param fileContent file content to validate (not {@code null})
     * @throws IllegalArgumentException if validation fails
     */
    public static void validate(String fileContent) {
        Objects.requireNonNull(fileContent, "fileContent");
        // Validation is performed as a side effect of extraction
        extractBlocks(fileContent);
    }

    /**
     * Detects orphaned blocks that exist in the old file but not in the new template.
     *
     * <p>
     * Orphaned blocks may indicate that the generator no longer creates a particular
     * custom section, which could require manual intervention.
     * </p>
     *
     * @param oldFileContent old file content (not {@code null})
     * @param newTemplate new template content (not {@code null})
     * @return list of orphaned block IDs (never {@code null}, possibly empty)
     */
    public static List<String> detectOrphanedBlocks(String oldFileContent, String newTemplate) {
        Objects.requireNonNull(oldFileContent, "oldFileContent");
        Objects.requireNonNull(newTemplate, "newTemplate");

        Map<String, String> oldBlocks = extractBlocks(oldFileContent);
        Map<String, String> newBlocks = extractBlocks(newTemplate);

        List<String> orphaned = new ArrayList<>();
        for (String blockId : oldBlocks.keySet()) {
            if (!newBlocks.containsKey(blockId)) {
                orphaned.add(blockId);
            }
        }

        return orphaned;
    }
}
