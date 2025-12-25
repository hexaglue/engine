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

import static com.google.common.truth.Truth.assertThat;

import io.hexaglue.core.frontend.AnnotationModel;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.RelationshipKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import io.hexaglue.spi.types.ClassRef;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link DomainSemanticEnricher} relationship detection.
 *
 * <p>These tests verify the end-to-end enrichment process, ensuring that relationships
 * are correctly detected and metadata is populated in the domain model.</p>
 *
 * @since 0.4.0
 */
class RelationshipEnrichmentIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Basic Enrichment Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void enrich_detectsRelationshipsAndPopulatesMetadata() {
        // Given: Domain model with Order → CustomerId relationship
        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Order"))
                .addProperty(DomainProperty.builder()
                        .name("customerId")
                        .type(classRef("com.example.domain.CustomerId"))
                        .build())
                .build();

        DomainModel initialModel = DomainModel.builder().addType(orderType).build();

        PortModel portModel = PortModel.builder().build();

        DomainSemanticEnricher enricher = DomainSemanticEnricher.defaults();

        // When
        DomainModel enrichedModel = enricher.enrich(initialModel, portModel);

        // Then
        DomainType enrichedOrder =
                enrichedModel.findType("com.example.domain.Order").orElseThrow();
        DomainProperty customerIdProp = enrichedOrder.properties().stream()
                .filter(p -> p.name().equals("customerId"))
                .findFirst()
                .orElseThrow();

        // Verify relationship metadata was populated
        assertThat(customerIdProp.relationship()).isPresent();

        RelationshipMetadata rel = customerIdProp.relationship().orElseThrow();
        assertThat(rel.isInterAggregate()).isTrue();
        assertThat(rel.targetQualifiedName()).isEqualTo("com.example.domain.Customer");
    }

    @Test
    void enrich_handlesMultipleRelationshipsPerType() {
        // Given: Order with multiple relationships
        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Order"))
                .addProperty(DomainProperty.builder()
                        .name("customerId")
                        .type(classRef("com.example.domain.CustomerId"))
                        .build())
                .addProperty(DomainProperty.builder()
                        .name("productId")
                        .type(classRef("com.example.domain.ProductId"))
                        .build())
                .addProperty(DomainProperty.builder()
                        .name("email") // Not a relationship
                        .type(classRef("java.lang.String"))
                        .build())
                .build();

        DomainModel initialModel = DomainModel.builder().addType(orderType).build();

        PortModel portModel = PortModel.builder().build();

        DomainSemanticEnricher enricher = DomainSemanticEnricher.defaults();

        // When
        DomainModel enrichedModel = enricher.enrich(initialModel, portModel);

        // Then
        DomainType enrichedOrder =
                enrichedModel.findType("com.example.domain.Order").orElseThrow();

        // customerId → relationship
        assertThat(findProperty(enrichedOrder, "customerId").relationship()).isPresent();

        // productId → relationship
        assertThat(findProperty(enrichedOrder, "productId").relationship()).isPresent();

        // email → NO relationship
        assertThat(findProperty(enrichedOrder, "email").relationship()).isEmpty();
    }

    @Test
    void enrich_detectsValueObjectAsEmbedded() {
        // Given: Customer with Address value object
        DomainType addressType = DomainType.builder()
                .qualifiedName("com.example.domain.Address")
                .simpleName("Address")
                .kind(DomainTypeKind.VALUE_OBJECT)
                .type(classRef("com.example.domain.Address"))
                .build();

        DomainType customerType = DomainType.builder()
                .qualifiedName("com.example.domain.Customer")
                .simpleName("Customer")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Customer"))
                .addProperty(DomainProperty.builder()
                        .name("address")
                        .type(classRef("com.example.domain.Address"))
                        .build())
                .build();

        DomainModel initialModel =
                DomainModel.builder().addType(addressType).addType(customerType).build();

        PortModel portModel = PortModel.builder().build();

        DomainSemanticEnricher enricher = DomainSemanticEnricher.defaults();

        // When
        DomainModel enrichedModel = enricher.enrich(initialModel, portModel);

        // Then
        DomainType enrichedCustomer =
                enrichedModel.findType("com.example.domain.Customer").orElseThrow();
        DomainProperty addressProp = findProperty(enrichedCustomer, "address");

        RelationshipMetadata rel = addressProp.relationship().orElseThrow();
        assertThat(rel.isInterAggregate()).isFalse(); // Embedded
        assertThat(rel.kind()).isEqualTo(RelationshipKind.ONE_TO_ONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Complete Scenario Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void scenario_ecommerceOrder_detectsAllRelationships() {
        // Given: E-commerce domain model
        // Order (aggregate root) with:
        // - customerId: CustomerId (@Association)
        // - shippingAddress: Address (value object)

        DomainType customerType = aggregateRoot("com.example.domain.Customer");
        DomainType addressType = valueObject("com.example.domain.Address");

        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Order"))
                .addProperty(DomainProperty.builder()
                        .name("customerId")
                        .type(classRef("com.example.domain.CustomerId"))
                        .annotations(List.of(association()))
                        .build())
                .addProperty(DomainProperty.builder()
                        .name("shippingAddress")
                        .type(classRef("com.example.domain.Address"))
                        .build())
                .build();

        DomainModel model = DomainModel.builder()
                .addType(customerType)
                .addType(addressType)
                .addType(orderType)
                .build();

        DomainSemanticEnricher enricher = DomainSemanticEnricher.defaults();

        // When
        DomainModel enriched = enricher.enrich(model, PortModel.builder().build());

        // Then
        DomainType order = enriched.findType("com.example.domain.Order").orElseThrow();

        // customerId → inter-aggregate (ID-only reference)
        RelationshipMetadata customerRel =
                findProperty(order, "customerId").relationship().orElseThrow();
        assertThat(customerRel.isInterAggregate()).isTrue();
        assertThat(customerRel.targetQualifiedName()).isEqualTo("com.example.domain.Customer");

        // shippingAddress → embedded value object
        RelationshipMetadata addressRel =
                findProperty(order, "shippingAddress").relationship().orElseThrow();
        assertThat(addressRel.isInterAggregate()).isFalse();
        assertThat(addressRel.kind()).isEqualTo(RelationshipKind.ONE_TO_ONE);
    }

    @Test
    void scenario_internalEntity_detectsIntraAggregateRelationship() {
        // Given: Order with internal OrderItem entities
        DomainType orderItemType = internalEntity("com.example.domain.OrderItem");

        DomainType orderType = DomainType.builder()
                .qualifiedName("com.example.domain.Order")
                .simpleName("Order")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Order"))
                .addProperty(DomainProperty.builder()
                        .name("item")
                        .type(classRef("com.example.domain.OrderItem"))
                        .build())
                .build();

        DomainModel model =
                DomainModel.builder().addType(orderItemType).addType(orderType).build();

        DomainSemanticEnricher enricher = DomainSemanticEnricher.defaults();

        // When
        DomainModel enriched = enricher.enrich(model, PortModel.builder().build());

        // Then
        DomainType order = enriched.findType("com.example.domain.Order").orElseThrow();

        RelationshipMetadata itemRel =
                findProperty(order, "item").relationship().orElseThrow();
        assertThat(itemRel.isInterAggregate()).isFalse(); // Intra-aggregate
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private static DomainProperty findProperty(DomainType type, String name) {
        return type.properties().stream()
                .filter(p -> p.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Property not found: " + name));
    }

    private static ClassRef classRef(String qualifiedName) {
        return ClassRef.of(qualifiedName);
    }

    private static DomainType aggregateRoot(String qualifiedName) {
        String simpleName = extractSimpleName(qualifiedName);
        return DomainType.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef(qualifiedName))
                .build();
    }

    private static DomainType internalEntity(String qualifiedName) {
        String simpleName = extractSimpleName(qualifiedName);
        return DomainType.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .kind(DomainTypeKind.ENTITY)
                .type(classRef(qualifiedName))
                .annotations(List.of(annotationModel("org.jmolecules.ddd.annotation.Entity")))
                .build();
    }

    private static DomainType valueObject(String qualifiedName) {
        String simpleName = extractSimpleName(qualifiedName);
        return DomainType.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .kind(DomainTypeKind.VALUE_OBJECT)
                .type(classRef(qualifiedName))
                .build();
    }

    private static AnnotationModel association() {
        return annotationModel("org.jmolecules.ddd.annotation.Association");
    }

    private static AnnotationModel annotationModel(String qualifiedName) {
        // Create a minimal AnnotationModel for testing
        AnnotationMirror mockMirror = new AnnotationMirror() {
            @Override
            public javax.lang.model.type.DeclaredType getAnnotationType() {
                return new javax.lang.model.type.DeclaredType() {
                    @Override
                    public String toString() {
                        return qualifiedName;
                    }

                    @Override
                    public javax.lang.model.element.Element asElement() {
                        return null;
                    }

                    @Override
                    public javax.lang.model.type.TypeMirror getEnclosingType() {
                        return null;
                    }

                    @Override
                    public List<? extends javax.lang.model.type.TypeMirror> getTypeArguments() {
                        return List.of();
                    }

                    @Override
                    public javax.lang.model.type.TypeKind getKind() {
                        return javax.lang.model.type.TypeKind.DECLARED;
                    }

                    @Override
                    public <R, P> R accept(javax.lang.model.type.TypeVisitor<R, P> v, P p) {
                        return null;
                    }

                    @Override
                    public List<? extends javax.lang.model.element.AnnotationMirror> getAnnotationMirrors() {
                        return List.of();
                    }

                    @Override
                    public <A extends java.lang.annotation.Annotation> A getAnnotation(Class<A> annotationType) {
                        return null;
                    }

                    @Override
                    public <A extends java.lang.annotation.Annotation> A[] getAnnotationsByType(
                            Class<A> annotationType) {
                        return null;
                    }
                };
            }

            @Override
            public java.util.Map<
                            ? extends javax.lang.model.element.ExecutableElement,
                            ? extends javax.lang.model.element.AnnotationValue>
                    getElementValues() {
                return java.util.Map.of();
            }
        };

        return new AnnotationModel(qualifiedName, mockMirror, java.util.Map.of());
    }

    private static String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
