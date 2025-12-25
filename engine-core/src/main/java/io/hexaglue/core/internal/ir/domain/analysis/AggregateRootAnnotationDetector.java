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
 * Detects aggregate root markers via annotations.
 *
 * <p>
 * This detector analyzes JSR-269 type elements to identify annotations that mark
 * a type as an Aggregate Root in the DDD sense.
 * </p>
 *
 * <h2>Supported Annotations</h2>
 * <p>
 * The following annotations are recognized as aggregate root markers:
 * </p>
 * <ul>
 *   <li><strong>jMolecules:</strong> {@code @AggregateRoot}
 *       (org.jmolecules.ddd.annotation.AggregateRoot)</li>
 *   <li><strong>Spring Data MongoDB:</strong> {@code @Document}
 *       (org.springframework.data.mongodb.core.mapping.Document)</li>
 *   <li><strong>Jakarta Persistence (JPA):</strong> {@code @Entity}
 *       (jakarta.persistence.Entity) - <em>hint only, requires repository port to confirm</em></li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <p>
 * This detector works at the annotation processing level (JSR-269) and only identifies
 * <strong>explicit</strong> aggregate root markers. Other heuristics (package conventions,
 * port analysis, etc.) are handled by {@link AggregateRootDetector}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal aggregate root annotation detection; not exposed to plugins")
public final class AggregateRootAnnotationDetector {

    /**
     * Spring Data MongoDB @Document - typically used for aggregate roots.
     */
    private static final String SPRING_DOCUMENT = "org.springframework.data.mongodb.core.mapping.Document";

    /**
     * Jakarta Persistence @Entity - hint only (may be internal entity).
     */
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";

    /**
     * Older JPA @Entity - hint only (may be internal entity).
     */
    private static final String JAVAX_ENTITY = "javax.persistence.Entity";

    /**
     * Creates an aggregate root annotation detector.
     */
    public AggregateRootAnnotationDetector() {
        // Default constructor
    }

    /**
     * Checks if a type element has an aggregate root annotation.
     *
     * <p>
     * This method inspects the type's annotations to determine if it's explicitly
     * marked as an aggregate root. It returns {@code true} for:
     * </p>
     * <ul>
     *   <li>jMolecules {@code @AggregateRoot}</li>
     *   <li>Spring Data MongoDB {@code @Document}</li>
     * </ul>
     *
     * <p>
     * <strong>Note:</strong> This does NOT check for jMolecules {@code @Entity}, which is
     * handled separately by {@link EntityAnnotationDetector}. JPA {@code @Entity} is also
     * NOT considered a strong signal by itself - use {@link #hasJpaEntityAnnotation(TypeElement)}
     * separately if you need this information.
     * </p>
     *
     * @param typeElement type element to analyze (not {@code null})
     * @return {@code true} if has aggregate root annotation
     * @throws NullPointerException if typeElement is null
     * @see EntityAnnotationDetector
     */
    public boolean hasAggregateRootAnnotation(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .anyMatch(this::isAggregateRootAnnotation);
    }

    /**
     * Checks if a type element has a JPA @Entity annotation.
     *
     * <p>
     * This is a weaker signal than {@link #hasAggregateRootAnnotation(TypeElement)}
     * because JPA entities can be either aggregate roots or internal entities.
     * Typically, this should be combined with port analysis to make the determination.
     * </p>
     *
     * @param typeElement type element to analyze (not {@code null})
     * @return {@code true} if has JPA @Entity annotation
     * @throws NullPointerException if typeElement is null
     */
    public boolean hasJpaEntityAnnotation(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .anyMatch(this::isJpaEntityAnnotation);
    }

    /**
     * Determines if an annotation qualified name indicates an aggregate root.
     *
     * <p>
     * <strong>Note:</strong> This does NOT check for jMolecules {@code @Entity}.
     * Use {@link EntityAnnotationDetector} for that.
     * </p>
     *
     * @param annotationQualifiedName annotation qualified name (not {@code null})
     * @return {@code true} if aggregate root annotation
     */
    private boolean isAggregateRootAnnotation(String annotationQualifiedName) {
        return JMoleculesAnnotations.AGGREGATE_ROOT.equals(annotationQualifiedName)
                || SPRING_DOCUMENT.equals(annotationQualifiedName);
    }

    /**
     * Determines if an annotation qualified name indicates a JPA entity.
     *
     * @param annotationQualifiedName annotation qualified name (not {@code null})
     * @return {@code true} if JPA entity annotation
     */
    private boolean isJpaEntityAnnotation(String annotationQualifiedName) {
        return JPA_ENTITY.equals(annotationQualifiedName) || JAVAX_ENTITY.equals(annotationQualifiedName);
    }
}
