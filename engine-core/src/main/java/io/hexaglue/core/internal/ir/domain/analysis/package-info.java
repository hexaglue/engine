/**
 * Domain model analysis and extraction components.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal domain analysis components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the analysis infrastructure for discovering, extracting, and validating
 * domain models from source code. It coordinates the transformation of JSR-269 source elements
 * into HexaGlue's internal domain representation.
 * </p>
 *
 * <h2>Analysis Components</h2>
 * <p>
 * The analysis pipeline consists of several specialized components:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.analysis.DomainAnalyzer} - Main orchestrator coordinating the analysis process</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.analysis.DomainTypeExtractor} - Extracts domain types (entities, value objects, etc.)</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.analysis.DomainServiceExtractor} - Extracts domain services (pure business logic)</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.analysis.DomainPropertyExtractor} - Extracts properties from domain types</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.analysis.DomainTypeKindResolver} - Classifies domain types by kind</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.analysis.DomainRules} - Validates domain model consistency</li>
 * </ul>
 *
 * <h2>Analysis Flow</h2>
 * <p>
 * The typical analysis flow follows these phases:
 * </p>
 * <ol>
 *   <li><strong>Discovery:</strong> Scan source elements to identify domain type candidates</li>
 *   <li><strong>Classification:</strong> Determine whether each element is a domain type, service, or other</li>
 *   <li><strong>Extraction:</strong> Extract detailed structural information (properties, identity, etc.)</li>
 *   <li><strong>Resolution:</strong> Resolve type kinds (entity, value object, identifier, etc.)</li>
 *   <li><strong>Validation:</strong> Apply domain rules and collect violations</li>
 *   <li><strong>Construction:</strong> Build immutable {@link io.hexaglue.core.internal.ir.domain.DomainModel}</li>
 * </ol>
 *
 * <h2>Domain Type Recognition</h2>
 * <p>
 * Analyzers identify domain types through several heuristics:
 * </p>
 * <ul>
 *   <li><strong>Package patterns:</strong> Types in {@code *.domain.*}, {@code *.model.*} packages</li>
 *   <li><strong>Naming conventions:</strong> Types ending with domain suffixes (Id, Service, etc.)</li>
 *   <li><strong>Annotations:</strong> Types marked with domain-specific annotations</li>
 *   <li><strong>Structural patterns:</strong> Classes with domain characteristics (identity, immutability)</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 * <p>
 * The {@link io.hexaglue.core.internal.ir.domain.analysis.DomainRules} class enforces:
 * </p>
 * <ul>
 *   <li><strong>Entities</strong> must have an identity property</li>
 *   <li><strong>Value objects</strong> should be immutable and have no identity</li>
 *   <li><strong>Identifiers</strong> should be immutable and follow naming conventions</li>
 *   <li><strong>Properties</strong> should follow JavaBean naming (camelCase)</li>
 *   <li><strong>Types</strong> should follow Java naming conventions (PascalCase)</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Separation of Concerns:</strong> Each analyzer has a single, focused responsibility</li>
 *   <li><strong>Composability:</strong> Analyzers can be combined and reused</li>
 *   <li><strong>Resilience:</strong> Analysis continues even when individual elements fail</li>
 *   <li><strong>Immutability:</strong> Analysis produces immutable domain models</li>
 *   <li><strong>Statelessness:</strong> Analyzers maintain no state between invocations</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.core.internal.ir.domain} - Domain model representations</li>
 *   <li>{@code io.hexaglue.core.frontend.jsr269} - JSR-269 source element access</li>
 *   <li>{@code io.hexaglue.core.types} - Type system and type references</li>
 *   <li>{@code io.hexaglue.core.diagnostics} - Error and warning reporting</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create analyzer (typically done once per compilation)
 * DomainAnalyzer analyzer = DomainAnalyzer.createDefault();
 *
 * // Analyze domain elements
 * Set<TypeElement> domainElements = scanForDomainTypes(roundEnv);
 * DomainModel model = analyzer.analyze(domainElements);
 *
 * // Validate
 * List<String> violations = analyzer.validate(model);
 * if (!violations.isEmpty()) {
 *     // Report diagnostics
 * }
 *
 * // Add to IR snapshot
 * irSnapshot.setDomainModel(model);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All analyzers in this package are designed to be stateless and thread-safe. They can be
 * safely shared across multiple compilation rounds and threads.
 * </p>
 *
 * <h2>Extension Points</h2>
 * <p>
 * While this package is internal, the analysis process can be customized through:
 * </p>
 * <ul>
 *   <li>Custom {@link io.hexaglue.core.internal.ir.domain.analysis.DomainRules} implementations</li>
 *   <li>Alternative {@link io.hexaglue.core.internal.ir.domain.analysis.DomainTypeKindResolver} strategies</li>
 *   <li>Annotation-driven extraction hints</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * Analysis is designed to be performed once per compilation round:
 * </p>
 * <ul>
 *   <li>Avoid repeated analysis of the same elements</li>
 *   <li>Cache extracted domain models when possible</li>
 *   <li>Use incremental compilation to minimize re-analysis</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with domain analysis:
 * </p>
 * <ol>
 *   <li>Keep analyzers stateless and focused</li>
 *   <li>Report issues through diagnostics, not exceptions</li>
 *   <li>Make validation rules explicit and actionable</li>
 *   <li>Document heuristics and classification logic</li>
 *   <li>Add tests for edge cases and malformed inputs</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal domain analysis; plugins use io.hexaglue.spi.ir.domain.*")
package io.hexaglue.core.internal.ir.domain.analysis;
