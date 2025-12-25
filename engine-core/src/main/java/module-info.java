/**
 * HexaGlue Core module.
 *
 * <p>
 * This module contains the HexaGlue compiler, annotation processor,
 * internal IR, type system, validation engine, and code generation
 * infrastructure.
 * </p>
 *
 * <p>
 * This module is <strong>not</strong> a public API. External extensions
 * must rely exclusively on {@code hexaglue-spi}.
 * </p>
 *
 * <p>
 * No internal package is exported. In particular,
 * {@code io.hexaglue.core.internal.*} is intentionally encapsulated
 * to prevent accidental dependencies from plugins.
 * </p>
 */
module io.hexaglue.core {

    /* ------------------------------------------------------------------
     * Required JDK modules
     * ------------------------------------------------------------------ */

    requires java.compiler; // JSR-269 annotation processing
    requires java.logging; // diagnostics / debug logs
    requires java.base;
    requires org.apache.commons.collections4; // Apache Commons
    requires org.apache.commons.lang3; // Apache Commons

    // Core consumes the SPI (HexaGluePlugin, context, views, diagnostics, ...)
    requires io.hexaglue.spi;
    requires org.yaml.snakeyaml;

    /* ------------------------------------------------------------------
     * Services
     * ------------------------------------------------------------------ */

    // Annotation processor entry point
    provides javax.annotation.processing.Processor with
            io.hexaglue.core.processor.HexaGlueProcessor;

/* ------------------------------------------------------------------
 * Strong encapsulation
 * ------------------------------------------------------------------ */

// No exports.
// All packages (including io.hexaglue.core.internal.*) remain
// strongly encapsulated within this module.

// No opens.
// No reflective access is required or allowed.

}
