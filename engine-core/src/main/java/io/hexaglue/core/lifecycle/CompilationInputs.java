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

import io.hexaglue.core.diagnostics.DiagnosticEngine;
import io.hexaglue.core.discovery.PluginClasspath;
import io.hexaglue.core.processor.RoundContext;
import io.hexaglue.spi.options.OptionsView;
import java.util.Objects;

/**
 * Inputs available to the compilation pipeline for a single annotation-processing round.
 *
 * <p>
 * This object is intentionally small and immutable. The pipeline may combine these inputs with
 * cached state (e.g., discovered plugins) across rounds.
 * </p>
 */
public final class CompilationInputs {

    private final RoundContext round;
    private final PluginClasspath pluginClasspath;
    private final DiagnosticEngine diagnosticEngine;
    private final OptionsView resolvedOptions;

    /**
     * Creates inputs for the current round.
     *
     * @param round current round context, not {@code null}
     * @param pluginClasspath plugin discovery classpath, not {@code null}
     * @param diagnosticEngine diagnostic engine for error reporting, not {@code null}
     */
    public CompilationInputs(
            RoundContext round,
            PluginClasspath pluginClasspath,
            DiagnosticEngine diagnosticEngine,
            OptionsView resolvedOptions) {
        this.round = Objects.requireNonNull(round, "round");
        this.pluginClasspath = Objects.requireNonNull(pluginClasspath, "pluginClasspath");
        this.diagnosticEngine = Objects.requireNonNull(diagnosticEngine, "diagnosticEngine");
        this.resolvedOptions = Objects.requireNonNull(resolvedOptions, "resolvedOptions");
    }

    /**
     * Returns the current round context.
     *
     * @return round context, never {@code null}
     */
    public RoundContext round() {
        return round;
    }

    /**
     * Returns the classpath descriptor to use for plugin discovery.
     *
     * @return plugin classpath, never {@code null}
     */
    public PluginClasspath pluginClasspath() {
        return pluginClasspath;
    }

    /**
     * Returns the diagnostic engine for error reporting.
     *
     * @return diagnostic engine, never {@code null}
     */
    public DiagnosticEngine diagnosticEngine() {
        return diagnosticEngine;
    }

    /**
     * Returns the resolved options to use by plugins.
     *
     * @return resolved options, never {@code null}
     */
    public OptionsView resolvedOptions() {
        return resolvedOptions;
    }

    /**
     * Returns whether this is the final processing round.
     *
     * @return {@code true} if processing is over
     */
    public boolean isLastRound() {
        return round.isProcessingOver();
    }

    @Override
    public String toString() {
        return "CompilationInputs{lastRound=" + isLastRound() + ", roots="
                + round.rootElements().size() + "}";
    }
}
