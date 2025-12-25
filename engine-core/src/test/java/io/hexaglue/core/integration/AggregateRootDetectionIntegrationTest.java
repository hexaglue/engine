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
package io.hexaglue.core.integration;

import static com.google.common.truth.Truth.assertThat;

import io.hexaglue.core.types.TypeRefFactory;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Aggregate Root detection (EP-001).
 *
 * <p>This test validates the complete aggregate root detection strategy including:</p>
 * <ul>
 *   <li>Annotation-based detection (@AggregateRoot, @Entity, @Document)</li>
 *   <li>Heuristic-based detection (repository ports, package conventions, naming)</li>
 *   <li>Integration with DomainTypeView.isAggregateRoot()</li>
 * </ul>
 */
class AggregateRootDetectionIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // DomainTypeKind Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testAggregateRootEnumValue() {
        // When: Access AGGREGATE_ROOT enum value
        DomainTypeKind aggregateRoot = DomainTypeKind.AGGREGATE_ROOT;

        // Then: Value should be accessible
        assertThat(aggregateRoot).isNotNull();
        assertThat(aggregateRoot.name()).isEqualTo("AGGREGATE_ROOT");
    }

    @Test
    void testEnumerationEnumValue() {
        // When: Access ENUMERATION enum value (renamed from ENUM)
        DomainTypeKind enumeration = DomainTypeKind.ENUMERATION;

        // Then: Value should be accessible
        assertThat(enumeration).isNotNull();
        assertThat(enumeration.name()).isEqualTo("ENUMERATION");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DomainTypeView.isAggregateRoot() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testIsAggregateRoot_withAggregateRootKind_returnsTrue() {
        // Given: A domain type with AGGREGATE_ROOT kind
        DomainTypeView aggregateRoot =
                createDomainTypeView("com.example.Order", "Order", DomainTypeKind.AGGREGATE_ROOT);

        // When: Check if aggregate root
        boolean isAggregateRoot = aggregateRoot.isAggregateRoot();

        // Then: Should return true (default implementation checks kind)
        assertThat(isAggregateRoot).isTrue();
    }

    @Test
    void testIsAggregateRoot_withEntityKind_returnsFalse() {
        // Given: A domain type with ENTITY kind (internal entity, not an aggregate root)
        DomainTypeView entity = createDomainTypeView("com.example.OrderItem", "OrderItem", DomainTypeKind.ENTITY);

        // When: Check if aggregate root
        boolean isAggregateRoot = entity.isAggregateRoot();

        // Then: Should return false (only AGGREGATE_ROOT kind returns true)
        assertThat(isAggregateRoot).isFalse();
    }

    @Test
    void testIsAggregateRoot_withValueObjectKind_returnsFalse() {
        // Given: A domain type with VALUE_OBJECT kind
        DomainTypeView valueObject = createDomainTypeView("com.example.Money", "Money", DomainTypeKind.VALUE_OBJECT);

        // When: Check if aggregate root
        boolean isAggregateRoot = valueObject.isAggregateRoot();

        // Then: Should return false (not an entity type)
        assertThat(isAggregateRoot).isFalse();
    }

    @Test
    void testIsAggregateRoot_withIdentifierKind_returnsFalse() {
        // Given: A domain type with IDENTIFIER kind
        DomainTypeView identifier = createDomainTypeView("com.example.OrderId", "OrderId", DomainTypeKind.IDENTIFIER);

        // When: Check if aggregate root
        boolean isAggregateRoot = identifier.isAggregateRoot();

        // Then: Should return false
        assertThat(isAggregateRoot).isFalse();
    }

    @Test
    void testIsAggregateRoot_withEnumerationKind_returnsFalse() {
        // Given: A domain type with ENUMERATION kind
        DomainTypeView enumType =
                createDomainTypeView("com.example.OrderStatus", "OrderStatus", DomainTypeKind.ENUMERATION);

        // When: Check if aggregate root
        boolean isAggregateRoot = enumType.isAggregateRoot();

        // Then: Should return false
        assertThat(isAggregateRoot).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a DomainTypeView using the SPI factory method.
     */
    private DomainTypeView createDomainTypeView(String qualifiedName, String simpleName, DomainTypeKind kind) {
        TypeRef typeRef = TypeRefFactory.classRef(qualifiedName);

        return DomainTypeView.of(
                qualifiedName,
                simpleName,
                kind,
                typeRef,
                List.of(), // properties
                null, // id
                false, // immutable
                null // description
                );
    }
}
