/**
 * Internal representation of port contracts in Hexagonal Architecture.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal port model components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins. Plugins should use
 * {@code io.hexaglue.spi.ir.ports} instead.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the internal representation of port contracts discovered during
 * source code analysis. Ports are the stable boundaries of the hexagon in Hexagonal Architecture,
 * defining what the application offers (driving/inbound ports) and what it requires
 * (driven/outbound ports).
 * </p>
 *
 * <h2>Port Model Components</h2>
 * <p>
 * The port model consists of four primary classes representing a hierarchical structure:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortModel} - Top-level container for all discovered ports</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.Port} - Individual port interface (repository, use case, gateway)</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortMethod} - Method declared in a port interface</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortParameter} - Method parameter in a port contract</li>
 * </ul>
 *
 * <h2>Hexagonal Architecture Concepts</h2>
 *
 * <h3>Driving Ports (Inbound)</h3>
 * <p>
 * Driving ports express what the application <strong>offers</strong> to the outside world:
 * </p>
 * <ul>
 *   <li>Use case interfaces</li>
 *   <li>Application service interfaces</li>
 *   <li>Command handlers</li>
 *   <li>Query interfaces</li>
 * </ul>
 *
 * <h3>Driven Ports (Outbound)</h3>
 * <p>
 * Driven ports express what the application <strong>requires</strong> from external systems:
 * </p>
 * <ul>
 *   <li>Repository interfaces</li>
 *   <li>Gateway interfaces</li>
 *   <li>External service clients</li>
 *   <li>Event publishers</li>
 * </ul>
 *
 * <h2>Model Structure</h2>
 * <p>
 * The port model follows a hierarchical structure:
 * </p>
 * <pre>
 * PortModel
 *   └── Port (CustomerRepository)
 *         ├── PortMethod (findById)
 *         │     ├── PortParameter (id)
 *         │     └── returnType: Optional&lt;Customer&gt;
 *         ├── PortMethod (save)
 *         │     ├── PortParameter (customer)
 *         │     └── returnType: void
 *         └── PortMethod (delete)
 *               ├── PortParameter (id)
 *               └── returnType: void
 * </pre>
 *
 * <h2>Key Properties</h2>
 *
 * <h3>Port Properties</h3>
 * <ul>
 *   <li><strong>Qualified Name:</strong> Full interface name (e.g., {@code com.example.CustomerRepository})</li>
 *   <li><strong>Simple Name:</strong> Interface simple name (e.g., {@code CustomerRepository})</li>
 *   <li><strong>Direction:</strong> DRIVING or DRIVEN</li>
 *   <li><strong>Type:</strong> Full type reference with generics</li>
 *   <li><strong>Methods:</strong> All methods declared by the port</li>
 *   <li><strong>Port ID:</strong> Optional stable identifier for grouping artifacts</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h3>Method Properties</h3>
 * <ul>
 *   <li><strong>Name:</strong> Method name</li>
 *   <li><strong>Return Type:</strong> Full return type with generics</li>
 *   <li><strong>Parameters:</strong> Ordered list of parameters</li>
 *   <li><strong>Modifiers:</strong> Whether default or static method</li>
 *   <li><strong>Signature ID:</strong> Optional stable signature identifier</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h3>Parameter Properties</h3>
 * <ul>
 *   <li><strong>Name:</strong> Parameter name (or synthetic name)</li>
 *   <li><strong>Type:</strong> Full parameter type with generics</li>
 *   <li><strong>VarArgs:</strong> Whether this is a varargs parameter</li>
 *   <li><strong>Description:</strong> Optional Javadoc description</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> All model classes are immutable after construction</li>
 *   <li><strong>Builder Pattern:</strong> Consistent builder-based construction</li>
 *   <li><strong>SPI Compatibility:</strong> All classes implement corresponding SPI view interfaces</li>
 *   <li><strong>Type Safety:</strong> Full type information preserved including generics</li>
 *   <li><strong>Metadata Rich:</strong> Captures all information needed for adapter generation</li>
 * </ul>
 *
 * <h2>Typical Usage Flow</h2>
 * <ol>
 *   <li><strong>Analysis:</strong> Port analyzer scans source code for port interfaces</li>
 *   <li><strong>Extraction:</strong> Port extractor builds Port instances from TypeElements</li>
 *   <li><strong>Model Construction:</strong> {@link io.hexaglue.core.internal.ir.ports.PortModel} is built</li>
 *   <li><strong>Validation:</strong> Port model is validated for consistency</li>
 *   <li><strong>Indexing:</strong> Port index is built for efficient lookups</li>
 *   <li><strong>SPI Exposure:</strong> Model is wrapped in PortModelView for plugin access</li>
 *   <li><strong>Generation:</strong> Plugins generate adapters based on port contracts</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Building a Port Parameter</h3>
 * <pre>{@code
 * PortParameter param = PortParameter.builder()
 *     .name("customerId")
 *     .type(customerIdTypeRef)
 *     .varArgs(false)
 *     .description("The unique customer identifier")
 *     .build();
 * }</pre>
 *
 * <h3>Building a Port Method</h3>
 * <pre>{@code
 * PortMethod method = PortMethod.builder()
 *     .name("findById")
 *     .returnType(optionalCustomerTypeRef)
 *     .addParameter(idParameter)
 *     .isDefault(false)
 *     .isStatic(false)
 *     .signatureId("findById(CustomerId):Optional<Customer>")
 *     .description("Finds a customer by their unique identifier")
 *     .build();
 * }</pre>
 *
 * <h3>Building a Port</h3>
 * <pre>{@code
 * Port port = Port.builder()
 *     .qualifiedName("com.example.CustomerRepository")
 *     .simpleName("CustomerRepository")
 *     .direction(PortDirection.DRIVEN)
 *     .type(repositoryTypeRef)
 *     .addMethod(findByIdMethod)
 *     .addMethod(saveMethod)
 *     .addMethod(deleteMethod)
 *     .portId("customer-repository")
 *     .description("Repository for customer persistence operations")
 *     .build();
 * }</pre>
 *
 * <h3>Building a Port Model</h3>
 * <pre>{@code
 * PortModel model = PortModel.builder()
 *     .addPort(customerRepositoryPort)
 *     .addPort(orderRepositoryPort)
 *     .addPort(orderUseCasePort)
 *     .build();
 *
 * // Query the model
 * Optional<Port> repository = model.findPort("com.example.CustomerRepository");
 * List<Port> drivenPorts = model.drivenPorts();
 * List<Port> drivingPorts = model.drivingPorts();
 * }</pre>
 *
 * <h3>Querying Ports by Direction</h3>
 * <pre>{@code
 * PortModel model = ...;
 *
 * // Get all driven ports (repositories, gateways)
 * List<Port> repositories = model.portsByDirection(PortDirection.DRIVEN);
 *
 * // Get all driving ports (use cases, APIs)
 * List<Port> useCases = model.portsByDirection(PortDirection.DRIVING);
 *
 * // Count by direction
 * int drivenCount = model.drivenPortCount();
 * int drivingCount = model.drivingPortCount();
 * }</pre>
 *
 * <h3>Accessing Port Details</h3>
 * <pre>{@code
 * Port port = ...;
 *
 * // Basic information
 * String qualifiedName = port.qualifiedName();
 * PortDirection direction = port.direction();
 * boolean isDriven = port.isDriven();
 *
 * // Methods
 * List<PortMethod> methods = port.internalMethods();
 * for (PortMethod method : methods) {
 *     String methodName = method.name();
 *     TypeRef returnType = method.returnType();
 *     List<PortParameter> params = method.internalParameters();
 *
 *     for (PortParameter param : params) {
 *         String paramName = param.name();
 *         TypeRef paramType = param.type();
 *         boolean isVarArgs = param.isVarArgs();
 *     }
 * }
 * }</pre>
 *
 * <h3>Package and Namespace Queries</h3>
 * <pre>{@code
 * PortModel model = ...;
 *
 * // Find ports in specific package
 * List<Port> domainPorts = model.findPortsByPackage("com.example.domain.ports");
 *
 * // Check if port exists
 * boolean hasRepo = model.containsPort("com.example.CustomerRepository");
 *
 * // Get package name from port
 * Port port = ...;
 * String packageName = port.packageName();
 * }</pre>
 *
 * <h2>SPI Integration</h2>
 * <p>
 * All port model classes implement their corresponding SPI view interfaces:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortModel} implements (exposes via) {@link io.hexaglue.spi.ir.ports.PortModelView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.Port} implements {@link io.hexaglue.spi.ir.ports.PortView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortMethod} implements {@link io.hexaglue.spi.ir.ports.PortMethodView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortParameter} implements {@link io.hexaglue.spi.ir.ports.PortParameterView}</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.spi.ir.ports} - SPI view interfaces for plugin access</li>
 *   <li>{@code io.hexaglue.spi.types} - Type reference system</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports.analysis} - Port analysis and extraction</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports.index} - Port indexing for efficient lookups</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - IR snapshot containing port model</li>
 *   <li>Code generators - Consumers of port information for adapter generation</li>
 * </ul>
 *
 * <h2>Validation and Consistency</h2>
 * <p>
 * Port model classes enforce consistency through:
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
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortModel} - Immutable, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.Port} - Immutable, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortMethod} - Immutable, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortParameter} - Immutable, thread-safe</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Construction:</strong> One-time builder-based construction</li>
 *   <li><strong>Memory:</strong> Shared TypeRef instances, no duplication</li>
 *   <li><strong>Collections:</strong> Unmodifiable views over defensive copies</li>
 *   <li><strong>Lookups:</strong> Linear search in model, use index for O(1) lookups</li>
 * </ul>
 *
 * <h2>Extension Points</h2>
 * <p>
 * While this package is internal, future enhancements could include:
 * </p>
 * <ul>
 *   <li>Port stereotypes (REPOSITORY, GATEWAY, USE_CASE, etc.)</li>
 *   <li>Port relationships (dependencies, hierarchies)</li>
 *   <li>Method categorization (queries vs commands)</li>
 *   <li>Transaction metadata</li>
 *   <li>Security constraints</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with port model classes:
 * </p>
 * <ol>
 *   <li>Keep all classes immutable and use builders for construction</li>
 *   <li>Validate all inputs in constructors</li>
 *   <li>Implement SPI view interfaces consistently</li>
 *   <li>Provide both view-compatible and internal-typed accessor methods</li>
 *   <li>Document thread safety guarantees</li>
 *   <li>Use defensive copying for collections</li>
 *   <li>Maintain consistent toString() format for debugging</li>
 *   <li>Ensure equals() and hashCode() work correctly</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal port model; not exposed to plugins")
package io.hexaglue.core.internal.ir.ports;
