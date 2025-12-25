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

import io.hexaglue.spi.codegen.ResourceFile;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a resource file with processing metadata.
 *
 * <p>
 * This class wraps the SPI {@link ResourceFile} with additional internal state needed
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
 * <h2>Resource Path Resolution</h2>
 * <p>
 * Resource files use relative paths that are resolved against the resource output
 * directory. The path in the SPI model is preserved as-is, while the resolved path
 * is computed during the planning phase.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ResourceFile spiFile = ResourceFile.builder()
 *     .path("config/application.properties")
 *     .text("key=value")
 *     .build();
 *
 * ResourceFileImpl impl = ResourceFileImpl.of(spiFile);
 * Path targetPath = impl.resolveTargetPath(resourceRoot);
 * }</pre>
 */
public final class ResourceFileImpl {

    private final ResourceFile resourceFile;
    private final Path resolvedPath;
    private final MergePlanner.MergePlan mergePlan;

    private ResourceFileImpl(ResourceFile resourceFile, Path resolvedPath, MergePlanner.MergePlan mergePlan) {
        this.resourceFile = Objects.requireNonNull(resourceFile, "resourceFile");
        this.resolvedPath = resolvedPath; // nullable until resolved
        this.mergePlan = mergePlan; // nullable until planned
    }

    /**
     * Creates an internal representation from an SPI resource file.
     *
     * @param resourceFile SPI resource file (not {@code null})
     * @return internal resource file (never {@code null})
     */
    public static ResourceFileImpl of(ResourceFile resourceFile) {
        Objects.requireNonNull(resourceFile, "resourceFile");
        return new ResourceFileImpl(resourceFile, null, null);
    }

    /**
     * Returns the underlying SPI resource file.
     *
     * @return SPI resource file (never {@code null})
     */
    public ResourceFile resourceFile() {
        return resourceFile;
    }

    /**
     * Returns the resolved file system path if available.
     *
     * <p>
     * The path is resolved during the planning phase based on the resource path
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
    public ResourceFileImpl withResolvedPath(Path path) {
        Objects.requireNonNull(path, "path");
        return new ResourceFileImpl(this.resourceFile, path, this.mergePlan);
    }

    /**
     * Returns a new instance with the merge plan set.
     *
     * @param plan merge plan (not {@code null})
     * @return new instance with merge plan (never {@code null})
     */
    public ResourceFileImpl withMergePlan(MergePlanner.MergePlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new ResourceFileImpl(this.resourceFile, this.resolvedPath, plan);
    }

    /**
     * Resolves the target file path based on the resource path.
     *
     * <p>
     * The resource path is treated as a relative path from the resource root.
     * For example, {@code "config/app.properties"} is resolved relative to the
     * resource output directory.
     * </p>
     *
     * @param resourceRoot resource root directory (not {@code null})
     * @return resolved absolute path (never {@code null})
     */
    public Path resolveTargetPath(Path resourceRoot) {
        Objects.requireNonNull(resourceRoot, "resourceRoot");
        return resourceRoot.resolve(resourceFile.path());
    }

    /**
     * Returns whether this resource is text-based.
     *
     * @return {@code true} if the resource has text content
     */
    public boolean isText() {
        return resourceFile.text().isPresent();
    }

    /**
     * Returns whether this resource is binary-based.
     *
     * @return {@code true} if the resource has binary content
     */
    public boolean isBinary() {
        return resourceFile.bytes().isPresent();
    }

    @Override
    public String toString() {
        return "ResourceFileImpl{" + "path="
                + resourceFile.path() + ", type="
                + (isText() ? "text" : "binary") + ", resolvedPath="
                + resolvedPath + ", mergePlan="
                + (mergePlan != null ? mergePlan.action() : "none") + '}';
    }
}
