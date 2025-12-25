// File: io/hexaglue/spi/package-info.java
/**
 * HexaGlue Service Provider Interface (SPI).
 *
 * <p>This module defines the stable public contract between HexaGlue core and plugins.
 * It is intentionally small, dependency-free (JDK-only), and designed for long-term stability.</p>
 *
 * <h2>Key rules</h2>
 * <ul>
 *   <li>Plugins depend on SPI only.</li>
 *   <li>Plugins must not depend on HexaGlue internals.</li>
 *   <li>All compiler-provided capabilities are accessed through the generation context.</li>
 * </ul>
 */
package io.hexaglue.spi;
