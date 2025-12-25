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
package io.hexaglue.spi.ir.domain;

import java.util.Objects;
import java.util.Optional;

/**
 * Metadata about a relationship between domain types.
 *
 * <p>This metadata helps plugins generate correct infrastructure mappings while respecting
 * Domain-Driven Design principles, particularly the <strong>ID-only rule</strong> for
 * inter-aggregate references.</p>
 *
 * <h2>DDD Principles</h2>
 * <p>In Hexagonal Architecture and DDD:</p>
 * <ul>
 *   <li><strong>Intra-aggregate relationships</strong> can use full object references and should
 *       cascade operations (e.g., {@code Order → List<OrderItem>})</li>
 *   <li><strong>Inter-aggregate relationships</strong> MUST use ID-only references to maintain
 *       aggregate boundaries (e.g., {@code Order → CustomerId}, NOT {@code Order → Customer})</li>
 * </ul>
 *
 * <h2>Detection Strategy</h2>
 * <p>Relationships are detected through:</p>
 * <ol>
 *   <li><strong>jMolecules annotations</strong> (highest priority):
 *       <ul>
 *         <li>{@code @Association} → Inter-aggregate relationship</li>
 *         <li>{@code @Entity} on target type → Intra-aggregate (internal entity)</li>
 *         <li>{@code @ValueObject} on target type → Embedded value object</li>
 *       </ul>
 *   </li>
 *   <li><strong>YAML configuration</strong>:
 *       <ul>
 *         <li>{@code types.<fqn>.properties.<name>.relationship.kind}</li>
 *         <li>{@code types.<fqn>.properties.<name>.relationship.interAggregate}</li>
 *       </ul>
 *   </li>
 *   <li><strong>Heuristics</strong>:
 *       <ul>
 *         <li>Property type ends with "Id" → Inter-aggregate reference</li>
 *         <li>Target type is {@code AGGREGATE_ROOT} → Inter-aggregate</li>
 *         <li>Target type is {@code VALUE_OBJECT} → Embedded</li>
 *         <li>Collection of entities → Check aggregate boundary</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In plugin generation code
 * for (DomainPropertyView property : domainType.properties()) {
 *     Optional<RelationshipMetadata> rel = property.relationship();
 *
 *     if (rel.isPresent()) {
 *         if (rel.get().isInterAggregate()) {
 *             // Generate ID-only foreign key column
 *             generateForeignKey(property.name() + "_id");
 *         } else {
 *             // Generate full relationship mapping
 *             switch (rel.get().kind()) {
 *                 case ONE_TO_MANY -> generateOneToMany(property, rel.get());
 *                 case ONE_TO_ONE -> {
 *                     if (isValueObject(rel.get().targetQualifiedName())) {
 *                         generateEmbedded(property);
 *                     } else {
 *                         generateOneToOne(property, rel.get());
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * }</pre>
 *
 * @since 0.4.0
 * @see RelationshipKind
 * @see DomainPropertyView#relationship()
 */
public interface RelationshipMetadata {

    /**
     * Type of relationship between the source and target types.
     *
     * @return relationship kind (never {@code null})
     */
    RelationshipKind kind();

    /**
     * Qualified name of the target type in the relationship.
     *
     * <p>For collections, this is the element type (e.g., for {@code List<OrderItem>},
     * returns {@code "com.example.domain.OrderItem"}).</p>
     *
     * <p>For ID-only references, this may be the aggregate root type, not the ID type
     * (e.g., for {@code CustomerId}, may return {@code "com.example.domain.Customer"}).</p>
     *
     * @return target type qualified name (never blank)
     */
    String targetQualifiedName();

    /**
     * Indicates whether this is a relationship between different aggregates.
     *
     * <p><strong>DDD Rule:</strong> Inter-aggregate relationships should be by ID-only.
     * If this returns {@code true}, the property should use an ID type (e.g., {@code CustomerId})
     * rather than a full object reference (e.g., {@code Customer}).</p>
     *
     * <p><strong>Infrastructure implications:</strong></p>
     * <ul>
     *   <li>{@code true} → Generate foreign key column only, no cascade operations</li>
     *   <li>{@code false} → Generate full relationship mapping with cascade</li>
     * </ul>
     *
     * @return {@code true} if relationship crosses aggregate boundaries, {@code false} otherwise
     */
    boolean isInterAggregate();

    /**
     * Whether this relationship is bidirectional.
     *
     * <p>A bidirectional relationship has an inverse side that references back to the owning entity.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // Bidirectional relationship
     * class Order {
     *     List<OrderItem> items;  // Owning side
     * }
     *
     * class OrderItem {
     *     Order order;  // Inverse side (mappedBy)
     * }
     * }</pre>
     *
     * @return {@code true} if bidirectional (has inverse side), {@code false} if unidirectional
     */
    default boolean isBidirectional() {
        return mappedBy().isPresent();
    }

    /**
     * If bidirectional, the name of the inverse property on the target type.
     *
     * <p>This is used for JPA {@code mappedBy} attribute to indicate which side owns the relationship.</p>
     *
     * <p><strong>Example:</strong> For {@code Order.items} pointing to {@code OrderItem}, if
     * {@code OrderItem} has a back-reference {@code Order order}, this returns {@code "order"}.</p>
     *
     * @return inverse property name if bidirectional, empty otherwise
     */
    default Optional<String> mappedBy() {
        return Optional.empty();
    }

    /**
     * Creates a relationship metadata instance.
     *
     * @param kind relationship kind
     * @param targetQualifiedName qualified name of target type
     * @param isInterAggregate whether relationship crosses aggregate boundaries
     * @return relationship metadata instance (never {@code null})
     * @throws NullPointerException if kind or targetQualifiedName is null
     * @throws IllegalArgumentException if targetQualifiedName is blank
     */
    static RelationshipMetadata of(RelationshipKind kind, String targetQualifiedName, boolean isInterAggregate) {
        return of(kind, targetQualifiedName, isInterAggregate, null);
    }

    /**
     * Creates a bidirectional relationship metadata instance.
     *
     * <p>Convenience method for creating bidirectional relationships with explicit mappedBy.</p>
     *
     * @param kind relationship kind
     * @param targetQualifiedName qualified name of target type
     * @param isInterAggregate whether relationship crosses aggregate boundaries
     * @param mappedBy inverse property name on the target type
     * @return bidirectional relationship metadata instance (never {@code null})
     * @throws NullPointerException if kind, targetQualifiedName, or mappedBy is null
     * @throws IllegalArgumentException if targetQualifiedName or mappedBy is blank
     * @since 0.4.0
     */
    static RelationshipMetadata bidirectional(
            RelationshipKind kind, String targetQualifiedName, boolean isInterAggregate, String mappedBy) {
        Objects.requireNonNull(mappedBy, "mappedBy");
        if (mappedBy.isBlank()) {
            throw new IllegalArgumentException("mappedBy must not be blank");
        }
        return of(kind, targetQualifiedName, isInterAggregate, mappedBy);
    }

    /**
     * Creates a relationship metadata instance with optional bidirectional mapping.
     *
     * @param kind relationship kind
     * @param targetQualifiedName qualified name of target type
     * @param isInterAggregate whether relationship crosses aggregate boundaries
     * @param mappedBy inverse property name if bidirectional, or {@code null}
     * @return relationship metadata instance (never {@code null})
     * @throws NullPointerException if kind or targetQualifiedName is null
     * @throws IllegalArgumentException if targetQualifiedName is blank
     */
    static RelationshipMetadata of(
            RelationshipKind kind, String targetQualifiedName, boolean isInterAggregate, String mappedBy) {

        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(targetQualifiedName, "targetQualifiedName");

        String tqn = targetQualifiedName.trim();
        if (tqn.isEmpty()) {
            throw new IllegalArgumentException("targetQualifiedName must not be blank");
        }

        String mb = (mappedBy == null || mappedBy.isBlank()) ? null : mappedBy.trim();

        return new RelationshipMetadata() {
            @Override
            public RelationshipKind kind() {
                return kind;
            }

            @Override
            public String targetQualifiedName() {
                return tqn;
            }

            @Override
            public boolean isInterAggregate() {
                return isInterAggregate;
            }

            @Override
            public Optional<String> mappedBy() {
                return Optional.ofNullable(mb);
            }

            @Override
            public String toString() {
                String boundary = isInterAggregate ? "inter-aggregate" : "intra-aggregate";
                String mapping = mb != null ? ", mappedBy=" + mb : "";
                return "RelationshipMetadata{kind=" + kind + ", target=" + tqn + ", " + boundary + mapping + "}";
            }
        };
    }
}
