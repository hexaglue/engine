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
package io.hexaglue.spi.options;

/**
 * Scope of an option.
 *
 * <p>Scopes are used to describe how an option is interpreted and where it can be defined.
 * The compiler may choose to support only a subset of scopes depending on integration.</p>
 */
public enum OptionScope {

    /**
     * A global option applies to the whole compilation and all plugins.
     */
    GLOBAL,

    /**
     * A plugin option applies to a specific plugin (identified by its plugin id).
     */
    PLUGIN
}
