/**
 * Plugin discovery for HexaGlue core.
 *
 * <p>
 * This package contains internal, JDK-only implementations to discover plugins from the compilation
 * classpath. The primary mechanism is {@link java.util.ServiceLoader}.
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This package is internal to {@code hexaglue-core} and is not a supported API for third parties.
 * Plugins must rely on {@code io.hexaglue.spi} and the ServiceLoader contract.
 * </p>
 */
package io.hexaglue.core.discovery;
