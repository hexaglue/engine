/**
 * Domain model indexing for efficient lookups and queries.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal domain indexing components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides efficient indexing structures for domain model elements. Indexes enable
 * fast lookups by various criteria (qualified name, simple name, kind, package) which are
 * essential for code generation performance and cross-referencing.
 * </p>
 *
 * <h2>Index Components</h2>
 * <p>
 * The indexing system consists of specialized indexes coordinated by a unified facade:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.index.DomainIndex} - Unified index providing access to all domain elements</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.index.DomainTypeIndex} - Index for domain types with multi-criteria lookups</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.index.DomainServiceIndex} - Index for domain services</li>
 * </ul>
 *
 * <h2>Indexing Strategy</h2>
 * <p>
 * Indexes are built from {@link io.hexaglue.core.internal.ir.domain.DomainModel} instances and
 * provide multiple access patterns:
 * </p>
 * <ul>
 *   <li><strong>By qualified name:</strong> Primary key lookup for exact matches</li>
 *   <li><strong>By simple name:</strong> Find all elements with the same simple name (may be in different packages)</li>
 *   <li><strong>By kind:</strong> Group domain types by classification (entity, value object, etc.)</li>
 *   <li><strong>By package:</strong> Find all elements in a specific package</li>
 *   <li><strong>By package prefix:</strong> Hierarchical search across package trees</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <p>
 * Indexes are optimized for read-heavy workloads typical in code generation:
 * </p>
 * <ul>
 *   <li><strong>Construction:</strong> O(n) where n is the number of elements</li>
 *   <li><strong>Qualified name lookup:</strong> O(1) hash-based lookup</li>
 *   <li><strong>Kind/package lookups:</strong> O(1) to retrieve result set</li>
 *   <li><strong>Memory:</strong> Multiple indexes share element instances (no duplication)</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * <p>
 * All indexes are immutable after construction. This provides:
 * </p>
 * <ul>
 *   <li>Thread-safe read access without synchronization</li>
 *   <li>Consistent snapshots throughout code generation</li>
 *   <li>Safe sharing across compilation rounds</li>
 * </ul>
 *
 * <h2>Typical Usage Flow</h2>
 * <ol>
 *   <li><strong>Analysis:</strong> Domain analyzer extracts types and services from source code</li>
 *   <li><strong>Model Construction:</strong> {@link io.hexaglue.core.internal.ir.domain.DomainModel} is built</li>
 *   <li><strong>Index Creation:</strong> {@link io.hexaglue.core.internal.ir.domain.index.DomainIndex} is built from the model</li>
 *   <li><strong>Query Phase:</strong> Code generators query the index for required elements</li>
 *   <li><strong>Generation:</strong> Generators use indexed elements to produce artifacts</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Building an Index</h3>
 * <pre>{@code
 * // From domain model
 * DomainModel model = domainAnalyzer.analyze(elements);
 * DomainIndex index = DomainIndex.from(model);
 *
 * // Or build specialized indexes separately
 * DomainTypeIndex typeIndex = DomainTypeIndex.from(model.types());
 * DomainServiceIndex serviceIndex = DomainServiceIndex.from(model.services());
 * }</pre>
 *
 * <h3>Querying Types</h3>
 * <pre>{@code
 * DomainIndex index = ...;
 *
 * // Find specific type
 * Optional<DomainType> customer = index.findType("com.example.Customer");
 *
 * // Find all entities
 * List<DomainType> entities = index.findTypesByKind(DomainTypeKind.ENTITY);
 *
 * // Find types in package
 * List<DomainType> domainTypes = index.types().findByPackage("com.example.domain");
 *
 * // Hierarchical package search
 * List<DomainType> allInExample = index.types().findByPackagePrefix("com.example");
 * }</pre>
 *
 * <h3>Querying Services</h3>
 * <pre>{@code
 * DomainIndex index = ...;
 *
 * // Find specific service
 * Optional<DomainService> pricing = index.findService("com.example.PricingService");
 *
 * // Find services by name
 * List<DomainService> calculators = index.services().findBySimpleName("Calculator");
 *
 * // Find services in package
 * List<DomainService> services = index.services().findByPackage("com.example.domain");
 * }</pre>
 *
 * <h3>Statistics and Introspection</h3>
 * <pre>{@code
 * DomainIndex index = ...;
 *
 * // Check existence
 * boolean hasCustomer = index.containsType("com.example.Customer");
 *
 * // Get counts
 * int totalTypes = index.typeCount();
 * int totalServices = index.serviceCount();
 *
 * // Check if empty
 * if (!index.isEmpty()) {
 *     // Generate code
 * }
 * }</pre>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Separation of Concerns:</strong> Each index focuses on one type of element</li>
 *   <li><strong>Composability:</strong> Indexes can be used independently or together via DomainIndex</li>
 *   <li><strong>Efficiency:</strong> Pre-computed indexes avoid repeated linear searches</li>
 *   <li><strong>Consistency:</strong> Immutable snapshots ensure consistent views</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.core.internal.ir.domain} - Domain model elements being indexed</li>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.analysis} - Analysis produces models to index</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - Indexes are part of IR snapshot</li>
 *   <li>Code generators - Consumers of indexed data</li>
 * </ul>
 *
 * <h2>Extension Points</h2>
 * <p>
 * While this package is internal, additional specialized indexes can be added:
 * </p>
 * <ul>
 *   <li>Property-based indexes (find types with specific properties)</li>
 *   <li>Relationship indexes (dependency graphs, aggregation hierarchies)</li>
 *   <li>Annotation-based indexes (types marked with specific annotations)</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Index Construction:</strong> Build once per compilation round</li>
 *   <li><strong>Memory Usage:</strong> Indexes hold references, not copies of elements</li>
 *   <li><strong>Query Performance:</strong> Most queries are O(1) or O(log n)</li>
 *   <li><strong>Iteration:</strong> Iterating all elements is O(n), use specific queries when possible</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All index classes are thread-safe for concurrent reads after construction. Index construction
 * itself should be done from a single thread (typically during IR building).
 * </p>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with domain indexes:
 * </p>
 * <ol>
 *   <li>Keep indexes immutable and thread-safe</li>
 *   <li>Optimize for common query patterns (by name, by kind)</li>
 *   <li>Document query complexity in method Javadoc</li>
 *   <li>Share element instances across indexes (avoid duplication)</li>
 *   <li>Provide convenience methods on DomainIndex for common operations</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal domain indexing; not exposed to plugins")
package io.hexaglue.core.internal.ir.domain.index;
