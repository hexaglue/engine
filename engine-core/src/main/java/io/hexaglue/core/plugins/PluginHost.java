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
import io.hexaglue.core.lifecycle.PluginExecutionPlan;
import io.hexaglue.spi.context.GenerationContextSpec;
import java.util.Objects;

/**
 * Orchestrates plugin execution for a compilation round.
 *
 * <p>
 * The plugin host is responsible for:
 * <ul>
 *   <li>Iterating through the plugin execution plan in deterministic order</li>
 *   <li>Delegating to {@link PluginErrorIsolation} for fault-tolerant execution</li>
 *   <li>Using {@link PluginContextBridge} to provide SPI-facing contexts to plugins</li>
 *   <li>Ensuring that a failing plugin does not break the compilation</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Isolation:</strong> Each plugin runs independently; errors are contained.</li>
 *   <li><strong>Determinism:</strong> Plugin execution order is stable across runs.</li>
 *   <li><strong>Observability:</strong> Execution is logged for debugging and diagnostics.</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * PluginHost host = new PluginHost(plan, debugLog);
 * host.executeAll(context);
 * }</pre>
 *
 * <h2>Threading</h2>
 * <p>
 * This class is not thread-safe. Plugin execution is expected to be sequential within a single
 * annotation processing round. Future implementations may introduce parallel execution with
 * proper synchronization.
 * </p>
 */
public final class PluginHost {

    private final PluginExecutionPlan plan;
    private final DebugLog debugLog;

    /**
     * Creates a plugin host for the given execution plan.
     *
     * @param plan      plugin execution plan, not {@code null}
     * @param debugLog  debug logger, not {@code null}
     */
    public PluginHost(PluginExecutionPlan plan, DebugLog debugLog) {
        this.plan = Objects.requireNonNull(plan, "plan");
        this.debugLog = Objects.requireNonNull(debugLog, "debugLog");
    }

    /**
     * Executes all plugins in the plan against the provided generation context.
     *
     * <p>
     * Each plugin is executed in isolation using {@link PluginErrorIsolation}. If a plugin throws
     * an unexpected exception, the host captures it, reports a diagnostic, and continues with the
     * next plugin.
     * </p>
     *
     * <p>
     * The provided context is typically bridged from core internals via {@link PluginContextBridge}.
     * </p>
     *
     * @param context stable SPI context (never {@code null})
     */
    public void executeAll(GenerationContextSpec context) {
        Objects.requireNonNull(context, "context");

        if (plan.isEmpty()) {
            debugLog.note("No plugins to execute");
            return;
        }

        debugLog.note("Executing " + plan.plugins().size() + " plugin(s)");

        for (DiscoveredPlugin discovered : plan.plugins()) {
            executePlugin(discovered, context);
        }

        debugLog.note("Plugin execution completed");
    }

    /**
     * Executes a single plugin with error isolation.
     *
     * @param discovered discovered plugin metadata
     * @param context    generation context
     */
    private void executePlugin(DiscoveredPlugin discovered, GenerationContextSpec context) {
        String pluginId = discovered.id();
        debugLog.note("Executing plugin: " + pluginId);

        try {
            PluginErrorIsolation.execute(discovered, context, debugLog);
        } catch (RuntimeException ex) {
            // This should rarely happen as PluginErrorIsolation handles most exceptions.
            // If we reach here, it's likely a bug in PluginErrorIsolation itself or a severe JVM issue.
            debugLog.note("Unexpected error executing plugin " + pluginId + ": " + ex.getMessage());
            // We continue execution to maintain fault tolerance at the host level.
        }
    }
}
