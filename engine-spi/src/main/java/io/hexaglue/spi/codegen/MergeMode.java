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

/**
 * Merge policy for writing generated artifacts.
 *
 * <p>HexaGlue supports user-maintained custom sections in generated files.
 * Merge policies define how the compiler should behave when a file already exists.</p>
 *
 * <p>This SPI is intentionally small. The core implementation defines the exact behavior
 * of each mode.</p>
 */
public enum MergeMode {

    /**
     * Always overwrite the target file.
     *
     * <p>This mode provides the strongest determinism but may erase user edits if any.</p>
     */
    OVERWRITE,

    /**
     * Merge generated content with existing content while preserving declared custom blocks.
     *
     * <p>This is the recommended default for source files that support user edits.</p>
     */
    MERGE_CUSTOM_BLOCKS,

    /**
     * Write the file only if it does not already exist.
     *
     * <p>Useful for templates, README stubs, or user-owned files.</p>
     */
    WRITE_ONCE,

    /**
     * Fail if the target file already exists.
     *
     * <p>Useful when conflicts must be explicit and surfaced as diagnostics.</p>
     */
    FAIL_IF_EXISTS
}
