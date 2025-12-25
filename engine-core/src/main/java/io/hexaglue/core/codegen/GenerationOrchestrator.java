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
package io.hexaglue.core.codegen;

import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.List;
import java.util.Objects;
import javax.annotation.processing.Filer;

/**
 * High-level coordinator for the code generation lifecycle.
 *
 * <p>
 * The generation orchestrator manages the entire artifact generation process:
 * </p>
 * <ol>
 *   <li>Create an {@link DefaultArtifactSink} for artifact collection</li>
 *   <li>Invoke plugins in dependency order via their {@code generate()} method</li>
 *   <li>Build an {@link ArtifactPlan} from collected artifacts</li>
 *   <li>Validate the plan and report conflicts</li>
 *   <li>Emit artifacts via {@link ArtifactEmitter}</li>
 * </ol>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Centralizing orchestration enables:
 * </p>
 * <ul>
 *   <li>Consistent plugin invocation order</li>
 *   <li>Transaction-like semantics (validate before writing)</li>
 *   <li>Centralized error handling and reporting</li>
 *   <li>Clear separation of concerns (collection → validation → emission)</li>
 * </ul>
 *
 * <h2>Plugin Invocation</h2>
 * <p>
 * Plugins are invoked in dependency order (determined elsewhere). Each plugin receives
 * a {@link GenerationContextSpec} containing an {@link io.hexaglue.spi.codegen.ArtifactSink}.
 * Plugins emit artifacts by calling methods on the sink.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * Errors during generation are categorized as:
 * </p>
 * <ul>
 *   <li><strong>Plugin errors:</strong> Exceptions thrown by plugins are caught,
 *       reported, and do not prevent other plugins from running</li>
 *   <li><strong>Conflict errors:</strong> Duplicate artifacts with incompatible merge modes
 *       are reported before emission</li>
 *   <li><strong>I/O errors:</strong> File writing failures are reported but don't stop
 *       emission of other artifacts</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe, but it uses non-thread-safe components
 * ({@link DefaultArtifactSink}, {@link Filer}). Therefore, concurrent invocations
 * are not supported.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * GenerationOrchestrator orchestrator = new GenerationOrchestrator(filer, diagnostics);
 *
 * // Prepare context for plugins
 * GenerationContextSpec context = buildContext();
 *
 * // Execute generation
 * orchestrator.generate(plugins, context);
 * }</pre>
 */
public final class GenerationOrchestrator {

    private static final DiagnosticCode CODE_INTERNAL_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-200");
    private static final DiagnosticCode CODE_CONFLICT = DiagnosticCode.of("HG-CORE-CODEGEN-201");
    private static final DiagnosticCode CODE_PLUGIN_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-202");

    private final Filer filer;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new generation orchestrator.
     *
     * @param filer JSR-269 filer for file emission (not {@code null})
     * @param diagnostics diagnostic reporter for errors and warnings (not {@code null})
     */
    public GenerationOrchestrator(Filer filer, DiagnosticReporter diagnostics) {
        this.filer = Objects.requireNonNull(filer, "filer");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Executes the complete generation lifecycle.
     *
     * <p>
     * This method:
     * </p>
     * <ol>
     *   <li>Creates an artifact sink</li>
     *   <li>Invokes all plugins with the provided context (which includes the sink)</li>
     *   <li>Builds an artifact plan from collected artifacts</li>
     *   <li>Validates the plan and reports conflicts</li>
     *   <li>Emits artifacts to the file system</li>
     * </ol>
     *
     * @param plugins plugins to invoke in order (not {@code null})
     * @param context generation context to pass to plugins (not {@code null})
     */
    public void generate(List<HexaGluePlugin> plugins, GenerationContextSpec context) {
        Objects.requireNonNull(plugins, "plugins");
        Objects.requireNonNull(context, "context");

        // Phase 1: Invoke plugins and collect artifacts
        for (HexaGluePlugin plugin : plugins) {
            invokePlugin(plugin, context);
        }

        // Phase 2: Build plan from sink
        // Note: The sink is embedded in the context's output()
        // We need to extract it to build the plan
        // For now, assume context.output() is a DefaultArtifactSink
        // In real usage, this would be passed or extracted properly
        if (!(context.output() instanceof DefaultArtifactSink sink)) {
            diagnostics.report(Diagnostic.builder()
                    .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                    .code(CODE_INTERNAL_ERROR)
                    .message("Internal error: ArtifactSink is not a DefaultArtifactSink")
                    .location(DiagnosticLocation.unknown())
                    .build());
            return;
        }

        ArtifactPlan plan = sink.buildPlan();

        // Phase 3: Validate plan
        if (plan.hasConflicts()) {
            for (String conflict : plan.getConflicts()) {
                diagnostics.report(Diagnostic.builder()
                        .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                        .code(CODE_CONFLICT)
                        .message(conflict)
                        .location(DiagnosticLocation.unknown())
                        .build());
            }
            // Continue with emission despite conflicts - let Filer handle duplicates
        }

        // Phase 4: Emit artifacts
        if (!plan.isEmpty()) {
            ArtifactEmitter emitter = new ArtifactEmitter(filer, diagnostics);
            emitter.emit(plan);
        }
    }

    /**
     * Executes generation with an explicit artifact sink.
     *
     * <p>
     * This is a convenience overload that creates the sink, wraps it in the context,
     * and executes the generation lifecycle.
     * </p>
     *
     * @param plugins plugins to invoke (not {@code null})
     * @param contextFactory function that creates a context given a sink (not {@code null})
     */
    public void generateWithSink(
            List<HexaGluePlugin> plugins,
            java.util.function.Function<DefaultArtifactSink, GenerationContextSpec> contextFactory) {
        Objects.requireNonNull(plugins, "plugins");
        Objects.requireNonNull(contextFactory, "contextFactory");

        // Create sink
        DefaultArtifactSink sink = new DefaultArtifactSink(diagnostics);

        // Create context
        GenerationContextSpec context = contextFactory.apply(sink);

        // Execute
        generate(plugins, context);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Plugin invocation
    // ─────────────────────────────────────────────────────────────────────────

    private void invokePlugin(HexaGluePlugin plugin, GenerationContextSpec context) {
        try {
            plugin.apply(context);
        } catch (Exception e) {
            diagnostics.report(Diagnostic.builder()
                    .severity(io.hexaglue.spi.diagnostics.DiagnosticSeverity.ERROR)
                    .code(CODE_PLUGIN_ERROR)
                    .message("Plugin '" + plugin.metadata().id() + "' failed during generation: " + e.getMessage())
                    .location(DiagnosticLocation.unknown())
                    .cause(e)
                    .build());
            // Continue with other plugins
        }
    }
}
