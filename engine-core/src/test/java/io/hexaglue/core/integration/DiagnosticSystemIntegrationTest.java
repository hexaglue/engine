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

import io.hexaglue.core.diagnostics.DiagnosticFactory;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating the contract between Diagnostic interfaces (SPI)
 * and their creation utilities (Core).
 *
 * <p>
 * This test ensures that:
 * <ul>
 *   <li>Diagnostics can be created using core utilities</li>
 *   <li>DiagnosticLocation can be created from various sources</li>
 *   <li>DiagnosticFactory provides convenient creation methods</li>
 *   <li>The diagnostic system provides a stable contract for plugins</li>
 * </ul>
 */
class DiagnosticSystemIntegrationTest {

    private static final String PLUGIN_ID = "test-plugin";

    @Test
    void testDiagnosticBuilderContract() {
        // Given: Diagnostic components
        DiagnosticCode code = DiagnosticCode.of("TEST_ERROR");
        DiagnosticLocation location = DiagnosticLocation.unknown();
        String message = "Test error message";

        // When: Build diagnostic using SPI builder
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(code)
                .message(message)
                .location(location)
                .build();

        // Then: Should have all properties set correctly
        assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(diagnostic.code()).isEqualTo(code);
        assertThat(diagnostic.message()).isEqualTo(message);
        assertThat(diagnostic.location()).isEqualTo(location);
    }

    @Test
    void testDiagnosticCodeCreation() {
        // When: Create diagnostic codes
        DiagnosticCode code1 = DiagnosticCode.of("ERROR_001");
        DiagnosticCode code2 = DiagnosticCode.of("WARNING_002");

        // Then: Should preserve code values
        assertThat(code1.toString()).contains("ERROR_001");
        assertThat(code2.toString()).contains("WARNING_002");
    }

    @Test
    void testDiagnosticLocationFromPath() {
        // When: Create location from path
        DiagnosticLocation location = DiagnosticLocation.ofPath("src/main/java/Example.java", null, null);

        // Then: Should have file information
        assertThat(location.path()).isPresent();
        assertThat(location.path().get()).contains("Example.java");
    }

    @Test
    void testDiagnosticLocationFromPathWithLineNumber() {
        // When: Create location with line number
        DiagnosticLocation location = DiagnosticLocation.ofPath("src/main/java/Example.java", 42, null);

        // Then: Should have file and line information
        assertThat(location.path()).isPresent();
        assertThat(location.line()).isPresent();
        assertThat(location.line().get()).isEqualTo(42);
    }

    @Test
    void testDiagnosticLocationFromPathWithLineAndColumn() {
        // When: Create location with line and column
        DiagnosticLocation location = DiagnosticLocation.ofPath("src/main/java/Example.java", 42, 15);

        // Then: Should have file, line, and column information
        assertThat(location.path()).isPresent();
        assertThat(location.line()).isPresent();
        assertThat(location.line().get()).isEqualTo(42);
        assertThat(location.column()).isPresent();
        assertThat(location.column().get()).isEqualTo(15);
    }

    @Test
    void testDiagnosticLocationUnknown() {
        // When: Create unknown location
        DiagnosticLocation location = DiagnosticLocation.unknown();

        // Then: Should have no file information
        assertThat(location.path()).isEmpty();
        assertThat(location.line()).isEmpty();
        assertThat(location.column()).isEmpty();
        assertThat(location.isUnknown()).isTrue();
    }

    @Test
    void testDiagnosticFactoryError() {
        // Given: Error components
        DiagnosticCode code = DiagnosticCode.of("INVALID_PORT");
        String message = "Port must be an interface";
        DiagnosticLocation location = DiagnosticLocation.unknown();

        // When: Create error using factory
        Diagnostic error = DiagnosticFactory.error(code, message, location, PLUGIN_ID);

        // Then: Should be an ERROR diagnostic
        assertThat(error.severity()).isEqualTo(DiagnosticSeverity.ERROR);
        assertThat(error.code()).isEqualTo(code);
        assertThat(error.message()).isEqualTo(message);
        assertThat(error.location()).isEqualTo(location);
    }

    @Test
    void testDiagnosticFactoryWarning() {
        // Given: Warning components
        DiagnosticCode code = DiagnosticCode.of("DEPRECATED_API");
        String message = "This API is deprecated";
        DiagnosticLocation location = DiagnosticLocation.unknown();

        // When: Create warning using factory
        Diagnostic warning = DiagnosticFactory.warning(code, message, location, PLUGIN_ID);

        // Then: Should be a WARNING diagnostic
        assertThat(warning.severity()).isEqualTo(DiagnosticSeverity.WARNING);
        assertThat(warning.code()).isEqualTo(code);
        assertThat(warning.message()).isEqualTo(message);
    }

    @Test
    void testDiagnosticFactoryInfo() {
        // Given: Info components
        DiagnosticCode code = DiagnosticCode.of("GENERATION_INFO");
        String message = "Generated 10 files";
        DiagnosticLocation location = DiagnosticLocation.unknown();

        // When: Create info diagnostic using factory
        Diagnostic info = DiagnosticFactory.info(code, message, location, PLUGIN_ID);

        // Then: Should be an INFO diagnostic
        assertThat(info.severity()).isEqualTo(DiagnosticSeverity.INFO);
        assertThat(info.code()).isEqualTo(code);
        assertThat(info.message()).isEqualTo(message);
    }

    @Test
    void testDiagnosticSeverityOrdering() {
        // Then: Severities should have natural ordering
        assertThat(DiagnosticSeverity.ERROR.ordinal()).isGreaterThan(DiagnosticSeverity.WARNING.ordinal());
        assertThat(DiagnosticSeverity.WARNING.ordinal()).isGreaterThan(DiagnosticSeverity.INFO.ordinal());
    }

    @Test
    void testDiagnosticBuilderRequiredFields() {
        // When/Then: Should require all mandatory fields
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> Diagnostic.builder()
                        .code(DiagnosticCode.of("TEST"))
                        .message("message")
                        .location(DiagnosticLocation.unknown())
                        .build() // Missing severity
                );

        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> Diagnostic.builder()
                        .severity(DiagnosticSeverity.ERROR)
                        .message("message")
                        .location(DiagnosticLocation.unknown())
                        .build() // Missing code
                );

        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> Diagnostic.builder()
                        .severity(DiagnosticSeverity.ERROR)
                        .code(DiagnosticCode.of("TEST"))
                        .location(DiagnosticLocation.unknown())
                        .build() // Missing message
                );

        // Location is optional - if not set, defaults to unknown()
        Diagnostic withoutLocation = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(DiagnosticCode.of("TEST"))
                .message("message")
                .build();
        assertThat(withoutLocation.location()).isNotNull();
        assertThat(withoutLocation.location().isUnknown()).isTrue();
    }

    @Test
    void testDiagnosticWithAttributes() {
        // Given: Diagnostic with attributes
        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(DiagnosticCode.of("TEST"))
                .message("Error message")
                .attribute("description", "Detailed description of the error")
                .attribute("suggestion", "Try this instead")
                .location(DiagnosticLocation.unknown())
                .build();

        // Then: Should have attributes
        assertThat(diagnostic.attributes()).isNotEmpty();
        assertThat(diagnostic.attributes()).containsEntry("description", "Detailed description of the error");
        assertThat(diagnostic.attributes()).containsEntry("suggestion", "Try this instead");
    }
}
