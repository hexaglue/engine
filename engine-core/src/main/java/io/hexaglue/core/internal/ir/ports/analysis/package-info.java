/**
 * Port contract analysis and extraction from source code.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal port analysis components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the complete analysis pipeline for discovering and extracting port contracts
 * from Java source code. Ports are the stable boundaries of the hexagon in Hexagonal Architecture,
 * representing what the application offers (driving/inbound ports) and what it requires
 * (driven/outbound ports).
 * </p>
 *
 * <h2>Analysis Components</h2>
 * <p>
 * The port analysis system consists of four coordinated components:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortAnalyzer} - Main orchestrator coordinating the analysis pipeline</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortExtractor} - Extracts port contracts from interface elements</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortDirectionResolver} - Determines port direction (DRIVING vs DRIVEN)</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortRules} - Validates port contracts against architectural rules</li>
 * </ul>
 *
 * <h2>Analysis Pipeline</h2>
 * <p>
 * Port analysis follows a systematic multi-phase workflow:
 * </p>
 * <ol>
 *   <li><strong>Discovery:</strong> Scan source elements for interface candidates</li>
 *   <li><strong>Filtering:</strong> Apply heuristics to identify likely port interfaces</li>
 *   <li><strong>Direction Resolution:</strong> Classify ports as DRIVING or DRIVEN</li>
 *   <li><strong>Method Extraction:</strong> Extract all method signatures with parameters and return types</li>
 *   <li><strong>Validation:</strong> Apply structural and semantic validation rules</li>
 *   <li><strong>Model Construction:</strong> Build immutable {@link io.hexaglue.core.internal.ir.ports.PortModel}</li>
 * </ol>
 *
 * <h2>Port Discovery Strategy</h2>
 *
 * <h3>Inclusion Criteria</h3>
 * <p>
 * An interface is considered a port candidate if:
 * </p>
 * <ul>
 *   <li>It is declared as an interface (not class, enum, record)</li>
 *   <li>It has at least one non-default, non-static method</li>
 *   <li>It is not from JDK packages (java.*, javax.*)</li>
 *   <li>It is not from common libraries (org.springframework.*, jakarta.*)</li>
 *   <li>Package name suggests port role (contains "port", "api", "spi", "repository", "gateway", etc.)</li>
 * </ul>
 *
 * <h3>Exclusion Criteria</h3>
 * <p>
 * Interfaces are excluded if:
 * </p>
 * <ul>
 *   <li>From JDK or common library packages</li>
 *   <li>Marker interfaces with no methods</li>
 *   <li>Only default and static methods (no contract)</li>
 *   <li>Validation rules fail</li>
 * </ul>
 *
 * <h2>Direction Resolution</h2>
 *
 * <h3>Driving Ports (Inbound)</h3>
 * <p>
 * Detected by:
 * </p>
 * <ul>
 *   <li><strong>Package markers:</strong> "inbound", "driving", "primary", "api", "usecase", "application"</li>
 *   <li><strong>Name suffixes:</strong> UseCase, Command, CommandHandler, Query, QueryHandler, Api, Facade, Service</li>
 * </ul>
 *
 * <h3>Driven Ports (Outbound)</h3>
 * <p>
 * Detected by:
 * </p>
 * <ul>
 *   <li><strong>Package markers:</strong> "outbound", "driven", "secondary", "spi"</li>
 *   <li><strong>Name suffixes:</strong> Repository, Gateway, Client, Publisher, EventPublisher, Provider, Adapter</li>
 * </ul>
 *
 * <h3>Default Direction</h3>
 * <p>
 * If no clear indicators are found, ports default to <strong>DRIVEN</strong> (most common for
 * infrastructure generation).
 * </p>
 *
 * <h2>Extraction Details</h2>
 *
 * <h3>Port Information</h3>
 * <p>
 * For each port, the extractor captures:
 * </p>
 * <ul>
 *   <li>Qualified and simple names</li>
 *   <li>Port direction (DRIVING or DRIVEN)</li>
 *   <li>Full type reference with generics</li>
 *   <li>All declared methods</li>
 *   <li>Javadoc documentation (first sentence)</li>
 * </ul>
 *
 * <h3>Method Information</h3>
 * <p>
 * For each method, the extractor captures:
 * </p>
 * <ul>
 *   <li>Method name</li>
 *   <li>Return type with full generic information</li>
 *   <li>Ordered parameter list</li>
 *   <li>Modifiers (default, static)</li>
 *   <li>Varargs indicator</li>
 *   <li>Signature ID for uniqueness checking</li>
 *   <li>Javadoc documentation</li>
 * </ul>
 *
 * <h3>Parameter Information</h3>
 * <p>
 * For each parameter, the extractor captures:
 * </p>
 * <ul>
 *   <li>Parameter name (or synthetic name if unavailable)</li>
 *   <li>Type with full generic information</li>
 *   <li>Varargs flag</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 *
 * <h3>Naming Conventions</h3>
 * <ul>
 *   <li>Port names should start with uppercase letter</li>
 *   <li>Port names should not contain underscores</li>
 *   <li>Port names should have common suffix (Repository, Gateway, UseCase, etc.)</li>
 *   <li>Method names should start with lowercase letter</li>
 * </ul>
 *
 * <h3>Structural Rules</h3>
 * <ul>
 *   <li>Ports must have at least one contract method (non-default, non-static)</li>
 *   <li>Method signatures must be unique within a port</li>
 * </ul>
 *
 * <h3>Parameter Rules</h3>
 * <ul>
 *   <li>Warns about synthetic parameter names (arg0, arg1, etc.)</li>
 *   <li>Suggests using -parameters compiler flag for better names</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Port Analysis</h3>
 * <pre>{@code
 * // Setup
 * Elements elements = processingEnv.getElementUtils();
 * Types types = processingEnv.getTypeUtils();
 * PortAnalyzer analyzer = PortAnalyzer.createDefault(elements, types);
 *
 * // Analyze
 * Set<TypeElement> interfaces = ...;
 * PortModel model = analyzer.analyze(interfaces);
 *
 * // Query results
 * List<Port> allPorts = model.ports();
 * List<Port> repositories = model.drivenPorts();
 * List<Port> useCases = model.drivingPorts();
 * }</pre>
 *
 * <h3>Direction Resolution</h3>
 * <pre>{@code
 * PortDirectionResolver resolver = new PortDirectionResolver();
 *
 * // Repository → DRIVEN
 * PortDirection dir1 = resolver.resolve("CustomerRepository", "com.example.domain");
 * assert dir1 == PortDirection.DRIVEN;
 *
 * // UseCase → DRIVING
 * PortDirection dir2 = resolver.resolve("CreateOrderUseCase", "com.example.application");
 * assert dir2 == PortDirection.DRIVING;
 *
 * // Package-based resolution
 * boolean isDriven = resolver.isDrivenPackage("com.example.outbound.adapters");
 * assert isDriven == true;
 * }</pre>
 *
 * <h3>Port Extraction</h3>
 * <pre>{@code
 * TypeResolver typeResolver = TypeResolver.create(elements, types);
 * PortDirectionResolver directionResolver = new PortDirectionResolver();
 * PortExtractor extractor = new PortExtractor(directionResolver, typeResolver, elements);
 *
 * TypeElement repositoryInterface = ...;
 * Optional<Port> port = extractor.extract(repositoryInterface);
 *
 * port.ifPresent(p -> {
 *     System.out.println("Port: " + p.qualifiedName());
 *     System.out.println("Direction: " + p.direction());
 *     System.out.println("Methods: " + p.methods().size());
 * });
 * }</pre>
 *
 * <h3>Port Validation</h3>
 * <pre>{@code
 * PortRules rules = new PortRules();
 * Port port = ...;
 *
 * List<String> violations = rules.validatePort(port);
 * if (!violations.isEmpty()) {
 *     for (String violation : violations) {
 *         reportDiagnostic(violation);
 *     }
 * }
 * }</pre>
 *
 * <h3>Complete Analysis Pipeline</h3>
 * <pre>{@code
 * // Phase 1: Discovery
 * Set<TypeElement> allInterfaces = discoverInterfaces(roundEnv);
 *
 * // Phase 2: Analysis
 * PortAnalyzer analyzer = PortAnalyzer.createDefault(elements, types);
 * PortModel model = analyzer.analyze(allInterfaces);
 *
 * // Phase 3: Querying
 * for (Port port : model.ports()) {
 *     System.out.println("Port: " + port.qualifiedName());
 *     System.out.println("Direction: " + port.direction());
 *
 *     for (PortMethod method : port.internalMethods()) {
 *         System.out.println("  Method: " + method.name());
 *         System.out.println("  Return: " + method.returnType().render());
 *
 *         for (PortParameter param : method.internalParameters()) {
 *             System.out.println("    Param: " + param.name() + ": " + param.type().render());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code javax.lang.model} - JSR-269 annotation processing APIs</li>
 *   <li>{@code io.hexaglue.core.types} - Type resolution system</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports} - Port model classes</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports.index} - Port indexing for efficient lookups</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - IR snapshot containing port model</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Discovery:</strong> Filtered early to avoid analyzing non-ports</li>
 *   <li><strong>Extraction:</strong> Single-pass analysis per interface</li>
 *   <li><strong>Validation:</strong> Rule application is O(n) per port</li>
 *   <li><strong>Memory:</strong> Shared TypeRef instances, no duplication</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The analysis pipeline is resilient to errors:
 * </p>
 * <ul>
 *   <li>Extraction errors are caught and logged, analysis continues</li>
 *   <li>Validation violations are collected and reported as diagnostics</li>
 *   <li>Invalid ports are excluded from the final model</li>
 *   <li>Best-effort model is always returned, never null</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All analysis classes are thread-safe for concurrent reads:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortAnalyzer} - Safe if dependencies are thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortExtractor} - Safe if dependencies are thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortDirectionResolver} - Stateless, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.analysis.PortRules} - Stateless, thread-safe</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with port analysis:
 * </p>
 * <ol>
 *   <li>Keep direction resolution heuristics simple and deterministic</li>
 *   <li>Document all validation rules with examples</li>
 *   <li>Handle JSR-269 API exceptions gracefully</li>
 *   <li>Preserve full type information including generics</li>
 *   <li>Report diagnostics with actionable guidance</li>
 *   <li>Test with diverse port patterns (repositories, use cases, gateways)</li>
 *   <li>Consider parameter name availability (-parameters flag)</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal port analysis; not exposed to plugins")
package io.hexaglue.core.internal.ir.ports.analysis;
