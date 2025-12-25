/**
 * Root package of the HexaGlue compiler ("core") module.
 *
 * <p>
 * This module contains the annotation processor, compilation pipeline and all internal
 * implementations (IR, type system, validation, codegen).
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This package is <strong>not</strong> a public API surface for plugins. External extensions must depend
 * exclusively on {@code io.hexaglue.spi}. No compatibility guarantees are provided for core internals.
 * </p>
 */
package io.hexaglue.core;
