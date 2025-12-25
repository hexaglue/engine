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
package io.hexaglue.core.internal.ir.ports.analysis;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.support.JMoleculesAnnotations;
import java.util.Objects;
import javax.lang.model.element.TypeElement;

/**
 * Detects repository port markers via annotations.
 *
 * <p>
 * This detector analyzes JSR-269 type elements to identify annotations that mark
 * an interface as a Repository port in the DDD sense.
 * </p>
 *
 * <h2>Supported Annotations</h2>
 * <p>
 * The following annotations are recognized as repository markers:
 * </p>
 * <ul>
 *   <li><strong>jMolecules:</strong> {@code @Repository}
 *       (org.jmolecules.ddd.annotation.Repository)</li>
 *   <li><strong>Spring Framework:</strong> {@code @Repository}
 *       (org.springframework.stereotype.Repository)</li>
 * </ul>
 *
 * <h2>Design Notes</h2>
 * <p>
 * This detector works at the annotation processing level (JSR-269) and only identifies
 * <strong>explicit</strong> repository markers. Other heuristics (package conventions,
 * naming patterns, etc.) are handled by {@link PortRules}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * @since 1.0.0
 */
@InternalMarker(reason = "Internal port annotation detection; not exposed to plugins")
public final class RepositoryAnnotationDetector {

    /**
     * Alternative repository annotation: Spring @Repository.
     */
    private static final String SPRING_REPOSITORY = "org.springframework.stereotype.Repository";

    /**
     * Creates a repository annotation detector.
     */
    public RepositoryAnnotationDetector() {
        // Default constructor
    }

    /**
     * Checks if a type element has a repository annotation.
     *
     * <p>
     * This method inspects the type's annotations to determine if it's explicitly
     * marked as a repository. It returns {@code true} for:
     * </p>
     * <ul>
     *   <li>jMolecules {@code @Repository}</li>
     *   <li>Spring {@code @Repository}</li>
     * </ul>
     *
     * @param typeElement type element to analyze (not {@code null})
     * @return {@code true} if has repository annotation
     * @throws NullPointerException if typeElement is null
     */
    public boolean hasRepositoryAnnotation(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .anyMatch(this::isRepositoryAnnotation);
    }

    /**
     * Determines if an annotation qualified name indicates a repository.
     *
     * @param annotationQualifiedName annotation qualified name (not {@code null})
     * @return {@code true} if repository annotation
     */
    private boolean isRepositoryAnnotation(String annotationQualifiedName) {
        return JMoleculesAnnotations.REPOSITORY.equals(annotationQualifiedName)
                || SPRING_REPOSITORY.equals(annotationQualifiedName);
    }
}
