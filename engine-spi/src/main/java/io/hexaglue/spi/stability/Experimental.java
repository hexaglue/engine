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
 * Marks SPI types and methods that are <strong>experimental</strong> and may change or be removed
 * in future minor or patch versions without prior deprecation.
 *
 * <p><strong>Experimental Status:</strong>
 * <ul>
 *   <li>⚠️ Method signatures may change in MINOR versions</li>
 *   <li>⚠️ Semantic behavior may change in MINOR versions</li>
 *   <li>⚠️ May be removed entirely in MINOR versions</li>
 *   <li>⚠️ No backward-compatibility guarantees</li>
 * </ul>
 *
 * <p><strong>When to Use Experimental APIs:</strong>
 * <ul>
 *   <li>Early access to new features for feedback and testing</li>
 *   <li>Proof-of-concept integrations</li>
 *   <li>Internal tools and prototypes</li>
 * </ul>
 *
 * <p><strong>Plugin Developer Guidelines:</strong>
 * <ul>
 *   <li><strong>DO:</strong> Use for experimentation and provide feedback to HexaGlue team</li>
 *   <li><strong>DO:</strong> Expect migration work in future versions</li>
 *   <li><strong>DO:</strong> Pin to specific HexaGlue versions when using experimental APIs</li>
 *   <li><strong>DO NOT:</strong> Use in production plugins without accepting breaking change risk</li>
 *   <li><strong>DO NOT:</strong> Expect stability or deprecation warnings</li>
 * </ul>
 *
 * <p><strong>Graduation Path:</strong><br>
 * Experimental APIs may graduate to {@link Stable} or {@link Evolvable} status in future
 * major or minor versions based on feedback and maturity. When graduated, the
 * {@code @Experimental} annotation will be removed and replaced with the appropriate
 * stability marker.
 *
 * <p><strong>Examples:</strong><br>
 * Currently, HexaGlue SPI 1.0 has no experimental APIs. All APIs are either
 * {@link Stable} or {@link Evolvable}. Future experimental features will be clearly
 * marked with this annotation.
 *
 * @since 1.0.0
 * @see Stable
 * @see Evolvable
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Experimental {

    /**
     * Optional description of experimental status or planned evolution.
     *
     * @return experimental notes (empty by default)
     */
    String value() default "";

    /**
     * Version since this API became experimental.
     *
     * @return version string (e.g., "1.1.0")
     */
    String since() default "";
}
