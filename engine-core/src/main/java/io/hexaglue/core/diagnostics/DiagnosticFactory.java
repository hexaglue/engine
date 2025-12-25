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
package io.hexaglue.core.diagnostics;

import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.Objects;
import javax.lang.model.element.Element;

/**
 * Factory for creating common diagnostic patterns.
 *
 * <p>
 * This factory provides convenience methods to construct diagnostics with common attributes,
 * reducing boilerplate and ensuring consistency across the codebase.
 * </p>
 *
 * <h2>Design</h2>
 * <p>
 * The factory intentionally does not maintain state; all methods are static. This design
 * encourages explicit passing of context (plugin id, location) at each call site, improving
 * traceability.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Diagnostic diag = DiagnosticFactory.error(
 *   DiagnosticCode.of("INVALID_PORT"),
 *   "Port must be an interface",
 *   element,
 *   "io.hexaglue.plugin.spring"
 * );
 * }</pre>
 */
public final class DiagnosticFactory {

    private DiagnosticFactory() {
        // utility class
    }

    /**
     * Creates an error diagnostic with element-based location.
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param element   source element (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic error(DiagnosticCode code, String message, Element element, String pluginId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(element, "element");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(code)
                .message(message)
                .location(Locations.fromElement(element))
                .pluginId(pluginId)
                .build();
    }

    /**
     * Creates an error diagnostic with explicit location.
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param location  diagnostic location (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic error(DiagnosticCode code, String message, DiagnosticLocation location, String pluginId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(location, "location");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(code)
                .message(message)
                .location(location)
                .pluginId(pluginId)
                .build();
    }

    /**
     * Creates a warning diagnostic with element-based location.
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param element   source element (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic warning(DiagnosticCode code, String message, Element element, String pluginId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(element, "element");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.WARNING)
                .code(code)
                .message(message)
                .location(Locations.fromElement(element))
                .pluginId(pluginId)
                .build();
    }

    /**
     * Creates a warning diagnostic with explicit location.
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param location  diagnostic location (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic warning(
            DiagnosticCode code, String message, DiagnosticLocation location, String pluginId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(location, "location");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.WARNING)
                .code(code)
                .message(message)
                .location(location)
                .pluginId(pluginId)
                .build();
    }

    /**
     * Creates an info diagnostic with element-based location.
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param element   source element (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic info(DiagnosticCode code, String message, Element element, String pluginId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(element, "element");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.INFO)
                .code(code)
                .message(message)
                .location(Locations.fromElement(element))
                .pluginId(pluginId)
                .build();
    }

    /**
     * Creates an info diagnostic with explicit location.
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param location  diagnostic location (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic info(DiagnosticCode code, String message, DiagnosticLocation location, String pluginId) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(location, "location");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.INFO)
                .code(code)
                .message(message)
                .location(location)
                .pluginId(pluginId)
                .build();
    }

    /**
     * Creates a diagnostic from a validation issue.
     *
     * @param issue     validation issue (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic fromValidationIssue(io.hexaglue.spi.diagnostics.ValidationIssue issue, String pluginId) {
        Objects.requireNonNull(issue, "issue");
        return issue.toDiagnostic(pluginId);
    }

    /**
     * Creates an error diagnostic with cause and element-based location.
     *
     * <p>
     * The cause is attached for debugging purposes but should not be exposed to end users
     * by default.
     * </p>
     *
     * @param code      diagnostic code (not {@code null})
     * @param message   user-facing message (not blank)
     * @param element   source element (not {@code null})
     * @param pluginId  optional plugin id (nullable)
     * @param cause     throwable cause (not {@code null})
     * @return diagnostic (never {@code null})
     */
    public static Diagnostic errorWithCause(
            DiagnosticCode code, String message, Element element, String pluginId, Throwable cause) {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(cause, "cause");

        return Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(code)
                .message(message)
                .location(Locations.fromElement(element))
                .pluginId(pluginId)
                .cause(cause)
                .build();
    }

    /**
     * Creates a diagnostic builder pre-configured with plugin id.
     *
     * <p>
     * This method is useful when multiple diagnostics need to be created with the same plugin id.
     * </p>
     *
     * @param pluginId plugin id (nullable)
     * @return builder (never {@code null})
     */
    public static Diagnostic.Builder builder(String pluginId) {
        return Diagnostic.builder().pluginId(pluginId);
    }
}
