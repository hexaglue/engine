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
package io.hexaglue.core.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks types, methods, and packages as internal to HexaGlue core.
 *
 * <p>
 * Elements annotated with {@code @InternalMarker} are:
 * <ul>
 *   <li><strong>Not part of the public API:</strong> No stability guarantees are provided.</li>
 *   <li><strong>Subject to change:</strong> May be modified or removed in any release without notice.</li>
 *   <li><strong>Off-limits to plugins:</strong> Plugins must use {@code io.hexaglue.spi} exclusively.</li>
 * </ul>
 *
 * <h2>Purpose</h2>
 * <p>
 * This annotation serves as a clear boundary marker between:
 * </p>
 * <ul>
 *   <li><strong>Public SPI</strong> ({@code io.hexaglue.spi.*}): Stable, versioned, plugin-facing contracts.</li>
 *   <li><strong>Internal implementation</strong> ({@code io.hexaglue.core.internal.*}): Compiler internals.</li>
 * </ul>
 *
 * <h2>Enforcement</h2>
 * <p>
 * While this annotation is primarily documentary, HexaGlue enforces the boundary using:
 * </p>
 * <ul>
 *   <li><strong>JPMS modules:</strong> The {@code module-info.java} in {@code hexaglue-core} does not export internal packages.</li>
 *   <li><strong>Convention:</strong> Internal packages follow the {@code *.internal.*} naming pattern.</li>
 *   <li><strong>Tooling:</strong> Build-time checks (e.g., ArchUnit, Maven Enforcer) can detect violations.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @InternalMarker
 * public final class IrSnapshot {
 *     // Internal IR implementation
 * }
 * }</pre>
 *
 * <h2>Design Note</h2>
 * <p>
 * This marker is intentionally lightweight. It does not use JSR-305 or other external annotation
 * libraries to keep HexaGlue strictly JDK-only.
 * </p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.PACKAGE})
public @interface InternalMarker {

    /**
     * Optional explanation of why this element is internal.
     *
     * <p>
     * This field is purely documentary and intended for maintainers. It can clarify:
     * <ul>
     *   <li>Why the element must remain internal</li>
     *   <li>What SPI alternative exists (if any)</li>
     *   <li>Future plans for stabilization (if applicable)</li>
     * </ul>
     *
     * @return reason string (defaults to empty)
     */
    String reason() default "";
}
