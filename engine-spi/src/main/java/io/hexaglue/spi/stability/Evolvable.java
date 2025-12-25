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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks SPI types and methods that are <strong>stable but may evolve</strong> in backward-compatible ways
 * across minor versions within the same major version.
 *
 * <p><strong>Evolution Guarantees (v1.0+):</strong>
 * <ul>
 *   <li>✅ New methods may be added (with default implementations or new overloads)</li>
 *   <li>✅ New fields may be added (via builders)</li>
 *   <li>✅ New subtypes or enum constants may be introduced</li>
 *   <li>❌ Existing methods, fields, and semantics never removed or changed</li>
 * </ul>
 *
 * <p><strong>Plugin Developer Guidelines:</strong>
 * <ul>
 *   <li><strong>DO:</strong> Read and use values returned by {@code @Evolvable} APIs</li>
 *   <li><strong>DO:</strong> Handle enum values with a default case to support new constants</li>
 *   <li><strong>DO NOT:</strong> Implement {@code @Evolvable} interfaces yourself (core provides implementations)</li>
 *   <li><strong>DO NOT:</strong> Extend or subclass {@code @Evolvable} types</li>
 * </ul>
 *
 * <p><strong>Impact on Plugins:</strong><br>
 * Plugins can safely depend on {@code @Evolvable} APIs. New features may be added
 * in minor versions, but existing functionality remains stable and backward-compatible.
 *
 * <p><strong>Examples:</strong>
 * <ul>
 *   <li>{@link io.hexaglue.spi.ir.domain.DomainTypeView} - May add new metadata accessors</li>
 *   <li>{@link io.hexaglue.spi.context.BuildEnvironment} - May add new environment properties</li>
 *   <li>{@link io.hexaglue.spi.ir.domain.DomainTypeKind} - May add new domain type classifications</li>
 *   <li>{@link io.hexaglue.spi.codegen.MergeMode} - May add new merge strategies</li>
 * </ul>
 *
 * @since 1.0.0
 * @see Stable
 * @see Experimental
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Evolvable {

    /**
     * Optional description of how this API may evolve.
     *
     * @return evolution notes (empty by default)
     */
    String value() default "";

    /**
     * Version since this API became evolvable.
     *
     * @return version string (e.g., "1.0.0")
     */
    String since() default "1.0.0";
}
