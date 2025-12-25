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
import io.hexaglue.core.internal.ir.domain.normalize.AnnotationIndex;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RelationshipSignals}.
 *
 * <p>These tests verify the detection of jMolecules annotations and heuristics for
 * relationship classification.</p>
 *
 * @since 0.4.0
 */
class RelationshipSignalsTest {

    private final RelationshipSignals signals = new RelationshipSignals();

    // ─────────────────────────────────────────────────────────────────────────
    // @Association Detection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void hasAssociationMarker_whenPresent_returnsTrue() {
        // Given: Annotation @Association present
        AnnotationIndex annotations =
                AnnotationIndex.of(List.of(createAnnotation("org.jmolecules.ddd.annotation.Association")));

        // When/Then
        assertThat(signals.hasAssociationMarker(annotations)).isTrue();
    }

    @Test
    void hasAssociationMarker_whenAbsent_returnsFalse() {
        // Given: Empty annotations
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When/Then
        assertThat(signals.hasAssociationMarker(annotations)).isFalse();
    }

    @Test
    void hasAssociationMarker_withOtherAnnotations_returnsFalse() {
        // Given: Other annotations but not @Association
        AnnotationIndex annotations =
                AnnotationIndex.of(List.of(createAnnotation("org.jmolecules.ddd.annotation.AggregateRoot")));

        // When/Then
        assertThat(signals.hasAssociationMarker(annotations)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Target Type Classification Annotations
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void targetIsAggregateRoot_detectsJMoleculesAnnotation() {
        // Given: @AggregateRoot annotation
        AnnotationIndex annotations =
                AnnotationIndex.of(List.of(createAnnotation("org.jmolecules.ddd.annotation.AggregateRoot")));

        // When/Then
        assertThat(signals.targetIsAggregateRoot(annotations)).isTrue();
    }

    @Test
    void targetIsAggregateRoot_whenAbsent_returnsFalse() {
        // Given: Empty annotations
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When/Then
        assertThat(signals.targetIsAggregateRoot(annotations)).isFalse();
    }

    @Test
    void targetIsInternalEntity_detectsJMoleculesEntity() {
        // Given: @Entity annotation
        AnnotationIndex annotations =
                AnnotationIndex.of(List.of(createAnnotation("org.jmolecules.ddd.annotation.Entity")));

        // When/Then
        assertThat(signals.targetIsInternalEntity(annotations)).isTrue();
    }

    @Test
    void targetIsInternalEntity_whenAbsent_returnsFalse() {
        // Given: Empty annotations
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When/Then
        assertThat(signals.targetIsInternalEntity(annotations)).isFalse();
    }

    @Test
    void targetIsValueObject_detectsJMoleculesValueObject() {
        // Given: @ValueObject annotation
        AnnotationIndex annotations =
                AnnotationIndex.of(List.of(createAnnotation("org.jmolecules.ddd.annotation.ValueObject")));

        // When/Then
        assertThat(signals.targetIsValueObject(annotations)).isTrue();
    }

    @Test
    void targetIsValueObject_whenAbsent_returnsFalse() {
        // Given: Empty annotations
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When/Then
        assertThat(signals.targetIsValueObject(annotations)).isFalse();
    }

    @Test
    void targetIsIdentity_detectsJMoleculesIdentity() {
        // Given: @Identity annotation
        AnnotationIndex annotations =
                AnnotationIndex.of(List.of(createAnnotation("org.jmolecules.ddd.annotation.Identity")));

        // When/Then
        assertThat(signals.targetIsIdentity(annotations)).isTrue();
    }

    @Test
    void targetIsIdentity_whenAbsent_returnsFalse() {
        // Given: Empty annotations
        AnnotationIndex annotations = AnnotationIndex.of(List.of());

        // When/Then
        assertThat(signals.targetIsIdentity(annotations)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Type Name Heuristics
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void typeNameSuggestsId_detectsIdSuffix() {
        // Given/When/Then: Various ID type names
        assertThat(signals.typeNameSuggestsId("CustomerId")).isTrue();
        assertThat(signals.typeNameSuggestsId("OrderID")).isTrue();
        assertThat(signals.typeNameSuggestsId("ProductId")).isTrue();
    }

    @Test
    void typeNameSuggestsId_detectsQualifiedIdNames() {
        // Given/When/Then: Qualified ID type names
        assertThat(signals.typeNameSuggestsId("com.example.domain.CustomerId")).isTrue();
        assertThat(signals.typeNameSuggestsId("com.example.domain.OrderID")).isTrue();
    }

    @Test
    void typeNameSuggestsId_rejectsNonIdNames() {
        // Given/When/Then: Non-ID type names
        assertThat(signals.typeNameSuggestsId("Customer")).isFalse();
        assertThat(signals.typeNameSuggestsId("String")).isFalse();
        assertThat(signals.typeNameSuggestsId("java.lang.String")).isFalse();
        assertThat(signals.typeNameSuggestsId("com.example.domain.Customer")).isFalse();
    }

    @Test
    void typeNameSuggestsId_handlesEdgeCases() {
        // Given/When/Then: Edge cases
        assertThat(signals.typeNameSuggestsId("Id")).isTrue(); // Just "Id"
        assertThat(signals.typeNameSuggestsId("ID")).isTrue(); // Just "ID"
        assertThat(signals.typeNameSuggestsId("")).isFalse(); // Empty string
        assertThat(signals.typeNameSuggestsId("   ")).isFalse(); // Whitespace only
    }

    @Test
    void extractEntityNameFromIdType_removesIdSuffix() {
        // Given/When/Then: Extract entity names
        assertThat(signals.extractEntityNameFromIdType("CustomerId")).isEqualTo("Customer");
        assertThat(signals.extractEntityNameFromIdType("OrderID")).isEqualTo("Order");
        assertThat(signals.extractEntityNameFromIdType("ProductId")).isEqualTo("Product");
    }

    @Test
    void extractEntityNameFromIdType_removesIdSuffixFromQualifiedNames() {
        // Given/When/Then: Extract from qualified names
        assertThat(signals.extractEntityNameFromIdType("com.example.domain.CustomerId"))
                .isEqualTo("com.example.domain.Customer");
        assertThat(signals.extractEntityNameFromIdType("com.example.domain.OrderID"))
                .isEqualTo("com.example.domain.Order");
    }

    @Test
    void extractEntityNameFromIdType_unchangedWhenNoSuffix() {
        // Given/When/Then: No suffix → unchanged
        assertThat(signals.extractEntityNameFromIdType("Customer")).isEqualTo("Customer");
        assertThat(signals.extractEntityNameFromIdType("Order")).isEqualTo("Order");
        assertThat(signals.extractEntityNameFromIdType("java.lang.String")).isEqualTo("java.lang.String");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Collection Type Detection
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void isCollectionType_detectsStandardCollections() {
        // Given/When/Then: Standard Java collections
        assertThat(signals.isCollectionType("java.util.List")).isTrue();
        assertThat(signals.isCollectionType("java.util.Set")).isTrue();
        assertThat(signals.isCollectionType("java.util.Collection")).isTrue();
        assertThat(signals.isCollectionType("java.util.ArrayList")).isTrue();
        assertThat(signals.isCollectionType("java.util.HashSet")).isTrue();
        assertThat(signals.isCollectionType("java.util.LinkedHashSet")).isTrue();
        assertThat(signals.isCollectionType("java.util.TreeSet")).isTrue();
    }

    @Test
    void isCollectionType_rejectsNonCollections() {
        // Given/When/Then: Non-collection types
        assertThat(signals.isCollectionType("java.lang.String")).isFalse();
        assertThat(signals.isCollectionType("java.lang.Integer")).isFalse();
        assertThat(signals.isCollectionType("com.example.domain.Customer")).isFalse();
    }

    @Test
    void isCollectionType_rejectsMap() {
        // Given/When/Then: Map is not a collection (for now)
        assertThat(signals.isCollectionType("java.util.Map")).isFalse();
        assertThat(signals.isCollectionType("java.util.HashMap")).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

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
}
