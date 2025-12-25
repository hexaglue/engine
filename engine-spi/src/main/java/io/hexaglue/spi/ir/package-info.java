// File: io/hexaglue/spi/ir/package-info.java
/**
 * Intermediate Representation (IR) read-only views.
 *
 * <p>This package provides stable, tool-agnostic, read-only access to the compiler's
 * Intermediate Representation. The IR itself is internal to HexaGlue core; plugins must
 * never depend on or access internals directly.</p>
 *
 * <p>The main entry-point is {@link io.hexaglue.spi.ir.IrView}, which provides access to:
 * <ul>
 *   <li>domain model view</li>
 *   <li>ports model view</li>
 *   <li>application services model view (optional)</li>
 * </ul>
 */
package io.hexaglue.spi.ir;
