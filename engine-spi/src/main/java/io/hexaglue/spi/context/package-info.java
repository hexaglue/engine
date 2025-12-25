// File: io/hexaglue/spi/context/package-info.java
/**
 * Generation context SPI.
 *
 * <p>This package contains stable, dependency-free (JDK-only) abstractions that
 * define the execution context provided to {@link io.hexaglue.spi.HexaGluePlugin} implementations.</p>
 *
 * <p>The core concept is {@link io.hexaglue.spi.context.GenerationContextSpec}, which aggregates:
 * <ul>
 *   <li>naming conventions</li>
 *   <li>read-only IR views</li>
 *   <li>type system access</li>
 *   <li>options/configuration</li>
 *   <li>diagnostics reporting</li>
 *   <li>artifact output</li>
 *   <li>build environment + request metadata</li>
 * </ul>
 *
 * <p>All types in this package must remain stable and must not expose compiler internals.</p>
 */
package io.hexaglue.spi.context;
