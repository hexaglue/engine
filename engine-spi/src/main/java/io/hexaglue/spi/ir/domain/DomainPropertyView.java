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

import io.hexaglue.spi.stability.Evolvable;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of a domain property/member.
 *
 * <p>A property is a semantic field of a domain type as understood by HexaGlue.
 * It may correspond to:
 * <ul>
 *   <li>a record component</li>
 *   <li>a field with an accessor</li>
 *   <li>an accessor method (property-style)</li>
 * </ul>
 *
 * <p>This view intentionally does not expose compiler symbols. It focuses on information
 * that matters for generation and validation.</p>
 */
@Evolvable(since = "1.0.0")
public interface DomainPropertyView {

    /**
     * Stable property name (as used in generated code and diagnostics).
     *
     * @return property name (never blank)
     */
    String name();

    /**
     * Type reference of the property.
     *
     * @return property type (never {@code null})
     */
    TypeRef type();

    /**
     * Whether the property is part of the domain identity.
     *
     * <p>For entities, this typically marks the id property.</p>
     *
     * @return {@code true} if identity-related
     */
    boolean isIdentity();

    /**
     * Whether the property is considered immutable from the domain perspective.
     *
     * <p>HexaGlue strongly favors immutability but must support multiple styles.</p>
     *
     * @return {@code true} if immutable
     */
    boolean isImmutable();

    /**
     * Returns the qualified name of the declaring domain type, if known.
     *
     * <p>Useful for diagnostics and traceability.</p>
     *
     * @return declaring type qualified name if known
     */
    Optional<String> declaringType();

    /**
     * Returns a user-facing description, if any.
     *
     * <p>This can be used for generated documentation.</p>
     *
     * @return description if available
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Annotations present on this property at the source level.
     *
     * <p>This includes annotations on:
     * <ul>
     *   <li>Record components</li>
     *   <li>Fields</li>
     *   <li>Getter methods (property-style access)</li>
     * </ul>
     *
     * <p><strong>Important:</strong> The SPI only exposes annotations that are relevant for
     * code generation. Internal compiler annotations or retention-source annotations may be filtered.</p>
     *
     * <p><strong>Common use cases:</strong>
     * <ul>
     *   <li><strong>Nullability:</strong> {@code @Nullable}, {@code @NotNull} (JSR-305, JetBrains, etc.)</li>
     *   <li><strong>Validation:</strong> {@code @Size}, {@code @Min}, {@code @Max}, {@code @Pattern} (Bean Validation)</li>
     *   <li><strong>JPA hints:</strong> {@code @Column}, {@code @Lob}, {@code @Temporal}, {@code @Enumerated}</li>
     *   <li><strong>Documentation:</strong> {@code @Deprecated}</li>
     * </ul>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * // Check nullability from annotations
     * boolean isNullable = property.annotations().stream()
     *     .anyMatch(a -> a.is("jakarta.annotation.Nullable")
     *                 || a.is("org.jetbrains.annotations.Nullable"));
     *
     * // Extract JPA column length
     * Optional<Integer> columnLength = property.annotations().stream()
     *     .filter(a -> a.is("jakarta.persistence.Column"))
     *     .flatMap(a -> a.attribute("length", Integer.class))
     *     .findFirst();
     *
     * // Check validation constraints
     * Optional<Integer> maxSize = property.annotations().stream()
     *     .filter(a -> a.is("jakarta.validation.constraints.Size"))
     *     .flatMap(a -> a.attribute("max", Integer.class))
     *     .findFirst();
     * }</pre>
     *
     * <p><strong>HexaGlue philosophy on annotations:</strong>
     * HexaGlue supports three approaches for metadata, offering flexibility based on project constraints:
     * <ul>
     *   <li><strong>Pure Domain (Recommended):</strong> Use only architectural annotations
     *       ({@code @AggregateRoot}, {@code @NotNull}) and configure technical metadata via
     *       {@code hexaglue.yaml} or rely on heuristics</li>
     *   <li><strong>Pragmatic:</strong> Use technical annotations ({@code @Column}, {@code @Size})
     *       directly in the domain for familiarity and convenience</li>
     *   <li><strong>Hybrid:</strong> Combine both approaches, using heuristics as fallback with
     *       YAML overrides for edge cases</li>
     * </ul>
     *
     * @return immutable list of annotations (never {@code null}, may be empty)
     * @since 0.3.0
     * @see AnnotationView
     */
    default List<AnnotationView> annotations() {
        return List.of();
    }

    /**
     * Relationship metadata if this property represents a relationship to another domain type.
     *
     * <p>This metadata helps plugins generate correct infrastructure mappings while respecting
     * Domain-Driven Design principles, particularly the <strong>ID-only rule</strong> for
     * inter-aggregate references.</p>
     *
     * <h2>When is a relationship detected?</h2>
     * <p>A property is considered a relationship when:</p>
     * <ul>
     *   <li>The property type is another domain type (entity, value object, or aggregate root)</li>
     *   <li>The property is a collection of domain types</li>
     *   <li>The property type is an ID type referencing another aggregate</li>
     *   <li>The property is explicitly marked with {@code @Association} (jMolecules)</li>
     * </ul>
     *
     * <h2>Detection Strategy</h2>
     * <p>Relationships are detected through (in order of priority):</p>
     * <ol>
     *   <li><strong>jMolecules annotations:</strong> {@code @Association}, target type's
     *       {@code @Entity}/{@code @ValueObject}/{@code @AggregateRoot}</li>
     *   <li><strong>YAML configuration:</strong> {@code types.<fqn>.properties.<name>.relationship.*}</li>
     *   <li><strong>Heuristics:</strong> Type naming (e.g., {@code CustomerId}), target type kind,
     *       collection analysis</li>
     * </ol>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * // In JPA plugin - Generate relationship mapping
     * for (DomainPropertyView property : domainType.properties()) {
     *     Optional<RelationshipMetadata> rel = property.relationship();
     *
     *     if (rel.isPresent()) {
     *         if (rel.get().isInterAggregate()) {
     *             // ✅ CORRECT: Inter-aggregate → ID-only FK
     *             builder.addColumn(property.name() + "_id", "BIGINT");
     *         } else {
     *             // Intra-aggregate → Full relationship
     *             switch (rel.get().kind()) {
     *                 case ONE_TO_MANY -> generateOneToMany(property, rel.get());
     *                 case ONE_TO_ONE -> {
     *                     if (isValueObject(rel.get().targetQualifiedName())) {
     *                         generateEmbedded(property);  // @Embedded
     *                     } else {
     *                         generateOneToOne(property, rel.get());
     *                     }
     *                 }
     *             }
     *         }
     *     } else {
     *         // Simple property (String, Integer, etc.)
     *         generateSimpleColumn(property);
     *     }
     * }
     * }</pre>
     *
     * <h2>DDD Compliance</h2>
     * <p>This metadata enforces the DDD rule that references between aggregates MUST be by ID only:</p>
     * <ul>
     *   <li>✅ {@code Order → CustomerId} (inter-aggregate, ID-only)</li>
     *   <li>✅ {@code Order → List<OrderItem>} (intra-aggregate, full objects)</li>
     *   <li>✅ {@code Customer → Address} (value object, embedded)</li>
     *   <li>❌ {@code Order → Customer} (inter-aggregate with full object - violation!)</li>
     * </ul>
     *
     * @return relationship metadata if this property represents a relationship, empty otherwise
     * @since 0.4.0
     * @see RelationshipMetadata
     * @see RelationshipKind
     */
    default Optional<RelationshipMetadata> relationship() {
        return Optional.empty();
    }

    /**
     * Creates a simple immutable {@link DomainPropertyView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param name property name
     * @param type property type
     * @param identity whether identity
     * @param immutable whether immutable
     * @param declaringType declaring type qualified name (nullable)
     * @return property view
     */
    static DomainPropertyView of(String name, TypeRef type, boolean identity, boolean immutable, String declaringType) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        final String n = name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        final String dt = (declaringType == null || declaringType.isBlank()) ? null : declaringType.trim();

        return new DomainPropertyView() {
            @Override
            public String name() {
                return n;
            }

            @Override
            public TypeRef type() {
                return type;
            }

            @Override
            public boolean isIdentity() {
                return identity;
            }

            @Override
            public boolean isImmutable() {
                return immutable;
            }

            @Override
            public Optional<String> declaringType() {
                return Optional.ofNullable(dt);
            }
        };
    }
}
