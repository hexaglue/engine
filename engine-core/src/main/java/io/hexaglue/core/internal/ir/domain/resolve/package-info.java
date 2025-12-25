/**
 * Domain type resolution, validation, and diagnostics.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal domain resolution components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the infrastructure for resolving type references to domain types,
 * validating type support, and generating diagnostic messages. It acts as a bridge between
 * {@link io.hexaglue.spi.types.TypeRef} instances (representing Java types) and
 * {@link io.hexaglue.core.internal.ir.domain.DomainType} instances (representing analyzed domain types).
 * </p>
 *
 * <h2>Resolution Components</h2>
 * <p>
 * The resolution system consists of three coordinated components:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.resolve.DomainTypeResolver} - Main resolver coordinating validation and lookup</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.resolve.DomainTypeSupportPolicy} - Policy determining which types are supported</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.resolve.DomainDiagnostics} - Factory for standardized diagnostic messages</li>
 * </ul>
 *
 * <h2>Resolution Process</h2>
 * <p>
 * Type resolution follows a systematic workflow:
 * </p>
 * <ol>
 *   <li><strong>Support Check:</strong> Verify the type is supported using {@link io.hexaglue.core.internal.ir.domain.resolve.DomainTypeSupportPolicy}</li>
 *   <li><strong>Resolvability Check:</strong> Ensure the type kind can be resolved (class or parameterized)</li>
 *   <li><strong>Lookup:</strong> Search for the type in {@link io.hexaglue.core.internal.ir.domain.index.DomainIndex}</li>
 *   <li><strong>Validation:</strong> Apply domain-specific validation rules</li>
 *   <li><strong>Diagnostics:</strong> Generate appropriate error messages for failures</li>
 * </ol>
 *
 * <h2>Type Support Policy</h2>
 * <p>
 * The {@link io.hexaglue.core.internal.ir.domain.resolve.DomainTypeSupportPolicy} defines what constitutes
 * a valid domain type:
 * </p>
 *
 * <h3>Supported Types</h3>
 * <ul>
 *   <li>Class types (e.g., {@code Customer}, {@code String})</li>
 *   <li>Parameterized types (e.g., {@code List<Order>}, {@code Map<String, Item>})</li>
 *   <li>Enum types</li>
 *   <li>Record types</li>
 * </ul>
 *
 * <h3>Unsupported Types</h3>
 * <ul>
 *   <li>Primitive types (e.g., {@code int}, {@code boolean}) - use wrappers instead</li>
 *   <li>Array types (e.g., {@code String[]}) - use collections instead</li>
 *   <li>Type variables (e.g., {@code T}) - not concrete domain types</li>
 *   <li>Wildcard types (e.g., {@code ? extends Number}) - too generic</li>
 *   <li>Intersection types - too complex for reliable generation</li>
 * </ul>
 *
 * <h3>Special Handling</h3>
 * <p>
 * Some supported types require special handling during code generation:
 * </p>
 * <ul>
 *   <li>Parameterized types - require generic type handling</li>
 *   <li>Nested classes - require proper qualified name handling</li>
 * </ul>
 *
 * <h2>Diagnostic Messages</h2>
 * <p>
 * {@link io.hexaglue.core.internal.ir.domain.resolve.DomainDiagnostics} provides standardized
 * diagnostic messages following these principles:
 * </p>
 * <ul>
 *   <li><strong>Clear:</strong> Explain what is wrong in simple terms</li>
 *   <li><strong>Actionable:</strong> Suggest how to fix the problem</li>
 *   <li><strong>Contextual:</strong> Include type names and relevant details</li>
 *   <li><strong>Consistent:</strong> Use standard terminology and formatting</li>
 * </ul>
 *
 * <h3>Diagnostic Levels</h3>
 * <ul>
 *   <li><strong>ERROR:</strong> Violations that prevent code generation (unsupported types, missing identity)</li>
 *   <li><strong>WARNING:</strong> Issues that should be addressed (non-immutable value objects)</li>
 *   <li><strong>INFO:</strong> Suggestions and recommendations (naming conventions)</li>
 * </ul>
 *
 * <h2>Use Cases</h2>
 * <p>
 * The resolution system supports various use cases:
 * </p>
 * <ul>
 *   <li>Validate that a property type is a supported domain type</li>
 *   <li>Resolve cross-references between domain types</li>
 *   <li>Check if a type is part of the analyzed domain model</li>
 *   <li>Determine if a type requires special handling during generation</li>
 *   <li>Generate clear error messages for unsupported types</li>
 *   <li>Validate identifier and property type suitability</li>
 * </ul>
 *
 * <h2>Performance Characteristics</h2>
 * <ul>
 *   <li><strong>Support Check:</strong> O(1) - simple kind comparison</li>
 *   <li><strong>Type Lookup:</strong> O(1) - delegated to domain index hash lookup</li>
 *   <li><strong>Validation:</strong> O(1) - policy-based checks</li>
 *   <li><strong>Diagnostics:</strong> O(1) - string formatting</li>
 * </ul>
 *
 * <h2>Typical Usage Flow</h2>
 * <ol>
 *   <li><strong>Initialization:</strong> Create resolver from domain model or index</li>
 *   <li><strong>Support Check:</strong> Verify type is supported before resolution</li>
 *   <li><strong>Resolution:</strong> Resolve type reference to domain type</li>
 *   <li><strong>Validation:</strong> Apply additional domain-specific rules</li>
 *   <li><strong>Diagnostics:</strong> Generate error messages for failures</li>
 *   <li><strong>Generation:</strong> Use resolved types for code generation</li>
 * </ol>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Basic Type Resolution</h3>
 * <pre>{@code
 * // Create resolver from domain model
 * DomainModel model = domainAnalyzer.analyze(elements);
 * DomainTypeResolver resolver = DomainTypeResolver.from(model);
 *
 * TypeRef customerTypeRef = ...;
 *
 * // Check if supported
 * if (!resolver.isSupported(customerTypeRef)) {
 *     String error = resolver.explainUnsupported(customerTypeRef);
 *     reportError(error);
 *     return;
 * }
 *
 * // Resolve to domain type
 * Optional<DomainType> domainType = resolver.resolve(customerTypeRef);
 * if (domainType.isPresent()) {
 *     // Use domain type for generation
 *     generate(domainType.get());
 * } else {
 *     // Type is supported but not in domain model (e.g., String, List)
 *     handleStandardType(customerTypeRef);
 * }
 * }</pre>
 *
 * <h3>Property Type Validation</h3>
 * <pre>{@code
 * DomainTypeResolver resolver = ...;
 * TypeRef propertyType = ...;
 *
 * // Check if suitable as property
 * if (!resolver.isSuitableAsProperty(propertyType)) {
 *     String error = "Property type " + propertyType.render() + " is not suitable";
 *     reportError(error);
 * }
 *
 * // Check if requires special handling
 * if (resolver.requiresSpecialHandling(propertyType)) {
 *     // Apply special generation logic for parameterized or nested types
 *     generateWithSpecialHandling(propertyType);
 * }
 * }</pre>
 *
 * <h3>Identifier Type Validation</h3>
 * <pre>{@code
 * DomainTypeResolver resolver = ...;
 * TypeRef identifierType = ...;
 *
 * // Identifiers have stricter requirements
 * if (!resolver.isSuitableAsIdentifier(identifierType)) {
 *     String error = "Type " + identifierType.render() + " cannot be used as identifier. "
 *         + "Identifiers must be simple, immutable class types.";
 *     reportError(error);
 * }
 * }</pre>
 *
 * <h3>Support Policy Usage</h3>
 * <pre>{@code
 * DomainTypeSupportPolicy policy = new DomainTypeSupportPolicy();
 *
 * TypeRef primitiveType = ...; // int
 * TypeRef wrapperType = ...;   // Integer
 * TypeRef arrayType = ...;     // String[]
 * TypeRef listType = ...;      // List<String>
 *
 * // Primitive types not supported
 * assertFalse(policy.isSupported(primitiveType));
 * String reason = policy.getUnsupportedReason(primitiveType);
 * // "Primitive types are not supported as domain types. Use wrapper types instead..."
 *
 * // Wrapper types supported
 * assertTrue(policy.isSupported(wrapperType));
 *
 * // Arrays not supported
 * assertFalse(policy.isSupported(arrayType));
 *
 * // Collections supported
 * assertTrue(policy.isSupported(listType));
 * assertTrue(policy.isSuitableAsProperty(listType));
 * assertFalse(policy.isSuitableAsIdentifier(listType)); // Too complex for ID
 * }</pre>
 *
 * <h3>Diagnostic Message Generation</h3>
 * <pre>{@code
 * DomainDiagnostics diagnostics = new DomainDiagnostics();
 *
 * // Unsupported type error
 * TypeRef primitiveType = ...;
 * String error = diagnostics.unsupportedType(
 *     primitiveType,
 *     "Primitive types must use wrappers"
 * );
 * // "[DOMAIN ERROR] Type 'int' is not supported as a domain type. Primitive types must use wrappers"
 *
 * // Entity validation error
 * String missingIdError = diagnostics.entityMissingIdentity("com.example.Customer");
 * // "[DOMAIN ERROR] Entity 'com.example.Customer' must have an identity property..."
 *
 * // Value object warning
 * String immutabilityWarning = diagnostics.valueObjectNotImmutable("com.example.Money");
 * // "[DOMAIN WARNING] Value object 'com.example.Money' should be immutable..."
 *
 * // Validation summary
 * DomainType type = ...;
 * String summary = diagnostics.validationSummary(type, 2, 1);
 * // "Domain type 'com.example.Customer' validation: 2 error(s), 1 warning(s)"
 * }</pre>
 *
 * <h3>Custom Resolver Configuration</h3>
 * <pre>{@code
 * // Build resolver with custom components
 * DomainIndex index = DomainIndex.from(domainModel);
 * DomainTypeSupportPolicy policy = new DomainTypeSupportPolicy();
 * DomainDiagnostics diagnostics = new DomainDiagnostics();
 *
 * DomainTypeResolver resolver = new DomainTypeResolver(index, policy, diagnostics);
 *
 * // Access components
 * DomainIndex resolverIndex = resolver.getDomainIndex();
 * DomainTypeSupportPolicy resolverPolicy = resolver.getSupportPolicy();
 * DomainDiagnostics resolverDiagnostics = resolver.getDiagnostics();
 * }</pre>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Separation of Concerns:</strong> Support policy, resolution, and diagnostics are independent</li>
 *   <li><strong>Composability:</strong> Components can be used together or independently</li>
 *   <li><strong>Fail-Fast:</strong> Validate type support before attempting resolution</li>
 *   <li><strong>Clear Messaging:</strong> Provide actionable diagnostic messages</li>
 *   <li><strong>Extensibility:</strong> Policy can be customized for different validation rules</li>
 * </ul>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.spi.types} - TypeRef instances being resolved</li>
 *   <li>{@code io.hexaglue.core.internal.ir.domain} - Domain types being resolved to</li>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.index} - Index for type lookups</li>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.analysis} - Analysis uses resolver for validation</li>
 *   <li>Code generators - Consumers of resolved types and diagnostics</li>
 * </ul>
 *
 * <h2>Extension Points</h2>
 * <p>
 * While this package is internal, it provides extension points for future enhancements:
 * </p>
 * <ul>
 *   <li>Custom support policies for different validation rules</li>
 *   <li>Pluggable diagnostic message templates</li>
 *   <li>Additional validation rules (e.g., annotation-based constraints)</li>
 *   <li>Custom type resolution strategies</li>
 * </ul>
 *
 * <h2>Error Handling Strategy</h2>
 * <p>
 * The resolution system uses a layered error handling approach:
 * </p>
 * <ol>
 *   <li><strong>Support Check:</strong> Fast fail for fundamentally unsupported types</li>
 *   <li><strong>Resolvability Check:</strong> Verify type can be looked up by name</li>
 *   <li><strong>Optional Return:</strong> Resolution returns Optional (type may not be in domain)</li>
 *   <li><strong>Validation:</strong> Additional checks after successful resolution</li>
 *   <li><strong>Diagnostics:</strong> Clear messages for each failure scenario</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All resolution classes are thread-safe for concurrent reads:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.resolve.DomainTypeSupportPolicy} - Stateless, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.resolve.DomainDiagnostics} - Stateless, thread-safe</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.resolve.DomainTypeResolver} - Immutable dependencies, thread-safe</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with domain resolution:
 * </p>
 * <ol>
 *   <li>Keep support policy stateless and efficient (avoid expensive checks)</li>
 *   <li>Ensure diagnostic messages are clear and actionable</li>
 *   <li>Document why types are unsupported in policy reason methods</li>
 *   <li>Use Optional for resolution results (types may not be in domain)</li>
 *   <li>Maintain thread-safety for concurrent code generation</li>
 *   <li>Add new diagnostic message methods to DomainDiagnostics for consistency</li>
 *   <li>Test support policy with edge cases (nested generics, inner classes, etc.)</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal domain resolution; not exposed to plugins")
package io.hexaglue.core.internal.ir.domain.resolve;
