/**
 * HexaGlue SPI module.
 *
 * <p>
 * This module defines the only stable contract between HexaGlue core and
 * plugins. It is intentionally JDK-only and contains no implementations.
 * </p>
 *
 * <p>
 * Plugins should require this module and provide {@code io.hexaglue.spi.HexaGluePlugin}
 * implementations through {@code provides ... with ...}.
 * </p>
 */
module io.hexaglue.spi {
    requires java.base;

    // Root SPI entry points
    exports io.hexaglue.spi;
    exports io.hexaglue.spi.stability;
    exports io.hexaglue.spi.discovery;
    exports io.hexaglue.spi.context;
    exports io.hexaglue.spi.options;
    exports io.hexaglue.spi.diagnostics;
    exports io.hexaglue.spi.naming;
    exports io.hexaglue.spi.types;

    // Read-only IR views (stable)
    exports io.hexaglue.spi.ir;
    exports io.hexaglue.spi.ir.domain;
    exports io.hexaglue.spi.ir.ports;
    exports io.hexaglue.spi.ir.app;

    // Artifact emission abstractions (stable)
    exports io.hexaglue.spi.codegen;

    // Small JDK-only helpers (optional but exported since used by plugins)
    exports io.hexaglue.spi.util;

    // ServiceLoader contract: plugins provide HexaGluePlugin implementations.
    uses io.hexaglue.spi.HexaGluePlugin;
}
