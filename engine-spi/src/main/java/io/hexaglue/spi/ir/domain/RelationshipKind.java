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

/**
 * Kind of relationship between domain types.
 *
 * <p>This classification helps plugins generate correct infrastructure mappings while respecting
 * DDD principles (especially the ID-only rule for inter-aggregate references).</p>
 *
 * <h2>Usage in Infrastructure Generation</h2>
 * <ul>
 *   <li><strong>Intra-aggregate relationships</strong> ({@link #ONE_TO_ONE}, {@link #ONE_TO_MANY}
 *       when target is internal entity or value object) → Full object mapping with cascade operations</li>
 *   <li><strong>Inter-aggregate relationships</strong> (target is aggregate root) → ID-only reference,
 *       no cascade operations</li>
 * </ul>
 *
 * @since 0.4.0
 */
public enum RelationshipKind {

    /**
     * One-to-one relationship.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Embedded value object: {@code Customer → Address} (value object)</li>
     *   <li>ID-only reference: {@code Order → CustomerId} (inter-aggregate)</li>
     * </ul>
     *
     * <p><strong>Infrastructure mapping:</strong></p>
     * <ul>
     *   <li>Value object → JPA {@code @Embedded}</li>
     *   <li>Inter-aggregate → Foreign key column (ID-only)</li>
     * </ul>
     */
    ONE_TO_ONE,

    /**
     * One-to-many relationship (collection of related entities or value objects).
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Intra-aggregate: {@code Order → List<OrderItem>} (internal entities)</li>
     *   <li>Intra-aggregate: {@code Customer → List<Address>} (value objects)</li>
     * </ul>
     *
     * <p><strong>Infrastructure mapping:</strong></p>
     * <ul>
     *   <li>Internal entities → JPA {@code @OneToMany(cascade=ALL, orphanRemoval=true)}</li>
     *   <li>Value objects → JPA {@code @ElementCollection}</li>
     * </ul>
     *
     * <p><strong>DDD Rule:</strong> One-to-many relationships to aggregate roots violate DDD principles.
     * Use ID-only collections instead: {@code List<CustomerId>}.</p>
     */
    ONE_TO_MANY,

    /**
     * Many-to-one relationship (reference to parent or owning entity).
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Intra-aggregate: {@code OrderItem → Order} (back-reference)</li>
     *   <li>Inter-aggregate: {@code Order → CustomerId} (ID-only reference)</li>
     * </ul>
     *
     * <p><strong>Infrastructure mapping:</strong></p>
     * <ul>
     *   <li>Intra-aggregate → JPA {@code @ManyToOne}</li>
     *   <li>Inter-aggregate → Foreign key column (ID-only)</li>
     * </ul>
     */
    MANY_TO_ONE,

    /**
     * Many-to-many relationship (collection of references).
     *
     * <p><strong>DDD Warning:</strong> Many-to-many relationships between aggregates should be
     * modeled as ID-only collections: {@code List<ProductId>}, not {@code List<Product>}.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>Inter-aggregate (correct): {@code Order → List<ProductId>}</li>
     *   <li>Inter-aggregate (violation): {@code Order → List<Product>} ❌</li>
     * </ul>
     *
     * <p><strong>Infrastructure mapping:</strong></p>
     * <ul>
     *   <li>ID-only collection → Join table with IDs</li>
     * </ul>
     */
    MANY_TO_MANY,

    /**
     * Collection of simple values (not domain entities).
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code Customer → List<String>} (tags, emails, etc.)</li>
     *   <li>{@code Product → Set<Category>} (enum values)</li>
     * </ul>
     *
     * <p><strong>Infrastructure mapping:</strong></p>
     * <ul>
     *   <li>JPA {@code @ElementCollection}</li>
     *   <li>Separate table with foreign key back to parent</li>
     * </ul>
     */
    ELEMENT_COLLECTION
}
