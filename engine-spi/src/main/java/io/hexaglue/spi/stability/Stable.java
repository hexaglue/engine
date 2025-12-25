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
package io.hexaglue.spi.stability;

import io.hexaglue.spi.HexaGluePlugin;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks SPI types and methods that are <strong>stable</strong> and guaranteed not to change
 * across minor and patch versions within the same major version.
 *
 * <p><strong>Stability Guarantees (v1.0+):</strong>
 * <ul>
 *   <li>✅ Method signatures never change</li>
 *   <li>✅ Types, methods, and fields never removed</li>
 *   <li>✅ Semantic behavior never changes</li>
 *   <li>✅ Source and binary compatibility maintained across all 1.x releases</li>
 * </ul>
 *
 * <p><strong>Evolution Strategy:</strong>
 * <ul>
 *   <li>New methods added with default implementations (interfaces)</li>
 *   <li>New optional fields added via builders (classes)</li>
 *   <li>Deprecation warnings at least one MINOR version before removal in MAJOR version</li>
 * </ul>
 *
 * <p><strong>Plugin Developer Impact:</strong><br>
 * Code written against {@code @Stable} APIs in version 1.0 will compile and run
 * without modification on all future 1.x versions.
 *
 * <p><strong>Examples:</strong>
 * <ul>
 *   <li>{@link HexaGluePlugin} - Core plugin contract</li>
 *   <li>{@link io.hexaglue.spi.ir.IrView} - Root IR view</li>
 *   <li>{@link io.hexaglue.spi.types.TypeRef} - Type system abstraction</li>
 *   <li>{@link io.hexaglue.spi.diagnostics.Diagnostic} - Diagnostic model</li>
 * </ul>
 *
 * @since 1.0.0
 * @see Evolvable
 * @see Experimental
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Stable {

    /**
     * Optional description of stability guarantees or evolution strategy.
     *
     * @return stability notes (empty by default)
     */
    String value() default "";

    /**
     * Version since this API became stable.
     *
     * @return version string (e.g., "1.0.0")
     */
    String since() default "1.0.0";
}
