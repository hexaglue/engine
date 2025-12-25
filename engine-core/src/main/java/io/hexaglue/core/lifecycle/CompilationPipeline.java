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
package io.hexaglue.core.lifecycle;

import io.hexaglue.core.context.DebugLog;
import io.hexaglue.core.discovery.DiscoveredPlugin;
import io.hexaglue.core.discovery.PluginClasspath;
import io.hexaglue.core.discovery.ServiceLoaderPluginDiscovery;
import io.hexaglue.core.internal.pipeline.PipelineOrchestrator;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the HexaGlue compilation across annotation-processing rounds.
 *
 * <p>
 * This pipeline is intentionally small and delegates "real work" (analysis, IR, validation, codegen)
 * to dedicated internal components. Its primary role is to:
 * </p>
 * <ul>
 *   <li>discover plugins once (or when needed),</li>
 *   <li>provide deterministic ordering,</li>
 *   <li>structure the compilation into phases for logs and testability.</li>
 * </ul>
 *
 * <p>
 * The pipeline instance is stateful across rounds; do not reuse it across independent compilations.
 * </p>
 */
public final class CompilationPipeline {

    private final ServiceLoaderPluginDiscovery discovery;

    private boolean pluginsDiscovered;
    private PluginExecutionPlan executionPlan;
    private PipelineOrchestrator orchestrator;

    /**
     * Creates a pipeline with default plugin discovery.
     */
    public CompilationPipeline() {
        this(new ServiceLoaderPluginDiscovery());
    }

    /**
     * Creates a pipeline with a provided discovery strategy.
     *
     * @param discovery discovery strategy, not {@code null}
     */
    public CompilationPipeline(ServiceLoaderPluginDiscovery discovery) {
        this.discovery = Objects.requireNonNull(discovery, "discovery");
    }

    /**
     * Executes a single processing round.
     *
     * <p>
     * This method is safe to call for each JSR-269 round. The pipeline will cache plugin discovery
     * and reuse it for subsequent rounds.
     * </p>
     *
     * @param session compilation session, not {@code null}
     * @param inputs current round inputs, not {@code null}
     * @return outputs summary for this round, never {@code null}
     */
    public CompilationOutputs executeRound(CompilationSession session, CompilationInputs inputs) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(inputs, "inputs");

        DebugLog debugLog =
                new DebugLog(inputs.round().messager(), inputs.round().options().isDebugEnabled(), "[HexaGlue]");

        debugLog.note("Starting compilation round (last=" + inputs.isLastRound() + ", rootElements="
                + inputs.round().rootElements().size() + ")");

        ensurePluginsDiscovered(
                inputs.pluginClasspath(), inputs.diagnosticEngine().reporter(), debugLog);
        ensureOrchestratorCreated(inputs);

        // Execute compilation phases
        // Only analyze if there are elements to process
        if (!inputs.round().rootElements().isEmpty()) {
            runPhase(session, CompilationPhase.ANALYZE, inputs, debugLog);
            runPhase(session, CompilationPhase.VALIDATE, inputs, debugLog);

            // Generate files in early rounds so other annotation processors (like MapStruct)
            // can process them in subsequent rounds. Files generated in the last round
            // cannot be processed by other processors.
            runPhase(session, CompilationPhase.GENERATE, inputs, debugLog);
            runPhase(session, CompilationPhase.WRITE, inputs, debugLog);
        }

        // Run finish phase only on the last round for cleanup and final validation
        if (inputs.isLastRound()) {
            runPhase(session, CompilationPhase.FINISH, inputs, debugLog);
        }

        debugLog.note("Compilation round completed");

        // Until real writers are wired: return an empty successful summary.
        return CompilationOutputs.emptySuccess();
    }

    /**
     * Returns the cached plugin execution plan, if already discovered.
     *
     * @return the plan, or {@code null} if discovery has not yet occurred
     */
    public PluginExecutionPlan executionPlanOrNull() {
        return executionPlan;
    }

    private void ensurePluginsDiscovered(
            PluginClasspath classpath, io.hexaglue.spi.diagnostics.DiagnosticReporter diagnostics, DebugLog debugLog) {
        if (pluginsDiscovered) {
            debugLog.note("Plugins already discovered, skipping discovery");
            return;
        }
        debugLog.note("Discovering plugins...");

        try {
            List<DiscoveredPlugin> discovered = discovery.discover(classpath);
            this.executionPlan = new PluginExecutionPlan(discovered);
            this.pluginsDiscovered = true;
            debugLog.note("Discovered " + discovered.size() + " plugin(s)");

        } catch (ServiceLoaderPluginDiscovery.PluginDiscoveryException e) {
            // Internal debug trace with full exception
            debugLog.note("Plugin discovery failed", e);

            // User-facing diagnostic
            diagnostics.error(
                    DiagnosticCode.of("HG-CORE-PLUGIN-001"),
                    "Failed to discover plugins: " + e.getMessage() + ". Compilation will continue without plugins.");

            // Continue with empty plugin plan
            this.executionPlan = new PluginExecutionPlan(Collections.emptyList());
            this.pluginsDiscovered = true;
            debugLog.note("Continuing with 0 plugin(s) after discovery failure");
        }
    }

    private void ensureOrchestratorCreated(CompilationInputs inputs) {
        if (orchestrator != null) {
            return;
        }
        this.orchestrator = new PipelineOrchestrator(
                inputs.round().processingEnv(), inputs.diagnosticEngine(), executionPlan, inputs.resolvedOptions());
    }

    private void runPhase(
            CompilationSession session, CompilationPhase phase, CompilationInputs inputs, DebugLog debugLog) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(inputs, "inputs");
        Objects.requireNonNull(debugLog, "debugLog");

        debugLog.note("Entering " + phase + " phase");

        switch (phase) {
            case DISCOVER_PLUGINS:
                // Plugin discovery is handled by ensurePluginsDiscovered()
                break;
            case ANALYZE:
                orchestrator.executeAnalyzePhase(inputs.round().rootElements());
                break;
            case VALIDATE:
                orchestrator.executeValidatePhase();
                break;
            case GENERATE:
                orchestrator.executeGeneratePhase();
                break;
            case WRITE:
                orchestrator.executeWritePhase();
                break;
            case FINISH:
                orchestrator.executeFinishPhase();
                break;
        }

        debugLog.note("Completed " + phase + " phase");
    }
}
