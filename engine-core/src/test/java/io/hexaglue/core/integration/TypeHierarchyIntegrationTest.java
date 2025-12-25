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
import io.hexaglue.spi.ir.domain.DomainIdView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Type Hierarchy extraction (EP-004).
 *
 * <p>This test validates that the type hierarchy information (superType, interfaces,
 * permittedSubtypes) is correctly exposed through the SPI.</p>
 *
 * <p>Since this is an integration test, we verify that the SPI contracts work correctly
 * with the default implementations.</p>
 */
class TypeHierarchyIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // DomainTypeView.superType() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testSuperType_defaultImplementation_returnsEmpty() {
        // Given: A domain type created with factory (no hierarchy info)
        DomainTypeView domainType = createSimpleDomainTypeView("com.example.Customer", "Customer");

        // When: Get super type
        Optional<TypeRef> superType = domainType.superType();

        // Then: Should return empty (default implementation)
        assertThat(superType).isEmpty();
    }

    @Test
    void testSuperType_customImplementation_returnsSuperType() {
        // Given: A domain type with custom superType() implementation
        TypeRef baseEntityRef = TypeRefFactory.classRef("com.example.BaseEntity");
        DomainTypeView domainType = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.Customer";
            }

            @Override
            public String simpleName() {
                return "Customer";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.ENTITY;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.Customer");
            }

            @Override
            public List<io.hexaglue.spi.ir.domain.DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return false;
            }

            @Override
            public Optional<TypeRef> superType() {
                return Optional.of(baseEntityRef);
            }
        };

        // When: Get super type
        Optional<TypeRef> superType = domainType.superType();

        // Then: Should return the super type
        assertThat(superType).isPresent();
        assertThat(superType.get().name().value()).isEqualTo("com.example.BaseEntity");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DomainTypeView.interfaces() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testInterfaces_defaultImplementation_returnsEmptyList() {
        // Given: A domain type created with factory (no hierarchy info)
        DomainTypeView domainType = createSimpleDomainTypeView("com.example.Customer", "Customer");

        // When: Get interfaces
        List<TypeRef> interfaces = domainType.interfaces();

        // Then: Should return empty list (default implementation)
        assertThat(interfaces).isEmpty();
    }

    @Test
    void testInterfaces_customImplementation_returnsInterfaces() {
        // Given: A domain type with custom interfaces() implementation
        TypeRef serializableRef = TypeRefFactory.classRef("java.io.Serializable");
        TypeRef cloneableRef = TypeRefFactory.classRef("java.lang.Cloneable");

        DomainTypeView domainType = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.Customer";
            }

            @Override
            public String simpleName() {
                return "Customer";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.ENTITY;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.Customer");
            }

            @Override
            public List<io.hexaglue.spi.ir.domain.DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return false;
            }

            @Override
            public List<TypeRef> interfaces() {
                return List.of(serializableRef, cloneableRef);
            }
        };

        // When: Get interfaces
        List<TypeRef> interfaces = domainType.interfaces();

        // Then: Should return the interfaces
        assertThat(interfaces).hasSize(2);
        assertThat(interfaces.get(0).name().value()).isEqualTo("java.io.Serializable");
        assertThat(interfaces.get(1).name().value()).isEqualTo("java.lang.Cloneable");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DomainTypeView.permittedSubtypes() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testPermittedSubtypes_defaultImplementation_returnsEmpty() {
        // Given: A domain type created with factory (no hierarchy info)
        DomainTypeView domainType = createSimpleDomainTypeView("com.example.Payment", "Payment");

        // When: Get permitted subtypes
        Optional<List<TypeRef>> permittedSubtypes = domainType.permittedSubtypes();

        // Then: Should return empty (default implementation)
        assertThat(permittedSubtypes).isEmpty();
    }

    @Test
    void testPermittedSubtypes_customImplementation_returnsPermittedSubtypes() {
        // Given: A sealed type with custom permittedSubtypes() implementation
        TypeRef creditCardRef = TypeRefFactory.classRef("com.example.CreditCardPayment");
        TypeRef cashRef = TypeRefFactory.classRef("com.example.CashPayment");

        DomainTypeView domainType = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.Payment";
            }

            @Override
            public String simpleName() {
                return "Payment";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.ENTITY;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.Payment");
            }

            @Override
            public List<io.hexaglue.spi.ir.domain.DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return false;
            }

            @Override
            public Optional<List<TypeRef>> permittedSubtypes() {
                return Optional.of(List.of(creditCardRef, cashRef));
            }
        };

        // When: Get permitted subtypes
        Optional<List<TypeRef>> permittedSubtypes = domainType.permittedSubtypes();

        // Then: Should return the permitted subtypes
        assertThat(permittedSubtypes).isPresent();
        assertThat(permittedSubtypes.get()).hasSize(2);
        assertThat(permittedSubtypes.get().get(0).name().value()).isEqualTo("com.example.CreditCardPayment");
        assertThat(permittedSubtypes.get().get(1).name().value()).isEqualTo("com.example.CashPayment");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Combined Hierarchy Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeHierarchy_fullHierarchyInfo() {
        // Given: A domain type with complete hierarchy information
        TypeRef baseEntityRef = TypeRefFactory.classRef("com.example.BaseEntity");
        TypeRef serializableRef = TypeRefFactory.classRef("java.io.Serializable");

        DomainTypeView domainType = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.Customer";
            }

            @Override
            public String simpleName() {
                return "Customer";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.AGGREGATE_ROOT;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.Customer");
            }

            @Override
            public List<io.hexaglue.spi.ir.domain.DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return false;
            }

            @Override
            public Optional<TypeRef> superType() {
                return Optional.of(baseEntityRef);
            }

            @Override
            public List<TypeRef> interfaces() {
                return List.of(serializableRef);
            }

            @Override
            public Optional<List<TypeRef>> permittedSubtypes() {
                return Optional.empty();
            }
        };

        // When/Then: Verify all hierarchy information is accessible
        assertThat(domainType.superType()).isPresent();
        assertThat(domainType.superType().get().name().value()).isEqualTo("com.example.BaseEntity");

        assertThat(domainType.interfaces()).hasSize(1);
        assertThat(domainType.interfaces().get(0).name().value()).isEqualTo("java.io.Serializable");

        assertThat(domainType.permittedSubtypes()).isEmpty();

        assertThat(domainType.isAggregateRoot()).isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private DomainTypeView createSimpleDomainTypeView(String qualifiedName, String simpleName) {
        TypeRef typeRef = TypeRefFactory.classRef(qualifiedName);
        return DomainTypeView.of(
                qualifiedName, simpleName, DomainTypeKind.ENTITY, typeRef, List.of(), null, false, null);
    }
}
