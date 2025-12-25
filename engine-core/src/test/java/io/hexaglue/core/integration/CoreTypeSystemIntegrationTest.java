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

import io.hexaglue.core.types.model.ArrayTypeRef;
import io.hexaglue.core.types.model.ClassTypeRef;
import io.hexaglue.core.types.model.ParameterizedTypeRef;
import io.hexaglue.core.types.model.PrimitiveTypeRef;
import io.hexaglue.core.types.model.TypeVariableTypeRef;
import io.hexaglue.core.types.model.WildcardTypeRef;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating Core type model implementations.
 *
 * <p>Tests the Core implementations (PrimitiveTypeRef, ClassTypeRef, etc.)
 * and their conversion to SPI types.</p>
 */
class CoreTypeSystemIntegrationTest {

    @Test
    void testPrimitiveTypeRefCreation() {
        PrimitiveTypeRef intType = PrimitiveTypeRef.of("int");
        assertThat(intType).isNotNull();
        assertThat(intType.name().value()).isEqualTo("int");
        assertThat(intType.nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    @Test
    void testPrimitiveTypeRefToSpiType() {
        PrimitiveTypeRef intType = PrimitiveTypeRef.of("int");
        TypeRef spiType = intType.toSpiType();

        assertThat(spiType).isNotNull();
        assertThat(spiType.kind()).isEqualTo(TypeKind.PRIMITIVE);
        assertThat(spiType.name().value()).isEqualTo("int");
    }

    @Test
    void testPrimitiveTypeRefWithNullability() {
        PrimitiveTypeRef intType = PrimitiveTypeRef.of("int");
        PrimitiveTypeRef nonnull = intType.withNullability(Nullability.NONNULL);

        assertThat(nonnull.nullability()).isEqualTo(Nullability.NONNULL);
        assertThat(nonnull.name().value()).isEqualTo("int");
    }

    @Test
    void testClassTypeRefCreation() {
        ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
        assertThat(stringType).isNotNull();
        assertThat(stringType.qualifiedName()).isPresent();
        assertThat(stringType.qualifiedName().get()).isEqualTo("java.lang.String");
        assertThat(stringType.simpleName()).isEqualTo("String");
        assertThat(stringType.packageName()).isPresent();
        assertThat(stringType.packageName().get()).isEqualTo("java.lang");
    }

    @Test
    void testClassTypeRefToSpiType() {
        ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
        TypeRef spiType = stringType.toSpiType();

        assertThat(spiType).isNotNull();
        assertThat(spiType.kind()).isEqualTo(TypeKind.CLASS);
        assertThat(spiType.name().value()).isEqualTo("java.lang.String");
    }

    @Test
    void testClassTypeRefWithNullability() {
        ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
        ClassTypeRef nullable = stringType.withNullability(Nullability.NULLABLE);

        assertThat(nullable.nullability()).isEqualTo(Nullability.NULLABLE);
        assertThat(nullable.qualifiedName()).isPresent();
        assertThat(nullable.qualifiedName().get()).isEqualTo("java.lang.String");
    }

    @Test
    void testArrayTypeRefCreation() {
        PrimitiveTypeRef intType = PrimitiveTypeRef.of("int");
        ArrayTypeRef intArray = ArrayTypeRef.of(intType);

        assertThat(intArray).isNotNull();
        assertThat(intArray.componentType()).isEqualTo(intType);
        assertThat(intArray.nullability()).isEqualTo(Nullability.UNSPECIFIED);
    }

    @Test
    void testArrayTypeRefToSpiType() {
        ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
        ArrayTypeRef stringArray = ArrayTypeRef.of(stringType);

        assertThat(stringArray.toSpiType()).isNotNull();
        assertThat(stringArray.toSpiType().kind()).isEqualTo(TypeKind.ARRAY);
    }

    @Test
    void testParameterizedTypeRefCreation() {
        ClassTypeRef listRaw = ClassTypeRef.of("java.util.List");
        ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");

        ParameterizedTypeRef listOfString = ParameterizedTypeRef.of(listRaw, List.of(stringType));

        assertThat(listOfString).isNotNull();
        assertThat(listOfString.rawType()).isEqualTo(listRaw);
        assertThat(listOfString.typeArguments()).hasSize(1);
        assertThat(listOfString.typeArguments().get(0)).isEqualTo(stringType);
    }

    @Test
    void testParameterizedTypeRefToSpiType() {
        ClassTypeRef listRaw = ClassTypeRef.of("java.util.List");
        ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
        ParameterizedTypeRef listOfString = ParameterizedTypeRef.of(listRaw, List.of(stringType));

        assertThat(listOfString.toSpiType()).isNotNull();
        assertThat(listOfString.toSpiType().kind()).isEqualTo(TypeKind.PARAMETERIZED);
    }

    @Test
    void testTypeVariableTypeRefCreation() {
        TypeVariableTypeRef tVar = TypeVariableTypeRef.of("T");

        assertThat(tVar).isNotNull();
        assertThat(tVar.name().value()).isEqualTo("T");
        assertThat(tVar.bounds()).isEmpty();
    }

    @Test
    void testTypeVariableTypeRefWithBounds() {
        ClassTypeRef numberType = ClassTypeRef.of("java.lang.Number");
        TypeVariableTypeRef tVar = TypeVariableTypeRef.of("T", List.of(numberType));

        assertThat(tVar.bounds()).hasSize(1);
        assertThat(tVar.bounds().get(0)).isEqualTo(numberType);
    }

    @Test
    void testWildcardTypeRefUnbounded() {
        WildcardTypeRef wildcard = WildcardTypeRef.unbounded();

        assertThat(wildcard).isNotNull();
        assertThat(wildcard.upperBound()).isEmpty();
        assertThat(wildcard.lowerBound()).isEmpty();
    }

    @Test
    void testWildcardTypeRefExtends() {
        ClassTypeRef numberType = ClassTypeRef.of("java.lang.Number");
        WildcardTypeRef wildcard = WildcardTypeRef.extendsBound(numberType);

        assertThat(wildcard.upperBound()).isPresent();
        assertThat(wildcard.upperBound().get()).isEqualTo(numberType);
        assertThat(wildcard.lowerBound()).isEmpty();
    }

    @Test
    void testWildcardTypeRefSuper() {
        ClassTypeRef integerType = ClassTypeRef.of("java.lang.Integer");
        WildcardTypeRef wildcard = WildcardTypeRef.superBound(integerType);

        assertThat(wildcard.lowerBound()).isPresent();
        assertThat(wildcard.lowerBound().get()).isEqualTo(integerType);
        assertThat(wildcard.upperBound()).isEmpty();
    }

    @Test
    void testWildcardTypeRefToSpiType() {
        WildcardTypeRef wildcard = WildcardTypeRef.unbounded();

        assertThat(wildcard.toSpiType()).isNotNull();
        assertThat(wildcard.toSpiType().kind()).isEqualTo(TypeKind.WILDCARD);
    }
}
