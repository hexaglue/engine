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
package io.hexaglue.spi.codegen;

import io.hexaglue.spi.stability.Stable;

/**
 * Output sink for generated artifacts.
 *
 * <p>This SPI abstracts file creation/writing. Plugins request artifact emission; the compiler
 * decides output locations, performs merge strategies, tracks generated files, and reports
 * diagnostics on conflicts.</p>
 *
 * <p>This interface is intentionally minimal and stable.</p>
 */
@Stable(since = "1.0.0")
public interface ArtifactSink {

    /**
     * Writes a generated Java source file.
     *
     * @param file source file (never {@code null})
     */
    void write(SourceFile file);

    /**
     * Writes a generated resource file.
     *
     * @param file resource file (never {@code null})
     */
    void write(ResourceFile file);

    /**
     * Writes a generated documentation file.
     *
     * @param file documentation file (never {@code null})
     */
    void write(DocFile file);

    /**
     * Convenience method to write plain-text resources.
     *
     * @param path resource path
     * @param content text content
     * @param mergeMode merge mode
     */
    default void writeTextResource(String path, String content, MergeMode mergeMode) {
        write(ResourceFile.builder()
                .path(path)
                .text(content)
                .mergeMode(mergeMode)
                .build());
    }

    /**
     * Convenience method to write plain-text documentation.
     *
     * @param path documentation path
     * @param content text content
     * @param mergeMode merge mode
     */
    default void writeDoc(String path, String content, MergeMode mergeMode) {
        write(DocFile.builder().path(path).content(content).mergeMode(mergeMode).build());
    }
}
