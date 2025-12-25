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

import io.hexaglue.core.types.TypeComparators;
import io.hexaglue.core.types.TypeDisplay;
import io.hexaglue.core.types.TypeRefFactory;
import io.hexaglue.spi.types.ArrayRef;
import io.hexaglue.spi.types.ClassRef;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.ParameterizedRef;
import io.hexaglue.spi.types.PrimitiveRef;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeName;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.TypeVariableRef;
import io.hexaglue.spi.types.WildcardRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating the type system SPI contracts and Core implementations.
 *
 * <p>
 * This test ensures that:
 * <ul>
 *   <li>Type system enums are accessible and complete</li>
 *   <li>SPI TypeRef implementations work correctly</li>
 *   <li>TypeRefFactory provides convenient creation methods</li>
 *   <li>TypeDisplay renders types correctly</li>
 *   <li>TypeComparators provides equality and comparison utilities</li>
 * </ul>
 * </p>
 */
class TypeSystemIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Enum Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNullabilityEnum() {
        // When: Access Nullability enum values
        Nullability unspecified = Nullability.UNSPECIFIED;
        Nullability nonnull = Nullability.NONNULL;
        Nullability nullable = Nullability.NULLABLE;

        // Then: All values should be accessible
        assertThat(unspecified).isNotNull();
        assertThat(nonnull).isNotNull();
        assertThat(nullable).isNotNull();
        assertThat(Nullability.values()).hasLength(3);
    }

    @Test
    void testTypeKindEnum() {
        // When: Access TypeKind enum values
        TypeKind primitive = TypeKind.PRIMITIVE;
        TypeKind classKind = TypeKind.CLASS;
        TypeKind array = TypeKind.ARRAY;
        TypeKind parameterized = TypeKind.PARAMETERIZED;
        TypeKind typeVariable = TypeKind.TYPE_VARIABLE;
        TypeKind wildcard = TypeKind.WILDCARD;

        // Then: All values should be accessible
        assertThat(primitive).isNotNull();
        assertThat(classKind).isNotNull();
        assertThat(array).isNotNull();
        assertThat(parameterized).isNotNull();
        assertThat(typeVariable).isNotNull();
        assertThat(wildcard).isNotNull();
        assertThat(TypeKind.values()).hasLength(6);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TypeName Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeNameCreation() {
        // When: Create TypeName instances
        TypeName simple = TypeName.of("String");
        TypeName qualified = TypeName.of("java.lang.String");

        // Then: Should preserve values
        assertThat(simple.value()).isEqualTo("String");
        assertThat(qualified.value()).isEqualTo("java.lang.String");
    }

    @Test
    void testTypeNameQualifiedChecks() {
        // Given: Simple and qualified names
        TypeName simple = TypeName.of("String");
        TypeName qualified = TypeName.of("java.lang.String");

        // Then: isQualified should work correctly
        assertThat(simple.isQualified()).isFalse();
        assertThat(qualified.isQualified()).isTrue();
    }

    @Test
    void testTypeNamePackageAndSimpleName() {
        // Given: A qualified type name
        TypeName name = TypeName.of("java.util.List");

        // When/Then: Should extract package and simple name
        assertThat(name.packageName()).isPresent();
        assertThat(name.packageName().get()).isEqualTo("java.util");
        assertThat(name.simpleName()).isEqualTo("List");
    }

    @Test
    void testTypeNameSimpleHasNoPackage() {
        // Given: A simple type name
        TypeName name = TypeName.of("String");

        // When/Then: Should have no package
        assertThat(name.packageName()).isEmpty();
        assertThat(name.simpleName()).isEqualTo("String");
    }

    @Test
    void testTypeNameEquality() {
        // Given: TypeName instances
        TypeName name1 = TypeName.of("java.lang.String");
        TypeName name2 = TypeName.of("java.lang.String");
        TypeName name3 = TypeName.of("String");

        // Then: Equality should work
        assertThat(name1).isEqualTo(name2);
        assertThat(name1).isNotEqualTo(name3);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PrimitiveRef Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testPrimitiveRefCreation() {
        // When: Create primitive type references
        PrimitiveRef intType = PrimitiveRef.of("int");
        PrimitiveRef booleanType = PrimitiveRef.of("boolean");
        PrimitiveRef voidType = PrimitiveRef.of("void");

        // Then: Should have correct properties
        assertThat(intType.kind()).isEqualTo(TypeKind.PRIMITIVE);
        assertThat(intType.name().value()).isEqualTo("int");
        assertThat(intType.nullability()).isEqualTo(Nullability.UNSPECIFIED);
        assertThat(booleanType.name().value()).isEqualTo("boolean");
        assertThat(voidType.name().value()).isEqualTo("void");
    }

    @Test
    void testPrimitiveRefRendering() {
        // Given: Primitive type references
        PrimitiveRef intType = PrimitiveRef.of("int");

        // When: Render type
        String rendered = intType.render();

        // Then: Should render as keyword
        assertThat(rendered).isEqualTo("int");
    }

    @Test
    void testPrimitiveRefWithNullability() {
        // Given: A primitive type
        PrimitiveRef intType = PrimitiveRef.of("int");

        // When: Create with different nullability
        PrimitiveRef nonnull = intType.withNullability(Nullability.NONNULL);

        // Then: Should preserve name but change nullability
        assertThat(nonnull.name().value()).isEqualTo("int");
        assertThat(nonnull.nullability()).isEqualTo(Nullability.NONNULL);
        assertThat(intType.nullability()).isEqualTo(Nullability.UNSPECIFIED); // Original unchanged
    }

    @Test
    void testPrimitiveRefRejectsQualifiedName() {
        // When/Then: Should reject qualified names
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> PrimitiveRef.of("java.lang.Integer"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ClassRef Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testClassRefCreation() {
        // When: Create class type references
        ClassRef stringType = ClassRef.of("java.lang.String");
        ClassRef listType = ClassRef.of("java.util.List");

        // Then: Should have correct properties
        assertThat(stringType.kind()).isEqualTo(TypeKind.CLASS);
        assertThat(stringType.name().value()).isEqualTo("java.lang.String");
        assertThat(stringType.nullability()).isEqualTo(Nullability.UNSPECIFIED);
        assertThat(listType.name().value()).isEqualTo("java.util.List");
    }

    @Test
    void testClassRefQualifiedName() {
        // Given: Qualified and simple class refs
        ClassRef qualified = ClassRef.of("java.lang.String");
        ClassRef simple = ClassRef.of("String");

        // When/Then: qualifiedName() should work
        assertThat(qualified.qualifiedName()).isPresent();
        assertThat(qualified.qualifiedName().get()).isEqualTo("java.lang.String");
        assertThat(simple.qualifiedName()).isEmpty();
    }

    @Test
    void testClassRefRendering() {
        // Given: Class type reference
        ClassRef stringType = ClassRef.of("java.lang.String");

        // When: Render type
        String rendered = stringType.render();

        // Then: Should render qualified name
        assertThat(rendered).isEqualTo("java.lang.String");
    }

    @Test
    void testClassRefWithNullability() {
        // Given: A class type
        ClassRef stringType = ClassRef.of("java.lang.String");

        // When: Create with different nullability
        ClassRef nullable = stringType.withNullability(Nullability.NULLABLE);

        // Then: Should preserve name but change nullability
        assertThat(nullable.name().value()).isEqualTo("java.lang.String");
        assertThat(nullable.nullability()).isEqualTo(Nullability.NULLABLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ArrayRef Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testArrayRefCreation() {
        // Given: Component type
        PrimitiveRef intType = PrimitiveRef.of("int");

        // When: Create array type
        ArrayRef intArray = ArrayRef.of(intType);

        // Then: Should have correct properties
        assertThat(intArray.kind()).isEqualTo(TypeKind.ARRAY);
        assertThat(intArray.componentType()).isEqualTo(intType);
        assertThat(intArray.nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    @Test
    void testArrayRefRendering() {
        // Given: Array type
        ClassRef stringType = ClassRef.of("java.lang.String");
        ArrayRef stringArray = ArrayRef.of(stringType);

        // When: Render array
        String rendered = stringArray.render();

        // Then: Should render with brackets
        assertThat(rendered).isEqualTo("java.lang.String[]");
    }

    @Test
    void testMultiDimensionalArray() {
        // Given: Component types
        PrimitiveRef intType = PrimitiveRef.of("int");
        ArrayRef intArray = ArrayRef.of(intType);
        ArrayRef int2dArray = ArrayRef.of(intArray);

        // When: Render
        String rendered = int2dArray.render();

        // Then: Should render as 2D array
        assertThat(rendered).isEqualTo("int[][]");
    }

    @Test
    void testArrayRefWithNullability() {
        // Given: Array type
        ArrayRef array = ArrayRef.of(PrimitiveRef.of("int"));

        // When: Create with nullability
        ArrayRef nonnull = array.withNullability(Nullability.NONNULL);

        // Then: Should change array's nullability, not component's
        assertThat(nonnull.nullability()).isEqualTo(Nullability.NONNULL);
        assertThat(nonnull.componentType().nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ParameterizedRef Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testParameterizedRefCreation() {
        // Given: Raw type and type arguments
        ClassRef listRaw = ClassRef.of("java.util.List");
        ClassRef stringType = ClassRef.of("java.lang.String");

        // When: Create parameterized type
        ParameterizedRef listOfString = ParameterizedRef.of(listRaw, List.of(stringType));

        // Then: Should have correct properties
        assertThat(listOfString.kind()).isEqualTo(TypeKind.PARAMETERIZED);
        assertThat(listOfString.rawType()).isEqualTo(listRaw);
        assertThat(listOfString.typeArguments()).hasSize(1);
        assertThat(listOfString.typeArguments().get(0)).isEqualTo(stringType);
    }

    @Test
    void testParameterizedRefRendering() {
        // Given: Parameterized type
        ClassRef mapRaw = ClassRef.of("java.util.Map");
        ClassRef stringType = ClassRef.of("java.lang.String");
        ClassRef integerType = ClassRef.of("java.lang.Integer");
        ParameterizedRef mapOfStringInteger = ParameterizedRef.of(mapRaw, List.of(stringType, integerType));

        // When: Render
        String rendered = mapOfStringInteger.render();

        // Then: Should render with angle brackets
        assertThat(rendered).isEqualTo("java.util.Map<java.lang.String, java.lang.Integer>");
    }

    @Test
    void testParameterizedRefRejectsEmptyArguments() {
        // Given: Raw type
        ClassRef listRaw = ClassRef.of("java.util.List");

        // When/Then: Should reject empty type arguments
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> ParameterizedRef.of(listRaw, List.of()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TypeVariableRef Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeVariableRefCreation() {
        // When: Create type variable
        TypeVariableRef tVar = TypeVariableRef.of("T");

        // Then: Should have correct properties
        assertThat(tVar.kind()).isEqualTo(TypeKind.TYPE_VARIABLE);
        assertThat(tVar.name().value()).isEqualTo("T");
        assertThat(tVar.bounds()).isEmpty();
        assertThat(tVar.nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    @Test
    void testTypeVariableRefWithBounds() {
        // Given: Bounds
        ClassRef numberType = ClassRef.of("java.lang.Number");
        TypeName tName = TypeName.of("T");

        // When: Create type variable with bounds
        TypeVariableRef tVar = new TypeVariableRef(tName, List.of(numberType), Nullability.UNSPECIFIED);

        // Then: Should have bounds
        assertThat(tVar.bounds()).hasSize(1);
        assertThat(tVar.bounds().get(0)).isEqualTo(numberType);
    }

    @Test
    void testTypeVariableRefRendering() {
        // Given: Type variable
        TypeVariableRef tVar = TypeVariableRef.of("T");

        // When: Render
        String rendered = tVar.render();

        // Then: Should render as variable name
        assertThat(rendered).isEqualTo("T");
    }

    @Test
    void testTypeVariableRefRejectsQualifiedName() {
        // When/Then: Should reject qualified names
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> TypeVariableRef.of("com.T"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WildcardRef Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testWildcardRefUnbounded() {
        // When: Create unbounded wildcard
        WildcardRef wildcard = WildcardRef.unbounded();

        // Then: Should have no bounds
        assertThat(wildcard.kind()).isEqualTo(TypeKind.WILDCARD);
        assertThat(wildcard.upperBoundOptional()).isEmpty();
        assertThat(wildcard.lowerBoundOptional()).isEmpty();
        assertThat(wildcard.render()).isEqualTo("?");
    }

    @Test
    void testWildcardRefExtends() {
        // Given: Upper bound
        ClassRef numberType = ClassRef.of("java.lang.Number");

        // When: Create extends wildcard
        WildcardRef wildcard = WildcardRef.extendsBound(numberType);

        // Then: Should have upper bound
        assertThat(wildcard.upperBoundOptional()).isPresent();
        assertThat(wildcard.upperBoundOptional().get()).isEqualTo(numberType);
        assertThat(wildcard.lowerBoundOptional()).isEmpty();
        assertThat(wildcard.render()).isEqualTo("? extends java.lang.Number");
    }

    @Test
    void testWildcardRefSuper() {
        // Given: Lower bound
        ClassRef integerType = ClassRef.of("java.lang.Integer");

        // When: Create super wildcard
        WildcardRef wildcard = WildcardRef.superBound(integerType);

        // Then: Should have lower bound
        assertThat(wildcard.lowerBoundOptional()).isPresent();
        assertThat(wildcard.lowerBoundOptional().get()).isEqualTo(integerType);
        assertThat(wildcard.upperBoundOptional()).isEmpty();
        assertThat(wildcard.render()).isEqualTo("? super java.lang.Integer");
    }

    @Test
    void testWildcardRefRejectsBothBounds() {
        // When/Then: Should reject both upper and lower bounds
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new WildcardRef(ClassRef.of("Number"), ClassRef.of("Integer"), Nullability.UNSPECIFIED));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TypeRefFactory Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeRefFactoryPrimitiveConstants() {
        // When: Access primitive constants
        PrimitiveRef intType = TypeRefFactory.INT;
        PrimitiveRef booleanType = TypeRefFactory.BOOLEAN;
        PrimitiveRef voidType = TypeRefFactory.VOID;

        // Then: Should have correct values
        assertThat(intType.name().value()).isEqualTo("int");
        assertThat(booleanType.name().value()).isEqualTo("boolean");
        assertThat(voidType.name().value()).isEqualTo("void");
    }

    @Test
    void testTypeRefFactoryClassConstants() {
        // When: Access class constants
        ClassRef objectType = TypeRefFactory.OBJECT;
        ClassRef stringType = TypeRefFactory.STRING;

        // Then: Should have correct values
        assertThat(objectType.name().value()).isEqualTo("java.lang.Object");
        assertThat(stringType.name().value()).isEqualTo("java.lang.String");
    }

    @Test
    void testTypeRefFactoryPrimitiveMethod() {
        // When: Create primitive via factory
        PrimitiveRef longType = TypeRefFactory.primitive("long");

        // Then: Should create correct type
        assertThat(longType.name().value()).isEqualTo("long");
        assertThat(longType.kind()).isEqualTo(TypeKind.PRIMITIVE);
    }

    @Test
    void testTypeRefFactoryClassRefMethod() {
        // When: Create class ref via factory
        ClassRef customerType = TypeRefFactory.classRef("com.example.Customer");

        // Then: Should create correct type
        assertThat(customerType.name().value()).isEqualTo("com.example.Customer");
        assertThat(customerType.kind()).isEqualTo(TypeKind.CLASS);
    }

    @Test
    void testTypeRefFactoryArrayOf() {
        // When: Create array via factory
        ArrayRef stringArray = TypeRefFactory.arrayOf(TypeRefFactory.STRING);

        // Then: Should create array
        assertThat(stringArray.componentType()).isEqualTo(TypeRefFactory.STRING);
        assertThat(stringArray.render()).isEqualTo("java.lang.String[]");
    }

    @Test
    void testTypeRefFactoryParameterized() {
        // When: Create parameterized type via factory
        ParameterizedRef listOfString =
                TypeRefFactory.parameterized(ClassRef.of("java.util.List"), TypeRefFactory.STRING);

        // Then: Should create parameterized type
        assertThat(listOfString.rawType().name().value()).isEqualTo("java.util.List");
        assertThat(listOfString.typeArguments()).hasSize(1);
        assertThat(listOfString.typeArguments().get(0)).isEqualTo(TypeRefFactory.STRING);
    }

    @Test
    void testTypeRefFactoryBoxUnbox() {
        // When: Box and unbox
        ClassRef boxed = TypeRefFactory.box(TypeRefFactory.INT);
        PrimitiveRef unboxed = TypeRefFactory.unbox(boxed);

        // Then: Should convert correctly
        assertThat(boxed.name().value()).isEqualTo("java.lang.Integer");
        assertThat(unboxed).isNotNull();
        assertThat(unboxed.name().value()).isEqualTo("int");
    }

    @Test
    void testTypeRefFactoryIsWrapper() {
        // Given: Wrapper and non-wrapper types
        ClassRef integerType = ClassRef.of("java.lang.Integer");
        ClassRef stringType = ClassRef.of("java.lang.String");

        // When/Then: isWrapper should detect correctly
        assertThat(TypeRefFactory.isWrapper(integerType)).isTrue();
        assertThat(TypeRefFactory.isWrapper(stringType)).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TypeDisplay Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeDisplayRender() {
        // Given: Type references
        ClassRef stringType = ClassRef.of("java.lang.String");

        // When: Render
        String rendered = TypeDisplay.render(stringType);

        // Then: Should render qualified
        assertThat(rendered).isEqualTo("java.lang.String");
    }

    @Test
    void testTypeDisplayRenderSimple() {
        // Given: Qualified type
        ClassRef stringType = ClassRef.of("java.lang.String");

        // When: Render simple
        String simple = TypeDisplay.renderSimple(stringType);

        // Then: Should render simple name
        assertThat(simple).isEqualTo("String");
    }

    @Test
    void testTypeDisplayRenderSimpleParameterized() {
        // Given: Parameterized type
        ParameterizedRef listOfString =
                TypeRefFactory.parameterized(ClassRef.of("java.util.List"), ClassRef.of("java.lang.String"));

        // When: Render simple
        String simple = TypeDisplay.renderSimple(listOfString);

        // Then: Should render simple with type args
        assertThat(simple).isEqualTo("List<String>");
    }

    @Test
    void testTypeDisplayRenderList() {
        // Given: List of types
        List<TypeRef> types = List.of(TypeRefFactory.STRING, TypeRefFactory.INT);

        // When: Render list
        String rendered = TypeDisplay.renderList(types);

        // Then: Should be comma-separated
        assertThat(rendered).isEqualTo("java.lang.String, int");
    }

    @Test
    void testTypeDisplayDescribe() {
        // Given: Type with nullability
        ClassRef stringType = ClassRef.of("java.lang.String").withNullability(Nullability.NULLABLE);

        // When: Describe
        String description = TypeDisplay.describe(stringType);

        // Then: Should include nullability
        assertThat(description).isEqualTo("String (nullable)");
    }

    @Test
    void testTypeDisplaySimpleNameExtraction() {
        // When: Extract simple names
        String simple = TypeDisplay.simpleName("java.util.List");
        String alreadySimple = TypeDisplay.simpleName("String");

        // Then: Should extract correctly
        assertThat(simple).isEqualTo("List");
        assertThat(alreadySimple).isEqualTo("String");
    }

    @Test
    void testTypeDisplayPackageNameExtraction() {
        // When: Extract package names
        String pkg = TypeDisplay.packageName("java.util.List");
        String noPkg = TypeDisplay.packageName("String");

        // Then: Should extract correctly
        assertThat(pkg).isEqualTo("java.util");
        assertThat(noPkg).isEmpty();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TypeComparators Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testTypeComparatorsEqualIgnoringNullability() {
        // Given: Same types with different nullability
        ClassRef string1 = ClassRef.of("java.lang.String").withNullability(Nullability.NULLABLE);
        ClassRef string2 = ClassRef.of("java.lang.String").withNullability(Nullability.NONNULL);

        // When/Then: Should be equal ignoring nullability
        assertThat(TypeComparators.equalIgnoringNullability(string1, string2)).isTrue();
    }

    @Test
    void testTypeComparatorsDeepEquals() {
        // Given: Types with same and different nullability
        ClassRef string1 = ClassRef.of("java.lang.String").withNullability(Nullability.NULLABLE);
        ClassRef string2 = ClassRef.of("java.lang.String").withNullability(Nullability.NULLABLE);
        ClassRef string3 = ClassRef.of("java.lang.String").withNullability(Nullability.NONNULL);

        // When/Then: Deep equals should check nullability
        assertThat(TypeComparators.deepEquals(string1, string2)).isTrue();
        assertThat(TypeComparators.deepEquals(string1, string3)).isFalse();
    }

    @Test
    void testTypeComparatorsIsPrimitive() {
        // Given: Primitive and non-primitive types
        io.hexaglue.spi.types.TypeRef intType = PrimitiveRef.of("int");
        io.hexaglue.spi.types.TypeRef voidType = PrimitiveRef.of("void");
        io.hexaglue.spi.types.TypeRef stringType = ClassRef.of("java.lang.String");

        // When/Then: isPrimitive should work (excluding void)
        assertThat(TypeComparators.isPrimitive(intType)).isTrue();
        assertThat(TypeComparators.isPrimitive(voidType)).isFalse();
        assertThat(TypeComparators.isPrimitive(stringType)).isFalse();
    }

    @Test
    void testTypeComparatorsIsVoid() {
        // Given: Void and non-void types
        io.hexaglue.spi.types.TypeRef voidType = PrimitiveRef.of("void");
        io.hexaglue.spi.types.TypeRef intType = PrimitiveRef.of("int");

        // When/Then: isVoid should detect void
        assertThat(TypeComparators.isVoid(voidType)).isTrue();
        assertThat(TypeComparators.isVoid(intType)).isFalse();
    }

    @Test
    void testTypeComparatorsIsNumericPrimitive() {
        // Given: Various primitives
        TypeRef intType = PrimitiveRef.of("int");
        TypeRef doubleType = PrimitiveRef.of("double");
        TypeRef booleanType = PrimitiveRef.of("boolean");

        // When/Then: Should detect numeric primitives
        assertThat(TypeComparators.isNumericPrimitive(intType)).isTrue();
        assertThat(TypeComparators.isNumericPrimitive(doubleType)).isTrue();
        assertThat(TypeComparators.isNumericPrimitive(booleanType)).isFalse();
    }

    @Test
    void testTypeComparatorsIsString() {
        // Given: String and non-string types
        TypeRef stringType = ClassRef.of("java.lang.String");
        TypeRef simpleString = ClassRef.of("String");
        TypeRef objectType = ClassRef.of("java.lang.Object");

        // When/Then: Should detect String types
        assertThat(TypeComparators.isString(stringType)).isTrue();
        assertThat(TypeComparators.isString(simpleString)).isTrue();
        assertThat(TypeComparators.isString(objectType)).isFalse();
    }

    @Test
    void testTypeComparatorsIsObject() {
        // Given: Object and non-object types
        TypeRef objectType = ClassRef.of("java.lang.Object");
        TypeRef simpleObject = ClassRef.of("Object");
        TypeRef stringType = ClassRef.of("java.lang.String");

        // When/Then: Should detect Object types
        assertThat(TypeComparators.isObject(objectType)).isTrue();
        assertThat(TypeComparators.isObject(simpleObject)).isTrue();
        assertThat(TypeComparators.isObject(stringType)).isFalse();
    }

    @Test
    void testTypeComparatorsByRenderedName() {
        // Given: Types to sort
        List<TypeRef> types = List.of(ClassRef.of("Zoo"), ClassRef.of("Apple"), ClassRef.of("Middle"));

        // When: Sort by rendered name
        List<TypeRef> sorted =
                types.stream().sorted(TypeComparators.byRenderedName()).toList();

        // Then: Should be alphabetically sorted
        assertThat(sorted.get(0).name().value()).isEqualTo("Apple");
        assertThat(sorted.get(1).name().value()).isEqualTo("Middle");
        assertThat(sorted.get(2).name().value()).isEqualTo("Zoo");
    }
}
