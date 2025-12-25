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
package io.hexaglue.spi.naming;

/**
 * Logical role of a generated name.
 *
 * <p>Roles allow the naming strategy to provide consistent naming across plugins without
 * plugins having to embed naming conventions in many places.</p>
 *
 * <p>This enum is intentionally small and may grow over time. Implementations should
 * provide sensible defaults for unknown roles (e.g., fall back to a generic suffix/prefix).</p>
 */
public enum NameRole {

    /** A generated Java package name. */
    PACKAGE,

    /** A generated type name (class/interface/record). */
    TYPE,

    /** A generated field name. */
    FIELD,

    /** A generated method name. */
    METHOD,

    /** A generated parameter name. */
    PARAMETER,

    /** A generated constant name. */
    CONSTANT,

    /** A generated resource path (e.g., "META-INF/..."). */
    RESOURCE_PATH,

    /** A generated documentation path or file name. */
    DOC_PATH
}
