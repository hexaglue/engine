/**
 * Application service analysis and extraction from source code.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal application service analysis
 * components that are <strong>NOT part of the public API</strong>. These classes are
 * implementation details of the HexaGlue compiler and must not be used directly by plugins.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the complete analysis pipeline for discovering and extracting application
 * service contracts from Java source code. Application services (also called use cases) represent
 * the application's procedural logic in Hexagonal Architecture, orchestrating domain operations
 * and coordinating calls to outbound ports.
 * </p>
 *
 * <h2>Analysis Components</h2>
 * <p>
 * The application service analysis system consists of three coordinated components:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.app.analysis.ApplicationAnalyzer} - Main orchestrator coordinating the analysis pipeline</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.analysis.ApplicationServiceExtractor} - Extracts service contracts from class elements</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.analysis.ApplicationRules} - Validates service contracts against architectural rules</li>
 * </ul>
 *
 * <h2>Analysis Pipeline</h2>
 * <p>
 * Application service analysis follows a systematic multi-phase workflow:
 * </p>
 * <ol>
 *   <li><strong>Discovery:</strong> Scan source elements for concrete class candidates</li>
 *   <li><strong>Filtering:</strong> Apply heuristics to identify likely application services</li>
 *   <li><strong>Operation Extraction:</strong> Extract all public method signatures as operations</li>
 *   <li><strong>Validation:</strong> Apply structural and semantic validation rules</li>
 *   <li><strong>Model Construction:</strong> Build immutable {@link io.hexaglue.core.internal.ir.app.ApplicationModel}</li>
 * </ol>
 *
 * <h2>Application Service Discovery Strategy</h2>
 *
 * <h3>Inclusion Criteria</h3>
 * <p>
 * A class is considered an application service candidate if:
 * </p>
 * <ul>
 *   <li>It is declared as a concrete class (not interface, abstract class, enum, record)</li>
 *   <li>It has at least one public operation (non-static method)</li>
 *   <li>It is not from JDK packages (java.*, javax.*)</li>
 *   <li>It is not from common libraries (org.springframework.*, jakarta.*)</li>
 *   <li>Package name suggests application layer (contains "application", "usecase", "service", etc.)</li>
 *   <li>Class name follows common patterns (UseCase, Service, Command, Query suffixes)</li>
 * </ul>
 *
 * <h3>Exclusion Criteria</h3>
 * <p>
 * Classes are excluded if:
 * </p>
 * <ul>
 *   <li>From JDK or common library packages</li>
 *   <li>Infrastructure components (Repository, Adapter, Controller suffixes)</li>
 *   <li>Configuration classes (Config, Configuration suffixes)</li>
 *   <li>No public operations</li>
 *   <li>Validation rules fail</li>
 * </ul>
 *
 * <h2>Application Service Characteristics</h2>
 *
 * <h3>Recognized Patterns</h3>
 * <p>
 * Services are identified by:
 * </p>
 * <ul>
 *   <li><strong>Package markers:</strong> "application", "usecase", "service", "command", "query"</li>
 *   <li><strong>Name suffixes:</strong> UseCase, Service, Command, Query, Handler, Orchestrator, Coordinator, Manager, Processor, Executor</li>
 * </ul>
 *
 * <h3>Application Layer Role</h3>
 * <p>
 * Application services in Hexagonal Architecture:
 * </p>
 * <ul>
 *   <li>Orchestrate domain entities and domain services</li>
 *   <li>Coordinate calls to outbound ports (repositories, gateways)</li>
 *   <li>Define transactional boundaries</li>
 *   <li>Implement use case workflows</li>
 *   <li>Should be thin, delegating business logic to domain layer</li>
 * </ul>
 *
 * <h2>Extraction Details</h2>
 *
 * <h3>Service Information</h3>
 * <p>
 * For each application service, the extractor captures:
 * </p>
 * <ul>
 *   <li>Qualified and simple names</li>
 *   <li>All public operations (methods)</li>
 *   <li>Javadoc documentation (first sentence)</li>
 * </ul>
 *
 * <h3>Operation Information</h3>
 * <p>
 * For each operation, the extractor captures:
 * </p>
 * <ul>
 *   <li>Operation name (method name)</li>
 *   <li>Return type with full generic information</li>
 *   <li>Parameter types (without names for simplicity)</li>
 *   <li>Signature ID for uniqueness checking</li>
 * </ul>
 *
 * <h2>Validation Rules</h2>
 *
 * <h3>Naming Conventions</h3>
 * <ul>
 *   <li>Service names should start with uppercase letter</li>
 *   <li>Service names should not contain underscores</li>
 *   <li>Service names should have common suffix (UseCase, Service, Command, etc.)</li>
 *   <li>Operation names should start with lowercase letter</li>
 *   <li>Operation names should not contain underscores</li>
 * </ul>
 *
 * <h3>Structural Rules</h3>
 * <ul>
 *   <li>Services must have at least one public operation</li>
 *   <li>Operation signatures must be unique within a service</li>
 * </ul>
 *
 * <h2>HexaGlue's Role</h2>
 * <p>
 * HexaGlue <strong>analyzes but never generates</strong> application services. They are discovered for:
 * </p>
 * <ul>
 *   <li>Architecture documentation and visualization</li>
 *   <li>Use case cataloging and discovery</li>
 *   <li>Dependency mapping (which ports are used by which use cases)</li>
 *   <li>Architectural validation and diagnostics</li>
 * </ul>
 *
 * <h2>Optional Nature</h2>
 * <p>
 * The application model is optional in HexaGlue:
 * </p>
 * <ul>
 *   <li>Not all projects expose application services explicitly</li>
 *   <li>The model may be empty if no services are discovered</li>
 *   <li>This is normal and does not prevent adapter generation</li>
 *   <li>Empty models indicate domain-centric or port-only architectures</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Service Analysis</h3>
 * <pre>{@code
 * // Setup
 * Elements elements = processingEnv.getElementUtils();
 * Types types = processingEnv.getTypeUtils();
 * ApplicationAnalyzer analyzer = ApplicationAnalyzer.createDefault(elements, types);
 *
 * // Analyze
 * Set<TypeElement> classes = ...;
 * ApplicationModel model = analyzer.analyze(classes);
 *
 * // Query results
 * List<ApplicationService> allServices = model.services();
 * Optional<ApplicationService> registerService = model.findService("com.example.application.RegisterCustomer");
 * List<ApplicationService> servicesInPackage = model.findServicesByPackage("com.example.application");
 * }</pre>
 *
 * <h3>Service Extraction</h3>
 * <pre>{@code
 * TypeResolver typeResolver = TypeResolver.create(elements, types);
 * ApplicationServiceExtractor extractor = new ApplicationServiceExtractor(typeResolver, elements);
 *
 * TypeElement serviceClass = ...;
 * Optional<ApplicationService> service = extractor.extract(serviceClass);
 *
 * service.ifPresent(s -> {
 *     System.out.println("Service: " + s.qualifiedName());
 *     System.out.println("Operations: " + s.internalOperations().size());
 *
 *     for (ApplicationService.Operation op : s.internalOperations()) {
 *         System.out.println("  Operation: " + op.name());
 *         System.out.println("  Return: " + op.returnType().render());
 *         System.out.println("  Parameters: " + op.parameterTypes().size());
 *     }
 * });
 * }</pre>
 *
 * <h3>Service Validation</h3>
 * <pre>{@code
 * ApplicationRules rules = new ApplicationRules();
 * ApplicationService service = ...;
 *
 * List<String> violations = rules.validateService(service);
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
 * Set<TypeElement> allClasses = discoverClasses(roundEnv);
 *
 * // Phase 2: Analysis
 * ApplicationAnalyzer analyzer = ApplicationAnalyzer.createDefault(elements, types);
 * ApplicationModel model = analyzer.analyze(allClasses);
 *
 * // Phase 3: Querying
 * for (ApplicationService service : model.services()) {
 *     System.out.println("Service: " + service.qualifiedName());
 *
 *     for (ApplicationService.Operation operation : service.internalOperations()) {
 *         System.out.println("  Operation: " + operation.name());
 *         System.out.println("  Return: " + operation.returnType().render());
 *
 *         for (TypeRef paramType : operation.parameterTypes()) {
 *             System.out.println("    Parameter: " + paramType.render());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Filtering Service Candidates</h3>
 * <pre>{@code
 * ApplicationRules rules = new ApplicationRules();
 *
 * // UseCase suffix → likely a service
 * boolean isService1 = rules.isLikelyApplicationService(
 *     "com.example.application.RegisterCustomerUseCase",
 *     "com.example.application",
 *     3  // 3 public operations
 * );
 * assert isService1 == true;
 *
 * // Repository suffix → NOT a service (infrastructure)
 * boolean isService2 = rules.isLikelyApplicationService(
 *     "com.example.CustomerRepository",
 *     "com.example.infrastructure",
 *     5
 * );
 * assert isService2 == false;
 *
 * // Application package → likely a service
 * boolean isService3 = rules.isLikelyApplicationService(
 *     "com.example.application.OrderProcessor",
 *     "com.example.application",
 *     2
 * );
 * assert isService3 == true;
 * }</pre>
 *
 * <h2>Differences from Port Analysis</h2>
 * <p>
 * Application service analysis differs from port analysis in several ways:
 * </p>
 * <ul>
 *   <li><strong>Element Type:</strong> Classes vs interfaces</li>
 *   <li><strong>Direction:</strong> No direction concept (services don't have direction)</li>
 *   <li><strong>Operations:</strong> Simpler than port methods (no parameter names, no modifiers)</li>
 *   <li><strong>Purpose:</strong> Documentation only vs adapter generation</li>
 *   <li><strong>Optionality:</strong> Application model is optional, port model is required</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code javax.lang.model} - JSR-269 annotation processing APIs</li>
 *   <li>{@code io.hexaglue.core.types} - Type resolution system</li>
 *   <li>{@code io.hexaglue.core.internal.ir.app} - Application model classes</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - IR snapshot containing application model</li>
 *   <li>{@code io.hexaglue.spi.ir.app} - SPI view interfaces for plugin access</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Discovery:</strong> Filtered early to avoid analyzing non-services</li>
 *   <li><strong>Extraction:</strong> Single-pass analysis per class</li>
 *   <li><strong>Validation:</strong> Rule application is O(n) per service</li>
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
 *   <li>Invalid services are excluded from the final model</li>
 *   <li>Best-effort model is always returned, never null</li>
 *   <li>Empty models are valid and expected in many projects</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All analysis classes are thread-safe for concurrent reads:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.app.analysis.ApplicationAnalyzer} - Safe if dependencies are thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.analysis.ApplicationServiceExtractor} - Safe if dependencies are thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.analysis.ApplicationRules} - Stateless, thread-safe</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with application service analysis:
 * </p>
 * <ol>
 *   <li>Keep service discovery heuristics simple and deterministic</li>
 *   <li>Document all validation rules with examples</li>
 *   <li>Handle JSR-269 API exceptions gracefully</li>
 *   <li>Preserve full type information including generics</li>
 *   <li>Report diagnostics with actionable guidance</li>
 *   <li>Test with diverse service patterns (use cases, commands, queries)</li>
 *   <li>Remember: analysis only, never generation</li>
 *   <li>Handle empty models gracefully (they're valid)</li>
 * </ol>
 *
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal application service analysis; not exposed to plugins")
package io.hexaglue.core.internal.ir.app.analysis;
