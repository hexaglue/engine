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
package io.hexaglue.core.codegen;

import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.codegen.ResourceFile;
import io.hexaglue.spi.codegen.SourceFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable plan representing all artifacts to be generated in this compilation round.
 *
 * <p>
 * An artifact plan aggregates {@link SourceFile}, {@link ResourceFile}, and {@link DocFile}
 * instances collected from plugins during the generation phase. It provides conflict detection,
 * duplicate analysis, and artifact grouping for the emission phase.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Separating collection ({@link DefaultArtifactSink}) from planning (this class) enables:
 * </p>
 * <ul>
 *   <li>Validation before any I/O occurs</li>
 *   <li>Centralized conflict detection across all plugins</li>
 *   <li>Clear separation between plugin API and core logic</li>
 *   <li>Testable artifact analysis without file system dependencies</li>
 * </ul>
 *
 * <h2>Conflict Detection</h2>
 * <p>
 * A conflict occurs when multiple artifacts target the same output location with incompatible
 * content or merge modes. The plan tracks:
 * </p>
 * <ul>
 *   <li>Duplicate qualified names for source files</li>
 *   <li>Duplicate paths for resource and documentation files</li>
 *   <li>Conflicting merge strategies for the same file</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ArtifactPlan plan = ArtifactPlan.builder()
 *     .addSource(sourceFile)
 *     .addResource(resourceFile)
 *     .addDoc(docFile)
 *     .build();
 *
 * if (plan.hasConflicts()) {
 *     for (String conflict : plan.getConflicts()) {
 *         diagnostics.error(conflict);
 *     }
 * }
 *
 * // Proceed with emission
 * emitter.emit(plan);
 * }</pre>
 */
public final class ArtifactPlan {

    private final List<SourceFile> sourceFiles;
    private final List<ResourceFile> resourceFiles;
    private final List<DocFile> docFiles;
    private final Map<String, List<SourceFile>> sourcesByQualifiedName;
    private final Map<String, List<ResourceFile>> resourcesByPath;
    private final Map<String, List<DocFile>> docsByPath;

    private ArtifactPlan(Builder builder) {
        this.sourceFiles = List.copyOf(builder.sourceFiles);
        this.resourceFiles = List.copyOf(builder.resourceFiles);
        this.docFiles = List.copyOf(builder.docFiles);

        // Build indexes for conflict detection
        this.sourcesByQualifiedName = buildSourceIndex(this.sourceFiles);
        this.resourcesByPath = buildResourceIndex(this.resourceFiles);
        this.docsByPath = buildDocIndex(this.docFiles);
    }

    /**
     * Returns all source files in this plan.
     *
     * @return source files (never {@code null}, possibly empty)
     */
    public List<SourceFile> sourceFiles() {
        return sourceFiles;
    }

    /**
     * Returns all resource files in this plan.
     *
     * @return resource files (never {@code null}, possibly empty)
     */
    public List<ResourceFile> resourceFiles() {
        return resourceFiles;
    }

    /**
     * Returns all documentation files in this plan.
     *
     * @return documentation files (never {@code null}, possibly empty)
     */
    public List<DocFile> docFiles() {
        return docFiles;
    }

    /**
     * Returns the total number of artifacts in this plan.
     *
     * @return total artifact count
     */
    public int totalArtifacts() {
        return sourceFiles.size() + resourceFiles.size() + docFiles.size();
    }

    /**
     * Returns whether this plan is empty (no artifacts).
     *
     * @return {@code true} if no artifacts are present
     */
    public boolean isEmpty() {
        return totalArtifacts() == 0;
    }

    /**
     * Returns whether any conflicts were detected.
     *
     * <p>
     * A conflict occurs when multiple artifacts target the same output location.
     * </p>
     *
     * @return {@code true} if conflicts exist
     */
    public boolean hasConflicts() {
        for (List<SourceFile> group : sourcesByQualifiedName.values()) {
            if (group.size() > 1) return true;
        }
        for (List<ResourceFile> group : resourcesByPath.values()) {
            if (group.size() > 1) return true;
        }
        for (List<DocFile> group : docsByPath.values()) {
            if (group.size() > 1) return true;
        }
        return false;
    }

    /**
     * Returns detailed conflict information for diagnostic reporting.
     *
     * <p>
     * Each string describes a specific conflict (e.g., "Duplicate source file 'com.example.Foo'
     * declared by 2 plugins").
     * </p>
     *
     * @return conflict descriptions (never {@code null}, empty if no conflicts)
     */
    public List<String> getConflicts() {
        List<String> conflicts = new ArrayList<>();

        for (Map.Entry<String, List<SourceFile>> entry : sourcesByQualifiedName.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add("Duplicate source file '" + entry.getKey() + "' ("
                        + entry.getValue().size() + " occurrences)");
            }
        }

        for (Map.Entry<String, List<ResourceFile>> entry : resourcesByPath.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add("Duplicate resource file '" + entry.getKey() + "' ("
                        + entry.getValue().size() + " occurrences)");
            }
        }

        for (Map.Entry<String, List<DocFile>> entry : docsByPath.entrySet()) {
            if (entry.getValue().size() > 1) {
                conflicts.add("Duplicate documentation file '" + entry.getKey() + "' ("
                        + entry.getValue().size() + " occurrences)");
            }
        }

        return conflicts;
    }

    /**
     * Returns source files grouped by qualified type name.
     *
     * <p>
     * This is useful for analyzing duplicates or conflicts. Each entry maps a qualified name
     * to all source files targeting that name.
     * </p>
     *
     * @return source files by qualified name (never {@code null})
     */
    public Map<String, List<SourceFile>> sourcesByQualifiedName() {
        return sourcesByQualifiedName;
    }

    /**
     * Returns resource files grouped by path.
     *
     * @return resource files by path (never {@code null})
     */
    public Map<String, List<ResourceFile>> resourcesByPath() {
        return resourcesByPath;
    }

    /**
     * Returns documentation files grouped by path.
     *
     * @return documentation files by path (never {@code null})
     */
    public Map<String, List<DocFile>> docsByPath() {
        return docsByPath;
    }

    /**
     * Creates a new builder.
     *
     * @return builder (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builder for {@link ArtifactPlan}.
     */
    public static final class Builder {

        private final List<SourceFile> sourceFiles = new ArrayList<>();
        private final List<ResourceFile> resourceFiles = new ArrayList<>();
        private final List<DocFile> docFiles = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a source file to the plan.
         *
         * @param file source file (not {@code null})
         * @return this builder
         */
        public Builder addSource(SourceFile file) {
            Objects.requireNonNull(file, "file");
            sourceFiles.add(file);
            return this;
        }

        /**
         * Adds a resource file to the plan.
         *
         * @param file resource file (not {@code null})
         * @return this builder
         */
        public Builder addResource(ResourceFile file) {
            Objects.requireNonNull(file, "file");
            resourceFiles.add(file);
            return this;
        }

        /**
         * Adds a documentation file to the plan.
         *
         * @param file documentation file (not {@code null})
         * @return this builder
         */
        public Builder addDoc(DocFile file) {
            Objects.requireNonNull(file, "file");
            docFiles.add(file);
            return this;
        }

        /**
         * Adds multiple source files to the plan.
         *
         * @param files source files (not {@code null})
         * @return this builder
         */
        public Builder addAllSources(Iterable<SourceFile> files) {
            Objects.requireNonNull(files, "files");
            for (SourceFile file : files) {
                addSource(file);
            }
            return this;
        }

        /**
         * Adds multiple resource files to the plan.
         *
         * @param files resource files (not {@code null})
         * @return this builder
         */
        public Builder addAllResources(Iterable<ResourceFile> files) {
            Objects.requireNonNull(files, "files");
            for (ResourceFile file : files) {
                addResource(file);
            }
            return this;
        }

        /**
         * Adds multiple documentation files to the plan.
         *
         * @param files documentation files (not {@code null})
         * @return this builder
         */
        public Builder addAllDocs(Iterable<DocFile> files) {
            Objects.requireNonNull(files, "files");
            for (DocFile file : files) {
                addDoc(file);
            }
            return this;
        }

        /**
         * Builds the artifact plan.
         *
         * @return artifact plan (never {@code null})
         */
        public ArtifactPlan build() {
            return new ArtifactPlan(this);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal indexing
    // ─────────────────────────────────────────────────────────────────────────

    private static Map<String, List<SourceFile>> buildSourceIndex(List<SourceFile> files) {
        Map<String, List<SourceFile>> index = new LinkedHashMap<>();
        for (SourceFile file : files) {
            index.computeIfAbsent(file.qualifiedTypeName(), k -> new ArrayList<>())
                    .add(file);
        }
        // Make each list unmodifiable
        Map<String, List<SourceFile>> result = new HashMap<>();
        for (Map.Entry<String, List<SourceFile>> entry : index.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<ResourceFile>> buildResourceIndex(List<ResourceFile> files) {
        Map<String, List<ResourceFile>> index = new LinkedHashMap<>();
        for (ResourceFile file : files) {
            index.computeIfAbsent(file.path(), k -> new ArrayList<>()).add(file);
        }
        Map<String, List<ResourceFile>> result = new HashMap<>();
        for (Map.Entry<String, List<ResourceFile>> entry : index.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, List<DocFile>> buildDocIndex(List<DocFile> files) {
        Map<String, List<DocFile>> index = new LinkedHashMap<>();
        for (DocFile file : files) {
            index.computeIfAbsent(file.path(), k -> new ArrayList<>()).add(file);
        }
        Map<String, List<DocFile>> result = new HashMap<>();
        for (Map.Entry<String, List<DocFile>> entry : index.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }
}
