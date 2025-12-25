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

import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.context.BuildEnvironment;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.context.GenerationRequest;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.naming.NameStrategySpec;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.Objects;

/**
 * Bridges core internal state to SPI-facing plugin contexts.
 *
 * <p>
 * This utility class provides a stable adapter layer between the core's internal representation
 * and the SPI contract exposed to plugins. It ensures that:
 * <ul>
 *   <li>Compiler internals (e.g., JSR-269 APIs) are never leaked to plugins</li>
 *   <li>Plugins receive immutable, well-defined SPI views</li>
 *   <li>The SPI contract remains stable even as core internals evolve</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Encapsulation:</strong> Internal core types must not appear in plugin-facing APIs.</li>
 *   <li><strong>Flexibility:</strong> Future enrichment (e.g., plugin-scoped contexts) can be added here.</li>
 *   <li><strong>Simplicity:</strong> Current implementation is a thin wrapper; complexity is added only when needed.</li>
 * </ul>
 *
 * <h2>Future Enhancements</h2>
 * <p>
 * This bridge may be extended to provide:
 * </p>
 * <ul>
 *   <li>Plugin-specific diagnostic reporters (tagged with plugin id)</li>
 *   <li>Plugin-scoped option views</li>
 *   <li>Context enrichment or filtering based on plugin metadata</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * GenerationContextSpec bridgedContext = PluginContextBridge.bridge(
 *     names, ir, types, options, diagnostics, output, environment, request
 * );
 * }</pre>
 */
public final class PluginContextBridge {

    private PluginContextBridge() {
        // utility class
    }

    /**
     * Bridges core components into a plugin-facing {@link GenerationContextSpec}.
     *
     * <p>
     * This method creates an immutable SPI context from core-managed components. The returned
     * context is safe to pass to plugins and will not expose compiler internals.
     * </p>
     *
     * <p>
     * All parameters are expected to be SPI-compliant implementations. If any internal core type
     * is passed, it must be wrapped in an SPI-compliant adapter first.
     * </p>
     *
     * @param names       naming strategy view, not {@code null}
     * @param ir          read-only IR view, not {@code null}
     * @param types       type system view, not {@code null}
     * @param options     options view, not {@code null}
     * @param diagnostics diagnostic reporter, not {@code null}
     * @param output      artifact sink, not {@code null}
     * @param environment build environment, not {@code null}
     * @param request     generation request metadata, not {@code null}
     * @return plugin-facing context, never {@code null}
     */
    public static GenerationContextSpec bridge(
            NameStrategySpec names,
            IrView ir,
            TypeSystemSpec types,
            OptionsView options,
            DiagnosticReporter diagnostics,
            ArtifactSink output,
            BuildEnvironment environment,
            GenerationRequest request) {
        Objects.requireNonNull(names, "names");
        Objects.requireNonNull(ir, "ir");
        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(diagnostics, "diagnostics");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(request, "request");

        // Use the SPI static factory for maximum compatibility.
        // This ensures we never accidentally expose core internals.
        return GenerationContextSpec.of(names, ir, types, options, diagnostics, output, environment, request);
    }

    /**
     * Creates a plugin-scoped wrapper around an existing context.
     *
     * <p>
     * This method allows enrichment or filtering of the context for a specific plugin.
     * Currently, it returns the context unchanged, but future implementations may add
     * plugin-specific adapters (e.g., auto-tagging diagnostics with plugin id).
     * </p>
     *
     * @param context  base context, not {@code null}
     * @param pluginId plugin identifier, not {@code null}
     * @return possibly enriched context, never {@code null}
     */
    public static GenerationContextSpec forPlugin(GenerationContextSpec context, String pluginId) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(pluginId, "pluginId");

        // For now, return the context unchanged.
        // Future enhancement: wrap diagnostics reporter to auto-tag with pluginId.
        return context;
    }
}
