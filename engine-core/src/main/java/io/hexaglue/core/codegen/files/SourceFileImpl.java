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

import io.hexaglue.spi.codegen.SourceFile;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of a source file with processing metadata.
 *
 * <p>
 * This class wraps the SPI {@link SourceFile} with additional internal state needed
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
 * <h2>Lifecycle</h2>
 * <pre>
 * 1. Creation:  from SPI SourceFile
 * 2. Planning:  resolve path, compute merge plan
 * 3. Emission:  write to file system, mark as processed
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * SourceFile spiFile = SourceFile.builder()
 *     .qualifiedTypeName("com.example.Foo")
 *     .content("public class Foo {}")
 *     .build();
 *
 * SourceFileImpl impl = SourceFileImpl.of(spiFile);
 * Path targetPath = impl.resolveTargetPath(sourceRoot);
 * }</pre>
 */
public final class SourceFileImpl {

    private final SourceFile sourceFile;
    private final Path resolvedPath;
    private final MergePlanner.MergePlan mergePlan;

    private SourceFileImpl(SourceFile sourceFile, Path resolvedPath, MergePlanner.MergePlan mergePlan) {
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile");
        this.resolvedPath = resolvedPath; // nullable until resolved
        this.mergePlan = mergePlan; // nullable until planned
    }

    /**
     * Creates an internal representation from an SPI source file.
     *
     * @param sourceFile SPI source file (not {@code null})
     * @return internal source file (never {@code null})
     */
    public static SourceFileImpl of(SourceFile sourceFile) {
        Objects.requireNonNull(sourceFile, "sourceFile");
        return new SourceFileImpl(sourceFile, null, null);
    }

    /**
     * Returns the underlying SPI source file.
     *
     * @return SPI source file (never {@code null})
     */
    public SourceFile sourceFile() {
        return sourceFile;
    }

    /**
     * Returns the resolved file system path if available.
     *
     * <p>
     * The path is resolved during the planning phase based on the qualified type name
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
    public SourceFileImpl withResolvedPath(Path path) {
        Objects.requireNonNull(path, "path");
        return new SourceFileImpl(this.sourceFile, path, this.mergePlan);
    }

    /**
     * Returns a new instance with the merge plan set.
     *
     * @param plan merge plan (not {@code null})
     * @return new instance with merge plan (never {@code null})
     */
    public SourceFileImpl withMergePlan(MergePlanner.MergePlan plan) {
        Objects.requireNonNull(plan, "plan");
        return new SourceFileImpl(this.sourceFile, this.resolvedPath, plan);
    }

    /**
     * Resolves the target file path based on the qualified type name.
     *
     * <p>
     * Converts the qualified type name to a relative path. For example,
     * {@code "com.example.Foo"} becomes {@code "com/example/Foo.java"}.
     * </p>
     *
     * @param sourceRoot source root directory (not {@code null})
     * @return resolved absolute path (never {@code null})
     */
    public Path resolveTargetPath(Path sourceRoot) {
        Objects.requireNonNull(sourceRoot, "sourceRoot");

        String qualifiedName = sourceFile.qualifiedTypeName();
        String relativePath = qualifiedName.replace('.', '/') + ".java";

        return sourceRoot.resolve(relativePath);
    }

    @Override
    public String toString() {
        return "SourceFileImpl{" + "qualifiedTypeName="
                + sourceFile.qualifiedTypeName() + ", resolvedPath="
                + resolvedPath + ", mergePlan="
                + (mergePlan != null ? mergePlan.action() : "none") + '}';
    }
}
