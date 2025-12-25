/**
 * Plugin orchestration and execution infrastructure for HexaGlue core.
 *
 * <p>
 * This package provides the core machinery for:
 * <ul>
 *   <li>Orchestrating plugin execution via {@link io.hexaglue.core.plugins.PluginHost}</li>
 *   <li>Bridging core internals to SPI-facing contexts via {@link io.hexaglue.core.plugins.PluginContextBridge}</li>
 *   <li>Isolating plugin errors to ensure fault tolerance via {@link io.hexaglue.core.plugins.PluginErrorIsolation}</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * The plugin system is designed to be:
 * </p>
 * <ul>
 *   <li><strong>Fault-tolerant:</strong> A failing plugin must not crash the compilation.</li>
 *   <li><strong>Deterministic:</strong> Plugin execution order is stable and predictable.</li>
 *   <li><strong>Encapsulated:</strong> Compiler internals (e.g., JSR-269 APIs) are never exposed to plugins.</li>
 * </ul>
 *
 * <h2>Plugin Execution Flow</h2>
 * <ol>
 *   <li>Plugins are discovered by {@link io.hexaglue.core.discovery.ServiceLoaderPluginDiscovery}</li>
 *   <li>A {@link io.hexaglue.core.lifecycle.PluginExecutionPlan} is built with deterministic ordering</li>
 *   <li>{@link io.hexaglue.core.plugins.PluginHost} orchestrates the execution</li>
 *   <li>{@link io.hexaglue.core.plugins.PluginContextBridge} provides SPI-compliant contexts</li>
 *   <li>{@link io.hexaglue.core.plugins.PluginErrorIsolation} catches and reports plugin errors</li>
 * </ol>
 *
 * <h2>Stability</h2>
 * <p>
 * This package is internal to {@code hexaglue-core} and is not a supported API for external plugins.
 * Plugins must use {@code io.hexaglue.spi} exclusively.
 * </p>
 */
package io.hexaglue.core.plugins;
