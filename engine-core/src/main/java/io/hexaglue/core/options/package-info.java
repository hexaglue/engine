/**
 * Core implementations of the HexaGlue SPI options layer.
 *
 * <p>
 * This package provides JDK-only adapters that expose compiler configuration to plugins
 * through the stable SPI ({@code io.hexaglue.spi.options}).
 * </p>
 *
 * <p>
 * Options are read from annotation processor arguments (or equivalent tool configuration),
 * parsed, validated and exposed via {@link io.hexaglue.spi.options.OptionsView}.
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This package is internal to {@code hexaglue-core}. Plugins must only depend on the SPI.
 * </p>
 */
package io.hexaglue.core.options;
