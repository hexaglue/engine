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
package io.hexaglue.spi.context;

/**
 * Indicates how HexaGlue is being executed.
 *
 * <p>This can be used by plugins to tune behavior (e.g., stricter validation in CI,
 * or extra logs during local development).</p>
 */
public enum ExecutionMode {

    /**
     * Local developer execution (IDE, incremental compilation, experimentation).
     */
    DEVELOPMENT,

    /**
     * Continuous integration / build server execution.
     */
    CI,

    /**
     * A release build (e.g., publishing artifacts). Typically the strictest mode.
     */
    RELEASE
}
