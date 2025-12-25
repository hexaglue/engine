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
package io.hexaglue.spi;

import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.stability.Stable;

/**
 * Main extension contract for HexaGlue.
 *
 * <p>Plugins are discovered by the compiler using {@link java.util.ServiceLoader}.
 * A plugin implementation must be registered under:
 *
 * <pre>
 * META-INF/services/io.hexaglue.spi.HexaGluePlugin
 * </pre>
 *
 * <p>Design goals:
 * <ul>
 *   <li>Stable, minimal public API</li>
 *   <li>No runtime dependency required for generated code</li>
 *   <li>Plugins must not depend on HexaGlue internals</li>
 * </ul>
 *
 * <p>Threading model:
 * <ul>
 *   <li>A plugin instance may be used across multiple rounds/compilations depending on the compiler implementation.</li>
 *   <li>Implementations should therefore be stateless or manage state carefully.</li>
 * </ul>
 *
 * @since 0.1.0
 */
@Stable(since = "1.0.0")
public interface HexaGluePlugin {

    /**
     * A stable identifier for this plugin.
     *
     * <p>Recommendations:
     * <ul>
     *   <li>Use a reverse-DNS style id (e.g., {@code "io.hexaglue.plugin.spring.jpa"}).</li>
     *   <li>Keep it stable across versions.</li>
     * </ul>
     *
     * @return the plugin id (non-blank)
     */
    String id();

    /**
     * Plugin metadata exposed for diagnostics, logging and documentation.
     *
     * <p>This method should be cheap and deterministic.</p>
     *
     * @return plugin metadata (never {@code null})
     */
    default PluginMetadata metadata() {
        return PluginMetadata.minimal(id());
    }

    /**
     * Declares the ordering group of this plugin relative to others.
     *
     * <p>Ordering is a best-effort mechanism used by the compiler to produce deterministic runs.
     * It must not be relied on for correctness. Plugins should be robust to varying order.</p>
     *
     * @return ordering group (defaults to {@link PluginOrder#NORMAL})
     */
    default PluginOrder order() {
        return PluginOrder.NORMAL;
    }

    /**
     * Execute this plugin for the current compilation.
     *
     * <p>The compiler orchestrates the lifecycle and provides a stable {@link GenerationContextSpec}
     * that gives access to:
     * <ul>
     *   <li>read-only IR views</li>
     *   <li>type system access</li>
     *   <li>naming conventions</li>
     *   <li>options/configuration</li>
     *   <li>diagnostics reporting</li>
     *   <li>artifact sinks (sources/resources/docs)</li>
     * </ul>
     *
     * <p>Implementations must:
     * <ul>
     *   <li>Never throw for expected user errors; report diagnostics instead.</li>
     *   <li>Fail fast (throw) only for unrecoverable internal plugin errors.</li>
     * </ul>
     *
     * @param context stable generation context for this compilation (never {@code null})
     */
    void apply(GenerationContextSpec context);
}
