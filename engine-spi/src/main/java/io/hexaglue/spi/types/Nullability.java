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
package io.hexaglue.spi.types;

/**
 * Nullability marker for a type reference.
 *
 * <p>This is a semantic hint used by HexaGlue and plugins when generating code
 * (e.g., annotations, validation, optional handling). It is intentionally not tied
 * to any specific annotation framework.</p>
 */
public enum Nullability {

    /**
     * Nullability is not specified or unknown.
     */
    UNSPECIFIED,

    /**
     * Value is intended to be non-null.
     */
    NONNULL,

    /**
     * Value may be null.
     */
    NULLABLE
}
