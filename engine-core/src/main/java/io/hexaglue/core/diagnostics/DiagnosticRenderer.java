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
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Renders diagnostics to human-readable text formats.
 *
 * <p>
 * This renderer is responsible for formatting diagnostics for display in:
 * <ul>
 *   <li>Compiler output</li>
 *   <li>IDE error lists</li>
 *   <li>Test reports</li>
 *   <li>Logs</li>
 * </ul>
 * </p>
 *
 * <p>
 * The renderer supports both single-line compact format and multi-line detailed format.
 * </p>
 *
 * <h2>Output Formats</h2>
 *
 * <h3>Compact (single-line)</h3>
 * <pre>
 * ERROR INVALID_PORT: Port must be an interface @ com.example.CustomerRepository
 * </pre>
 *
 * <h3>Detailed (multi-line)</h3>
 * <pre>
 * ERROR [INVALID_PORT]
 *   Location: com.example.CustomerRepository @ src/main/java/com/example/CustomerRepository.java:15:8
 *   Plugin: io.hexaglue.plugin.spring
 *   Message: Port must be an interface
 *   Attributes:
 *     expectedType = Interface
 *     foundType = Class
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This renderer is stateless and thread-safe.
 * </p>
 */
public final class DiagnosticRenderer {

    private DiagnosticRenderer() {
        // utility class
    }

    /**
     * Renders a single diagnostic in compact format.
     *
     * <p>
     * Format: {@code SEVERITY CODE: message @ location [pluginId]}
     * </p>
     *
     * @param diagnostic diagnostic to render (not {@code null})
     * @return compact string (never {@code null})
     */
    public static String compact(Diagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");

        StringBuilder sb = new StringBuilder();
        sb.append(diagnostic.severity());
        sb.append(" ");
        sb.append(diagnostic.code().value());
        sb.append(": ");
        sb.append(diagnostic.message());

        DiagnosticLocation loc = diagnostic.location();
        if (loc != null && !loc.isUnknown()) {
            sb.append(" @ ");
            sb.append(renderLocationCompact(loc));
        }

        if (diagnostic.pluginId() != null) {
            sb.append(" [");
            sb.append(diagnostic.pluginId());
            sb.append("]");
        }

        return sb.toString();
    }

    /**
     * Renders a single diagnostic in detailed multi-line format.
     *
     * @param diagnostic diagnostic to render (not {@code null})
     * @return detailed multi-line string (never {@code null})
     */
    public static String detailed(Diagnostic diagnostic) {
        Objects.requireNonNull(diagnostic, "diagnostic");

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(diagnostic.severity())
                .append(" [")
                .append(diagnostic.code().value())
                .append("]\n");

        // Location
        DiagnosticLocation loc = diagnostic.location();
        if (loc != null && !loc.isUnknown()) {
            sb.append("  Location: ").append(renderLocationDetailed(loc)).append("\n");
        }

        // Plugin
        if (diagnostic.pluginId() != null) {
            sb.append("  Plugin: ").append(diagnostic.pluginId()).append("\n");
        }

        // Message
        sb.append("  Message: ").append(diagnostic.message()).append("\n");

        // Attributes
        Map<String, String> attrs = diagnostic.attributes();
        if (attrs != null && !attrs.isEmpty()) {
            sb.append("  Attributes:\n");
            for (Map.Entry<String, String> e : attrs.entrySet()) {
                sb.append("    ")
                        .append(e.getKey())
                        .append(" = ")
                        .append(e.getValue())
                        .append("\n");
            }
        }

        // Cause (debug only)
        if (diagnostic.cause() != null) {
            sb.append("  Cause: ").append(diagnostic.cause().getClass().getSimpleName());
            if (diagnostic.cause().getMessage() != null) {
                sb.append(": ").append(diagnostic.cause().getMessage());
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Renders multiple diagnostics in compact format, one per line.
     *
     * @param diagnostics diagnostics to render (not {@code null})
     * @return multi-line string (never {@code null})
     */
    public static String compactList(List<Diagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");

        if (diagnostics.isEmpty()) {
            return "(no diagnostics)";
        }

        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            if (d != null) {
                sb.append(compact(d)).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Renders multiple diagnostics in detailed format, separated by blank lines.
     *
     * @param diagnostics diagnostics to render (not {@code null})
     * @return multi-line string (never {@code null})
     */
    public static String detailedList(List<Diagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");

        if (diagnostics.isEmpty()) {
            return "(no diagnostics)";
        }

        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : diagnostics) {
            if (d != null) {
                sb.append(detailed(d)).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Renders a summary of diagnostics by severity.
     *
     * <p>
     * Format: {@code X errors, Y warnings, Z infos}
     * </p>
     *
     * @param diagnostics diagnostics to summarize (not {@code null})
     * @return summary string (never {@code null})
     */
    public static String summary(List<Diagnostic> diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");

        long errors = diagnostics.stream()
                .filter(d -> d.severity() == DiagnosticSeverity.ERROR)
                .count();
        long warnings = diagnostics.stream()
                .filter(d -> d.severity() == DiagnosticSeverity.WARNING)
                .count();
        long infos = diagnostics.stream()
                .filter(d -> d.severity() == DiagnosticSeverity.INFO)
                .count();

        StringBuilder sb = new StringBuilder();
        sb.append(errors).append(" error");
        if (errors != 1) sb.append("s");
        sb.append(", ");
        sb.append(warnings).append(" warning");
        if (warnings != 1) sb.append("s");
        sb.append(", ");
        sb.append(infos).append(" info");
        if (infos != 1) sb.append("s");

        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String renderLocationCompact(DiagnosticLocation loc) {
        if (loc.qualifiedName().isPresent()) {
            return loc.qualifiedName().get();
        }
        if (loc.path().isPresent()) {
            StringBuilder sb = new StringBuilder(loc.path().get());
            if (loc.line().isPresent()) {
                sb.append(":").append(loc.line().get());
                if (loc.column().isPresent()) {
                    sb.append(":").append(loc.column().get());
                }
            }
            return sb.toString();
        }
        return "<unknown>";
    }

    private static String renderLocationDetailed(DiagnosticLocation loc) {
        StringBuilder sb = new StringBuilder();
        if (loc.qualifiedName().isPresent()) {
            sb.append(loc.qualifiedName().get());
        }
        if (loc.path().isPresent()) {
            if (sb.length() > 0) {
                sb.append(" @ ");
            }
            sb.append(loc.path().get());
            if (loc.line().isPresent()) {
                sb.append(":").append(loc.line().get());
                if (loc.column().isPresent()) {
                    sb.append(":").append(loc.column().get());
                }
            }
        }
        if (sb.length() == 0) {
            return "<unknown>";
        }
        return sb.toString();
    }
}
