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
 * Kind of a domain type.
 *
 * <p>This classification is intentionally coarse-grained and stable. It enables plugins to
 * apply different generation rules (e.g., persistence mapping) without needing compiler internals.</p>
 */
public enum DomainTypeKind {

    /**
     * An Aggregate Root in the DDD sense.
     *
     * <p>An Aggregate Root is the single entry point to a cluster of domain objects (aggregate).
     * It has a stable identity across the system and is referenced from outside the aggregate by ID only.</p>
     *
     * <p>Only Aggregate Roots should have repository ports in Hexagonal Architecture.</p>
     *
     * @since 0.3.0
     */
    AGGREGATE_ROOT,

    /**
     * A class-like entity with an identity (internal entity, not an aggregate root).
     *
     * <p>Internal entities are manipulated through their Aggregate Root and should not have
     * their own repository ports.</p>
     */
    ENTITY,

    /**
     * A value object (immutable semantic type, no identity).
     */
    VALUE_OBJECT,

    /**
     * A record type (Java {@code record}).
     */
    RECORD,

    /**
     * An enum type.
     */
    ENUMERATION,

    /**
     * A domain event type - represents an event in the domain model.
     *
     * <p>Domain events capture something that happened in the domain that
     * domain experts care about. Events are typically immutable records of
     * facts that occurred at a specific point in time.</p>
     *
     * @since 1.0.0
     */
    DOMAIN_EVENT,

    /**
     * A domain identifier type (e.g., {@code CustomerId}).
     *
     * <p>Identifiers may be records, value objects, or other wrappers; this kind indicates
     * semantic role rather than Java syntax.</p>
     */
    IDENTIFIER,

    /**
     * A domain-specific collection or wrapper around a collection.
     */
    COLLECTION,

    /**
     * Unknown or not classified.
     */
    OTHER
}
