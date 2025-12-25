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

import io.hexaglue.spi.naming.NameRole;
import io.hexaglue.spi.naming.QualifiedName;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating naming SPI contracts.
 *
 * <p>Tests the naming system including qualified names and name roles.</p>
 */
class NamingIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // NameRole Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNameRoleEnum() {
        // When: Access NameRole enum values
        assertThat(NameRole.values()).hasLength(8);
        assertThat(NameRole.PACKAGE).isNotNull();
        assertThat(NameRole.TYPE).isNotNull();
        assertThat(NameRole.FIELD).isNotNull();
        assertThat(NameRole.METHOD).isNotNull();
        assertThat(NameRole.PARAMETER).isNotNull();
        assertThat(NameRole.CONSTANT).isNotNull();
        assertThat(NameRole.RESOURCE_PATH).isNotNull();
        assertThat(NameRole.DOC_PATH).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QualifiedName Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testQualifiedNameCreation() {
        // When: Create qualified name
        QualifiedName qn = QualifiedName.of("com.example.Customer");

        // Then: Should have correct value
        assertThat(qn).isNotNull();
        assertThat(qn.value()).isEqualTo("com.example.Customer");
        assertThat(qn.toString()).isEqualTo("com.example.Customer");
    }

    @Test
    void testQualifiedNameTrimming() {
        // When: Create qualified name with whitespace
        QualifiedName qn = QualifiedName.of("  com.example.Customer  ");

        // Then: Should trim value
        assertThat(qn.value()).isEqualTo("com.example.Customer");
    }

    @Test
    void testQualifiedNamePackageName() {
        // When: Get package name from qualified name
        QualifiedName qn = QualifiedName.of("com.example.Customer");

        // Then: Should extract package
        assertThat(qn.packageName().isPresent()).isTrue();
        assertThat(qn.packageName().get()).isEqualTo("com.example");
    }

    @Test
    void testQualifiedNamePackageNameSimple() {
        // When: Get package name from simple name
        QualifiedName qn = QualifiedName.of("Customer");

        // Then: Should have no package
        assertThat(qn.packageName().isPresent()).isFalse();
    }

    @Test
    void testQualifiedNameSimpleName() {
        // When: Get simple name
        QualifiedName qn = QualifiedName.of("com.example.Customer");

        // Then: Should extract simple name
        assertThat(qn.simpleName()).isEqualTo("Customer");
    }

    @Test
    void testQualifiedNameSimpleNameFromSimple() {
        // When: Get simple name from simple name
        QualifiedName qn = QualifiedName.of("Customer");

        // Then: Should return same value
        assertThat(qn.simpleName()).isEqualTo("Customer");
    }

    @Test
    void testQualifiedNameEnclosing() {
        // When: Get enclosing qualified name
        QualifiedName qn = QualifiedName.of("com.example.Outer.Inner");

        // Then: Should extract enclosing
        assertThat(qn.enclosing().isPresent()).isTrue();
        assertThat(qn.enclosing().get().value()).isEqualTo("com.example.Outer");
    }

    @Test
    void testQualifiedNameEnclosingSimple() {
        // When: Get enclosing from simple name
        QualifiedName qn = QualifiedName.of("Customer");

        // Then: Should have no enclosing
        assertThat(qn.enclosing().isPresent()).isFalse();
    }

    @Test
    void testQualifiedNameEnclosingTwoLevels() {
        // When: Get enclosing from two-level name
        QualifiedName qn = QualifiedName.of("example.Customer");

        // Then: Should have enclosing
        assertThat(qn.enclosing().isPresent()).isTrue();
        assertThat(qn.enclosing().get().value()).isEqualTo("example");
    }

    @Test
    void testQualifiedNameEquality() {
        // Given: Two qualified names with same value
        QualifiedName qn1 = QualifiedName.of("com.example.Customer");
        QualifiedName qn2 = QualifiedName.of("com.example.Customer");

        // Then: Should be equal
        assertThat(qn1).isEqualTo(qn2);
        assertThat(qn1.hashCode()).isEqualTo(qn2.hashCode());
    }

    @Test
    void testQualifiedNameComparison() {
        // Given: Three qualified names
        QualifiedName qn1 = QualifiedName.of("a.B");
        QualifiedName qn2 = QualifiedName.of("a.C");
        QualifiedName qn3 = QualifiedName.of("b.A");

        // Then: Should compare lexicographically
        assertThat(qn1.compareTo(qn2)).isLessThan(0);
        assertThat(qn2.compareTo(qn1)).isGreaterThan(0);
        assertThat(qn2.compareTo(qn3)).isLessThan(0);
        assertThat(qn1.compareTo(qn1)).isEqualTo(0);
    }

    @Test
    void testQualifiedNameRejectsBlank() {
        // When/Then: Should reject blank value
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> QualifiedName.of("   "));
    }

    @Test
    void testQualifiedNameRejectsNull() {
        // When/Then: Should reject null value
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> QualifiedName.of(null));
    }

    @Test
    void testQualifiedNameNestedClasses() {
        // When: Parse nested class name
        QualifiedName qn = QualifiedName.of("com.example.Outer.Inner.DeepNested");

        // Then: Should handle nested structure
        assertThat(qn.simpleName()).isEqualTo("DeepNested");
        assertThat(qn.packageName()).isPresent();
        assertThat(qn.packageName().get()).isEqualTo("com.example.Outer.Inner");
        assertThat(qn.enclosing()).isPresent();
        assertThat(qn.enclosing().get().value()).isEqualTo("com.example.Outer.Inner");
    }

    @Test
    void testQualifiedNameChainEnclosing() {
        // Given: Nested qualified name
        QualifiedName qn = QualifiedName.of("a.b.c.D");

        // When: Chain enclosing calls
        QualifiedName enc1 = qn.enclosing().get();
        QualifiedName enc2 = enc1.enclosing().get();
        QualifiedName enc3 = enc2.enclosing().get();

        // Then: Should navigate up the hierarchy
        assertThat(enc1.value()).isEqualTo("a.b.c");
        assertThat(enc2.value()).isEqualTo("a.b");
        assertThat(enc3.value()).isEqualTo("a");
        assertThat(enc3.enclosing().isPresent()).isFalse();
    }
}
