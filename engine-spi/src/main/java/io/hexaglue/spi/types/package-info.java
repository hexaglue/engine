// File: io/hexaglue/spi/types/package-info.java
/**
 * Types SPI.
 *
 * <p>This package defines a stable, dependency-free abstraction of Java types used by HexaGlue plugins.</p>
 *
 * <p>Goals:
 * <ul>
 *   <li>Do not expose compiler internals (e.g., JSR-269 types).</li>
 *   <li>Provide enough structure for generation and validation.</li>
 *   <li>Remain small and stable over time.</li>
 * </ul>
 *
 * <p>Main entry-points:
 * <ul>
 *   <li>{@link io.hexaglue.spi.types.TypeSystemSpec}: compiler-backed type operations</li>
 *   <li>{@link io.hexaglue.spi.types.TypeRef}: stable type representation</li>
 * </ul>
 */
package io.hexaglue.spi.types;
