/**
 * Internal IR representation of the domain model (types, properties, identities, services).
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal domain model representation that is
 * <strong>NOT part of the public API</strong>. Plugins must use
 * {@link io.hexaglue.spi.ir.domain.DomainModelView} and related SPI types exclusively.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the internal, mutable representation of domain concepts discovered during
 * source analysis. It captures the complete structural and semantic information needed for:
 * </p>
 * <ul>
 *   <li>Infrastructure generation (entities, DTOs, mappers)</li>
 *   <li>Validation and diagnostics</li>
 *   <li>Documentation generation</li>
 *   <li>Dependency analysis</li>
 * </ul>
 *
 * <h2>Domain Model Components</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainModel} - Container for all domain types and services</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainType} - Entity, value object, aggregate, enum, or record</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainProperty} - Field or accessor of a domain type</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainId} - Entity identity definition</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainService} - Pure business rules (analyzed but not generated)</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Analysis, Not Generation:</strong> Domain is analyzed, never modified by HexaGlue</li>
 *   <li><strong>Richness:</strong> Capture all structural and semantic information</li>
 *   <li><strong>Immutability:</strong> Once built, domain models are immutable</li>
 *   <li><strong>Encapsulation:</strong> Never exposed directly to plugins</li>
 * </ul>
 *
 * <h2>Domain Types</h2>
 * <p>
 * HexaGlue recognizes several domain type categories:
 * </p>
 * <ul>
 *   <li><strong>Entity:</strong> Type with stable identity (e.g., Customer, Order)</li>
 *   <li><strong>Value Object:</strong> Immutable type without identity (e.g., Money, Address)</li>
 *   <li><strong>Aggregate Root:</strong> Entity that defines consistency boundary</li>
 *   <li><strong>Identifier:</strong> Type-safe wrapper for entity IDs (e.g., CustomerId)</li>
 *   <li><strong>Enum:</strong> Enumerated domain values</li>
 *   <li><strong>Record:</strong> Immutable data carrier (Java record)</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Domain extractors (in {@code *.internal.ir.domain.analysis}) analyze source elements</li>
 *   <li>Internal domain models are built using builder patterns</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainModel} aggregates all discovered types</li>
 *   <li>Domain model is added to {@link io.hexaglue.core.internal.ir.IrSnapshot}</li>
 *   <li>SPI views ({@link io.hexaglue.spi.ir.domain.DomainModelView}) are created as read-only adapters</li>
 *   <li>Plugins query domain via SPI during generation</li>
 * </ol>
 *
 * <h2>Relationship to SPI</h2>
 * <p>
 * Internal domain types are never exposed directly. Instead:
 * </p>
 * <ul>
 *   <li>SPI views provide read-only access to domain information</li>
 *   <li>Views are created by adapting internal models</li>
 *   <li>Views hide implementation details and JSR-269 references</li>
 * </ul>
 *
 * <h2>Subpackages</h2>
 * <ul>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.analysis} - Domain extractors and analyzers</li>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.index} - Domain-specific indexes</li>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.resolve} - Type resolution and support policies</li>
 * </ul>
 *
 * <h2>For Plugin Authors</h2>
 * <p>
 * <strong>Never depend on this package.</strong> Use {@link io.hexaglue.spi.ir.domain.DomainModelView} instead:
 * </p>
 * <pre>{@code
 * public void apply(GenerationContextSpec context) {
 *     DomainModelView domain = context.model().domain();
 *     List<DomainTypeView> types = domain.types();
 *     // ... use SPI views
 * }
 * }</pre>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with domain models:
 * </p>
 * <ol>
 *   <li>Build internal models using builder patterns</li>
 *   <li>Ensure all domain types are immutable after construction</li>
 *   <li>Create SPI view adapters that hide internal details</li>
 *   <li>Never reference JSR-269 types in public methods</li>
 *   <li>Document semantic intent, not just structure</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal domain model; plugins use io.hexaglue.spi.ir.domain.*")
package io.hexaglue.core.internal.ir.domain;
