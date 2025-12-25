// File: io/hexaglue/spi/discovery/package-info.java
/**
 * Plugin discovery utilities for HexaGlue.
 *
 * <p>This package provides a stable, JDK-only way to discover {@link io.hexaglue.spi.HexaGluePlugin}
 * implementations using {@link java.util.ServiceLoader}.</p>
 *
 * <p>Discovery is intentionally separated from orchestration. Selection rules, ordering, and
 * lifecycle management are responsibilities of the compiler (typically in {@code hexaglue-core}).</p>
 */
package io.hexaglue.spi.discovery;
