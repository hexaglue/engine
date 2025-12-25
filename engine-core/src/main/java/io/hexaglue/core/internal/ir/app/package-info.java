/**
 * Internal representation of application services (use cases) in Hexagonal Architecture.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal application model components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins. Plugins should use
 * {@code io.hexaglue.spi.ir.app} instead.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the internal representation of application services (use cases) discovered
 * during source code analysis. Application services orchestrate domain operations and coordinate
 * calls to outbound ports, sitting at the boundary of the hexagon in Hexagonal Architecture.
 * </p>
 *
 * <h2>Application Model Components</h2>
 * <p>
 * The application model consists of two primary classes:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationModel} - Container for all application services</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService} - Individual application service (use case)</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService.Operation} - Operation (method) in an application service</li>
 * </ul>
 *
 * <h2>Application Layer Concepts</h2>
 *
 * <h3>Application Services</h3>
 * <p>
 * Application services (also called use cases) represent the application's procedural logic:
 * </p>
 * <ul>
 *   <li>Orchestrate domain entities and domain services</li>
 *   <li>Coordinate calls to outbound ports (repositories, gateways)</li>
 *   <li>Define transactional boundaries</li>
 *   <li>Implement use case workflows</li>
 *   <li>Should be thin, delegating business logic to domain layer</li>
 * </ul>
 *
 * <h3>Examples</h3>
 * <ul>
 *   <li><strong>RegisterCustomer:</strong> Validates input, creates Customer entity, saves via CustomerRepository</li>
 *   <li><strong>PlaceOrder:</strong> Checks inventory, creates Order, publishes OrderPlaced event</li>
 *   <li><strong>ProcessPayment:</strong> Validates payment, calls PaymentGateway, updates Order status</li>
 * </ul>
 *
 * <h2>Model Structure</h2>
 * <p>
 * The application model follows a hierarchical structure:
 * </p>
 * <pre>
 * ApplicationModel
 *   └── ApplicationService (RegisterCustomer)
 *         ├── Operation (register)
 *         │     ├── returnType: void
 *         │     └── parameterTypes: [CustomerDto]
 *         └── Operation (validate)
 *               ├── returnType: boolean
 *               └── parameterTypes: [String]
 * </pre>
 *
 * <h2>Key Properties</h2>
 *
 * <h3>Application Service Properties</h3>
 * <ul>
 *   <li><strong>Qualified Name:</strong> Full service class name (e.g., {@code com.example.application.RegisterCustomer})</li>
 *   <li><strong>Simple Name:</strong> Service simple name (e.g., {@code RegisterCustomer})</li>
 *   <li><strong>Operations:</strong> All public methods exposed by the service</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h3>Operation Properties</h3>
 * <ul>
 *   <li><strong>Name:</strong> Operation/method name</li>
 *   <li><strong>Return Type:</strong> Full return type with generics</li>
 *   <li><strong>Parameter Types:</strong> List of parameter types (without names for simplicity)</li>
 *   <li><strong>Signature ID:</strong> Optional stable signature identifier</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> All model classes are immutable after construction</li>
 *   <li><strong>Builder Pattern:</strong> Consistent builder-based construction</li>
 *   <li><strong>SPI Compatibility:</strong> All classes implement corresponding SPI view interfaces</li>
 *   <li><strong>Analysis Only:</strong> HexaGlue never generates application services</li>
 *   <li><strong>Minimal Metadata:</strong> Captures only what's needed for documentation and analysis</li>
 * </ul>
 *
 * <h2>HexaGlue's Role</h2>
 * <p>
 * HexaGlue <strong>analyzes but never generates</strong> application services:
 * </p>
 * <ul>
 *   <li>✅ Discovers and catalogs use cases</li>
 *   <li>✅ Analyzes dependencies on ports</li>
 *   <li>✅ Generates architecture documentation</li>
 *   <li>✅ Validates architectural patterns</li>
 *   <li>❌ Does NOT generate application service implementations</li>
 *   <li>❌ Does NOT modify existing application service code</li>
 * </ul>
 *
 * <h2>Typical Usage Flow</h2>
 * <ol>
 *   <li><strong>Analysis:</strong> Application service analyzer scans source code for use case classes</li>
 *   <li><strong>Extraction:</strong> Service extractor builds ApplicationService instances</li>
 *   <li><strong>Model Construction:</strong> {@link io.hexaglue.core.internal.ir.app.ApplicationModel} is built</li>
 *   <li><strong>Validation:</strong> Model is validated for architectural consistency</li>
 *   <li><strong>SPI Exposure:</strong> Model is wrapped in ApplicationModelView for plugin access</li>
 *   <li><strong>Documentation:</strong> Plugins generate documentation based on application model</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Building an Operation</h3>
 * <pre>{@code
 * ApplicationService.Operation operation = ApplicationService.Operation.builder()
 *     .name("register")
 *     .returnType(voidTypeRef)
 *     .addParameterType(customerDtoTypeRef)
 *     .signatureId("register(CustomerDto):void")
 *     .build();
 * }</pre>
 *
 * <h3>Building an Application Service</h3>
 * <pre>{@code
 * ApplicationService service = ApplicationService.builder()
 *     .qualifiedName("com.example.application.RegisterCustomer")
 *     .simpleName("RegisterCustomer")
 *     .addOperation(registerOperation)
 *     .addOperation(validateOperation)
 *     .description("Registers new customers in the system")
 *     .build();
 * }</pre>
 *
 * <h3>Building an Application Model</h3>
 * <pre>{@code
 * ApplicationModel model = ApplicationModel.builder()
 *     .addService(registerCustomerService)
 *     .addService(placeOrderService)
 *     .addService(processPaymentService)
 *     .build();
 *
 * // Query the model
 * Optional<ApplicationService> service =
 *     model.findService("com.example.application.RegisterCustomer");
 *
 * List<ApplicationService> servicesInPackage =
 *     model.findServicesByPackage("com.example.application");
 * }</pre>
 *
 * <h3>Accessing Service Details</h3>
 * <pre>{@code
 * ApplicationService service = ...;
 *
 * // Basic information
 * String qualifiedName = service.qualifiedName();
 * String simpleName = service.simpleName();
 * Optional<String> description = service.description();
 *
 * // Operations
 * List<ApplicationService.Operation> operations = service.internalOperations();
 * for (ApplicationService.Operation operation : operations) {
 *     String name = operation.name();
 *     TypeRef returnType = operation.returnType();
 *     List<TypeRef> paramTypes = operation.parameterTypes();
 *     Optional<String> signatureId = operation.signatureId();
 *
 *     System.out.println(name + ": " + paramTypes + " -> " + returnType.render());
 * }
 * }</pre>
 *
 * <h3>Model Statistics</h3>
 * <pre>{@code
 * ApplicationModel model = ...;
 *
 * // Check existence
 * boolean hasService = model.containsService("com.example.application.RegisterCustomer");
 *
 * // Get counts
 * int totalServices = model.serviceCount();
 *
 * // Check if empty
 * if (!model.isEmpty()) {
 *     // Generate documentation
 * }
 * }</pre>
 *
 * <h2>SPI Integration</h2>
 * <p>
 * All application model classes implement their corresponding SPI view interfaces:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationModel} exposes via {@link io.hexaglue.spi.ir.app.ApplicationModelView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService} implements {@link io.hexaglue.spi.ir.app.ApplicationServiceView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService.Operation} implements {@link io.hexaglue.spi.ir.app.ApplicationServiceView.OperationView}</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.spi.ir.app} - SPI view interfaces for plugin access</li>
 *   <li>{@code io.hexaglue.spi.types} - Type reference system</li>
 *   <li>{@code io.hexaglue.core.internal.ir.app.analysis} - Application service analysis and extraction</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - IR snapshot containing application model</li>
 *   <li>Documentation generators - Consumers of application service information</li>
 * </ul>
 *
 * <h2>Validation and Consistency</h2>
 * <p>
 * Application model classes enforce consistency through:
 * </p>
 * <ul>
 *   <li><strong>Null Checks:</strong> All required fields validated on construction</li>
 *   <li><strong>Blank Checks:</strong> Names cannot be blank strings</li>
 *   <li><strong>List Validation:</strong> Collections checked for null elements</li>
 *   <li><strong>Immutability:</strong> Defensive copies of mutable collections</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package are immutable after construction and safe for concurrent access:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationModel} - Immutable, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService} - Immutable, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService.Operation} - Immutable, thread-safe</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Construction:</strong> One-time builder-based construction</li>
 *   <li><strong>Memory:</strong> Shared TypeRef instances, no duplication</li>
 *   <li><strong>Collections:</strong> Unmodifiable views over defensive copies</li>
 *   <li><strong>Lookups:</strong> Linear search in model (typically small number of services)</li>
 * </ul>
 *
 * <h2>Optional Nature</h2>
 * <p>
 * The application model is optional in HexaGlue:
 * </p>
 * <ul>
 *   <li>Not all projects expose application services explicitly</li>
 *   <li>Model can be empty if no services are discovered</li>
 *   <li>Plugins should handle empty models gracefully</li>
 *   <li>Use {@link io.hexaglue.spi.ir.app.ApplicationModelView#isSupported()} to check availability</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with application model classes:
 * </p>
 * <ol>
 *   <li>Keep all classes immutable and use builders for construction</li>
 *   <li>Validate all inputs in constructors</li>
 *   <li>Implement SPI view interfaces consistently</li>
 *   <li>Provide both view-compatible and internal-typed accessor methods</li>
 *   <li>Document thread safety guarantees</li>
 *   <li>Use defensive copying for collections</li>
 *   <li>Maintain consistent toString() format for debugging</li>
 *   <li>Remember: analysis only, never generation</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal application model; not exposed to plugins")
package io.hexaglue.core.internal.ir.app;
