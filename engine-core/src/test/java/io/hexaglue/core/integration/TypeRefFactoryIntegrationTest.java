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
import io.hexaglue.spi.types.ArrayRef;
import io.hexaglue.spi.types.ClassRef;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.ParameterizedRef;
import io.hexaglue.spi.types.PrimitiveRef;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.TypeVariableRef;
import io.hexaglue.spi.types.WildcardRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating TypeRefFactory and its contracts.
 *
 * <p>Tests the type reference factory which provides convenience methods
 * for creating SPI type references and serves as the foundation for
 * TypeSystemSpec implementations.</p>
 */
class TypeRefFactoryIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Common Type Constants Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testPrimitiveConstants() {
        // When: Access primitive type constants
        assertThat(TypeRefFactory.VOID).isNotNull();
        assertThat(TypeRefFactory.BOOLEAN).isNotNull();
        assertThat(TypeRefFactory.BYTE).isNotNull();
        assertThat(TypeRefFactory.SHORT).isNotNull();
        assertThat(TypeRefFactory.INT).isNotNull();
        assertThat(TypeRefFactory.LONG).isNotNull();
        assertThat(TypeRefFactory.FLOAT).isNotNull();
        assertThat(TypeRefFactory.DOUBLE).isNotNull();
        assertThat(TypeRefFactory.CHAR).isNotNull();

        // Then: All should have correct kind and names
        assertThat(TypeRefFactory.INT.kind()).isEqualTo(TypeKind.PRIMITIVE);
        assertThat(TypeRefFactory.INT.name().value()).isEqualTo("int");
        assertThat(TypeRefFactory.BOOLEAN.name().value()).isEqualTo("boolean");
    }

    @Test
    void testClassConstants() {
        // When: Access class type constants
        assertThat(TypeRefFactory.OBJECT).isNotNull();
        assertThat(TypeRefFactory.STRING).isNotNull();
        assertThat(TypeRefFactory.INTEGER).isNotNull();
        assertThat(TypeRefFactory.LONG_WRAPPER).isNotNull();
        assertThat(TypeRefFactory.BOOLEAN_WRAPPER).isNotNull();
        assertThat(TypeRefFactory.DOUBLE_WRAPPER).isNotNull();

        // Then: All should have correct kind and names
        assertThat(TypeRefFactory.OBJECT.kind()).isEqualTo(TypeKind.CLASS);
        assertThat(TypeRefFactory.OBJECT.name().value()).isEqualTo("java.lang.Object");
        assertThat(TypeRefFactory.STRING.name().value()).isEqualTo("java.lang.String");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Primitive Type Factory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testPrimitiveFactory() {
        // When: Create primitive types via factory
        PrimitiveRef intType = TypeRefFactory.primitive("int");
        PrimitiveRef boolType = TypeRefFactory.primitive("boolean");

        // Then: Should create correct types
        assertThat(intType.kind()).isEqualTo(TypeKind.PRIMITIVE);
        assertThat(intType.name().value()).isEqualTo("int");
        assertThat(boolType.name().value()).isEqualTo("boolean");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Class Type Factory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testClassRefFactory() {
        // When: Create class reference
        ClassRef customerType = TypeRefFactory.classRef("com.example.Customer");

        // Then: Should create correct type
        assertThat(customerType.kind()).isEqualTo(TypeKind.CLASS);
        assertThat(customerType.name().value()).isEqualTo("com.example.Customer");
        assertThat(customerType.nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    @Test
    void testClassRefFactoryWithNullability() {
        // When: Create class reference with nullability
        ClassRef nullable = TypeRefFactory.classRef("com.example.Customer", Nullability.NULLABLE);

        // Then: Should have specified nullability
        assertThat(nullable.nullability()).isEqualTo(Nullability.NULLABLE);
        assertThat(nullable.name().value()).isEqualTo("com.example.Customer");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Array Type Factory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testArrayOfFactory() {
        // Given: Component type
        ClassRef stringType = TypeRefFactory.STRING;

        // When: Create array type
        ArrayRef stringArray = TypeRefFactory.arrayOf(stringType);

        // Then: Should create array with correct component
        assertThat(stringArray.kind()).isEqualTo(TypeKind.ARRAY);
        assertThat(stringArray.componentType()).isEqualTo(stringType);
        assertThat(stringArray.nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    @Test
    void testArrayOfFactoryWithNullability() {
        // Given: Component type
        PrimitiveRef intType = TypeRefFactory.INT;

        // When: Create array with nullability
        ArrayRef intArray = TypeRefFactory.arrayOf(intType, Nullability.NONNULL);

        // Then: Should have specified nullability
        assertThat(intArray.nullability()).isEqualTo(Nullability.NONNULL);
        assertThat(intArray.componentType()).isEqualTo(intType);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parameterized Type Factory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testParameterizedFactory() {
        // Given: Raw type and type arguments
        ClassRef listRaw = TypeRefFactory.classRef("java.util.List");
        ClassRef stringType = TypeRefFactory.STRING;

        // When: Create parameterized type
        ParameterizedRef listOfString = TypeRefFactory.parameterized(listRaw, List.of(stringType));

        // Then: Should create parameterized type
        assertThat(listOfString.kind()).isEqualTo(TypeKind.PARAMETERIZED);
        assertThat(listOfString.rawType()).isEqualTo(listRaw);
        assertThat(listOfString.typeArguments()).hasSize(1);
        assertThat(listOfString.typeArguments().get(0)).isEqualTo(stringType);
    }

    @Test
    void testParameterizedFactoryVarargs() {
        // Given: Raw type and type arguments
        ClassRef mapRaw = TypeRefFactory.classRef("java.util.Map");
        ClassRef stringType = TypeRefFactory.STRING;
        ClassRef integerType = TypeRefFactory.INTEGER;

        // When: Create parameterized type with varargs
        ParameterizedRef mapOfStringInteger = TypeRefFactory.parameterized(mapRaw, stringType, integerType);

        // Then: Should create parameterized type
        assertThat(mapOfStringInteger.typeArguments()).hasSize(2);
        assertThat(mapOfStringInteger.typeArguments().get(0)).isEqualTo(stringType);
        assertThat(mapOfStringInteger.typeArguments().get(1)).isEqualTo(integerType);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wildcard Type Factory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testWildcardFactory() {
        // When: Create unbounded wildcard
        WildcardRef wildcard = TypeRefFactory.wildcard();

        // Then: Should be unbounded
        assertThat(wildcard.kind()).isEqualTo(TypeKind.WILDCARD);
        assertThat(wildcard.upperBound()).isNull();
        assertThat(wildcard.lowerBound()).isNull();
    }

    @Test
    void testWildcardExtendsFactory() {
        // Given: Upper bound
        ClassRef numberType = TypeRefFactory.classRef("java.lang.Number");

        // When: Create wildcard with upper bound
        WildcardRef wildcard = TypeRefFactory.wildcardExtends(numberType);

        // Then: Should have upper bound
        assertThat(wildcard.upperBound()).isNotNull();
        assertThat(wildcard.upperBound()).isEqualTo(numberType);
        assertThat(wildcard.lowerBound()).isNull();
    }

    @Test
    void testWildcardSuperFactory() {
        // Given: Lower bound
        ClassRef integerType = TypeRefFactory.INTEGER;

        // When: Create wildcard with lower bound
        WildcardRef wildcard = TypeRefFactory.wildcardSuper(integerType);

        // Then: Should have lower bound
        assertThat(wildcard.lowerBound()).isNotNull();
        assertThat(wildcard.lowerBound()).isEqualTo(integerType);
        assertThat(wildcard.upperBound()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Type Variable Factory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeVariableFactory() {
        // When: Create type variable with bounds
        ClassRef numberType = TypeRefFactory.classRef("java.lang.Number");
        TypeVariableRef tVar = TypeRefFactory.typeVariable("T", List.of(numberType));

        // Then: Should have correct properties
        assertThat(tVar.kind()).isEqualTo(TypeKind.TYPE_VARIABLE);
        assertThat(tVar.name().value()).isEqualTo("T");
        assertThat(tVar.bounds()).hasSize(1);
        assertThat(tVar.bounds().get(0)).isEqualTo(numberType);
    }

    @Test
    void testTypeVariableFactoryNoBounds() {
        // When: Create type variable with null bounds
        TypeVariableRef tVar = TypeRefFactory.typeVariable("T", null);

        // Then: Should have no bounds
        assertThat(tVar.name().value()).isEqualTo("T");
        assertThat(tVar.bounds()).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Boxing and Unboxing Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testBoxPrimitives() {
        // When: Box all primitive types
        assertThat(TypeRefFactory.box(TypeRefFactory.INT).name().value()).isEqualTo("java.lang.Integer");
        assertThat(TypeRefFactory.box(TypeRefFactory.BOOLEAN).name().value()).isEqualTo("java.lang.Boolean");
        assertThat(TypeRefFactory.box(TypeRefFactory.BYTE).name().value()).isEqualTo("java.lang.Byte");
        assertThat(TypeRefFactory.box(TypeRefFactory.SHORT).name().value()).isEqualTo("java.lang.Short");
        assertThat(TypeRefFactory.box(TypeRefFactory.LONG).name().value()).isEqualTo("java.lang.Long");
        assertThat(TypeRefFactory.box(TypeRefFactory.FLOAT).name().value()).isEqualTo("java.lang.Float");
        assertThat(TypeRefFactory.box(TypeRefFactory.DOUBLE).name().value()).isEqualTo("java.lang.Double");
        assertThat(TypeRefFactory.box(TypeRefFactory.CHAR).name().value()).isEqualTo("java.lang.Character");
        assertThat(TypeRefFactory.box(TypeRefFactory.VOID).name().value()).isEqualTo("java.lang.Void");
    }

    @Test
    void testUnboxWrappers() {
        // When: Unbox all wrapper types
        assertThat(TypeRefFactory.unbox(TypeRefFactory.INTEGER)).isEqualTo(TypeRefFactory.INT);
        assertThat(TypeRefFactory.unbox(TypeRefFactory.BOOLEAN_WRAPPER)).isEqualTo(TypeRefFactory.BOOLEAN);
        assertThat(TypeRefFactory.unbox(TypeRefFactory.LONG_WRAPPER)).isEqualTo(TypeRefFactory.LONG);
        assertThat(TypeRefFactory.unbox(TypeRefFactory.DOUBLE_WRAPPER)).isEqualTo(TypeRefFactory.DOUBLE);

        // Test with simple names too
        ClassRef simpleInteger = TypeRefFactory.classRef("Integer");
        assertThat(TypeRefFactory.unbox(simpleInteger)).isEqualTo(TypeRefFactory.INT);
    }

    @Test
    void testUnboxNonWrapper() {
        // When: Try to unbox a non-wrapper type
        ClassRef stringType = TypeRefFactory.STRING;
        PrimitiveRef result = TypeRefFactory.unbox(stringType);

        // Then: Should return null
        assertThat(result).isNull();
    }

    @Test
    void testIsWrapper() {
        // When: Check wrapper detection
        assertThat(TypeRefFactory.isWrapper(TypeRefFactory.INTEGER)).isTrue();
        assertThat(TypeRefFactory.isWrapper(TypeRefFactory.BOOLEAN_WRAPPER)).isTrue();
        assertThat(TypeRefFactory.isWrapper(TypeRefFactory.STRING)).isFalse();
        assertThat(TypeRefFactory.isWrapper(TypeRefFactory.OBJECT)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Complex Type Construction Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testComplexTypeConstruction() {
        // Given: Complex type List<? extends Number>
        ClassRef listRaw = TypeRefFactory.classRef("java.util.List");
        ClassRef numberType = TypeRefFactory.classRef("java.lang.Number");
        WildcardRef wildcard = TypeRefFactory.wildcardExtends(numberType);

        // When: Create parameterized type with wildcard
        ParameterizedRef listOfWildcard = TypeRefFactory.parameterized(listRaw, wildcard);

        // Then: Should create correct structure
        assertThat(listOfWildcard.rawType()).isEqualTo(listRaw);
        assertThat(listOfWildcard.typeArguments()).hasSize(1);

        TypeRef arg = listOfWildcard.typeArguments().get(0);
        assertThat(arg.kind()).isEqualTo(TypeKind.WILDCARD);
        WildcardRef wildcardArg = (WildcardRef) arg;
        assertThat(wildcardArg.upperBound()).isNotNull();
        assertThat(wildcardArg.upperBound()).isEqualTo(numberType);
    }
}
