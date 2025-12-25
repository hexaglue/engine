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
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Integration test for Enum Constants extraction (EP-005).
 *
 * <p>This test validates that enum constants are correctly exposed through the SPI.</p>
 *
 * <p>Since this is an integration test, we verify that the SPI contracts work correctly
 * with the default implementations.</p>
 */
class EnumConstantsIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // DomainTypeView.enumConstants() Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testEnumConstants_defaultImplementation_returnsEmptyForNonEnum() {
        // Given: A non-enum domain type created with factory
        DomainTypeView domainType =
                createSimpleDomainTypeView("com.example.Customer", "Customer", DomainTypeKind.ENTITY);

        // When: Get enum constants
        Optional<List<String>> enumConstants = domainType.enumConstants();

        // Then: Should return empty (not an enum)
        assertThat(enumConstants).isEmpty();
    }

    @Test
    void testEnumConstants_defaultImplementation_returnsEmptyListForEnum() {
        // Given: An enum domain type created with factory (default implementation)
        DomainTypeView domainType =
                createSimpleDomainTypeView("com.example.Status", "Status", DomainTypeKind.ENUMERATION);

        // When: Get enum constants
        Optional<List<String>> enumConstants = domainType.enumConstants();

        // Then: Should return empty list (default implementation)
        assertThat(enumConstants).isPresent();
        assertThat(enumConstants.get()).isEmpty();
    }

    @Test
    void testEnumConstants_customImplementation_returnsConstants() {
        // Given: An enum domain type with custom enumConstants() implementation
        DomainTypeView domainType = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.OrderStatus";
            }

            @Override
            public String simpleName() {
                return "OrderStatus";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.ENUMERATION;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.OrderStatus");
            }

            @Override
            public List<DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return true;
            }

            @Override
            public Optional<List<String>> enumConstants() {
                return Optional.of(List.of("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED"));
            }
        };

        // When: Get enum constants
        Optional<List<String>> enumConstants = domainType.enumConstants();

        // Then: Should return the enum constants
        assertThat(enumConstants).isPresent();
        assertThat(enumConstants.get()).hasSize(4);
        assertThat(enumConstants.get())
                .containsExactly("PENDING", "CONFIRMED", "COMPLETED", "CANCELLED")
                .inOrder();
    }

    @Test
    void testEnumConstants_customImplementation_preservesOrder() {
        // Given: An enum with constants in specific order
        DomainTypeView domainType = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.Priority";
            }

            @Override
            public String simpleName() {
                return "Priority";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.ENUMERATION;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.Priority");
            }

            @Override
            public List<DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return true;
            }

            @Override
            public Optional<List<String>> enumConstants() {
                return Optional.of(List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
            }
        };

        // When: Get enum constants
        Optional<List<String>> enumConstants = domainType.enumConstants();

        // Then: Order should be preserved
        assertThat(enumConstants).isPresent();
        assertThat(enumConstants.get().get(0)).isEqualTo("LOW");
        assertThat(enumConstants.get().get(1)).isEqualTo("MEDIUM");
        assertThat(enumConstants.get().get(2)).isEqualTo("HIGH");
        assertThat(enumConstants.get().get(3)).isEqualTo("CRITICAL");
    }

    @Test
    void testEnumConstants_valueObject_returnsEmpty() {
        // Given: A value object domain type
        DomainTypeView domainType =
                createSimpleDomainTypeView("com.example.Money", "Money", DomainTypeKind.VALUE_OBJECT);

        // When: Get enum constants
        Optional<List<String>> enumConstants = domainType.enumConstants();

        // Then: Should return empty (not an enum)
        assertThat(enumConstants).isEmpty();
    }

    @Test
    void testEnumConstants_record_returnsEmpty() {
        // Given: A record domain type
        DomainTypeView domainType = createSimpleDomainTypeView("com.example.Address", "Address", DomainTypeKind.RECORD);

        // When: Get enum constants
        Optional<List<String>> enumConstants = domainType.enumConstants();

        // Then: Should return empty (not an enum)
        assertThat(enumConstants).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Use Case Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testEnumConstants_validationUseCase() {
        // Given: An enum with known constants
        DomainTypeView statusEnum = new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return "com.example.Status";
            }

            @Override
            public String simpleName() {
                return "Status";
            }

            @Override
            public DomainTypeKind kind() {
                return DomainTypeKind.ENUMERATION;
            }

            @Override
            public TypeRef type() {
                return TypeRefFactory.classRef("com.example.Status");
            }

            @Override
            public List<DomainPropertyView> properties() {
                return List.of();
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.empty();
            }

            @Override
            public boolean isImmutable() {
                return true;
            }

            @Override
            public Optional<List<String>> enumConstants() {
                return Optional.of(List.of("ACTIVE", "INACTIVE", "SUSPENDED"));
            }
        };

        // When: Validate a value against enum constants
        String validValue = "ACTIVE";
        String invalidValue = "DELETED";

        Optional<List<String>> constants = statusEnum.enumConstants();

        // Then: Can determine if value is valid
        assertThat(constants).isPresent();
        assertThat(constants.get()).contains(validValue);
        assertThat(constants.get()).doesNotContain(invalidValue);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods
    // ─────────────────────────────────────────────────────────────────────────

    private DomainTypeView createSimpleDomainTypeView(String qualifiedName, String simpleName, DomainTypeKind kind) {
        TypeRef typeRef = TypeRefFactory.classRef(qualifiedName);
        return DomainTypeView.of(
                qualifiedName, simpleName, kind, typeRef, List.of(), null, kind == DomainTypeKind.ENUMERATION, null);
    }
}
