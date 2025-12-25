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
package io.hexaglue.spi.diagnostics;

import java.util.Objects;

/**
 * Represents a validation issue detected while analyzing the model.
 *
 * <p>This is a lightweight, stable structure that can be used:
 * <ul>
 *   <li>by core to build richer {@link Diagnostic} instances</li>
 *   <li>by plugins to express validation findings before emitting diagnostics</li>
 * </ul>
 *
 * <p>Unlike {@link Diagnostic}, this type is intended to be a compact internal transport object.</p>
 *
 * @param code stable issue code
 * @param severity severity
 * @param message user-facing message
 * @param location optional location (never {@code null}, may be {@link DiagnosticLocation#unknown()})
 */
public record ValidationIssue(
        DiagnosticCode code, DiagnosticSeverity severity, String message, DiagnosticLocation location) {

    public ValidationIssue {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(message, "message");
        location = (location == null) ? DiagnosticLocation.unknown() : location;
        if (message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }

    /**
     * Converts this issue into a {@link Diagnostic}.
     *
     * @param pluginId optional plugin id (nullable)
     * @return diagnostic instance
     */
    public Diagnostic toDiagnostic(String pluginId) {
        return Diagnostic.builder()
                .severity(severity)
                .code(code)
                .message(message)
                .location(location)
                .pluginId(pluginId)
                .build();
    }
}
