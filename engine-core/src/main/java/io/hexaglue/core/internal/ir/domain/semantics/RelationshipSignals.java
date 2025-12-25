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
 * Encapsulates well-known jMolecules annotations for relationship detection.
 *
 * <p>This class centralizes all the "signals" that might indicate a property is a relationship
 * to another domain type. It focuses on jMolecules DDD annotations to keep the domain pure
 * and framework-agnostic.</p>
 *
 * <h2>Supported jMolecules Annotations</h2>
 * <ul>
 *   <li><strong>@Association:</strong> Explicitly marks an inter-aggregate relationship</li>
 *   <li><strong>@AggregateRoot:</strong> Indicates target type is an aggregate root</li>
 *   <li><strong>@Entity:</strong> Indicates target type is an internal entity</li>
 *   <li><strong>@ValueObject:</strong> Indicates target type is a value object (embedded)</li>
 *   <li><strong>@Identity:</strong> Indicates target type is an ID type</li>
 * </ul>
 *
 * <h2>Detection Strategy</h2>
 * <p>Relationships are detected by examining:</p>
 * <ol>
 *   <li>Annotations on the property itself ({@code @Association})</li>
 *   <li>Annotations on the target type ({@code @AggregateRoot}, {@code @Entity}, {@code @ValueObject})</li>
 *   <li>Property type naming conventions (e.g., ends with "Id")</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is stateless and thread-safe.</p>
 *
 * @since 0.4.0
 */
@InternalMarker(reason = "Internal semantics signals; not exposed to plugins")
public final class RelationshipSignals {

    /**
     * Creates a relationship signals detector.
     */
    public RelationshipSignals() {
        // Default constructor
    }

    /**
     * Checks if property is explicitly marked as an association (inter-aggregate relationship).
     *
     * <p>jMolecules {@code @Association} explicitly indicates that this property references
     * another aggregate. This is the strongest signal for inter-aggregate relationships.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * public record Order(
     *     OrderId id,
     *     @Association CustomerId customerId  // ← Explicit inter-aggregate reference
     * ) {}
     * }</pre>
     *
     * @param propertyAnnotations annotations on the property
     * @return {@code true} if property has {@code @Association}
     * @throws NullPointerException if annotations is null
     */
    public boolean hasAssociationMarker(AnnotationIndex propertyAnnotations) {
        Objects.requireNonNull(propertyAnnotations, "propertyAnnotations");
        return propertyAnnotations.has(JMoleculesAnnotations.ASSOCIATION);
    }

    /**
     * Checks if target type is marked as an aggregate root.
     *
     * <p>If the property's type is annotated with {@code @AggregateRoot}, this indicates
     * an inter-aggregate relationship that should use ID-only references.</p>
     *
     * @param targetTypeAnnotations annotations on the target type
     * @return {@code true} if target is aggregate root
     * @throws NullPointerException if annotations is null
     */
    public boolean targetIsAggregateRoot(AnnotationIndex targetTypeAnnotations) {
        Objects.requireNonNull(targetTypeAnnotations, "targetTypeAnnotations");
        return targetTypeAnnotations.has(JMoleculesAnnotations.AGGREGATE_ROOT);
    }

    /**
     * Checks if target type is marked as an internal entity.
     *
     * <p>jMolecules {@code @Entity} (not {@code @AggregateRoot}) indicates an internal
     * entity within an aggregate. Relationships to internal entities are intra-aggregate.</p>
     *
     * @param targetTypeAnnotations annotations on the target type
     * @return {@code true} if target is internal entity
     * @throws NullPointerException if annotations is null
     */
    public boolean targetIsInternalEntity(AnnotationIndex targetTypeAnnotations) {
        Objects.requireNonNull(targetTypeAnnotations, "targetTypeAnnotations");
        return targetTypeAnnotations.has(JMoleculesAnnotations.ENTITY);
    }

    /**
     * Checks if target type is marked as a value object.
     *
     * <p>Value objects should be embedded rather than stored as separate entities.
     * This indicates a {@link io.hexaglue.spi.ir.domain.RelationshipKind#ONE_TO_ONE}
     * relationship that should be mapped with {@code @Embedded} in JPA.</p>
     *
     * @param targetTypeAnnotations annotations on the target type
     * @return {@code true} if target is value object
     * @throws NullPointerException if annotations is null
     */
    public boolean targetIsValueObject(AnnotationIndex targetTypeAnnotations) {
        Objects.requireNonNull(targetTypeAnnotations, "targetTypeAnnotations");
        return targetTypeAnnotations.has(JMoleculesAnnotations.VALUE_OBJECT);
    }

    /**
     * Checks if target type is marked as an identity type.
     *
     * <p>Identity types (e.g., {@code CustomerId}) are ID wrappers that reference
     * aggregate roots. This is a strong signal for inter-aggregate relationships.</p>
     *
     * @param targetTypeAnnotations annotations on the target type
     * @return {@code true} if target is identity type
     * @throws NullPointerException if annotations is null
     */
    public boolean targetIsIdentity(AnnotationIndex targetTypeAnnotations) {
        Objects.requireNonNull(targetTypeAnnotations, "targetTypeAnnotations");
        return targetTypeAnnotations.has(JMoleculesAnnotations.IDENTITY);
    }

    /**
     * Checks if property type name suggests it's an ID reference.
     *
     * <p>Common conventions:</p>
     * <ul>
     *   <li>Ends with "Id" (e.g., {@code CustomerId}, {@code customerId})</li>
     *   <li>Ends with "ID" (e.g., {@code CustomerID})</li>
     * </ul>
     *
     * <p>This is a heuristic signal for inter-aggregate references.</p>
     *
     * @param typeName simple or qualified type name
     * @return {@code true} if name suggests ID type
     * @throws NullPointerException if typeName is null
     */
    public boolean typeNameSuggestsId(String typeName) {
        Objects.requireNonNull(typeName, "typeName");
        String name = typeName.trim();
        if (name.isEmpty()) {
            return false;
        }

        // Extract simple name if qualified
        String simpleName = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : name;

        return simpleName.endsWith("Id") || simpleName.endsWith("ID");
    }

    /**
     * Checks if property is a collection type.
     *
     * <p>Collections of domain objects indicate {@code ONE_TO_MANY} or
     * {@code ELEMENT_COLLECTION} relationships depending on the element type.</p>
     *
     * @param typeQualifiedName fully qualified type name
     * @return {@code true} if collection type
     * @throws NullPointerException if typeQualifiedName is null
     */
    public boolean isCollectionType(String typeQualifiedName) {
        Objects.requireNonNull(typeQualifiedName, "typeQualifiedName");
        String qn = typeQualifiedName.trim();

        return qn.equals("java.util.List")
                || qn.equals("java.util.Set")
                || qn.equals("java.util.Collection")
                || qn.equals("java.util.ArrayList")
                || qn.equals("java.util.HashSet")
                || qn.equals("java.util.LinkedHashSet")
                || qn.equals("java.util.TreeSet");
    }

    /**
     * Extracts the root entity type name from an ID type name.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code CustomerId} → {@code Customer}</li>
     *   <li>{@code OrderItemId} → {@code OrderItem}</li>
     *   <li>{@code com.example.domain.CustomerId} → {@code com.example.domain.Customer}</li>
     * </ul>
     *
     * @param idTypeName ID type name (simple or qualified)
     * @return inferred entity type name, or original name if no "Id" suffix
     * @throws NullPointerException if idTypeName is null
     */
    public String extractEntityNameFromIdType(String idTypeName) {
        Objects.requireNonNull(idTypeName, "idTypeName");
        String name = idTypeName.trim();

        if (name.endsWith("Id")) {
            return name.substring(0, name.length() - 2);
        }
        if (name.endsWith("ID")) {
            return name.substring(0, name.length() - 2);
        }

        return name;
    }
}
