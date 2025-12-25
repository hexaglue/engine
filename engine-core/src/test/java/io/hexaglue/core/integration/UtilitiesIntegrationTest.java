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

import io.hexaglue.spi.util.Preconditions;
import io.hexaglue.spi.util.Strings;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating SPI utility classes (Preconditions and Strings).
 *
 * <p>
 * This test ensures that:
 * <ul>
 *   <li>Preconditions utility provides validation methods</li>
 *   <li>Strings utility provides string manipulation methods</li>
 *   <li>All utility methods behave correctly</li>
 * </ul>
 * </p>
 */
class UtilitiesIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Preconditions Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testPreconditionsCheckArgument() {
        // When/Then: Valid argument passes
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Preconditions.checkArgument(true, "Should pass"));

        // When/Then: Invalid argument throws
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkArgument(false, "Should fail"));
    }

    @Test
    void testPreconditionsCheckArgumentWithSupplier() {
        // When/Then: Valid argument doesn't call supplier
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> Preconditions.checkArgument(true, () -> "Should not be called"));

        // When/Then: Invalid argument throws with supplier message
        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkArgument(false, () -> "Supplier message"));
        assertThat(ex.getMessage()).isEqualTo("Supplier message");
    }

    @Test
    void testPreconditionsCheckState() {
        // When/Then: Valid state passes
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Preconditions.checkState(true, "Valid state"));

        // When/Then: Invalid state throws
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> Preconditions.checkState(false, "Invalid state"));
    }

    @Test
    void testPreconditionsCheckStateWithSupplier() {
        // When/Then: Valid state doesn't call supplier
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> Preconditions.checkState(true, () -> "Should not be called"));

        // When/Then: Invalid state throws with supplier message
        IllegalStateException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> Preconditions.checkState(false, () -> "State error"));
        assertThat(ex.getMessage()).isEqualTo("State error");
    }

    @Test
    void testPreconditionsCheckIndex() {
        // When/Then: Valid indices pass
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Preconditions.checkIndex(0, 10, "idx"));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Preconditions.checkIndex(5, 10, "idx"));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> Preconditions.checkIndex(9, 10, "idx"));

        // When/Then: Negative index throws
        org.junit.jupiter.api.Assertions.assertThrows(
                IndexOutOfBoundsException.class, () -> Preconditions.checkIndex(-1, 10, "idx"));

        // When/Then: Index >= size throws
        org.junit.jupiter.api.Assertions.assertThrows(
                IndexOutOfBoundsException.class, () -> Preconditions.checkIndex(10, 10, "idx"));

        // When/Then: Negative size throws
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> Preconditions.checkIndex(0, -1, "idx"));
    }

    @Test
    void testPreconditionsRequireNonBlank() {
        // When: Valid non-blank string
        String result = Preconditions.requireNonBlank("  hello  ", "value");

        // Then: Should trim and return
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void testPreconditionsRequireNonBlankThrowsOnNull() {
        // When/Then: Null throws NullPointerException
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> Preconditions.requireNonBlank(null, "value"));
    }

    @Test
    void testPreconditionsRequireNonBlankThrowsOnBlank() {
        // When/Then: Blank throws IllegalArgumentException
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> Preconditions.requireNonBlank("   ", "value"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Strings Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testStringsIsBlank() {
        // When/Then: Blank detection
        assertThat(Strings.isBlank(null)).isTrue();
        assertThat(Strings.isBlank("")).isTrue();
        assertThat(Strings.isBlank("   ")).isTrue();

        assertThat(Strings.isBlank("hello")).isFalse();
        assertThat(Strings.isBlank("  hello  ")).isFalse();
    }

    @Test
    void testStringsTrimToNull() {
        // When/Then: Trim to null behavior
        assertThat(Strings.trimToNull(null)).isNull();
        assertThat(Strings.trimToNull("")).isNull();
        assertThat(Strings.trimToNull("   ")).isNull();

        assertThat(Strings.trimToNull("  hello  ")).isEqualTo("hello");
        assertThat(Strings.trimToNull("world")).isEqualTo("world");
    }

    @Test
    void testStringsTrimToEmpty() {
        // When/Then: Trim to empty behavior
        assertThat(Strings.trimToEmpty(null)).isEqualTo("");
        assertThat(Strings.trimToEmpty("")).isEqualTo("");
        assertThat(Strings.trimToEmpty("   ")).isEqualTo("");

        assertThat(Strings.trimToEmpty("  hello  ")).isEqualTo("hello");
        assertThat(Strings.trimToEmpty("world")).isEqualTo("world");
    }

    @Test
    void testStringsToLowerCamel() {
        // When: Convert to lower camel case
        String result1 = Strings.toLowerCamel("CustomerId");
        String result2 = Strings.toLowerCamel("customer_id");
        String result3 = Strings.toLowerCamel("customer-id");

        // Then: Should convert to lowerCamelCase
        assertThat(result1).isEqualTo("customerId");
        assertThat(result2).isEqualTo("customerId");
        assertThat(result3).isEqualTo("customerId");
    }

    @Test
    void testStringsToPascalCase() {
        // When: Convert to PascalCase
        String result1 = Strings.toPascalCase("customer_id");
        String result2 = Strings.toPascalCase("customer-id");
        String result3 = Strings.toPascalCase("Customer id");

        // Then: Should convert to PascalCase
        assertThat(result1).isEqualTo("CustomerId");
        assertThat(result2).isEqualTo("CustomerId");
        assertThat(result3).isEqualTo("CustomerId");
    }

    @Test
    void testStringsCapitalize() {
        // When: Capitalize strings
        String result1 = Strings.capitalize("hello");
        String result2 = Strings.capitalize("World");
        String result3 = Strings.capitalize("a");

        // Then: Should capitalize first character
        assertThat(result1).isEqualTo("Hello");
        assertThat(result2).isEqualTo("World");
        assertThat(result3).isEqualTo("A");
    }

    @Test
    void testStringsDecapitalize() {
        // When: Decapitalize strings
        String result1 = Strings.decapitalize("Hello");
        String result2 = Strings.decapitalize("world");
        String result3 = Strings.decapitalize("A");

        // Then: Should decapitalize first character
        assertThat(result1).isEqualTo("hello");
        assertThat(result2).isEqualTo("world");
        assertThat(result3).isEqualTo("a");
    }

    @Test
    void testStringsEnsureStartsWith() {
        // When: Ensure strings start with prefix
        String result1 = Strings.ensureStartsWith("world", "hello");
        String result2 = Strings.ensureStartsWith("helloworld", "hello");

        // Then: Should add prefix if missing
        assertThat(result1).isEqualTo("helloworld");
        assertThat(result2).isEqualTo("helloworld");
    }

    @Test
    void testStringsEnsureEndsWith() {
        // When: Ensure strings end with suffix
        String result1 = Strings.ensureEndsWith("hello", "world");
        String result2 = Strings.ensureEndsWith("helloworld", "world");

        // Then: Should add suffix if missing
        assertThat(result1).isEqualTo("helloworld");
        assertThat(result2).isEqualTo("helloworld");
    }
}
