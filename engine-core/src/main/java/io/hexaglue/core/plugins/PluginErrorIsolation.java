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
package io.hexaglue.core.plugins;

import io.hexaglue.core.context.DebugLog;
import io.hexaglue.core.discovery.DiscoveredPlugin;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.util.Objects;

/**
 * Provides error isolation for plugin execution.
 *
 * <p>
 * This utility ensures that plugins are executed in a controlled environment where unexpected
 * exceptions are caught, logged, and converted to diagnostics. This prevents a misbehaving plugin
 * from crashing the entire compilation.
 * </p>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Fault Tolerance:</strong> One plugin failure must not affect others.</li>
 *   <li><strong>Clear Diagnostics:</strong> Plugin errors are reported as structured diagnostics.</li>
 *   <li><strong>Minimal Overhead:</strong> No performance penalty in the happy path.</li>
 * </ul>
 *
 * <h2>Error Handling Strategy</h2>
 * <p>
 * When a plugin throws an exception during {@link HexaGluePlugin#apply(GenerationContextSpec)}:
 * </p>
 * <ol>
 *   <li>The exception is caught and logged.</li>
 *   <li>A diagnostic with severity {@link DiagnosticSeverity#ERROR} is reported.</li>
 *   <li>The diagnostic includes the plugin id and a user-facing message.</li>
 *   <li>Execution continues with the next plugin.</li>
 * </ol>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * PluginErrorIsolation.execute(discoveredPlugin, context, debugLog);
 * }</pre>
 */
public final class PluginErrorIsolation {

    private PluginErrorIsolation() {
        // utility class
    }

    /**
     * Executes a plugin with full error isolation.
     *
     * <p>
     * If the plugin throws any exception, it is caught, logged, and reported as a diagnostic.
     * The method always returns normally, even if the plugin fails.
     * </p>
     *
     * @param discovered discovered plugin to execute, not {@code null}
     * @param context    generation context, not {@code null}
     * @param debugLog   debug logger, not {@code null}
     */
    public static void execute(DiscoveredPlugin discovered, GenerationContextSpec context, DebugLog debugLog) {
        Objects.requireNonNull(discovered, "discovered");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(debugLog, "debugLog");

        HexaGluePlugin plugin = discovered.plugin();
        String pluginId = discovered.id();

        try {
            plugin.apply(context);
        } catch (Throwable ex) {
            handlePluginError(pluginId, ex, context, debugLog);
        }
    }

    /**
     * Handles a plugin execution error by logging and reporting diagnostics.
     *
     * @param pluginId plugin identifier
     * @param error    caught error
     * @param context  generation context
     * @param debugLog debug logger
     */
    private static void handlePluginError(
            String pluginId, Throwable error, GenerationContextSpec context, DebugLog debugLog) {

        debugLog.note(
                "Plugin " + pluginId + " failed with error: " + error.getClass().getSimpleName(), error);

        String userMessage = buildUserMessage(pluginId, error);

        Diagnostic diagnostic = Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(DiagnosticCode.of("PLUGIN_EXECUTION_FAILED"))
                .message(userMessage)
                .location(DiagnosticLocation.unknown())
                .pluginId(pluginId)
                .cause(error)
                .build();

        context.diagnostics().report(diagnostic);
    }

    /**
     * Builds a user-facing error message from a plugin exception.
     *
     * <p>
     * The message is intentionally concise and avoids exposing stack traces directly to the user.
     * Full diagnostic details (including cause) are available via the diagnostic object.
     * </p>
     *
     * @param pluginId plugin id
     * @param error    caught error
     * @return user-facing message
     */
    private static String buildUserMessage(String pluginId, Throwable error) {
        String errorType = error.getClass().getSimpleName();
        String errorMsg = error.getMessage();

        if (errorMsg != null && !errorMsg.isBlank()) {
            return "Plugin '" + pluginId + "' failed with " + errorType + ": " + errorMsg;
        } else {
            return "Plugin '" + pluginId + "' failed with " + errorType;
        }
    }
}
