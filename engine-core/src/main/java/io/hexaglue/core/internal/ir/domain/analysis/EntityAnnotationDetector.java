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
 * Detects domain entity markers via annotations.
 *
 * <p>
 * This detector analyzes JSR-269 type elements to identify annotations that mark
 * a type as a Domain Entity in the DDD sense.
 * </p>
 *
 * <h2>Semantic Distinction</h2>
 * <p>
 * In DDD, an {@code @Entity} annotation indicates an object with identity, but it's
 * <strong>ambiguous</strong> regarding aggregate boundaries:
 * </p>
 * <ul>
 *   <li><strong>Aggregate Root Entity:</strong> The entry point to an aggregate (consistency boundary)</li>
 *   <li><strong>Internal Entity:</strong> An entity contained within an aggregate</li>
 * </ul>
 *
 * <p>
 * This detector <strong>only identifies the presence of {@code @Entity}</strong>.
 * The classification as aggregate root vs. internal entity requires additional
 * context analysis (port detection, relationships, explicit {@code @AggregateRoot}).
 * </p>
 *
 * <h2>Supported Annotations</h2>
 * <p>
 * The following annotations are recognized as entity markers:
 * </p>
 * <ul>
 *   <li><strong>jMolecules:</strong> {@code @Entity}
 *       (org.jmolecules.ddd.annotation.Entity)</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <p>
 * This detector is intentionally <strong>separate</strong> from {@link AggregateRootAnnotationDetector}
 * because:
 * </p>
 * <ul>
 *   <li>{@code @AggregateRoot} is <strong>explicit and unambiguous</strong></li>
 *   <li>{@code @Entity} is <strong>implicit and requires context</strong></li>
 *   <li>Separating them allows proper semantic handling in {@link DomainTypeKindResolver}</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * EntityAnnotationDetector entityDetector = new EntityAnnotationDetector();
 * AggregateRootAnnotationDetector aggregateDetector = new AggregateRootAnnotationDetector();
 *
 * if (aggregateDetector.hasAggregateRootAnnotation(typeElement)) {
 *     // Explicit aggregate root - high confidence
 *     kind = DomainTypeKind.AGGREGATE_ROOT;
 * } else if (entityDetector.hasEntityAnnotation(typeElement)) {
 *     // Entity detected - requires context analysis
 *     kind = analyzeEntityKind(typeElement); // Port analysis, relationships, etc.
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * @since 1.0.0
 * @see AggregateRootAnnotationDetector
 * @see DomainTypeKindResolver
 */
@InternalMarker(reason = "Internal entity annotation detection; not exposed to plugins")
public final class EntityAnnotationDetector {

    /**
     * Creates an entity annotation detector.
     */
    public EntityAnnotationDetector() {
        // Default constructor
    }

    /**
     * Checks if a type element has an entity annotation.
     *
     * <p>
     * This method inspects the type's annotations to determine if it's explicitly
     * marked as a domain entity. It returns {@code true} for:
     * </p>
     * <ul>
     *   <li>jMolecules {@code @Entity}</li>
     * </ul>
     *
     * <p>
     * <strong>Important:</strong> This does NOT check for {@code @AggregateRoot}.
     * Use {@link AggregateRootAnnotationDetector} for that. The presence of
     * {@code @Entity} alone does NOT determine if the type is an aggregate root.
     * </p>
     *
     * @param typeElement type element to analyze (not {@code null})
     * @return {@code true} if has entity annotation
     * @throws NullPointerException if typeElement is null
     */
    public boolean hasEntityAnnotation(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .anyMatch(JMoleculesAnnotations.ENTITY::equals);
    }
}
