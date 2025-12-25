// File: io/hexaglue/spi/codegen/package-info.java
/**
 * Code generation output SPI.
 *
 * <p>This package defines stable, dependency-free models for artifacts emitted by plugins:
 * <ul>
 *   <li>Java sources</li>
 *   <li>resources</li>
 *   <li>documentation</li>
 * </ul>
 *
 * <p>The main entry-point is {@link io.hexaglue.spi.codegen.ArtifactSink}. Plugins request emission,
 * while HexaGlue core controls output locations, merge policies, and conflict handling.</p>
 */
package io.hexaglue.spi.codegen;
