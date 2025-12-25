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

import java.util.Objects;

/**
 * Declares a user-maintained custom block region inside a generated text file.
 *
 * <p>Custom blocks enable safe regeneration without losing user edits. A typical pattern:
 *
 * <pre>
 * // @hexaglue-custom-start: imports
 * // user imports
 * // @hexaglue-custom-end: imports
 * </pre>
 *
 * <p>The exact marker syntax and merge semantics are implementation-defined by the compiler,
 * but the block identity ({@link #id()}) must be stable.</p>
 *
 * @param id stable block identifier (non-blank)
 * @param description optional user-facing description (nullable)
 */
public record CustomBlock(String id, String description) {

    public CustomBlock {
        Objects.requireNonNull(id, "id");
        String t = id.trim();
        if (t.isEmpty()) throw new IllegalArgumentException("id must not be blank");
        id = t;

        if (description != null) {
            String d = description.trim();
            description = d.isEmpty() ? null : d;
        }
    }

    /**
     * Creates a custom block with no description.
     *
     * @param id stable block identifier
     * @return custom block
     */
    public static CustomBlock of(String id) {
        return new CustomBlock(id, null);
    }
}
