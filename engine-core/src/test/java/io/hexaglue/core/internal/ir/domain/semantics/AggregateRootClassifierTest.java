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
import io.hexaglue.core.internal.ir.SourceRef;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.domain.normalize.AnnotationIndex;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.types.TypeRefFactory;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.ports.PortDirection;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AggregateRootClassifier}.
 *
 * <p>These tests verify the complete classification logic for aggregate roots, including:</p>
 * <ul>
 *   <li>Explicit annotation detection (jMolecules, Spring, JPA)</li>
 *   <li>Repository port detection</li>
 *   <li>Package convention detection</li>
 *   <li>Naming convention detection</li>
 * </ul>
 */
class AggregateRootClassifierTest {

    private final AggregateRootClassifier classifier = AggregateRootClassifier.defaults();

    // ─────────────────────────────────────────────────────────────────────────
    // Pre-classified Types
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_alreadyAggregateRoot_returnsTrue() {
        // Given: A type already classified as AGGREGATE_ROOT
        DomainType type = createDomainType("com.example.Order", "Order", DomainTypeKind.AGGREGATE_ROOT);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with explicit annotation kind
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.EXPLICIT_ANNOTATION);
    }

    @Test
    void classify_valueObject_returnsFalse() {
        // Given: A value object type
        DomainType type = createDomainType("com.example.Money", "Money", DomainTypeKind.VALUE_OBJECT);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns false (only entities can be aggregate roots)
        assertThat(evidence.isAggregateRoot()).isFalse();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.NONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strong Annotation Markers
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_withJMoleculesAggregateRoot_returnsTrue() {
        // Given: An entity with @AggregateRoot annotation
        DomainType type = createDomainType("com.example.Order", "Order", DomainTypeKind.ENTITY);
        AnnotationModel annotation = createAnnotation("org.jmolecules.ddd.annotation.AggregateRoot");
        AnnotationIndex annotations = AnnotationIndex.of(List.of(annotation));

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with explicit annotation
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.EXPLICIT_ANNOTATION);
        assertThat(evidence.detail()).hasValue("Strong aggregate marker present");
    }

    @Test
    void classify_withSpringDocument_returnsTrue() {
        // Given: An entity with @Document annotation (MongoDB)
        DomainType type = createDomainType("com.example.Order", "Order", DomainTypeKind.ENTITY);
        AnnotationModel annotation = createAnnotation("org.springframework.data.mongodb.core.mapping.Document");
        AnnotationIndex annotations = AnnotationIndex.of(List.of(annotation));

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with explicit annotation
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.EXPLICIT_ANNOTATION);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weak Marker + Repository Port
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_jpaEntityWithRepositoryPort_returnsTrue() {
        // Given: An entity with @Entity and a repository port
        DomainType type = createDomainType("com.example.Order", "Order", DomainTypeKind.ENTITY);
        AnnotationModel annotation = createAnnotation("jakarta.persistence.Entity");
        AnnotationIndex annotations = AnnotationIndex.of(List.of(annotation));
        Port repositoryPort = createRepositoryPort("OrderRepository", type);

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of(repositoryPort));

        // Then: Returns true with repository port evidence
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.REPOSITORY_PORT);
        assertThat(evidence.detail()).hasValue("JPA @Entity + repository port");
    }

    @Test
    void classify_jpaEntityWithoutRepositoryPort_returnsFalse() {
        // Given: An entity with @Entity but NO repository port
        DomainType type = createDomainType("com.example.OrderItem", "OrderItem", DomainTypeKind.ENTITY);
        AnnotationModel annotation = createAnnotation("jakarta.persistence.Entity");
        AnnotationIndex annotations = AnnotationIndex.of(List.of(annotation));

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns false (weak signal not confirmed)
        assertThat(evidence.isAggregateRoot()).isFalse();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.NONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Package Convention
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_inAggregatePackage_returnsTrue() {
        // Given: An entity in "aggregate" package
        DomainType type = createDomainType("com.example.order.aggregate.Order", "Order", DomainTypeKind.ENTITY);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with package convention
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.PACKAGE_CONVENTION);
        assertThat(evidence.detail()).hasValue("Package matches *.aggregate(s).*");
    }

    @Test
    void classify_inAggregatesPackage_returnsTrue() {
        // Given: An entity in "aggregates" package
        DomainType type = createDomainType("com.example.aggregates.Customer", "Customer", DomainTypeKind.ENTITY);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with package convention
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.PACKAGE_CONVENTION);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Naming Convention
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_nameEndsWithAggregate_returnsTrue() {
        // Given: An entity with name ending with "Aggregate"
        DomainType type = createDomainType("com.example.OrderAggregate", "OrderAggregate", DomainTypeKind.ENTITY);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with naming convention
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.NAMING_CONVENTION);
        assertThat(evidence.detail()).hasValue("Name ends with Aggregate/AggregateRoot");
    }

    @Test
    void classify_nameEndsWithAggregateRoot_returnsTrue() {
        // Given: An entity with name ending with "AggregateRoot"
        DomainType type =
                createDomainType("com.example.CustomerAggregateRoot", "CustomerAggregateRoot", DomainTypeKind.ENTITY);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns true with naming convention
        assertThat(evidence.isAggregateRoot()).isTrue();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.NAMING_CONVENTION);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // No Signals (Internal Entity)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void classify_plainEntity_returnsFalse() {
        // Given: A plain entity without any aggregate root signals
        DomainType type = createDomainType("com.example.domain.OrderItem", "OrderItem", DomainTypeKind.ENTITY);
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When: Classify
        AggregateRootEvidence evidence = classifier.classify(type, annotations, List.of());

        // Then: Returns false (no signals)
        assertThat(evidence.isAggregateRoot()).isFalse();
        assertThat(evidence.kind()).isEqualTo(AggregateRootEvidence.Kind.NONE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private DomainType createDomainType(String qualifiedName, String simpleName, DomainTypeKind kind) {
        TypeRef typeRef = TypeRefFactory.classRef(qualifiedName);
        SourceRef sourceRef = SourceRef.builder(SourceRef.Kind.TYPE, qualifiedName)
                .origin("test")
                .build();

        return DomainType.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .kind(kind)
                .type(typeRef)
                .immutable(false)
                .sourceRef(sourceRef)
                .build();
    }

    private AnnotationModel createAnnotation(String qualifiedName) {
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

    private Port createRepositoryPort(String portName, DomainType domainType) {
        // Create a repository port with methods that use the domain type
        // This allows RepositoryPortMatcher to detect the relationship

        // Create findById(Long): Optional<Order> method
        io.hexaglue.core.internal.ir.ports.PortMethod findByIdMethod =
                io.hexaglue.core.internal.ir.ports.PortMethod.builder()
                        .name("findById")
                        .returnType(TypeRefFactory.parameterized(
                                TypeRefFactory.classRef("java.util.Optional"), domainType.type()))
                        .addParameter(io.hexaglue.core.internal.ir.ports.PortParameter.builder()
                                .name("id")
                                .type(TypeRefFactory.LONG)
                                .build())
                        .build();

        // Create save(Order): Order method
        io.hexaglue.core.internal.ir.ports.PortMethod saveMethod =
                io.hexaglue.core.internal.ir.ports.PortMethod.builder()
                        .name("save")
                        .returnType(domainType.type())
                        .addParameter(io.hexaglue.core.internal.ir.ports.PortParameter.builder()
                                .name("entity")
                                .type(domainType.type())
                                .build())
                        .build();

        return Port.builder()
                .qualifiedName("com.example.ports." + portName)
                .simpleName(portName)
                .direction(PortDirection.DRIVEN)
                .type(TypeRefFactory.classRef("com.example.ports." + portName))
                .addMethod(findByIdMethod)
                .addMethod(saveMethod)
                .build();
    }
}
