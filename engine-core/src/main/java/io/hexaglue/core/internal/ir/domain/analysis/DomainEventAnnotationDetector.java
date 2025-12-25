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
import javax.lang.model.element.TypeElement;

/**
 * Detects domain event markers via annotations.
 *
 * <p>
 * This detector analyzes JSR-269 type elements to identify annotations that mark
 * a type as a Domain Event in the DDD sense.
 * </p>
 *
 * <h2>Supported Annotations</h2>
 * <p>
 * The following annotations are recognized as domain event markers:
 * </p>
 * <ul>
 *   <li><strong>jMolecules:</strong> {@code @DomainEvent}
 *       (org.jmolecules.event.annotation.DomainEvent)</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <p>
 * This detector works at the annotation processing level (JSR-269) and only identifies
 * <strong>explicit</strong> domain event markers. Other heuristics (naming patterns,
 * structural analysis, etc.) are handled by {@link DomainTypeKindResolver}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * @since 1.0.0
 */
@InternalMarker(reason = "Internal domain event annotation detection; not exposed to plugins")
public final class DomainEventAnnotationDetector {

    /**
     * Creates a domain event annotation detector.
     */
    public DomainEventAnnotationDetector() {
        // Default constructor
    }

    /**
     * Checks if a type element has a domain event annotation.
     *
     * <p>
     * This method inspects the type's annotations to determine if it's explicitly
     * marked as a domain event. It returns {@code true} for:
     * </p>
     * <ul>
     *   <li>jMolecules {@code @DomainEvent}</li>
     * </ul>
     *
     * @param typeElement type element to analyze (not {@code null})
     * @return {@code true} if has domain event annotation
     * @throws NullPointerException if typeElement is null
     */
    public boolean hasDomainEventAnnotation(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .anyMatch(JMoleculesAnnotations.DOMAIN_EVENT::equals);
    }
}
