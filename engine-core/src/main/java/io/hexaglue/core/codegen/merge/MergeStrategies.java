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

import io.hexaglue.spi.codegen.MergeMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Collection of merge strategy implementations for different {@link MergeMode} values.
 *
 * <p>
 * This class provides concrete merge strategies that determine how to combine
 * newly generated content with existing file content. Each strategy corresponds
 * to a {@link MergeMode} and implements specific merge logic.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Centralizing merge strategies enables:
 * </p>
 * <ul>
 *   <li>Consistent merge behavior across all file types</li>
 *   <li>Clear separation between merge logic and file writing</li>
 *   <li>Testable merge algorithms without I/O dependencies</li>
 *   <li>Easy addition of new merge strategies</li>
 * </ul>
 *
 * <h2>Available Strategies</h2>
 * <ul>
 *   <li><strong>OVERWRITE:</strong> Replace existing content unconditionally</li>
 *   <li><strong>MERGE_CUSTOM_BLOCKS:</strong> Preserve custom block regions</li>
 *   <li><strong>WRITE_ONCE:</strong> Keep existing file if present</li>
 *   <li><strong>FAIL_IF_EXISTS:</strong> Error if file exists</li>
 * </ul>
 *
 * <h2>Strategy Selection</h2>
 * <p>
 * Strategies are selected based on {@link MergeMode} and file state:
 * </p>
 * <pre>
 * MergeStrategy strategy = MergeStrategies.forMode(mergeMode);
 * MergeResult result = strategy.merge(newContent, existingContent);
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All strategies are stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * String newContent = "public class Foo {}";
 * String existingContent = loadExistingFile();
 *
 * MergeStrategy strategy = MergeStrategies.forMode(MergeMode.MERGE_CUSTOM_BLOCKS);
 * MergeResult result = strategy.merge(newContent, existingContent);
 *
 * if (result.shouldWrite()) {
 *     writeFile(result.finalContent());
 * }
 * }</pre>
 */
public final class MergeStrategies {

    private MergeStrategies() {
        // Utility class, no instantiation
    }

    /**
     * Returns the merge strategy for the given merge mode.
     *
     * @param mode merge mode (not {@code null})
     * @return merge strategy (never {@code null})
     */
    public static MergeStrategy forMode(MergeMode mode) {
        Objects.requireNonNull(mode, "mode");

        switch (mode) {
            case OVERWRITE:
                return new OverwriteStrategy();
            case MERGE_CUSTOM_BLOCKS:
                return new CustomBlockMergeStrategy();
            case WRITE_ONCE:
                return new WriteOnceStrategy();
            case FAIL_IF_EXISTS:
                return new FailIfExistsStrategy();
            default:
                throw new IllegalArgumentException("Unsupported merge mode: " + mode);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Merge Strategy Interface
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Strategy for merging new content with existing content.
     */
    public interface MergeStrategy {

        /**
         * Merges new content with existing content.
         *
         * @param newContent new generated content (not {@code null})
         * @param existingContent existing file content (nullable)
         * @return merge result (never {@code null})
         */
        MergeResult merge(String newContent, String existingContent);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overwrite Strategy
    // ─────────────────────────────────────────────────────────────────────────

    private static final class OverwriteStrategy implements MergeStrategy {

        @Override
        public MergeResult merge(String newContent, String existingContent) {
            Objects.requireNonNull(newContent, "newContent");
            // Always write, always use new content
            return MergeResult.write(newContent, "Overwriting existing file");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Custom Block Merge Strategy
    // ─────────────────────────────────────────────────────────────────────────

    private static final class CustomBlockMergeStrategy implements MergeStrategy {

        @Override
        public MergeResult merge(String newContent, String existingContent) {
            Objects.requireNonNull(newContent, "newContent");

            // No existing file, write new content
            if (existingContent == null || existingContent.isEmpty()) {
                return MergeResult.write(newContent, "No existing file, writing new content");
            }

            try {
                // Parse existing content to extract custom blocks
                Map<String, String> preservedBlocks = extractCustomBlocks(existingContent);

                // Merge preserved content into new template
                String mergedContent = mergeCustomBlocks(newContent, preservedBlocks);

                return MergeResult.write(mergedContent, "Merged " + preservedBlocks.size() + " custom block(s)");

            } catch (CustomBlockParser.ParseException e) {
                return MergeResult.error("Custom block merge failed: " + e.getMessage());
            }
        }

        private Map<String, String> extractCustomBlocks(String content) throws CustomBlockParser.ParseException {
            Map<String, String> blocks = new HashMap<>();
            for (CustomBlockParser.ParsedBlock block : CustomBlockParser.parse(content)) {
                blocks.put(block.id(), block.content());
            }
            return blocks;
        }

        private String mergeCustomBlocks(String newContent, Map<String, String> preservedBlocks)
                throws CustomBlockParser.ParseException {
            // Parse new content to find block placeholders
            String[] lines = newContent.split("\n", -1);
            StringBuilder result = new StringBuilder();

            String currentBlockId = null;
            boolean skippingTemplateContent = false;

            for (String line : lines) {
                // Check for start marker
                if (isStartMarker(line)) {
                    currentBlockId = extractBlockId(line);
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

                } else if (isEndMarker(line)) {
                    skippingTemplateContent = false;
                    currentBlockId = null;

                    // Write end marker
                    result.append(line).append('\n');

                } else if (!skippingTemplateContent) {
                    // Outside custom block, preserve template line
                    result.append(line).append('\n');
                }
                // If skippingTemplateContent, discard template placeholder lines
            }

            // Remove final newline if original didn't have one
            String merged = result.toString();
            if (!newContent.endsWith("\n") && merged.endsWith("\n")) {
                merged = merged.substring(0, merged.length() - 1);
            }

            return merged;
        }

        private boolean isStartMarker(String line) {
            return line.contains("@hexaglue-custom-start:");
        }

        private boolean isEndMarker(String line) {
            return line.contains("@hexaglue-custom-end:");
        }

        private String extractBlockId(String line) {
            int colonIndex = line.indexOf("@hexaglue-custom-start:");
            if (colonIndex < 0) {
                colonIndex = line.indexOf("@hexaglue-custom-end:");
            }
            if (colonIndex < 0) {
                return "";
            }

            String afterMarker = line.substring(colonIndex).split(":", 2)[1];
            return afterMarker.trim().split("\\s+")[0].replaceAll("[^a-zA-Z0-9_-]", "");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Write Once Strategy
    // ─────────────────────────────────────────────────────────────────────────

    private static final class WriteOnceStrategy implements MergeStrategy {

        @Override
        public MergeResult merge(String newContent, String existingContent) {
            Objects.requireNonNull(newContent, "newContent");

            if (existingContent != null && !existingContent.isEmpty()) {
                // File exists, skip writing
                return MergeResult.skip("File already exists (WRITE_ONCE mode)");
            }

            // No existing file, write new content
            return MergeResult.write(newContent, "No existing file, writing new content");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fail If Exists Strategy
    // ─────────────────────────────────────────────────────────────────────────

    private static final class FailIfExistsStrategy implements MergeStrategy {

        @Override
        public MergeResult merge(String newContent, String existingContent) {
            Objects.requireNonNull(newContent, "newContent");

            if (existingContent != null && !existingContent.isEmpty()) {
                // File exists, error
                return MergeResult.error("File already exists and FAIL_IF_EXISTS mode is active");
            }

            // No existing file, write new content
            return MergeResult.write(newContent, "No existing file, writing new content");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Merge Result
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Result of a merge operation.
     */
    public static final class MergeResult {

        private final MergeAction action;
        private final String finalContent;
        private final String message;

        private MergeResult(MergeAction action, String finalContent, String message) {
            this.action = Objects.requireNonNull(action, "action");
            this.finalContent = finalContent; // nullable for SKIP and ERROR
            this.message = Objects.requireNonNull(message, "message");
        }

        /**
         * Creates a result indicating write should proceed.
         *
         * @param content final content to write (not {@code null})
         * @param message descriptive message (not {@code null})
         * @return merge result (never {@code null})
         */
        public static MergeResult write(String content, String message) {
            Objects.requireNonNull(content, "content");
            return new MergeResult(MergeAction.WRITE, content, message);
        }

        /**
         * Creates a result indicating write should be skipped.
         *
         * @param message reason for skipping (not {@code null})
         * @return merge result (never {@code null})
         */
        public static MergeResult skip(String message) {
            return new MergeResult(MergeAction.SKIP, null, message);
        }

        /**
         * Creates a result indicating an error occurred.
         *
         * @param message error message (not {@code null})
         * @return merge result (never {@code null})
         */
        public static MergeResult error(String message) {
            return new MergeResult(MergeAction.ERROR, null, message);
        }

        /**
         * Returns the merge action.
         *
         * @return action (never {@code null})
         */
        public MergeAction action() {
            return action;
        }

        /**
         * Returns whether the write should proceed.
         *
         * @return {@code true} if action is WRITE
         */
        public boolean shouldWrite() {
            return action == MergeAction.WRITE;
        }

        /**
         * Returns the final merged content.
         *
         * @return content (nullable, only present for WRITE action)
         */
        public String finalContent() {
            return finalContent;
        }

        /**
         * Returns a descriptive message about the merge result.
         *
         * @return message (never {@code null})
         */
        public String message() {
            return message;
        }

        @Override
        public String toString() {
            return "MergeResult{action=" + action + ", message='" + message + "'}";
        }
    }

    /**
     * Action to take based on merge result.
     */
    public enum MergeAction {
        /** Write the merged content. */
        WRITE,
        /** Skip writing (preserve existing file). */
        SKIP,
        /** Error occurred during merge. */
        ERROR
    }
}
