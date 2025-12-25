// File: io/hexaglue/spi/ir/domain/package-info.java
/**
 * Domain IR views.
 *
 * <p>This package contains read-only views of the domain model as understood by HexaGlue.</p>
 *
 * <p>Key principles:
 * <ul>
 *   <li>The domain is a source of truth: it is analyzed, never modified.</li>
 *   <li>Views are stable and tool-agnostic: no compiler internals are exposed.</li>
 *   <li>The model is intentionally minimal to keep the SPI small and stable.</li>
 * </ul>
 *
 * <p>Main entry-point: {@link io.hexaglue.spi.ir.domain.DomainModelView}.</p>
 */
package io.hexaglue.spi.ir.domain;
