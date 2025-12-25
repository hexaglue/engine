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
package io.hexaglue.core.internal.spi;

import static com.google.common.truth.Truth.assertThat;

import io.hexaglue.core.internal.ir.IrSnapshot;
import io.hexaglue.core.internal.ir.app.ApplicationModel;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.ir.domain.DomainModelView;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.ir.domain.RelationshipKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import io.hexaglue.spi.types.ClassRef;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for relationship metadata exposure through the SPI.
 *
 * <p>These tests verify that relationship metadata stored in the internal IR is properly
 * exposed to plugins through the SPI view interfaces.</p>
 *
 * @since 0.4.0
 */
class RelationshipSpiIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // DomainPropertyView Relationship Exposure
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void domainPropertyView_exposesRelationshipMetadata() {
        // Given: Internal DomainProperty with relationship
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.MANY_TO_ONE, "com.example.domain.Customer", true);

        DomainProperty internalProperty = DomainProperty.builder()
                .name("customerId")
                .type(ClassRef.of("com.example.domain.CustomerId"))
                .relationshipMetadata(metadata)
                .build();

        // Build a complete IR snapshot
        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(ClassRef.of("com.example.domain.Order"))
                .addProperty(internalProperty)
                .build();

        DomainModel domainModel = DomainModel.builder().addType(orderType).build();

        IrSnapshot snapshot = IrSnapshot.builder()
                .domainModel(domainModel)
                .portModel(PortModel.builder().build())
                .applicationModel(ApplicationModel.builder().build())
                .build();

        // When: Convert to SPI view
        IrView irView = IrViewAdapter.from(snapshot);
        DomainModelView domainView = irView.domain();

        // Then: Navigate to property and verify relationship is accessible
        DomainTypeView orderView =
                domainView.findType("com.example.domain.Order").orElseThrow();

        DomainPropertyView customerIdView = orderView.properties().stream()
                .filter(p -> p.name().equals("customerId"))
                .findFirst()
                .orElseThrow();

        assertThat(customerIdView.relationship()).isPresent();

        RelationshipMetadata exposedRel = customerIdView.relationship().orElseThrow();
        assertThat(exposedRel.kind()).isEqualTo(RelationshipKind.MANY_TO_ONE);
        assertThat(exposedRel.isInterAggregate()).isTrue();
        assertThat(exposedRel.targetQualifiedName()).isEqualTo("com.example.domain.Customer");
    }

    @Test
    void domainPropertyView_withoutRelationship_returnsEmpty() {
        // Given: Property without relationship
        DomainProperty simpleProperty = DomainProperty.builder()
                .name("email")
                .type(ClassRef.of("java.lang.String"))
                .build();

        DomainType customerType = DomainType.builder()
                .qualifiedName("com.example.domain.Customer")
                .simpleName("Customer")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(ClassRef.of("com.example.domain.Customer"))
                .addProperty(simpleProperty)
                .build();

        DomainModel domainModel = DomainModel.builder().addType(customerType).build();

        IrSnapshot snapshot = IrSnapshot.builder()
                .domainModel(domainModel)
                .portModel(PortModel.builder().build())
                .applicationModel(ApplicationModel.builder().build())
                .build();

        // When: Convert to SPI view
        IrView irView = IrViewAdapter.from(snapshot);
        DomainModelView domainView = irView.domain();

        // Then: Property has no relationship
        DomainTypeView customerView =
                domainView.findType("com.example.domain.Customer").orElseThrow();

        DomainPropertyView emailView = customerView.properties().stream()
                .filter(p -> p.name().equals("email"))
                .findFirst()
                .orElseThrow();

        assertThat(emailView.relationship()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multiple Relationships Exposure
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void domainModelView_exposesMultipleRelationships() {
        // Given: Domain type with multiple relationships
        RelationshipMetadata customerRel =
                RelationshipMetadata.of(RelationshipKind.MANY_TO_ONE, "com.example.domain.Customer", true);

        RelationshipMetadata productRel =
                RelationshipMetadata.of(RelationshipKind.MANY_TO_ONE, "com.example.domain.Product", true);

        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(ClassRef.of("com.example.domain.Order"))
                .addProperty(DomainProperty.builder()
                        .name("customerId")
                        .type(ClassRef.of("com.example.domain.CustomerId"))
                        .relationshipMetadata(customerRel)
                        .build())
                .addProperty(DomainProperty.builder()
                        .name("productId")
                        .type(ClassRef.of("com.example.domain.ProductId"))
                        .relationshipMetadata(productRel)
                        .build())
                .addProperty(DomainProperty.builder()
                        .name("status")
                        .type(ClassRef.of("java.lang.String"))
                        .build())
                .build();

        DomainModel domainModel = DomainModel.builder().addType(orderType).build();

        IrSnapshot snapshot = IrSnapshot.builder()
                .domainModel(domainModel)
                .portModel(PortModel.builder().build())
                .applicationModel(ApplicationModel.builder().build())
                .build();

        // When: Convert to SPI view
        IrView irView = IrViewAdapter.from(snapshot);
        DomainTypeView orderView =
                irView.domain().findType("com.example.domain.Order").orElseThrow();

        // Then: Both relationships are exposed
        long relationshipCount = orderView.properties().stream()
                .filter(p -> p.relationship().isPresent())
                .count();

        assertThat(relationshipCount).isEqualTo(2);

        // Verify customerId relationship
        DomainPropertyView customerIdView = orderView.properties().stream()
                .filter(p -> p.name().equals("customerId"))
                .findFirst()
                .orElseThrow();

        assertThat(customerIdView.relationship()).isPresent();
        assertThat(customerIdView.relationship().orElseThrow().targetQualifiedName())
                .isEqualTo("com.example.domain.Customer");

        // Verify productId relationship
        DomainPropertyView productIdView = orderView.properties().stream()
                .filter(p -> p.name().equals("productId"))
                .findFirst()
                .orElseThrow();

        assertThat(productIdView.relationship()).isPresent();
        assertThat(productIdView.relationship().orElseThrow().targetQualifiedName())
                .isEqualTo("com.example.domain.Product");

        // Verify status has no relationship
        DomainPropertyView statusView = orderView.properties().stream()
                .filter(p -> p.name().equals("status"))
                .findFirst()
                .orElseThrow();

        assertThat(statusView.relationship()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RelationshipMetadata Interface Verification
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void relationshipMetadata_exposesAllRequiredFields() {
        // Given: Relationship with all fields
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.ONE_TO_MANY, "com.example.domain.OrderItem", false);

        DomainProperty property = DomainProperty.builder()
                .name("items")
                .type(ClassRef.of("java.util.List"))
                .relationshipMetadata(metadata)
                .build();

        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(ClassRef.of("com.example.domain.Order"))
                .addProperty(property)
                .build();

        DomainModel domainModel = DomainModel.builder().addType(orderType).build();

        IrSnapshot snapshot = IrSnapshot.builder()
                .domainModel(domainModel)
                .portModel(PortModel.builder().build())
                .applicationModel(ApplicationModel.builder().build())
                .build();

        // When: Access via SPI
        IrView irView = IrViewAdapter.from(snapshot);
        DomainPropertyView itemsView =
                irView.domain().findType("com.example.domain.Order").orElseThrow().properties().stream()
                        .filter(p -> p.name().equals("items"))
                        .findFirst()
                        .orElseThrow();

        RelationshipMetadata exposedMetadata = itemsView.relationship().orElseThrow();

        // Then: All fields are accessible
        assertThat(exposedMetadata.kind()).isEqualTo(RelationshipKind.ONE_TO_MANY);
        assertThat(exposedMetadata.targetQualifiedName()).isEqualTo("com.example.domain.OrderItem");
        assertThat(exposedMetadata.isInterAggregate()).isFalse();
        assertThat(exposedMetadata.isBidirectional()).isFalse(); // Default
        assertThat(exposedMetadata.mappedBy()).isEmpty(); // Default
    }
}
