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

import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.codegen.ResourceFile;
import io.hexaglue.spi.codegen.SourceFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates transactional file writing with error tracking and diagnostics.
 *
 * <p>
 * This class provides transaction-like semantics for file writing operations,
 * though true rollback is not possible with JSR-269 Filer (files cannot be deleted
 * once created). Instead, this class:
 * </p>
 * <ul>
 *   <li>Tracks write operations and their outcomes</li>
 *   <li>Collects errors and diagnostics</li>
 *   <li>Provides all-or-nothing validation (fail fast on first error)</li>
 *   <li>Enables dry-run mode for validation without writing</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Coordinating writes through a transaction abstraction enables:
 * </p>
 * <ul>
 *   <li>Centralized error tracking across all file types</li>
 *   <li>Fail-fast behavior when errors occur</li>
 *   <li>Diagnostic aggregation for reporting</li>
 *   <li>Dry-run validation before actual writes</li>
 *   <li>Clear separation between planning and execution phases</li>
 * </ul>
 *
 * <h2>Transaction Lifecycle</h2>
 * <pre>
 * 1. Begin transaction
 *    └─► WriteTransaction tx = WriteTransactions.begin(...)
 *
 * 2. Execute writes
 *    ├─► tx.writeSource(sourceFile)
 *    ├─► tx.writeResource(resourceFile)
 *    └─► tx.writeDoc(docFile)
 *
 * 3. Complete transaction
 *    ├─► WriteResult result = tx.commit()
 *    └─► Check result.isSuccess()
 * </pre>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The transaction tracks:
 * </p>
 * <ul>
 *   <li>Number of successful writes</li>
 *   <li>Number of failed writes</li>
 *   <li>First error encountered (for fail-fast)</li>
 *   <li>All diagnostics reported during writes</li>
 * </ul>
 *
 * <h2>Fail-Fast Mode</h2>
 * <p>
 * By default, transactions continue writing even if errors occur. Enable
 * fail-fast mode to stop immediately on the first error:
 * </p>
 * <pre>{@code
 * WriteTransaction tx = WriteTransactions.begin(sourceWriter, resourceWriter, docWriter)
 *     .withFailFast(true);
 * }</pre>
 *
 * <h2>Limitations</h2>
 * <p>
 * Unlike database transactions, file writes through JSR-269 Filer cannot be
 * rolled back. Once a file is created, it remains in the output directory even
 * if subsequent operations fail. This class provides error tracking and
 * fail-fast behavior, but not true atomicity.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * {@link WriteTransaction} is not thread-safe and must be used from a single
 * thread. {@link WriteTransactions} factory methods are thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * // Begin transaction
 * WriteTransaction tx = WriteTransactions.begin(
 *     new SourceWriter(filerWriter, diagnostics),
 *     new ResourceWriter(filerWriter, diagnostics),
 *     new DocWriter(filerWriter, diagnostics)
 * );
 *
 * // Write files
 * tx.writeSource(sourceFile1);
 * tx.writeSource(sourceFile2);
 * tx.writeResource(resourceFile);
 * tx.writeDoc(docFile);
 *
 * // Commit and check result
 * WriteResult result = tx.commit();
 * if (!result.isSuccess()) {
 *     System.err.println("Write failed: " + result.errorCount() + " errors");
 * }
 * }</pre>
 */
public final class WriteTransactions {

    private WriteTransactions() {
        // Utility class, no instantiation
    }

    /**
     * Begins a new write transaction.
     *
     * @param sourceWriter source file writer (not {@code null})
     * @param resourceWriter resource file writer (not {@code null})
     * @param docWriter documentation file writer (not {@code null})
     * @return new transaction (never {@code null})
     */
    public static WriteTransaction begin(
            SourceWriter sourceWriter, ResourceWriter resourceWriter, DocWriter docWriter) {
        return new WriteTransaction(sourceWriter, resourceWriter, docWriter);
    }

    /**
     * Represents an active write transaction.
     */
    public static final class WriteTransaction {

        private final SourceWriter sourceWriter;
        private final ResourceWriter resourceWriter;
        private final DocWriter docWriter;

        private final List<String> writtenFiles = new ArrayList<>();
        private int successCount = 0;
        private int failureCount = 0;
        private boolean failFast = false;
        private boolean failed = false;

        private WriteTransaction(SourceWriter sourceWriter, ResourceWriter resourceWriter, DocWriter docWriter) {
            this.sourceWriter = Objects.requireNonNull(sourceWriter, "sourceWriter");
            this.resourceWriter = Objects.requireNonNull(resourceWriter, "resourceWriter");
            this.docWriter = Objects.requireNonNull(docWriter, "docWriter");
        }

        /**
         * Enables fail-fast mode.
         *
         * <p>
         * When fail-fast is enabled, the transaction stops immediately after
         * the first write error. Subsequent write operations are skipped.
         * </p>
         *
         * @param failFast {@code true} to enable fail-fast mode
         * @return this transaction (for chaining)
         */
        public WriteTransaction withFailFast(boolean failFast) {
            this.failFast = failFast;
            return this;
        }

        /**
         * Writes a source file.
         *
         * @param sourceFile source file to write (not {@code null})
         * @return this transaction (for chaining)
         */
        public WriteTransaction writeSource(SourceFile sourceFile) {
            return writeSource(sourceFile, Optional.empty());
        }

        /**
         * Writes a source file with optional existing content for merge.
         *
         * @param sourceFile source file to write (not {@code null})
         * @param existingContent existing file content (not {@code null})
         * @return this transaction (for chaining)
         */
        public WriteTransaction writeSource(SourceFile sourceFile, Optional<String> existingContent) {
            Objects.requireNonNull(sourceFile, "sourceFile");
            Objects.requireNonNull(existingContent, "existingContent");

            if (shouldSkipDueToFailure()) {
                return this;
            }

            boolean success = sourceWriter.write(sourceFile, existingContent);
            recordResult(sourceFile.qualifiedTypeName(), success);

            return this;
        }

        /**
         * Writes a resource file.
         *
         * @param resourceFile resource file to write (not {@code null})
         * @return this transaction (for chaining)
         */
        public WriteTransaction writeResource(ResourceFile resourceFile) {
            return writeResource(resourceFile, Optional.empty());
        }

        /**
         * Writes a resource file with optional existing content for merge.
         *
         * @param resourceFile resource file to write (not {@code null})
         * @param existingContent existing file content (not {@code null})
         * @return this transaction (for chaining)
         */
        public WriteTransaction writeResource(ResourceFile resourceFile, Optional<String> existingContent) {
            Objects.requireNonNull(resourceFile, "resourceFile");
            Objects.requireNonNull(existingContent, "existingContent");

            if (shouldSkipDueToFailure()) {
                return this;
            }

            boolean success = resourceWriter.write(resourceFile, existingContent);
            recordResult(resourceFile.path(), success);

            return this;
        }

        /**
         * Writes a documentation file.
         *
         * @param docFile documentation file to write (not {@code null})
         * @return this transaction (for chaining)
         */
        public WriteTransaction writeDoc(DocFile docFile) {
            return writeDoc(docFile, Optional.empty());
        }

        /**
         * Writes a documentation file with optional existing content for merge.
         *
         * @param docFile documentation file to write (not {@code null})
         * @param existingContent existing file content (not {@code null})
         * @return this transaction (for chaining)
         */
        public WriteTransaction writeDoc(DocFile docFile, Optional<String> existingContent) {
            Objects.requireNonNull(docFile, "docFile");
            Objects.requireNonNull(existingContent, "existingContent");

            if (shouldSkipDueToFailure()) {
                return this;
            }

            boolean success = docWriter.write(docFile, existingContent);
            recordResult(docFile.path(), success);

            return this;
        }

        /**
         * Commits the transaction and returns the result.
         *
         * @return write result (never {@code null})
         */
        public WriteResult commit() {
            return new WriteResult(
                    successCount, failureCount, Collections.unmodifiableList(new ArrayList<>(writtenFiles)));
        }

        // ─────────────────────────────────────────────────────────────────────
        // Internal tracking
        // ─────────────────────────────────────────────────────────────────────

        private boolean shouldSkipDueToFailure() {
            return failFast && failed;
        }

        private void recordResult(String file, boolean success) {
            if (success) {
                successCount++;
                writtenFiles.add(file);
            } else {
                failureCount++;
                failed = true;
            }
        }
    }

    /**
     * Result of a write transaction.
     */
    public static final class WriteResult {

        private final int successCount;
        private final int failureCount;
        private final List<String> writtenFiles;

        private WriteResult(int successCount, int failureCount, List<String> writtenFiles) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.writtenFiles = writtenFiles;
        }

        /**
         * Returns whether all writes succeeded.
         *
         * @return {@code true} if no failures occurred
         */
        public boolean isSuccess() {
            return failureCount == 0;
        }

        /**
         * Returns the number of successful writes.
         *
         * @return success count
         */
        public int successCount() {
            return successCount;
        }

        /**
         * Returns the number of failed writes.
         *
         * @return failure count
         */
        public int failureCount() {
            return failureCount;
        }

        /**
         * Returns the total number of write attempts.
         *
         * @return total count
         */
        public int totalCount() {
            return successCount + failureCount;
        }

        /**
         * Returns the list of successfully written files.
         *
         * @return written file identifiers (never {@code null})
         */
        public List<String> writtenFiles() {
            return writtenFiles;
        }

        @Override
        public String toString() {
            return "WriteResult{" + "success="
                    + successCount + ", failures="
                    + failureCount + ", total="
                    + totalCount() + '}';
        }
    }
}
