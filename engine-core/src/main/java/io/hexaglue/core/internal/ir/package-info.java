/**
 * Internal Intermediate Representation (IR) infrastructure for HexaGlue core.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal IR implementation that is
 * <strong>NOT part of the public API</strong>. Plugins must use {@link io.hexaglue.spi.ir.IrView}
 * and related SPI types exclusively.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * The IR (Intermediate Representation) is the core data structure that captures the analyzed
 * domain model, ports, and application services during compilation. This package provides:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.IrSnapshot} - Immutable IR state at a compilation phase</li>
 *   <li>{@link io.hexaglue.core.internal.ir.IrIndexes} - Optimized lookup structures for IR queries</li>
 *   <li>{@link io.hexaglue.core.internal.ir.IrInternals} - Internal utilities for IR manipulation</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * The IR is organized into three conceptual layers:
 * </p>
 * <ol>
 *   <li><strong>Internal Models</strong> ({@code *.internal.ir.domain}, {@code *.internal.ir.ports}, {@code *.internal.ir.app}) -
 *       Mutable, rich internal representation built during analysis</li>
 *   <li><strong>Snapshot &amp; Indexes</strong> (this package) - Immutable IR state with fast lookups</li>
 *   <li><strong>SPI Views</strong> ({@link io.hexaglue.spi.ir.IrView}) - Read-only, stable plugin-facing contract</li>
 * </ol>
 *
 * <h2>IR Lifecycle</h2>
 * <ol>
 *   <li>Core analyzes source elements using extractors and analyzers</li>
 *   <li>Internal models are populated in {@code *.internal.ir.domain.*}, {@code *.internal.ir.ports.*}, etc.</li>
 *   <li>{@link io.hexaglue.core.internal.ir.IrSnapshot} is created from the populated models</li>
 *   <li>{@link io.hexaglue.core.internal.ir.IrIndexes} are built for fast lookups</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.IrView}) are created as adapters over the snapshot</li>
 *   <li>Plugins consume SPI views during generation</li>
 * </ol>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> IR snapshots and indexes are immutable after construction</li>
 *   <li><strong>Performance:</strong> Indexes provide O(1) lookups for common queries</li>
 *   <li><strong>Encapsulation:</strong> Internal representation never leaks to SPI</li>
 *   <li><strong>Extensibility:</strong> New IR elements can be added without breaking SPI</li>
 * </ul>
 *
 * <h2>Subpackages</h2>
 * <ul>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.*} - Domain types, properties, ids, services</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports.*} - Port interfaces, methods, parameters</li>
 *   <li>{@code io.hexaglue.core.internal.ir.app.*} - Application services / use cases</li>
 * </ul>
 *
 * <h2>For Plugin Authors</h2>
 * <p>
 * <strong>Never depend on this package.</strong> Use {@link io.hexaglue.spi.ir.IrView} instead:
 * </p>
 * <pre>{@code
 * public void apply(GenerationContextSpec context) {
 *     IrView model = context.model();
 *     DomainModelView domain = model.domain();
 *     PortModelView ports = model.ports();
 *     // ... use SPI views
 * }
 * }</pre>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When adding new IR elements:
 * </p>
 * <ol>
 *   <li>Define internal model classes in appropriate subpackages ({@code domain}, {@code ports}, {@code app})</li>
 *   <li>Update {@link io.hexaglue.core.internal.ir.IrSnapshot} to include the new model</li>
 *   <li>Update {@link io.hexaglue.core.internal.ir.IrIndexes} if fast lookups are needed</li>
 *   <li>Create SPI view adapters if plugins need access</li>
 *   <li>Never expose internal types directly in SPI</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal IR infrastructure; plugins use io.hexaglue.spi.ir.*")
package io.hexaglue.core.internal.ir;
