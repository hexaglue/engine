/**
 * Annotation processing entry point for HexaGlue.
 *
 * <p>
 * This package contains the JSR-269 processor and round orchestration glue.
 * The heavy lifting (IR construction, validation, code generation) lives in
 * core internal packages and is invoked by {@link io.hexaglue.core.processor.HexaGlueProcessor}.
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This package is part of the HexaGlue core module and is <strong>not</strong>
 * a public plugin API. Plugins must depend exclusively on {@code io.hexaglue.spi}.
 * </p>
 */
package io.hexaglue.core.processor;
