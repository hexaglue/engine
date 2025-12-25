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

import io.hexaglue.spi.codegen.DocFile;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a documentation file with processing metadata.
 *
 * <p>
 * This class wraps the SPI {@link DocFile} with additional internal state needed
 * during the file writing phase. It tracks file system paths, merge planning results,
 * and processing status.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating the SPI model from internal processing state enables:
 * </p>
 * <ul>
 *   <li>Clean separation between public API and internal implementation</li>
 *   <li>Tracking of processing status without polluting the SPI</li>
 *   <li>Resolution of file system paths once during planning</li>
 *   <li>Association of merge plans with specific files</li>
 * </ul>
 *
 * <h2>Documentation Path Resolution</h2>
 * <p>
 * Documentation files use relative paths that are resolved against the documentation
 * output directory. Common patterns include:
 * </p>
 * <ul>
 *   <li>{@code README.md} - Project documentation</li>
 *   <li>{@code api/OpenAPI.yaml} - API specifications</li>
 *   <li>{@code docs/integration-guide.md} - Integration guides</li>
 * </ul>
 *
 * <h2>Custom Block Support</h2>
 * <p>
 * Documentation files commonly include custom blocks for user-maintained sections
 * such as:
 * </p>
 * <ul>
 *   <li>Example code snippets</li>
 *   <li>Integration notes</li>
 *   <li>Configuration examples</li>
 *   <li>Deployment instructions</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * DocFile spiFile = DocFile.builder()
 *     .path("API.md")
 *     .content("# Generated API Documentation\\n...")
 *     .build();
 *
 * DocFileImpl impl = DocFileImpl.of(spiFile);
 * Path targetPath = impl.resolveTargetPath(docRoot);
 * }</pre>
 */
public final class DocFileImpl {

    private final DocFile docFile;
    private final Path resolvedPath;
    private final MergePlanner.MergePlan mergePlan;

    private DocFileImpl(DocFile docFile, Path resolvedPath, MergePlanner.MergePlan mergePlan) {
        this.docFile = Objects.requireNonNull(docFile, "docFile");
        this.resolvedPath = resolvedPath; // nullable until resolved
        this.mergePlan = mergePlan; // nullable until planned
    }

    /**
     * Creates an internal representation from an SPI documentation file.
     *
     * @param docFile SPI documentation file (not {@code null})
     * @return internal documentation file (never {@code null})
     */
    public static DocFileImpl of(DocFile docFile) {
        Objects.requireNonNull(docFile, "docFile");
        return new DocFileImpl(docFile, null, null);
    }

    /**
     * Returns the underlying SPI documentation file.
     *
     * @return SPI documentation file (never {@code null})
     */
    public DocFile docFile() {
        return docFile;
    }

    /**
     * Returns the resolved file system path if available.
     *
     * <p>
     * The path is resolved during the planning phase based on the documentation path
     * and the output directory configuration.
     * </p>
     *
     * @return resolved path if set
     */
    public Optional<Path> resolvedPath() {
        return Optional.ofNullable(resolvedPath);
    }

    /**
     * Returns the merge plan if available.
     *
     * <p>
     * The merge plan is computed during the planning phase based on the merge mode
     * and existing file content.
     * </p>
     *
     * @return merge plan if set
     */
    public Optional<MergePlanner.MergePlan> mergePlan() {
        return Optional.ofNullable(mergePlan);
    }

    /**
     * Returns a new instance with the resolved path set.
     *
     * @param path resolved file system path (not {@code null})
     * @return new instance with path (never {@code null})
     */
    public DocFileImpl withResolvedPath(Path path) {
        Objects.requireNonNull(path, "path");
        return new DocFileImpl(this.docFile, path, this.mergePlan);
    }

    /**
     * Returns a new instance with the merge plan set.
     *
     * @param plan merge plan (not {@code null})
     * @return new instance with merge plan (never {@code null})
     */
    public DocFileImpl withMergePlan(MergePlanner.MergePlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new DocFileImpl(this.docFile, this.resolvedPath, plan);
    }

    /**
     * Resolves the target file path based on the documentation path.
     *
     * <p>
     * The documentation path is treated as a relative path from the documentation root.
     * For example, {@code "api/OpenAPI.yaml"} is resolved relative to the documentation
     * output directory.
     * </p>
     *
     * @param docRoot documentation root directory (not {@code null})
     * @return resolved absolute path (never {@code null})
     */
    public Path resolveTargetPath(Path docRoot) {
        Objects.requireNonNull(docRoot, "docRoot");
        return docRoot.resolve(docFile.path());
    }

    /**
     * Returns whether this documentation file declares custom blocks.
     *
     * @return {@code true} if custom blocks are declared
     */
    public boolean hasCustomBlocks() {
        return !docFile.customBlocks().isEmpty();
    }

    /**
     * Returns the number of custom blocks declared in this documentation file.
     *
     * @return custom block count
     */
    public int customBlockCount() {
        return docFile.customBlocks().size();
    }

    @Override
    public String toString() {
        return "DocFileImpl{" + "path="
                + docFile.path() + ", customBlocks="
                + customBlockCount() + ", resolvedPath="
                + resolvedPath + ", mergePlan="
                + (mergePlan != null ? mergePlan.action() : "none") + '}';
    }
}
