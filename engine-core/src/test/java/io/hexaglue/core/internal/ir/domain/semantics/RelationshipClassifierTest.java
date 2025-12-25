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
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.RelationshipKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import io.hexaglue.spi.types.ClassRef;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RelationshipClassifier}.
 *
 * <p>These tests verify the priority-based classification of relationships and the detection
 * of DDD patterns (inter-aggregate, intra-aggregate, embedded value objects).</p>
 *
 * @since 0.4.0
 */
class RelationshipClassifierTest {

    private final RelationshipSignals signals = new RelationshipSignals();
    private final RelationshipClassifier classifier = new RelationshipClassifier(signals, null);

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 1: @Association Annotation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_withExplicitAssociation_detectsInterAggregate() {
        // Given: Property with @Association annotation
        DomainProperty property = DomainProperty.builder()
                .name("customerId")
                .type(classRef("com.example.domain.CustomerId"))
                .annotations(List.of(annotationModel("org.jmolecules.ddd.annotation.Association")))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Detected as inter-aggregate relationship via annotation
        assertThat(evidence.hasRelationship()).isTrue();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.JMOLECULES_ANNOTATION);

        RelationshipMetadata rel = evidence.relationship();
        assertThat(rel.isInterAggregate()).isTrue();
        assertThat(rel.targetQualifiedName()).isEqualTo("com.example.domain.Customer");
        assertThat(evidence.detail()).hasValue("@Association → inter-aggregate to com.example.domain.Customer");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 2: Target Type Classification
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_targetIsAggregateRoot_detectsInterAggregate() {
        // Given: Target type is an aggregate root
        DomainType customerType = DomainType.builder()
                .qualifiedName("com.example.domain.Customer")
                .simpleName("Customer")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Customer"))
                .build();

        DomainModel domainModel = DomainModel.builder().addType(customerType).build();

        DomainProperty property = DomainProperty.builder()
                .name("customer")
                .type(classRef("com.example.domain.Customer"))
                .build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Detected as DDD violation (direct reference to aggregate root)
        assertThat(evidence.hasRelationship()).isTrue();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.HEURISTIC);

        RelationshipMetadata rel = evidence.relationship();
        assertThat(rel.isInterAggregate()).isTrue();
        assertThat(evidence.detail())
                .hasValue("Target is aggregate root (DDD violation: should use ID-only) → com.example.domain.Customer");
    }

    @Test
    void classify_targetIsValueObject_detectsEmbedded() {
        // Given: Target type is a value object
        DomainType addressType = DomainType.builder()
                .qualifiedName("com.example.domain.Address")
                .simpleName("Address")
                .kind(DomainTypeKind.VALUE_OBJECT)
                .type(classRef("com.example.domain.Address"))
                .build();

        DomainModel domainModel = DomainModel.builder().addType(addressType).build();

        DomainProperty property = DomainProperty.builder()
                .name("address")
                .type(classRef("com.example.domain.Address"))
                .build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Detected as embedded value object
        assertThat(evidence.hasRelationship()).isTrue();

        RelationshipMetadata rel = evidence.relationship();
        assertThat(rel.isInterAggregate()).isFalse(); // Value objects are embedded
        assertThat(rel.kind()).isEqualTo(RelationshipKind.ONE_TO_ONE);
        assertThat(evidence.detail()).hasValue("@ValueObject target → embedded com.example.domain.Address");
    }

    @Test
    void classify_targetIsInternalEntity_detectsIntraAggregate() {
        // Given: Target type is an internal entity
        DomainType orderItemType = DomainType.builder()
                .qualifiedName("com.example.domain.OrderItem")
                .simpleName("OrderItem")
                .kind(DomainTypeKind.ENTITY)
                .type(classRef("com.example.domain.OrderItem"))
                .annotations(List.of(annotationModel("org.jmolecules.ddd.annotation.Entity")))
                .build();

        DomainModel domainModel = DomainModel.builder().addType(orderItemType).build();

        DomainProperty property = DomainProperty.builder()
                .name("item")
                .type(classRef("com.example.domain.OrderItem"))
                .build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Detected as intra-aggregate relationship
        assertThat(evidence.hasRelationship()).isTrue();

        RelationshipMetadata rel = evidence.relationship();
        assertThat(rel.isInterAggregate()).isFalse(); // Internal entity
        assertThat(evidence.detail()).hasValue("@Entity target → intra-aggregate com.example.domain.OrderItem");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 3: ID Type Heuristic
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_idTypeHeuristic_detectsInterAggregate() {
        // Given: Property type ends with "Id"
        DomainProperty property = DomainProperty.builder()
                .name("customerId")
                .type(classRef("com.example.domain.CustomerId"))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Detected as ID-based inter-aggregate reference
        assertThat(evidence.hasRelationship()).isTrue();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.HEURISTIC);

        RelationshipMetadata rel = evidence.relationship();
        assertThat(rel.isInterAggregate()).isTrue();
        assertThat(rel.targetQualifiedName()).isEqualTo("com.example.domain.Customer");
        assertThat(evidence.detail())
                .hasValue(
                        "ID type name heuristic (com.example.domain.CustomerId) → inter-aggregate to com.example.domain.Customer");
    }

    @Test
    void classify_idTypeHeuristic_withQualifiedName_extractsCorrectEntity() {
        // Given: Fully qualified ID type
        DomainProperty property = DomainProperty.builder()
                .name("orderId")
                .type(classRef("com.example.ecommerce.domain.OrderID"))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Correctly extracts entity name from qualified ID type
        assertThat(evidence.hasRelationship()).isTrue();

        RelationshipMetadata rel = evidence.relationship();
        assertThat(rel.targetQualifiedName()).isEqualTo("com.example.ecommerce.domain.Order");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Priority 4: No Relationship Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_simpleType_returnsNoRelationship() {
        // Given: Simple property (String)
        DomainProperty property = DomainProperty.builder()
                .name("email")
                .type(classRef("java.lang.String"))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Not a relationship
        assertThat(evidence.hasRelationship()).isFalse();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.NOT_A_RELATIONSHIP);
        assertThat(evidence.detail()).hasValue("Simple property type: java.lang.String");
    }

    @Test
    void classify_primitiveType_returnsNoRelationship() {
        // Given: Primitive property
        DomainProperty property = DomainProperty.builder()
                .name("quantity")
                .type(ClassRef.of("int"))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Not a relationship
        assertThat(evidence.hasRelationship()).isFalse();
        assertThat(evidence.detail()).hasValue("Simple property type: int");
    }

    @Test
    void classify_javaUtilType_returnsNoRelationship() {
        // Given: java.util type (not a collection)
        DomainProperty property = DomainProperty.builder()
                .name("createdAt")
                .type(classRef("java.time.Instant"))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Not a relationship
        assertThat(evidence.hasRelationship()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_multipleSignals_prioritizesAnnotation() {
        // Given: Property with @Association AND ID type name
        DomainProperty property = DomainProperty.builder()
                .name("customerId")
                .type(classRef("com.example.domain.CustomerId"))
                .annotations(List.of(annotationModel("org.jmolecules.ddd.annotation.Association")))
                .build();

        DomainModel domainModel = DomainModel.builder().build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: Annotation takes precedence (Priority 1)
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.JMOLECULES_ANNOTATION);
        assertThat(evidence.detail()).hasValue("@Association → inter-aggregate to com.example.domain.Customer");
    }

    @Test
    void classify_idTypeWithTargetPresent_prioritizesTargetType() {
        // Given: ID type + target type exists in model
        DomainType customerType = DomainType.builder()
                .qualifiedName("com.example.domain.Customer")
                .simpleName("Customer")
                .kind(DomainTypeKind.AGGREGATE_ROOT)
                .type(classRef("com.example.domain.Customer"))
                .build();

        DomainModel domainModel = DomainModel.builder().addType(customerType).build();

        DomainProperty property = DomainProperty.builder()
                .name("customerId")
                .type(classRef("com.example.domain.CustomerId"))
                .build();

        // When
        RelationshipEvidence evidence = classifier.classify(property, domainModel);

        // Then: ID heuristic is used (target type is not the property type)
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.HEURISTIC);
        assertThat(evidence.detail())
                .hasValue(
                        "ID type name heuristic (com.example.domain.CustomerId) → inter-aggregate to com.example.domain.Customer");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private AnnotationModel annotationModel(String qualifiedName) {
        // Create a minimal AnnotationModel for testing
        // We use a mock AnnotationMirror since we only need the qualified name
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

    private static ClassRef classRef(String qualifiedName) {
        return ClassRef.of(qualifiedName);
    }
}
