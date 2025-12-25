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
package io.hexaglue.core.internal.ir.domain.semantics;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.domain.normalize.AnnotationIndex;
import io.hexaglue.core.internal.ir.support.JMoleculesAnnotations;
import java.util.Objects;

/**
 * Encapsulates well-known annotation names and naming/package conventions for aggregate roots.
 *
 * <p>This class centralizes all the "signals" that might indicate a type is an aggregate root.
 * Keeping these in one place makes it easy to add support for jMolecules and other libraries.</p>
 *
 * <h2>Supported Annotations</h2>
 * <ul>
 *   <li><strong>jMolecules:</strong> {@code @AggregateRoot} (strong signal)</li>
 *   <li><strong>jMolecules:</strong> {@code @Entity} (ambiguous - see {@link #hasEntityMarker(AnnotationIndex)})</li>
 *   <li><strong>Spring Data MongoDB:</strong> {@code @Document} (strong signal)</li>
 *   <li><strong>JPA:</strong> {@code @Entity} (weak signal, requires repository port confirmation)</li>
 * </ul>
 *
 * <h2>Conventions</h2>
 * <ul>
 *   <li><strong>Package:</strong> Types in packages containing "aggregate" or "aggregates"</li>
 *   <li><strong>Naming:</strong> Types ending with "Aggregate" or "AggregateRoot"</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is stateless and thread-safe.</p>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal semantics signals; not exposed to plugins")
public final class AggregateRootSignals {

    // Spring Data annotations
    private static final String SPRING_DOCUMENT = "org.springframework.data.mongodb.core.mapping.Document";

    // JPA annotations (weak signals)
    private static final String JPA_ENTITY = "jakarta.persistence.Entity";
    private static final String JAVAX_ENTITY = "javax.persistence.Entity";

    /**
     * Creates an aggregate root signals detector.
     */
    public AggregateRootSignals() {
        // Default constructor
    }

    /**
     * Checks if the annotations contain a strong aggregate root marker.
     *
     * <p>Strong markers are explicit DDD annotations that unambiguously indicate
     * an aggregate root, such as {@code @AggregateRoot} (jMolecules) or
     * {@code @Document} (Spring Data MongoDB).</p>
     *
     * <p><strong>Note:</strong> jMolecules {@code @Entity} is NOT considered a strong
     * aggregate root marker because it can indicate either an aggregate root or an internal
     * entity. Use {@link #hasEntityMarker(AnnotationIndex)} to check for {@code @Entity}.</p>
     *
     * @param annotations annotation index (not {@code null})
     * @return {@code true} if has strong marker
     * @throws NullPointerException if annotations is null
     */
    public boolean hasStrongAggregateMarker(AnnotationIndex annotations) {
        Objects.requireNonNull(annotations, "annotations");
        return annotations.hasAny(JMoleculesAnnotations.AGGREGATE_ROOT, SPRING_DOCUMENT);
    }

    /**
     * Checks if the annotations contain a jMolecules {@code @Entity} marker.
     *
     * <p>jMolecules {@code @Entity} is <strong>ambiguous</strong> - it marks a domain
     * entity with identity, but doesn't specify whether it's an aggregate root or an
     * internal entity within an aggregate. This should be combined with port analysis
     * and relationship detection for accurate classification.</p>
     *
     * @param annotations annotation index (not {@code null})
     * @return {@code true} if has jMolecules @Entity marker
     * @throws NullPointerException if annotations is null
     */
    public boolean hasEntityMarker(AnnotationIndex annotations) {
        Objects.requireNonNull(annotations, "annotations");
        return annotations.has(JMoleculesAnnotations.ENTITY);
    }

    /**
     * Checks if the annotations contain a JPA entity marker.
     *
     * <p>JPA {@code @Entity} is considered a <strong>weak signal</strong> because
     * JPA entities can be either aggregate roots or internal entities. This should
     * be combined with repository port analysis for accurate classification.</p>
     *
     * @param annotations annotation index (not {@code null})
     * @return {@code true} if has JPA entity marker
     * @throws NullPointerException if annotations is null
     */
    public boolean hasJpaEntityMarker(AnnotationIndex annotations) {
        Objects.requireNonNull(annotations, "annotations");
        return annotations.hasAny(JPA_ENTITY, JAVAX_ENTITY);
    }

    /**
     * Checks if a type is in an "aggregate" or "aggregates" package.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code com.example.order.aggregate.Order} → {@code true}</li>
     *   <li>{@code com.example.aggregates.Customer} → {@code true}</li>
     *   <li>{@code com.example.domain.Product} → {@code false}</li>
     * </ul>
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if in aggregate package
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean isInAggregatePackage(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return qualifiedName.matches(".*\\.aggregates?\\..*");
    }

    /**
     * Checks if a type name ends with "Aggregate" or "AggregateRoot".
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code OrderAggregate} → {@code true}</li>
     *   <li>{@code CustomerAggregateRoot} → {@code true}</li>
     *   <li>{@code Customer} → {@code false}</li>
     * </ul>
     *
     * @param simpleName simple name (not {@code null})
     * @return {@code true} if has aggregate name
     * @throws NullPointerException if simpleName is null
     */
    public boolean hasAggregateRootName(String simpleName) {
        Objects.requireNonNull(simpleName, "simpleName");
        return simpleName.endsWith("Aggregate") || simpleName.endsWith("AggregateRoot");
    }
}
