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

import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.diagnostics.ValidationIssue;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating diagnostics SPI contracts.
 *
 * <p>Tests the diagnostic reporting system including diagnostic codes,
 * locations, severity levels, and validation issues.</p>
 */
class DiagnosticsIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // DiagnosticSeverity Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDiagnosticSeverityEnum() {
        // When: Access severity enum values
        assertThat(DiagnosticSeverity.values()).hasLength(3);
        assertThat(DiagnosticSeverity.INFO).isNotNull();
        assertThat(DiagnosticSeverity.WARNING).isNotNull();
        assertThat(DiagnosticSeverity.ERROR).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DiagnosticCode Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDiagnosticCodeCreation() {
        // When: Create diagnostic code
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-1001");

        // Then: Should have correct value
        assertThat(code).isNotNull();
        assertThat(code.value()).isEqualTo("HG-CORE-1001");
        assertThat(code.toString()).isEqualTo("HG-CORE-1001");
    }

    @Test
    void testDiagnosticCodeTrimming() {
        // When: Create code with whitespace
        DiagnosticCode code = DiagnosticCode.of("  HG-CORE-1001  ");

        // Then: Should trim value
        assertThat(code.value()).isEqualTo("HG-CORE-1001");
    }

    @Test
    void testDiagnosticCodeEquality() {
        // Given: Two codes with same value
        DiagnosticCode code1 = DiagnosticCode.of("HG-CORE-1001");
        DiagnosticCode code2 = DiagnosticCode.of("HG-CORE-1001");

        // Then: Should be equal
        assertThat(code1).isEqualTo(code2);
        assertThat(code1.hashCode()).isEqualTo(code2.hashCode());
    }

    @Test
    void testDiagnosticCodeRejectsBlank() {
        // When/Then: Should reject blank code
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> DiagnosticCode.of("   "));
    }

    @Test
    void testDiagnosticCodeRejectsNull() {
        // When/Then: Should reject null code
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> DiagnosticCode.of(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DiagnosticLocation Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDiagnosticLocationUnknown() {
        // When: Create unknown location
        DiagnosticLocation location = DiagnosticLocation.unknown();

        // Then: Should have no information
        assertThat(location.isUnknown()).isTrue();
        assertThat(location.qualifiedName().isPresent()).isFalse();
        assertThat(location.path().isPresent()).isFalse();
        assertThat(location.line().isPresent()).isFalse();
        assertThat(location.column().isPresent()).isFalse();
        assertThat(location.toString()).isEqualTo("<unknown>");
    }

    @Test
    void testDiagnosticLocationOfQualifiedName() {
        // When: Create location by qualified name
        DiagnosticLocation location = DiagnosticLocation.ofQualifiedName("com.example.Customer");

        // Then: Should have qualified name only
        assertThat(location.isUnknown()).isFalse();
        assertThat(location.qualifiedName().isPresent()).isTrue();
        assertThat(location.qualifiedName().get()).isEqualTo("com.example.Customer");
        assertThat(location.path().isPresent()).isFalse();
        assertThat(location.toString()).isEqualTo("com.example.Customer");
    }

    @Test
    void testDiagnosticLocationOfPath() {
        // When: Create location by path
        DiagnosticLocation location = DiagnosticLocation.ofPath("src/main/java/Foo.java", 42, 15);

        // Then: Should have path, line, column
        assertThat(location.isUnknown()).isFalse();
        assertThat(location.path().isPresent()).isTrue();
        assertThat(location.path().get()).isEqualTo("src/main/java/Foo.java");
        assertThat(location.line().isPresent()).isTrue();
        assertThat(location.line().get()).isEqualTo(42);
        assertThat(location.column().isPresent()).isTrue();
        assertThat(location.column().get()).isEqualTo(15);
        assertThat(location.toString()).isEqualTo("src/main/java/Foo.java:42:15");
    }

    @Test
    void testDiagnosticLocationOfPathWithNullLineColumn() {
        // When: Create location with path but no line/column
        DiagnosticLocation location = DiagnosticLocation.ofPath("src/main/java/Foo.java", null, null);

        // Then: Should have path only
        assertThat(location.path().isPresent()).isTrue();
        assertThat(location.line().isPresent()).isFalse();
        assertThat(location.column().isPresent()).isFalse();
        assertThat(location.toString()).isEqualTo("src/main/java/Foo.java");
    }

    @Test
    void testDiagnosticLocationComplete() {
        // When: Create complete location
        DiagnosticLocation location =
                DiagnosticLocation.of("com.example.Customer", "src/main/java/Customer.java", 100, 5);

        // Then: Should have all information
        assertThat(location.qualifiedName().isPresent()).isTrue();
        assertThat(location.qualifiedName().get()).isEqualTo("com.example.Customer");
        assertThat(location.path().isPresent()).isTrue();
        assertThat(location.path().get()).isEqualTo("src/main/java/Customer.java");
        assertThat(location.line().isPresent()).isTrue();
        assertThat(location.line().get()).isEqualTo(100);
        assertThat(location.column().isPresent()).isTrue();
        assertThat(location.column().get()).isEqualTo(5);
        assertThat(location.toString()).isEqualTo("com.example.Customer @ src/main/java/Customer.java:100:5");
    }

    @Test
    void testDiagnosticLocationNormalizesBlankToNull() {
        // When: Create location with blank strings
        DiagnosticLocation location = DiagnosticLocation.of("   ", "   ", null, null);

        // Then: Should normalize to unknown
        assertThat(location.isUnknown()).isTrue();
    }

    @Test
    void testDiagnosticLocationIgnoresInvalidLineColumn() {
        // When: Create location with invalid line/column (zero or negative)
        DiagnosticLocation location = DiagnosticLocation.ofPath("file.java", 0, -1);

        // Then: Should ignore invalid values
        assertThat(location.line().isPresent()).isFalse();
        assertThat(location.column().isPresent()).isFalse();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diagnostic Builder Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDiagnosticBuilder() {
        // Given: Diagnostic components
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-1001");
        DiagnosticLocation location = DiagnosticLocation.ofQualifiedName("com.example.Customer");

        // When: Build diagnostic
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(code)
                .message("Port method must return a value")
                .location(location)
                .pluginId("my-plugin")
                .build();

        // Then: Should have all properties
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(diagnostic.code()).isEqualTo(code);
        assertThat(diagnostic.message()).isEqualTo("Port method must return a value");
        assertThat(diagnostic.location()).isEqualTo(location);
        assertThat(diagnostic.pluginId()).isEqualTo("my-plugin");
        assertThat(diagnostic.attributes()).isEmpty();
        assertThat(diagnostic.cause()).isNull();
    }

    @Test
    void testDiagnosticBuilderWithDefaults() {
        // When: Build minimal diagnostic
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.WARNING)
                .code(DiagnosticCode.of("HG-CORE-2001"))
                .message("Unused parameter")
                .build();

        // Then: Should have defaults
        assertThat(diagnostic.location()).isNotNull();
        assertThat(diagnostic.location().isUnknown()).isTrue();
        assertThat(diagnostic.pluginId()).isNull();
        assertThat(diagnostic.attributes()).isEmpty();
    }

    @Test
    void testDiagnosticBuilderWithAttributes() {
        // When: Build diagnostic with attributes
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(DiagnosticCode.of("HG-CORE-3001"))
                .message("Type mismatch")
                .attribute("expectedType", "CustomerId")
                .attribute("foundType", "String")
                .build();

        // Then: Should have attributes
        assertThat(diagnostic.attributes()).hasSize(2);
        assertThat(diagnostic.attributes().get("expectedType")).isEqualTo("CustomerId");
        assertThat(diagnostic.attributes().get("foundType")).isEqualTo("String");
    }

    @Test
    void testDiagnosticBuilderWithAttributesMap() {
        // Given: Attributes map
        Map<String, String> attrs = Map.of("key1", "value1", "key2", "value2");

        // When: Build diagnostic with attributes map
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.INFO)
                .code(DiagnosticCode.of("HG-CORE-4001"))
                .message("Info message")
                .attributes(attrs)
                .build();

        // Then: Should have attributes
        assertThat(diagnostic.attributes()).hasSize(2);
        assertThat(diagnostic.attributes().get("key1")).isEqualTo("value1");
    }

    @Test
    void testDiagnosticBuilderWithCause() {
        // Given: Exception cause
        Exception cause = new RuntimeException("Original error");

        // When: Build diagnostic with cause
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(DiagnosticCode.of("HG-CORE-5001"))
                .message("Internal error")
                .cause(cause)
                .build();

        // Then: Should have cause
        assertThat(diagnostic.cause()).isEqualTo(cause);
    }

    @Test
    void testDiagnosticBuilderRejectsMissingSeverity() {
        // When/Then: Should reject missing severity
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> Diagnostic.builder()
                .code(DiagnosticCode.of("HG-CORE-1001"))
                .message("message")
                .build());
    }

    @Test
    void testDiagnosticBuilderRejectsMissingCode() {
        // When/Then: Should reject missing code
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .message("message")
                .build());
    }

    @Test
    void testDiagnosticBuilderRejectsBlankMessage() {
        // When/Then: Should reject blank message
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(DiagnosticCode.of("HG-CORE-1001"))
                .message("   ")
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Diagnostic Convenience Methods Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDiagnosticErrorConvenience() {
        // When: Create error diagnostic
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-1001");
        Diagnostic diagnostic = Diagnostic.error(code, "Error message");

        // Then: Should be error
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(diagnostic.code()).isEqualTo(code);
        assertThat(diagnostic.message()).isEqualTo("Error message");
    }

    @Test
    void testDiagnosticWarningConvenience() {
        // When: Create warning diagnostic
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-2001");
        Diagnostic diagnostic = Diagnostic.warning(code, "Warning message");

        // Then: Should be warning
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.WARNING);
        assertThat(diagnostic.code()).isEqualTo(code);
        assertThat(diagnostic.message()).isEqualTo("Warning message");
    }

    @Test
    void testDiagnosticInfoConvenience() {
        // When: Create info diagnostic
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-3001");
        Diagnostic diagnostic = Diagnostic.info(code, "Info message");

        // Then: Should be info
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.INFO);
        assertThat(diagnostic.code()).isEqualTo(code);
        assertThat(diagnostic.message()).isEqualTo("Info message");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ValidationIssue Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testValidationIssueCreation() {
        // Given: Issue components
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-1001");
        DiagnosticLocation location = DiagnosticLocation.ofQualifiedName("com.example.Foo");

        // When: Create validation issue
        ValidationIssue issue = new ValidationIssue(code, DiagnosticSeverity.ERROR, "Validation failed", location);

        // Then: Should have correct properties
        assertThat(issue.code()).isEqualTo(code);
        assertThat(issue.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(issue.message()).isEqualTo("Validation failed");
        assertThat(issue.location()).isEqualTo(location);
    }

    @Test
    void testValidationIssueWithNullLocation() {
        // When: Create issue with null location
        ValidationIssue issue =
                new ValidationIssue(DiagnosticCode.of("HG-CORE-1001"), DiagnosticSeverity.WARNING, "Warning", null);

        // Then: Should default to unknown location
        assertThat(issue.location()).isNotNull();
        assertThat(issue.location().isUnknown()).isTrue();
    }

    @Test
    void testValidationIssueToDiagnostic() {
        // Given: Validation issue
        DiagnosticCode code = DiagnosticCode.of("HG-CORE-1001");
        ValidationIssue issue = new ValidationIssue(
                code,
                DiagnosticSeverity.ERROR,
                "Validation error",
                DiagnosticLocation.ofQualifiedName("com.example.Foo"));

        // When: Convert to diagnostic
        Diagnostic diagnostic = issue.toDiagnostic("my-plugin");

        // Then: Should create diagnostic with plugin id
        assertThat(diagnostic.code()).isEqualTo(code);
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(diagnostic.message()).isEqualTo("Validation error");
        assertThat(diagnostic.pluginId()).isEqualTo("my-plugin");
        assertThat(diagnostic.location().qualifiedName().get()).isEqualTo("com.example.Foo");
    }

    @Test
    void testValidationIssueRejectsBlankMessage() {
        // When/Then: Should reject blank message
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ValidationIssue(
                        DiagnosticCode.of("HG-CORE-1001"),
                        DiagnosticSeverity.ERROR,
                        "   ",
                        DiagnosticLocation.unknown()));
    }

    @Test
    void testValidationIssueRejectsNullCode() {
        // When/Then: Should reject null code
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new ValidationIssue(null, DiagnosticSeverity.ERROR, "message", DiagnosticLocation.unknown()));
    }

    @Test
    void testValidationIssueRejectsNullSeverity() {
        // When/Then: Should reject null severity
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> new ValidationIssue(
                        DiagnosticCode.of("HG-CORE-1001"), null, "message", DiagnosticLocation.unknown()));
    }
}
