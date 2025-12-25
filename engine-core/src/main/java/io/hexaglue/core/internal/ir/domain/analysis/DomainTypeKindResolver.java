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
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import java.util.Objects;

/**
 * Resolves the domain type kind classification for analyzed types.
 *
 * <p>
 * This resolver determines whether a type should be classified as an entity, value object,
 * identifier, record, enum, collection, or other domain type. The classification is based
 * on structural analysis, naming conventions, and semantic hints from annotations or patterns.
 * </p>
 *
 * <h2>Classification Strategy</h2>
 * <p>
 * The resolver applies the following heuristics in order:
 * </p>
 * <ol>
 *   <li><strong>Enums:</strong> Java {@code enum} types → {@link DomainTypeKind#ENUMERATION}</li>
 *   <li><strong>Records:</strong> Java {@code record} types → {@link DomainTypeKind#RECORD}</li>
 *   <li><strong>Identifiers:</strong> Types ending with "Id" or containing identity markers → {@link DomainTypeKind#IDENTIFIER}</li>
 *   <li><strong>Collections:</strong> Types wrapping collections → {@link DomainTypeKind#COLLECTION}</li>
 *   <li><strong>Value Objects:</strong> Immutable types without identity → {@link DomainTypeKind#VALUE_OBJECT}</li>
 *   <li><strong>Entities:</strong> Types with identity → {@link DomainTypeKind#ENTITY}</li>
 *   <li><strong>Other:</strong> Unknown or unclassified types → {@link DomainTypeKind#OTHER}</li>
 * </ol>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Deterministic:</strong> Same input always produces same output</li>
 *   <li><strong>Extensible:</strong> Can be enhanced with annotation-based hints</li>
 *   <li><strong>Conservative:</strong> Prefers {@link DomainTypeKind#OTHER} when uncertain</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainTypeKindResolver resolver = new DomainTypeKindResolver();
 * TypeElement element = ...;
 *
 * DomainTypeKind kind = resolver.resolve(
 *     element.getSimpleName().toString(),
 *     element.getKind() == ElementKind.ENUM,
 *     element.getKind() == ElementKind.RECORD,
 *     hasIdentityProperty(element),
 *     isImmutable(element)
 * );
 * }</pre>
 */
@InternalMarker(reason = "Internal domain analysis; not exposed to plugins")
public final class DomainTypeKindResolver {

    /**
     * Creates a domain type kind resolver.
     */
    public DomainTypeKindResolver() {
        // Default constructor
    }

    /**
     * Resolves the domain type kind for a given type.
     *
     * <p>
     * This method applies classification heuristics based on the type's structure and naming.
     * </p>
     *
     * <p>
     * <strong>Note on AGGREGATE_ROOT vs ENTITY:</strong> This resolver returns {@code ENTITY}
     * for all entity types. The distinction between aggregate roots and internal entities
     * is made later during IR enrichment, based on port analysis, annotations, and configuration.
     * See aggregate root detection in {@code DomainTypeViewImpl}.
     * </p>
     *
     * @param typeName   simple or qualified name of the type (not {@code null})
     * @param isEnum     whether the type is a Java enum
     * @param isRecord   whether the type is a Java record
     * @param hasIdentity whether the type has an identity property
     * @param isImmutable whether the type is immutable
     * @param hasDomainEventAnnotation whether the type has {@code @DomainEvent} annotation
     * @param hasValueObjectAnnotation whether the type has {@code @ValueObject} annotation
     * @param hasEntityAnnotation whether the type has {@code @Entity} annotation (jMolecules)
     * @return domain type kind classification (never {@code null})
     * @throws NullPointerException if typeName is null
     */
    public DomainTypeKind resolve(
            String typeName,
            boolean isEnum,
            boolean isRecord,
            boolean hasIdentity,
            boolean isImmutable,
            boolean hasDomainEventAnnotation,
            boolean hasValueObjectAnnotation,
            boolean hasEntityAnnotation) {
        Objects.requireNonNull(typeName, "typeName");

        // Domain event types (explicit annotation takes precedence)
        if (hasDomainEventAnnotation) {
            return DomainTypeKind.DOMAIN_EVENT;
        }

        // Enum types
        if (isEnum) {
            return DomainTypeKind.ENUMERATION;
        }

        // Record types
        if (isRecord) {
            return DomainTypeKind.RECORD;
        }

        // Extract simple name if fully qualified
        String simpleName = extractSimpleName(typeName);

        // Identifier types (naming convention)
        if (looksLikeIdentifier(simpleName)) {
            return DomainTypeKind.IDENTIFIER;
        }

        // Collection types (naming convention)
        if (looksLikeCollection(simpleName)) {
            return DomainTypeKind.COLLECTION;
        }

        // Explicit @ValueObject annotation takes precedence
        if (hasValueObjectAnnotation) {
            return DomainTypeKind.VALUE_OBJECT;
        }

        // Explicit jMolecules @Entity annotation
        // NOTE: @Entity indicates domain entity but doesn't specify if it's an aggregate root.
        // The AGGREGATE_ROOT vs ENTITY distinction is made later by aggregate root detection.
        if (hasEntityAnnotation) {
            return DomainTypeKind.ENTITY;
        }

        // Entity types (have identity property)
        // NOTE: We return ENTITY here. The distinction between AGGREGATE_ROOT and
        // internal ENTITY is determined later by aggregate root detection logic.
        if (hasIdentity) {
            return DomainTypeKind.ENTITY;
        }

        // Value objects (immutable without identity)
        if (isImmutable) {
            return DomainTypeKind.VALUE_OBJECT;
        }

        // Default to OTHER when uncertain
        return DomainTypeKind.OTHER;
    }

    /**
     * Determines if a type name suggests it's an identifier.
     *
     * <p>
     * Identifiers typically end with "Id", "ID", or "Identifier".
     * Examples: CustomerId, OrderID, UserIdentifier
     * </p>
     *
     * @param simpleName simple name (not {@code null})
     * @return {@code true} if looks like an identifier
     */
    private boolean looksLikeIdentifier(String simpleName) {
        return simpleName.endsWith("Id") || simpleName.endsWith("ID") || simpleName.endsWith("Identifier");
    }

    /**
     * Determines if a type name suggests it's a collection wrapper.
     *
     * <p>
     * Collections typically end with "List", "Set", "Collection", or plurals.
     * Examples: OrderItems, ProductList, TagSet
     * </p>
     *
     * @param simpleName simple name (not {@code null})
     * @return {@code true} if looks like a collection
     */
    private boolean looksLikeCollection(String simpleName) {
        return simpleName.endsWith("List")
                || simpleName.endsWith("Set")
                || simpleName.endsWith("Collection")
                || simpleName.endsWith("Items");
    }

    /**
     * Extracts the simple name from a potentially qualified name.
     *
     * <p>
     * Examples:
     * </p>
     * <ul>
     *   <li>{@code "com.example.Customer"} → {@code "Customer"}</li>
     *   <li>{@code "CustomerId"} → {@code "CustomerId"}</li>
     * </ul>
     *
     * @param typeName type name (not {@code null})
     * @return simple name (never {@code null})
     */
    private String extractSimpleName(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot >= 0 ? typeName.substring(lastDot + 1) : typeName;
    }
}
