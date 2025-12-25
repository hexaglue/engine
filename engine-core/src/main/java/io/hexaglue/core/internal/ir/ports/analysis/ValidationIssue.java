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
package io.hexaglue.core.internal.ir.ports.analysis;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.Objects;

/**
 * Represents a validation issue with an associated severity level.
 *
 * <p>This allows validation rules to distinguish between:
 * <ul>
 *   <li><strong>Errors:</strong> Structural problems that prevent proper analysis or generation</li>
 *   <li><strong>Warnings:</strong> Style recommendations or potential issues that don't block processing</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Critical error - port must be an interface
 * ValidationIssue.error("Port must be an interface, found class")
 *
 * // Style warning - optional naming convention
 * ValidationIssue.warning("Port name should have suffix like Repository, Gateway, etc.")
 * }</pre>
 */
@InternalMarker(reason = "Internal validation infrastructure; not exposed to plugins")
public final class ValidationIssue {

    private final DiagnosticSeverity severity;
    private final String message;

    private ValidationIssue(DiagnosticSeverity severity, String message) {
        this.severity = Objects.requireNonNull(severity, "severity");
        this.message = Objects.requireNonNull(message, "message");
    }

    /**
     * Creates an error-level validation issue.
     *
     * <p>Use for structural problems that prevent proper analysis or generation.
     *
     * @param message error message (not {@code null})
     * @return validation issue with ERROR severity
     */
    public static ValidationIssue error(String message) {
        return new ValidationIssue(DiagnosticSeverity.ERROR, message);
    }

    /**
     * Creates a warning-level validation issue.
     *
     * <p>Use for style recommendations or potential issues that don't block processing.
     *
     * @param message warning message (not {@code null})
     * @return validation issue with WARNING severity
     */
    public static ValidationIssue warning(String message) {
        return new ValidationIssue(DiagnosticSeverity.WARNING, message);
    }

    /**
     * Returns the severity level of this validation issue.
     *
     * @return severity (never {@code null})
     */
    public DiagnosticSeverity severity() {
        return severity;
    }

    /**
     * Returns the message describing this validation issue.
     *
     * @return message (never {@code null})
     */
    public String message() {
        return message;
    }

    /**
     * Returns whether this is an error-level issue.
     *
     * @return {@code true} if severity is ERROR
     */
    public boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }

    /**
     * Returns whether this is a warning-level issue.
     *
     * @return {@code true} if severity is WARNING
     */
    public boolean isWarning() {
        return severity == DiagnosticSeverity.WARNING;
    }

    @Override
    public String toString() {
        return severity + ": " + message;
    }
}
