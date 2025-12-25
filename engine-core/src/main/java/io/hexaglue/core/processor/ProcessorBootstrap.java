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
package io.hexaglue.core.processor;

import io.hexaglue.core.diagnostics.DiagnosticEngine;
import io.hexaglue.core.discovery.PluginClasspath;
import io.hexaglue.core.lifecycle.CompilationInputs;
import io.hexaglue.core.lifecycle.CompilationOutputs;
import io.hexaglue.core.lifecycle.CompilationPipeline;
import io.hexaglue.core.lifecycle.CompilationSession;
import io.hexaglue.core.options.CoreOptionsViewLoader;
import io.hexaglue.core.testing.HexaGlueTestHooks;
import io.hexaglue.spi.options.OptionsView;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

/**
 * Bootstraps the HexaGlue compilation pipeline for annotation processing.
 *
 * <p>
 * This class owns per-compilation state across rounds (options, scanners, and
 * future internal pipeline
 * handles). It is deliberately minimal and JDK-only.
 * </p>
 *
 * <p>
 * The concrete compilation pipeline (IR construction, validation, plugin
 * orchestration and writing)
 * is invoked from {@link #processRound(RoundContext)} and must remain internal
 * to core.
 * </p>
 */
public final class ProcessorBootstrap {

    // private final ProcessingEnvironment processingEnv;
    private final ProcessorOptions options;
    private final ElementScanner scanner;
    private final CompilationPipeline pipeline;

    private OptionsView resolvedOptions;
    private DiagnosticEngine diagnosticEngine;
    private boolean started;
    private boolean finished;

    private ProcessorBootstrap(ProcessingEnvironment processingEnv, ProcessorOptions options) {
        // this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv");
        this.options = Objects.requireNonNull(options, "options");
        this.scanner = new ElementScanner();
        this.pipeline = new CompilationPipeline();
    }

    /**
     * Creates a new bootstrap for the given processing environment.
     *
     * @param processingEnv the processing environment, not {@code null}
     * @return a bootstrap instance, never {@code null}
     */
    public static ProcessorBootstrap create(ProcessingEnvironment processingEnv) {
        Objects.requireNonNull(processingEnv, "processingEnv");
        ProcessorOptions options = ProcessorOptions.parse(processingEnv);
        return new ProcessorBootstrap(processingEnv, options);
    }

    public static ProcessorBootstrap create(ProcessingEnvironment env, HexaGlueTestHooks.Overrides overrides) {
        // ✅ 1) Construire le bootstrap "normal"
        ProcessorBootstrap b = createDefault(env);

        // ✅ 2) Si overrides présents, remplacer ce qui doit l’être
        if (overrides != null) {
            b = b.withOverrides(overrides);
        }
        return b;
    }

    private static ProcessorBootstrap createDefault(ProcessingEnvironment env) {
        // 👉 ici tu mets exactement ce que faisait l’ancien create(env)
        // (ou tu fais simplement "return new ProcessorBootstrap(...)" )
        throw new UnsupportedOperationException("Implement by extracting existing create(env) logic");
    }

    private ProcessorBootstrap withOverrides(HexaGlueTestHooks.Overrides o) {
        // 👉 principe : on garde tout ce qui dépend de ProcessingEnv,
        // mais on remplace:
        // - plugins (au lieu de ServiceLoader)
        // - options (OptionsView)
        // - diagnostics (DiagnosticReporter)
        // - output (ArtifactSink)
        //
        // Selon ta structure actuelle, tu peux:
        // - muter des champs (si bootstrap mutable)
        // - ou retourner une nouvelle instance (idéal)

        // Exemple si tu as des champs:
        // this.plugins = o.plugins();
        // this.options = o.options();
        // this.diagnostics = o.diagnostics();
        // this.output = o.output();
        // return this;

        throw new UnsupportedOperationException("Wire overrides into bootstrap fields");
    }

    /**
     * Returns the parsed processor options.
     *
     * @return options, never {@code null}
     */
    public ProcessorOptions options() {
        return options;
    }

    /**
     * Processes one annotation processing round.
     *
     * <p>
     * The internal pipeline is executed progressively across rounds. The last round
     * is detected via
     * {@link RoundContext#isProcessingOver()} and used to flush outputs and perform
     * final validation.
     * </p>
     *
     * @param round current round context, not {@code null}
     */
    public void processRound(RoundContext round) {
        Objects.requireNonNull(round, "round");
        if (finished) {
            return;
        }

        if (!started) {
            started = true;
            onStart(round);
        }

        onRound(round);

        if (round.isProcessingOver()) {
            finished = true;
            onFinish(round);
        }
    }

    private void onStart(RoundContext round) {
        if (options.isDebugEnabled()) {
            round.messager().printMessage(Diagnostic.Kind.NOTE, "[HexaGlue] processor started");
            round.messager().printMessage(Diagnostic.Kind.NOTE, "[HexaGlue] options: " + options.toDebugString());
        }
    }

    private void onRound(RoundContext round) {
        // Scan elements (for future use)
        scanner.scan(round);

        // Create diagnostic engine on first round
        if (diagnosticEngine == null) {
            diagnosticEngine = DiagnosticEngine.create(round.messager());
        }

        // Compute resolved options once per compilation (YAML optional).
        if (resolvedOptions == null) {
            resolvedOptions = new CoreOptionsViewLoader(round.processingEnv()).load();

            if (options.isDebugEnabled()) {
                round.messager().printMessage(Diagnostic.Kind.NOTE, "[HexaGlue] options view loaded (yaml optional)");
            }
        }

        // Create compilation session
        CompilationSession session = CompilationSession.create(options.isDebugEnabled(), "hexaglue-compilation");

        // Create plugin classpath (from classloader)
        PluginClasspath pluginClasspath = PluginClasspath.of(getClass().getClassLoader());

        // Create compilation inputs
        CompilationInputs inputs = new CompilationInputs(round, pluginClasspath, diagnosticEngine, resolvedOptions);

        // Execute compilation pipeline
        CompilationOutputs outputs = pipeline.executeRound(session, inputs);

        if (options.isDebugEnabled()) {
            round.messager()
                    .printMessage(
                            Diagnostic.Kind.NOTE,
                            "[HexaGlue] round completed: roots="
                                    + round.rootElements().size() + ", over=" + round.isProcessingOver()
                                    + ", outputs=" + outputs);
        }
    }

    private void onFinish(RoundContext round) {
        if (options.isDebugEnabled()) {
            round.messager().printMessage(Diagnostic.Kind.NOTE, "[HexaGlue] processor finished");
        }
    }
}
