/**
 * Port model indexing for efficient lookups and queries.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal port indexing components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides efficient indexing structures for port model elements. Indexes enable
 * fast lookups by various criteria (qualified name, simple name, direction, package) which are
 * essential for adapter generation performance and cross-referencing.
 * </p>
 *
 * <h2>Index Components</h2>
 * <p>
 * The indexing system consists of specialized indexes coordinated by a unified facade:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.index.PortIndex} - Unified index providing access to all ports</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.index.PortByDirectionIndex} - Index for ports by direction (DRIVING/DRIVEN)</li>
 * </ul>
 *
 * <h2>Indexing Strategy</h2>
 * <p>
 * Indexes are built from {@link io.hexaglue.core.internal.ir.ports.PortModel} instances and
 * provide multiple access patterns:
 * </p>
 * <ul>
 *   <li><strong>By qualified name:</strong> Primary key lookup for exact matches</li>
 *   <li><strong>By simple name:</strong> Find all ports with the same simple name (may be in different packages)</li>
 *   <li><strong>By direction:</strong> Group ports by DRIVING or DRIVEN classification</li>
 *   <li><strong>By package:</strong> Find all ports in a specific package</li>
 *   <li><strong>By package prefix:</strong> Hierarchical search across package trees</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <p>
 * Indexes are optimized for read-heavy workloads typical in adapter generation:
 * </p>
 * <ul>
 *   <li><strong>Construction:</strong> O(n) where n is the number of ports</li>
 *   <li><strong>Qualified name lookup:</strong> O(1) hash-based lookup</li>
 *   <li><strong>Direction lookup:</strong> O(1) to retrieve result set</li>
 *   <li><strong>Package lookups:</strong> O(1) to retrieve result set</li>
 *   <li><strong>Memory:</strong> Multiple indexes share Port instances (no duplication)</li>
 * </ul>
 *
 * <h2>Immutability</h2>
 * <p>
 * All indexes are immutable after construction. This provides:
 * </p>
 * <ul>
 *   <li>Thread-safe read access without synchronization</li>
 *   <li>Consistent snapshots throughout adapter generation</li>
 *   <li>Safe sharing across compilation rounds</li>
 * </ul>
 *
 * <h2>Typical Usage Flow</h2>
 * <ol>
 *   <li><strong>Analysis:</strong> Port analyzer extracts ports from source code</li>
 *   <li><strong>Model Construction:</strong> {@link io.hexaglue.core.internal.ir.ports.PortModel} is built</li>
 *   <li><strong>Index Creation:</strong> {@link io.hexaglue.core.internal.ir.ports.index.PortIndex} is built from the model</li>
 *   <li><strong>Query Phase:</strong> Adapter generators query the index for required ports</li>
 *   <li><strong>Generation:</strong> Generators use indexed ports to produce adapters</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Building an Index</h3>
 * <pre>{@code
 * // From port model
 * PortModel model = portAnalyzer.analyze(elements);
 * PortIndex index = PortIndex.from(model);
 *
 * // Or build specialized index separately
 * PortByDirectionIndex directionIndex = PortByDirectionIndex.from(model.ports());
 * }</pre>
 *
 * <h3>Querying Ports</h3>
 * <pre>{@code
 * PortIndex index = ...;
 *
 * // Find specific port
 * Optional<Port> repository = index.findPort("com.example.CustomerRepository");
 *
 * // Find all driven ports (repositories, gateways)
 * List<Port> drivenPorts = index.drivenPorts();
 *
 * // Find all driving ports (use cases, APIs)
 * List<Port> drivingPorts = index.drivingPorts();
 *
 * // Find ports in package
 * List<Port> domainPorts = index.findByPackage("com.example.domain.ports");
 *
 * // Hierarchical package search
 * List<Port> allInExample = index.findByPackagePrefix("com.example");
 * }</pre>
 *
 * <h3>Direction-Based Queries</h3>
 * <pre>{@code
 * PortIndex index = ...;
 *
 * // Get direction index
 * PortByDirectionIndex directionIndex = index.byDirection();
 *
 * // Query by direction
 * List<Port> drivenPorts = directionIndex.findByDirection(PortDirection.DRIVEN);
 * List<Port> drivingPorts = directionIndex.findByDirection(PortDirection.DRIVING);
 *
 * // Convenience methods
 * List<Port> repositories = directionIndex.drivenPorts();
 * List<Port> useCases = directionIndex.drivingPorts();
 *
 * // Count by direction
 * int drivenCount = directionIndex.drivenPortCount();
 * int drivingCount = directionIndex.drivingPortCount();
 * }</pre>
 *
 * <h3>Simple Name Lookups</h3>
 * <pre>{@code
 * PortIndex index = ...;
 *
 * // Find all ports named "Repository" (may be in different packages)
 * List<Port> repositories = index.findBySimpleName("Repository");
 *
 * // Handle multiple matches
 * for (Port port : repositories) {
 *     System.out.println(port.qualifiedName() + " in package " + port.packageName());
 * }
 * }</pre>
 *
 * <h3>Package-Based Queries</h3>
 * <pre>{@code
 * PortIndex index = ...;
 *
 * // Exact package match
 * List<Port> portsInDomain = index.findByPackage("com.example.domain");
 *
 * // Hierarchical search (all sub-packages)
 * List<Port> allExamplePorts = index.findByPackagePrefix("com.example");
 *
 * // This will find ports in:
 * // - com.example
 * // - com.example.domain
 * // - com.example.domain.ports
 * // - com.example.application
 * // etc.
 * }</pre>
 *
 * <h3>Statistics and Introspection</h3>
 * <pre>{@code
 * PortIndex index = ...;
 *
 * // Check existence
 * boolean hasRepository = index.contains("com.example.CustomerRepository");
 *
 * // Get counts
 * int totalPorts = index.size();
 * int drivenCount = index.drivenPortCount();
 * int drivingCount = index.drivingPortCount();
 *
 * // Check if empty
 * if (!index.isEmpty()) {
 *     // Generate adapters
 * }
 * }</pre>
 *
 * <h3>Integration with Adapter Generation</h3>
 * <pre>{@code
 * // Typical adapter generation workflow
 * PortIndex index = PortIndex.from(portModel);
 *
 * // Generate driven adapters (repositories, gateways)
 * for (Port drivenPort : index.drivenPorts()) {
 *     if (drivenPort.simpleName().endsWith("Repository")) {
 *         generateJpaRepository(drivenPort);
 *     } else if (drivenPort.simpleName().endsWith("Gateway")) {
 *         generateRestClient(drivenPort);
 *     }
 * }
 *
 * // Generate driving adapters (REST controllers)
 * for (Port drivingPort : index.drivingPorts()) {
 *     if (drivingPort.simpleName().endsWith("UseCase")) {
 *         generateRestController(drivingPort);
 *     }
 * }
 * }</pre>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Separation of Concerns:</strong> Each index focuses on one access pattern</li>
 *   <li><strong>Composability:</strong> Indexes can be used independently or together via PortIndex</li>
 *   <li><strong>Efficiency:</strong> Pre-computed indexes avoid repeated linear searches</li>
 *   <li><strong>Consistency:</strong> Immutable snapshots ensure consistent views</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.core.internal.ir.ports} - Port model elements being indexed</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports.analysis} - Analysis produces models to index</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - Indexes are part of IR snapshot</li>
 *   <li>Adapter generators - Consumers of indexed data</li>
 * </ul>
 *
 * <h2>Extension Points</h2>
 * <p>
 * While this package is internal, additional specialized indexes could be added:
 * </p>
 * <ul>
 *   <li>Method-based indexes (find ports with specific method signatures)</li>
 *   <li>Stereotype-based indexes (REPOSITORY, GATEWAY, USE_CASE, etc.)</li>
 *   <li>Relationship indexes (port dependencies, compositions)</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Index Construction:</strong> Build once per compilation round</li>
 *   <li><strong>Memory Usage:</strong> Indexes hold references, not copies of ports</li>
 *   <li><strong>Query Performance:</strong> Most queries are O(1) or O(log n)</li>
 *   <li><strong>Iteration:</strong> Iterating all ports is O(n), use specific queries when possible</li>
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
 * When working with port indexes:
 * </p>
 * <ol>
 *   <li>Keep indexes immutable and thread-safe</li>
 *   <li>Optimize for common query patterns (by name, by direction)</li>
 *   <li>Document query complexity in method Javadoc</li>
 *   <li>Share Port instances across indexes (avoid duplication)</li>
 *   <li>Provide convenience methods on PortIndex for common operations</li>
 *   <li>Ensure empty indexes are handled correctly</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal port indexing; not exposed to plugins")
package io.hexaglue.core.internal.ir.ports.index;
