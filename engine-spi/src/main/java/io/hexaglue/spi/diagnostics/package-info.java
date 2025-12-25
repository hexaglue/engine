// File: io/hexaglue/spi/diagnostics/package-info.java
/**
 * Diagnostics SPI.
 *
 * <p>This package defines the stable, tool-agnostic diagnostic model used by HexaGlue core and plugins.</p>
 *
 * <p>Key goals:
 * <ul>
 *   <li>Provide actionable, structured feedback to end users.</li>
 *   <li>Avoid exposing compiler internals (JSR-269 element handles, positions, etc.).</li>
 *   <li>Enable forward compatibility through extensible attributes.</li>
 * </ul>
 *
 * <p>Primary types:
 * <ul>
 *   <li>{@link io.hexaglue.spi.diagnostics.Diagnostic}: structured message model</li>
 *   <li>{@link io.hexaglue.spi.diagnostics.DiagnosticReporter}: reporting interface</li>
 *   <li>{@link io.hexaglue.spi.diagnostics.DiagnosticCode}: stable code identifier</li>
 * </ul>
 */
package io.hexaglue.spi.diagnostics;
