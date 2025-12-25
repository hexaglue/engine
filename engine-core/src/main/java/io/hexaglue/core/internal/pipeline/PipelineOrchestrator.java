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
package io.hexaglue.core.internal.pipeline;

import io.hexaglue.core.context.DebugLog;
import io.hexaglue.core.diagnostics.DiagnosticEngine;
import io.hexaglue.core.discovery.DiscoveredPlugin;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.IrInternals;
import io.hexaglue.core.internal.ir.IrSnapshot;
import io.hexaglue.core.internal.ir.app.ApplicationModel;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.analysis.DomainAnalyzer;
import io.hexaglue.core.internal.ir.domain.semantics.DomainSemanticEnricher;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.core.internal.ir.ports.analysis.PortAnalyzer;
import io.hexaglue.core.internal.spi.GenerationContextBuilder;
import io.hexaglue.core.lifecycle.PluginExecutionPlan;
import io.hexaglue.core.processor.ProcessorOptions;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.options.OptionsView;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Orchestrates the compilation pipeline phases: ANALYZE, GENERATE, WRITE.
 *
 * <p>
 * This orchestrator coordinates the execution of analyzers, plugin execution, and diagnostic
 * flushing. It bridges the gap between the compilation pipeline and the internal IR models.
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li><strong>ANALYZE:</strong> Invoke port/domain/application analyzers to build IR</li>
 *   <li><strong>GENERATE:</strong> Execute plugins with GenerationContextSpec</li>
 *   <li><strong>WRITE:</strong> Flush diagnostics to JSR-269 Messager</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is not thread-safe. Each compilation round should have its own orchestrator instance.
 * </p>
 */
@InternalMarker(reason = "Internal compilation orchestration; not exposed to plugins")
public final class PipelineOrchestrator {

    private final ProcessingEnvironment processingEnv;
    private final DiagnosticEngine diagnosticEngine;
    private final PluginExecutionPlan pluginPlan;
    private final OptionsView resolvedOptions;
    private final DebugLog debugLog;
    private IrSnapshot currentSnapshot;
    private io.hexaglue.core.codegen.DefaultArtifactSink artifactSink;

    /**
     * Creates a pipeline orchestrator with the given dependencies.
     *
     * @param processingEnv processing environment (not {@code null})
     * @param diagnosticEngine diagnostic engine for error reporting (not {@code null})
     * @param pluginPlan plugin execution plan (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public PipelineOrchestrator(
            ProcessingEnvironment processingEnv,
            DiagnosticEngine diagnosticEngine,
            PluginExecutionPlan pluginPlan,
            OptionsView resolvedOptions) {
        this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv");
        this.diagnosticEngine = Objects.requireNonNull(diagnosticEngine, "diagnosticEngine");
        this.pluginPlan = Objects.requireNonNull(pluginPlan, "pluginPlan");
        this.resolvedOptions = Objects.requireNonNull(resolvedOptions, "resolvedOptions");

        // Initialize debug log
        ProcessorOptions options = ProcessorOptions.parse(processingEnv);
        this.debugLog = new DebugLog(processingEnv.getMessager(), options.isDebugEnabled(), "[HexaGlue]");
    }

    /**
     * Executes the ANALYZE phase: analyzes source code and builds the IR snapshot.
     *
     * <p>
     * This phase:
     * </p>
     * <ol>
     *   <li>Collects all TypeElements from root elements (including nested types)</li>
     *   <li>Creates and invokes DomainAnalyzer to extract domain model</li>
     *   <li>Creates and invokes PortAnalyzer to extract port model</li>
     *   <li>Enriches domain model with cross-model semantics (e.g., aggregate root classification)</li>
     *   <li>Builds IrSnapshot with enriched domain model and port model (app model empty for now)</li>
     * </ol>
     *
     * @param rootElements the root elements for this round (not {@code null})
     * @throws NullPointerException if rootElements is null
     */
    public void executeAnalyzePhase(Set<? extends Element> rootElements) {
        Objects.requireNonNull(rootElements, "rootElements");

        debugLog.note("Starting IR analysis (rootElements=" + rootElements.size() + ")");

        // 1. Collect all TypeElements
        Set<TypeElement> allTypes = collectAllTypes(rootElements);
        debugLog.note("Collected " + allTypes.size() + " type elements");

        // 2. Get JSR-269 utilities
        Elements elements = processingEnv.getElementUtils();
        Types types = processingEnv.getTypeUtils();

        // 3. Analyze domain
        debugLog.note("Analyzing domain...");
        DomainAnalyzer domainAnalyzer = DomainAnalyzer.createDefault(elements, types, diagnosticEngine.reporter());
        DomainModel domainModel = domainAnalyzer.analyze(allTypes);
        debugLog.note("Domain analysis completed: " + domainModel.types().size() + " type(s) discovered");

        // 4. Analyze ports
        debugLog.note("Analyzing ports...");
        PortAnalyzer portAnalyzer = PortAnalyzer.createDefault(elements, types, diagnosticEngine.reporter());
        PortModel portModel = portAnalyzer.analyze(allTypes);
        debugLog.note("Port analysis completed: " + portModel.ports().size() + " port(s) discovered");

        // 5. Enrich domain with cross-model semantics (e.g., aggregate root classification)
        debugLog.note("Enriching domain with semantic analysis...");
        DomainSemanticEnricher semanticEnricher = DomainSemanticEnricher.withDiagnostics(diagnosticEngine.reporter());
        DomainModel enrichedDomainModel = semanticEnricher.enrich(domainModel, portModel);
        debugLog.note("Semantic enrichment completed");

        // 6. Create snapshot with enriched domain model (ApplicationModel empty for now)
        this.currentSnapshot = IrSnapshot.builder()
                .domainModel(enrichedDomainModel)
                .portModel(portModel)
                .applicationModel(ApplicationModel.empty())
                .build();

        debugLog.note("IR snapshot created successfully");
    }

    /**
     * Executes the GENERATE phase: runs all plugins with the current IR snapshot.
     *
     * <p>
     * This phase:
     * </p>
     * <ol>
     *   <li>Builds GenerationContextSpec from IR snapshot</li>
     *   <li>Executes each plugin in order</li>
     *   <li>Catches and reports plugin execution errors via diagnostics</li>
     * </ol>
     *
     * <p>
     * If no snapshot exists (analyze phase wasn't run), this method does nothing.
     * </p>
     */
    public void executeGeneratePhase() {
        if (currentSnapshot == null) {
            debugLog.note("Skipping GENERATE phase: no IR snapshot available");
            return;
        }

        debugLog.note("Starting GENERATE phase with " + pluginPlan.plugins().size() + " plugin(s)");

        // 1. Build GenerationContextSpec
        GenerationContextSpec context = buildGenerationContext();

        // 2. Execute each plugin
        for (DiscoveredPlugin plugin : pluginPlan.plugins()) {
            debugLog.note("Executing plugin: " + plugin.id() + " (priority=" + plugin.priority() + ")");
            try {
                plugin.plugin().apply(context);
                debugLog.note("Plugin " + plugin.id() + " completed successfully");
            } catch (Exception e) {
                // Internal debug trace with full exception
                debugLog.note("Plugin " + plugin.id() + " threw exception during GENERATE phase", e);

                // User-facing diagnostic
                diagnosticEngine
                        .reporter()
                        .error(
                                DiagnosticCode.of("HG-CORE-PLUGIN-200"),
                                "Plugin '" + plugin.id() + "' encountered an unexpected error during GENERATE. "
                                        + "Check plugin compatibility and report issue to plugin maintainer.");
            }
        }

        debugLog.note("GENERATE phase completed");
    }

    /**
     * Executes the WRITE phase: emits generated artifacts and flushes diagnostics.
     *
     * <p>
     * This critical phase:
     * </p>
     * <ol>
     *   <li>Builds artifact plan from collected artifacts</li>
     *   <li>Emits all artifacts (sources, resources, docs) using JSR-269 Filer</li>
     *   <li>Flushes diagnostic messages (INFO, WARNING, ERROR) to JSR-269 Messager</li>
     * </ol>
     */
    public void executeWritePhase() {
        debugLog.note("Starting WRITE phase");

        // 1. Emit artifacts if any were collected
        if (artifactSink != null) {
            io.hexaglue.core.codegen.ArtifactPlan plan = artifactSink.buildPlan();
            debugLog.note("Emitting " + plan.sourceFiles().size() + " source file(s)");

            io.hexaglue.core.codegen.ArtifactEmitter emitter =
                    new io.hexaglue.core.codegen.ArtifactEmitter(processingEnv.getFiler(), diagnosticEngine.reporter());

            emitter.emit(plan);
            debugLog.note("Artifact emission completed");
        } else {
            debugLog.note("No artifacts to emit");
        }

        // 2. Flush diagnostics to Messager
        debugLog.note("Flushing diagnostics to Messager");
        diagnosticEngine.flushToMessager();

        debugLog.note("WRITE phase completed");
    }

    /**
     * Executes the VALIDATE phase: validates IR snapshot integrity.
     *
     * <p>
     * This phase performs structural validation on the IR snapshot:
     * </p>
     * <ol>
     *   <li>Validates snapshot is well-formed (no nulls, unique qualified names)</li>
     *   <li>Validates domain model structure</li>
     *   <li>Validates port model structure</li>
     *   <li>Validates application model structure</li>
     * </ol>
     *
     * <p>
     * If validation fails, an error is reported via diagnostics and compilation may fail.
     * If no snapshot exists (analyze phase wasn't run), this method does nothing.
     * </p>
     */
    public void executeValidatePhase() {
        if (currentSnapshot == null) {
            debugLog.note("Skipping VALIDATE phase: no IR snapshot available");
            return;
        }

        debugLog.note("Starting VALIDATE phase");

        try {
            // Validate IR snapshot structure
            IrInternals.validateSnapshot(currentSnapshot);
            debugLog.note("IR snapshot validation passed");

            // Log summary of validated IR
            String summary = IrInternals.summarize(currentSnapshot);
            debugLog.note("Validated " + summary);
        } catch (IllegalStateException e) {
            // IR validation failed - report as error
            diagnosticEngine
                    .reporter()
                    .error(
                            DiagnosticCode.of("HG-CORE-IR-207"),
                            "IR snapshot validation failed: " + e.getMessage() + ". "
                                    + "This indicates an internal compiler error. Please report this issue.");

            debugLog.note("VALIDATE phase failed: " + e.getMessage(), e);
            return;
        }

        debugLog.note("VALIDATE phase completed successfully");
    }

    /**
     * Executes the FINISH phase: cleanup and final logging.
     *
     * <p>
     * This phase runs only on the last compilation round and performs:
     * </p>
     * <ol>
     *   <li>Logs final compilation statistics</li>
     *   <li>Performs cleanup (if needed)</li>
     *   <li>Logs completion message</li>
     * </ol>
     *
     * <p>
     * This phase does not generate any artifacts or diagnostics.
     * </p>
     */
    public void executeFinishPhase() {
        debugLog.note("Starting FINISH phase");

        // Log final statistics
        if (currentSnapshot != null) {
            String summary = IrInternals.summarize(currentSnapshot);
            debugLog.note("Final IR: " + summary);
        } else {
            debugLog.note("No IR snapshot was created during compilation");
        }

        // Log plugin execution summary
        debugLog.note("Executed " + pluginPlan.plugins().size() + " plugin(s)");

        debugLog.note("FINISH phase completed - compilation finished");
    }

    /**
     * Collects all TypeElements from root elements by recursively traversing nested types.
     *
     * <p>
     * This method starts from the root elements and recursively collects:
     * </p>
     * <ul>
     *   <li>The root TypeElements themselves</li>
     *   <li>All enclosed TypeElements (nested classes, interfaces, enums)</li>
     * </ul>
     *
     * <p>
     * Port interfaces don't require annotations, so we need to discover all types in the
     * compilation unit.
     * </p>
     *
     * @param roots root elements from the round (not {@code null})
     * @return set of all TypeElements (never {@code null})
     */
    private Set<TypeElement> collectAllTypes(Set<? extends Element> roots) {
        Set<TypeElement> collected = new HashSet<>();
        Queue<Element> toProcess = new LinkedList<>(roots);

        while (!toProcess.isEmpty()) {
            Element current = toProcess.poll();

            // If it's a TypeElement, add it
            if (current instanceof TypeElement te) {
                if (!collected.add(te)) {
                    continue; // Already processed
                }
            }

            // Add all enclosed elements for further processing
            toProcess.addAll(current.getEnclosedElements());
        }

        return collected;
    }

    /**
     * Builds a GenerationContextSpec for plugin execution.
     *
     * <p>
     * This method uses the {@link GenerationContextBuilder} to assemble all required
     * SPI components from the current IR snapshot and processor environment.
     * Creates the artifact sink that will collect all generated artifacts.
     * </p>
     *
     * @return generation context for plugins (never {@code null})
     */
    private GenerationContextSpec buildGenerationContext() {
        // Create artifact sink for collecting generated artifacts
        this.artifactSink = new io.hexaglue.core.codegen.DefaultArtifactSink(diagnosticEngine.reporter());

        return GenerationContextBuilder.build(
                currentSnapshot, diagnosticEngine, processingEnv, pluginPlan, artifactSink, resolvedOptions);
    }
}
