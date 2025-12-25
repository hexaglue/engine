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
package io.hexaglue.core.internal.ir.support;

import io.hexaglue.core.internal.InternalMarker;

/**
 * Central registry of jMolecules annotation qualified names.
 *
 * <p>
 * This class provides a single source of truth for all jMolecules DDD annotation
 * qualified names used throughout HexaGlue. By centralizing these constants, we:
 * </p>
 * <ul>
 *   <li>Eliminate duplication across detectors and signal classes</li>
 *   <li>Ensure consistency in annotation detection</li>
 *   <li>Simplify maintenance and updates</li>
 *   <li>Provide clear documentation of supported jMolecules features</li>
 * </ul>
 *
 * <h2>jMolecules Support Strategy</h2>
 * <p>
 * HexaGlue supports jMolecules annotations <strong>optionally</strong>. These annotations
 * are detected via string-based qualified name matching, with NO compile-time dependency
 * on jMolecules libraries. This means:
 * </p>
 * <ul>
 *   <li>HexaGlue works perfectly WITHOUT jMolecules on the classpath</li>
 *   <li>When jMolecules IS present, annotations enrich heuristic-based detection</li>
 *   <li>Annotations take precedence over naming conventions and structural heuristics</li>
 * </ul>
 *
 * <h2>Supported Annotations</h2>
 *
 * <h3>DDD Tactical Patterns (org.jmolecules.ddd.annotation.*)</h3>
 * <ul>
 *   <li>{@link #AGGREGATE_ROOT} - Marks a type as an aggregate root</li>
 *   <li>{@link #ENTITY} - Marks a type as a domain entity (may be aggregate root or internal)</li>
 *   <li>{@link #VALUE_OBJECT} - Marks a type as a value object</li>
 *   <li>{@link #IDENTITY} - Marks a field as an identity property</li>
 *   <li>{@link #REPOSITORY} - Marks an interface as a repository port</li>
 *   <li>{@link #ASSOCIATION} - Marks a property as an inter-aggregate relationship</li>
 * </ul>
 *
 * <h3>Domain Events (org.jmolecules.event.annotation.*)</h3>
 * <ul>
 *   <li>{@link #DOMAIN_EVENT} - Marks a type as a domain event</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>In Detectors (JSR-269 TypeElement)</h3>
 * <pre>{@code
 * public boolean hasValueObjectAnnotation(TypeElement typeElement) {
 *     return typeElement.getAnnotationMirrors().stream()
 *         .map(mirror -> mirror.getAnnotationType().toString())
 *         .anyMatch(JMoleculesAnnotations.VALUE_OBJECT::equals);
 * }
 * }</pre>
 *
 * <h3>In Signal Classes (AnnotationIndex)</h3>
 * <pre>{@code
 * public boolean targetIsValueObject(AnnotationIndex annotations) {
 *     return annotations.has(JMoleculesAnnotations.VALUE_OBJECT);
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class contains only immutable constants and is inherently thread-safe.
 * </p>
 *
 * @since 1.0.0
 */
@InternalMarker(reason = "Central jMolecules annotation constants registry; not exposed to plugins")
public final class JMoleculesAnnotations {

    // ========================================================================
    // DDD Tactical Patterns (org.jmolecules.ddd.annotation.*)
    // ========================================================================

    /**
     * jMolecules {@code @AggregateRoot} annotation.
     *
     * <p>
     * Explicitly marks a type as an aggregate root - the entry point to a consistency boundary.
     * This is the strongest signal for aggregate root classification.
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.ddd.annotation}</p>
     */
    public static final String AGGREGATE_ROOT = "org.jmolecules.ddd.annotation.AggregateRoot";

    /**
     * jMolecules {@code @Entity} annotation.
     *
     * <p>
     * Marks a type as a domain entity. Note that this can indicate either:
     * </p>
     * <ul>
     *   <li>An aggregate root (if used without {@code @AggregateRoot})</li>
     *   <li>An internal entity within an aggregate</li>
     * </ul>
     * <p>
     * Additional context (port analysis, package conventions) is needed for precise classification.
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.ddd.annotation}</p>
     */
    public static final String ENTITY = "org.jmolecules.ddd.annotation.Entity";

    /**
     * jMolecules {@code @ValueObject} annotation.
     *
     * <p>
     * Explicitly marks a type as a value object - an immutable type without identity.
     * When present, this annotation takes precedence over immutability heuristics.
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.ddd.annotation}</p>
     */
    public static final String VALUE_OBJECT = "org.jmolecules.ddd.annotation.ValueObject";

    /**
     * jMolecules {@code @Identity} annotation.
     *
     * <p>
     * Marks a <strong>field</strong> as an identity property of an entity or aggregate root.
     * When present, this annotation takes precedence over naming conventions (e.g., "id", "identifier").
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.ddd.annotation}</p>
     * <p><strong>Target:</strong> FIELD</p>
     */
    public static final String IDENTITY = "org.jmolecules.ddd.annotation.Identity";

    /**
     * jMolecules {@code @Repository} annotation.
     *
     * <p>
     * Marks an interface as a repository - a port for aggregate persistence.
     * When present, this annotation is a strong signal for port detection, even without
     * conventional port package names.
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.ddd.annotation}</p>
     */
    public static final String REPOSITORY = "org.jmolecules.ddd.annotation.Repository";

    /**
     * jMolecules {@code @Association} annotation.
     *
     * <p>
     * Explicitly marks a property as an inter-aggregate relationship. This indicates
     * that the property references another aggregate root and should use ID-only references
     * rather than direct object references.
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.ddd.annotation}</p>
     * <p><strong>Target:</strong> FIELD</p>
     */
    public static final String ASSOCIATION = "org.jmolecules.ddd.annotation.Association";

    // ========================================================================
    // Domain Events (org.jmolecules.event.annotation.*)
    // ========================================================================

    /**
     * jMolecules {@code @DomainEvent} annotation.
     *
     * <p>
     * Marks a type as a domain event - an immutable record of something that happened
     * in the domain that domain experts care about. When present, this annotation
     * classifies the type as {@link io.hexaglue.spi.ir.domain.DomainTypeKind#DOMAIN_EVENT}.
     * </p>
     *
     * <p><strong>Package:</strong> {@code org.jmolecules.event.annotation}</p>
     *
     * <p><strong>Note:</strong> This is in a different package than the DDD tactical patterns
     * ({@code event.annotation} vs {@code ddd.annotation}).</p>
     */
    public static final String DOMAIN_EVENT = "org.jmolecules.event.annotation.DomainEvent";

    // ========================================================================
    // Private Constructor
    // ========================================================================

    /**
     * Private constructor to prevent instantiation.
     *
     * @throws AssertionError always, to prevent instantiation
     */
    private JMoleculesAnnotations() {
        throw new AssertionError("JMoleculesAnnotations is a constants class and cannot be instantiated");
    }
}
