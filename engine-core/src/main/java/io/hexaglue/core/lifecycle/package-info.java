/**
 * Compilation lifecycle orchestration for HexaGlue core.
 *
 * <p>
 * This package defines internal, JDK-only primitives used to orchestrate the compilation pipeline
 * (plugin discovery, analysis, validation, generation, writing).
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This package is internal to {@code hexaglue-core} and is not a supported API for external plugins.
 * Plugins must use {@code io.hexaglue.spi} exclusively.
 * </p>
 */
package io.hexaglue.core.lifecycle;
