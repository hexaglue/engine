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
package io.hexaglue.core.internal.ir.domain.analysis;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.support.JMoleculesAnnotations;
import java.util.Objects;
import javax.lang.model.element.VariableElement;

/**
 * Detects identity property markers via annotations.
 *
 * <p>
 * This detector analyzes JSR-269 field elements to identify annotations that mark
 * a property as an identity in the DDD sense.
 * </p>
 *
 * <h2>Supported Annotations</h2>
 * <p>
 * The following annotations are recognized as identity markers:
 * </p>
 * <ul>
 *   <li><strong>jMolecules:</strong> {@code @Identity}
 *       (org.jmolecules.ddd.annotation.Identity)</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <p>
 * This detector works at the annotation processing level (JSR-269) at the field level
 * and only identifies <strong>explicit</strong> identity markers. Other heuristics
 * (naming conventions like "id", "identifier", etc.) are handled by property extraction logic.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * @since 1.0.0
 */
@InternalMarker(reason = "Internal identity annotation detection; not exposed to plugins")
public final class IdentityAnnotationDetector {

    /**
     * Creates an identity annotation detector.
     */
    public IdentityAnnotationDetector() {
        // Default constructor
    }

    /**
     * Checks if a field element has an identity annotation.
     *
     * <p>
     * This method inspects the field's annotations to determine if it's explicitly
     * marked as an identity property. It returns {@code true} for:
     * </p>
     * <ul>
     *   <li>jMolecules {@code @Identity}</li>
     * </ul>
     *
     * @param fieldElement field element to analyze (not {@code null})
     * @return {@code true} if has identity annotation
     * @throws NullPointerException if fieldElement is null
     */
    public boolean hasIdentityAnnotation(VariableElement fieldElement) {
        Objects.requireNonNull(fieldElement, "fieldElement");

        return fieldElement.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .anyMatch(JMoleculesAnnotations.IDENTITY::equals);
    }
}
