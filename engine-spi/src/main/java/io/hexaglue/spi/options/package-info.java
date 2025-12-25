// File: io/hexaglue/spi/options/package-info.java
/**
 * Options SPI.
 *
 * <p>This package defines a stable, dependency-free way for plugins to access configuration values.
 * Option sources (compiler args, annotations, config files, presets, etc.) are implementation details
 * of HexaGlue core and are intentionally abstracted behind {@link io.hexaglue.spi.options.OptionsView}.</p>
 *
 * <p>Key concepts:
 * <ul>
 *   <li>{@link io.hexaglue.spi.options.OptionKey}: typed, stable key</li>
 *   <li>{@link io.hexaglue.spi.options.OptionValue}: decoded value + provenance</li>
 *   <li>{@link io.hexaglue.spi.options.OptionScope}: global vs plugin-scoped</li>
 * </ul>
 *
 * <p>All types in this package are JDK-only and designed for long-term stability.</p>
 */
package io.hexaglue.spi.options;
