// File: io/hexaglue/spi/ir/ports/package-info.java
/**
 * Ports IR views.
 *
 * <p>This package contains read-only views of port contracts (driving/inbound and driven/outbound)
 * discovered by HexaGlue.</p>
 *
 * <p>Design principles:
 * <ul>
 *   <li>Tool-agnostic: does not expose compiler internals.</li>
 *   <li>Read-only: plugins cannot mutate the IR.</li>
 *   <li>Stable: minimal surface area with forward-compatible evolution.</li>
 * </ul>
 *
 * <p>Main entry-point: {@link io.hexaglue.spi.ir.ports.PortModelView}.</p>
 */
package io.hexaglue.spi.ir.ports;
