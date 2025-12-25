/**
 * Internal APIs and implementation details of HexaGlue core.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package and all its subpackages contain internal implementation
 * details that are <strong>NOT part of the public API</strong>.
 * </p>
 *
 * <h2>Scope</h2>
 * <p>
 * The {@code io.hexaglue.core.internal} package tree includes:
 * </p>
 * <ul>
 *   <li><strong>Intermediate Representation (IR):</strong> Internal models for domain types, ports, and application services.</li>
 *   <li><strong>IR Analysis:</strong> Extractors, analyzers, resolvers, and rules that populate the IR.</li>
 *   <li><strong>IR Indexes:</strong> Performance-optimized lookup structures for IR queries.</li>
 *   <li><strong>Plugin Internals:</strong> Internal adapters between core IR and SPI views.</li>
 *   <li><strong>Utilities:</strong> Internal-only helper classes (preconditions, collections, strings).</li>
 * </ul>
 *
 * <h2>Stability</h2>
 * <p>
 * Types in this package and its subpackages:
 * </p>
 * <ul>
 *   <li>Have <strong>no stability guarantees</strong></li>
 *   <li>May change or be removed in any release without notice</li>
 *   <li>Must <strong>never be used by plugins</strong></li>
 * </ul>
 *
 * <h2>Plugin Contract</h2>
 * <p>
 * Plugins must <strong>only</strong> depend on {@code io.hexaglue.spi.*}. The SPI provides:
 * </p>
 * <ul>
 *   <li>Read-only IR views ({@link io.hexaglue.spi.ir.IrView})</li>
 *   <li>Type system access ({@link io.hexaglue.spi.types.TypeSystemSpec})</li>
 *   <li>Naming conventions ({@link io.hexaglue.spi.naming.NameStrategySpec})</li>
 *   <li>Diagnostics reporting ({@link io.hexaglue.spi.diagnostics.DiagnosticReporter})</li>
 *   <li>Artifact generation ({@link io.hexaglue.spi.codegen.ArtifactSink})</li>
 * </ul>
 *
 * <h2>Enforcement</h2>
 * <p>
 * The internal boundary is enforced by:
 * </p>
 * <ul>
 *   <li><strong>JPMS:</strong> {@code module-info.java} does not export {@code *.internal.*} packages.</li>
 *   <li><strong>Convention:</strong> Internal packages follow the {@code *.internal.*} naming pattern.</li>
 *   <li><strong>Annotation:</strong> Types are marked with {@link InternalMarker} for clarity.</li>
 *   <li><strong>Build checks:</strong> Static analysis tools (ArchUnit, Maven Enforcer) detect violations.</li>
 * </ul>
 *
 * <h2>Rationale</h2>
 * <p>
 * This separation allows HexaGlue to:
 * </p>
 * <ul>
 *   <li>Evolve internal implementation rapidly without breaking plugins</li>
 *   <li>Optimize IR representation without SPI compatibility concerns</li>
 *   <li>Provide a stable, minimal SPI surface for third-party extensions</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When adding new internal types:
 * </p>
 * <ol>
 *   <li>Place them under {@code io.hexaglue.core.internal.*}</li>
 *   <li>Mark with {@link InternalMarker} if they may be tempting to use externally</li>
 *   <li>Ensure no SPI types reference internal types directly</li>
 *   <li>If plugin access is needed, expose a read-only view in the SPI</li>
 * </ol>
 */
@InternalMarker(reason = "Root package for all internal HexaGlue core APIs")
package io.hexaglue.core.internal;
