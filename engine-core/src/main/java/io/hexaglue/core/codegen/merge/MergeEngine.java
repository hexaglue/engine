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
import io.hexaglue.spi.codegen.MergeMode;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * High-level merge engine coordinating file merge operations.
 *
 * <p>
 * This engine provides the main entry point for merging newly generated content
 * with existing files. It coordinates parsing, strategy selection, execution,
 * and diagnostic reporting to provide a complete merge workflow.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Centralizing merge coordination enables:
 * </p>
 * <ul>
 *   <li>Consistent merge workflow across all file types</li>
 *   <li>Unified diagnostic reporting for merge errors</li>
 *   <li>Clear separation between merge logic and file I/O</li>
 *   <li>Testable merge operations without file system dependencies</li>
 * </ul>
 *
 * <h2>Merge Workflow</h2>
 * <pre>
 * 1. Validate inputs
 *    └─► Check merge mode, content, etc.
 *
 * 2. Select strategy
 *    └─► Based on MergeMode
 *
 * 3. Execute merge
 *    ├─► Parse existing content (if needed)
 *    ├─► Apply merge strategy
 *    └─► Produce merged content
 *
 * 4. Validate result
 *    ├─► Check for errors
 *    ├─► Detect orphaned blocks
 *    └─► Report diagnostics
 *
 * 5. Return result
 *    └─► Action + final content
 * </pre>
 *
 * <h2>Diagnostic Integration</h2>
 * <p>
 * The engine reports merge-related diagnostics:
 * </p>
 * <ul>
 *   <li>Parse errors in existing files</li>
 *   <li>Strategy execution failures</li>
 *   <li>Orphaned custom blocks</li>
 *   <li>Conflicting merge requirements</li>
 * </ul>
 *
 * <h2>Custom Block Analysis</h2>
 * <p>
 * When merging with custom blocks, the engine:
 * </p>
 * <ul>
 *   <li>Detects orphaned blocks (in old file but not in new template)</li>
 *   <li>Identifies new blocks (in template but not in old file)</li>
 *   <li>Reports block statistics for diagnostics</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * MergeEngine engine = new MergeEngine(diagnostics);
 *
 * MergeRequest request = MergeRequest.builder()
 *     .newContent(generatedContent)
 *     .existingContent(loadExistingFile())
 *     .mergeMode(MergeMode.MERGE_CUSTOM_BLOCKS)
 *     .customBlocks(List.of(CustomBlock.of("imports")))
 *     .location(DiagnosticLocation.ofQualifiedName("com.example.Foo"))
 *     .build();
 *
 * MergeResponse response = engine.merge(request);
 *
 * if (response.shouldWrite()) {
 *     writeFile(response.finalContent());
 * }
 * }</pre>
 */
public final class MergeEngine {

    private static final DiagnosticCode CODE_PARSE_ERROR = DiagnosticCode.of("HG-MERGE-200");
    private static final DiagnosticCode CODE_ORPHANED_BLOCKS = DiagnosticCode.of("HG-MERGE-100");
    private static final DiagnosticCode CODE_MERGE_FAILED = DiagnosticCode.of("HG-MERGE-201");
    private static final DiagnosticCode CODE_INTERNAL_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-200");

    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new merge engine.
     *
     * @param diagnostics diagnostic reporter (not {@code null})
     */
    public MergeEngine(DiagnosticReporter diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Executes a merge operation.
     *
     * @param request merge request (not {@code null})
     * @return merge response (never {@code null})
     */
    public MergeResponse merge(MergeRequest request) {
        Objects.requireNonNull(request, "request");

        // Validate request
        validateRequest(request);

        // Select merge strategy
        MergeStrategies.MergeStrategy strategy = MergeStrategies.forMode(request.mergeMode());

        // Execute merge
        MergeStrategies.MergeResult result = strategy.merge(request.newContent(), request.existingContent());

        // Handle result
        switch (result.action()) {
            case WRITE:
                // Analyze custom blocks if applicable
                if (request.mergeMode() == MergeMode.MERGE_CUSTOM_BLOCKS) {
                    analyzeCustomBlocks(request, result);
                }
                return MergeResponse.write(result.finalContent(), result.message());

            case SKIP:
                return MergeResponse.skip(result.message());

            case ERROR:
                reportMergeError(request.location(), result.message());
                return MergeResponse.error(result.message());

            default:
                // This should never happen if MergeAction enum is complete
                String errorMsg = "Internal error: unexpected merge action '" + result.action() + "'";
                diagnostics.report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.ERROR)
                        .code(CODE_INTERNAL_ERROR)
                        .message(errorMsg)
                        .location(request.location())
                        .build());
                return MergeResponse.error(errorMsg);
        }
    }

    /**
     * Performs a dry-run merge to validate without modifying files.
     *
     * @param request merge request (not {@code null})
     * @return {@code true} if merge would succeed
     */
    public boolean canMerge(MergeRequest request) {
        Objects.requireNonNull(request, "request");

        try {
            MergeResponse response = merge(request);
            return response.action() != MergeAction.ERROR;
        } catch (Exception e) {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────────────────

    private void validateRequest(MergeRequest request) {
        if (request.newContent() == null || request.newContent().isEmpty()) {
            throw new IllegalArgumentException("New content must not be empty");
        }

        if (request.mergeMode() == null) {
            throw new IllegalArgumentException("Merge mode must not be null");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Custom Block Analysis
    // ─────────────────────────────────────────────────────────────────────────

    private void analyzeCustomBlocks(MergeRequest request, MergeStrategies.MergeResult result) {
        if (request.existingContent() == null || request.existingContent().isEmpty()) {
            return; // No existing file, nothing to analyze
        }

        try {
            // Parse existing and new content
            List<CustomBlockParser.ParsedBlock> existingBlocks = CustomBlockParser.parse(request.existingContent());
            List<CustomBlockParser.ParsedBlock> newBlocks = CustomBlockParser.parse(request.newContent());

            // Detect orphaned blocks
            List<String> orphanedIds = findOrphanedBlocks(existingBlocks, newBlocks);

            if (!orphanedIds.isEmpty()) {
                reportOrphanedBlocks(request.location(), orphanedIds);
            }

        } catch (CustomBlockParser.ParseException e) {
            reportParseError(request.location(), e);
        }
    }

    private List<String> findOrphanedBlocks(
            List<CustomBlockParser.ParsedBlock> existingBlocks, List<CustomBlockParser.ParsedBlock> newBlocks) {
        List<String> orphaned = new ArrayList<>();
        List<String> newIds = new ArrayList<>();

        for (CustomBlockParser.ParsedBlock block : newBlocks) {
            newIds.add(block.id());
        }

        for (CustomBlockParser.ParsedBlock block : existingBlocks) {
            if (!newIds.contains(block.id())) {
                orphaned.add(block.id());
            }
        }

        return orphaned;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diagnostic Reporting
    // ─────────────────────────────────────────────────────────────────────────

    private void reportParseError(DiagnosticLocation location, CustomBlockParser.ParseException e) {
        diagnostics.report(Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(CODE_PARSE_ERROR)
                .message("Failed to parse custom blocks: " + e.getMessage())
                .location(location)
                .cause(e)
                .build());
    }

    private void reportOrphanedBlocks(DiagnosticLocation location, List<String> orphanedIds) {
        diagnostics.report(Diagnostic.builder()
                .severity(DiagnosticSeverity.WARNING)
                .code(CODE_ORPHANED_BLOCKS)
                .message("Orphaned custom blocks detected: " + String.join(", ", orphanedIds)
                        + ". These blocks exist in the old file but not in the new template.")
                .location(location)
                .build());
    }

    private void reportMergeError(DiagnosticLocation location, String message) {
        diagnostics.report(Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(CODE_MERGE_FAILED)
                .message("Merge operation failed: " + message)
                .location(location)
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Merge Request
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Request for a merge operation.
     */
    public static final class MergeRequest {
        private final String newContent;
        private final String existingContent;
        private final MergeMode mergeMode;
        private final List<CustomBlock> customBlocks;
        private final DiagnosticLocation location;

        private MergeRequest(Builder builder) {
            this.newContent = builder.newContent;
            this.existingContent = builder.existingContent;
            this.mergeMode = builder.mergeMode;
            this.customBlocks = (builder.customBlocks == null) ? List.of() : List.copyOf(builder.customBlocks);
            this.location = (builder.location == null) ? DiagnosticLocation.unknown() : builder.location;
        }

        public String newContent() {
            return newContent;
        }

        public String existingContent() {
            return existingContent;
        }

        public MergeMode mergeMode() {
            return mergeMode;
        }

        public List<CustomBlock> customBlocks() {
            return customBlocks;
        }

        public DiagnosticLocation location() {
            return location;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String newContent;
            private String existingContent;
            private MergeMode mergeMode;
            private List<CustomBlock> customBlocks;
            private DiagnosticLocation location;

            private Builder() {}

            public Builder newContent(String newContent) {
                this.newContent = newContent;
                return this;
            }

            public Builder existingContent(String existingContent) {
                this.existingContent = existingContent;
                return this;
            }

            public Builder mergeMode(MergeMode mergeMode) {
                this.mergeMode = mergeMode;
                return this;
            }

            public Builder customBlocks(List<CustomBlock> customBlocks) {
                this.customBlocks = customBlocks;
                return this;
            }

            public Builder location(DiagnosticLocation location) {
                this.location = location;
                return this;
            }

            public MergeRequest build() {
                return new MergeRequest(this);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Merge Response
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Response from a merge operation.
     */
    public static final class MergeResponse {
        private final MergeAction action;
        private final String finalContent;
        private final String message;

        private MergeResponse(MergeAction action, String finalContent, String message) {
            this.action = Objects.requireNonNull(action, "action");
            this.finalContent = finalContent;
            this.message = Objects.requireNonNull(message, "message");
        }

        public static MergeResponse write(String content, String message) {
            return new MergeResponse(MergeAction.WRITE, content, message);
        }

        public static MergeResponse skip(String message) {
            return new MergeResponse(MergeAction.SKIP, null, message);
        }

        public static MergeResponse error(String message) {
            return new MergeResponse(MergeAction.ERROR, null, message);
        }

        public MergeAction action() {
            return action;
        }

        public String finalContent() {
            return finalContent;
        }

        public String message() {
            return message;
        }

        public boolean shouldWrite() {
            return action == MergeAction.WRITE;
        }

        @Override
        public String toString() {
            return "MergeResponse{action=" + action + ", message='" + message + "'}";
        }
    }

    /**
     * Action to take based on merge result.
     */
    public enum MergeAction {
        WRITE,
        SKIP,
        ERROR
    }
}
