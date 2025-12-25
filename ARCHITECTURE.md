# HexaGlue Engine Architecture

This document provides a technical overview of HexaGlue Engine's internal architecture for advanced users, plugin developers, and contributors.

---

## Table of Contents

- [Design Philosophy](#design-philosophy)
- [System Architecture](#system-architecture)
- [Module Structure](#module-structure)
- [Compilation Pipeline](#compilation-pipeline)
- [Intermediate Representation](#intermediate-representation)
- [Type System](#type-system)
- [Plugin SPI Architecture](#plugin-spi-architecture)
- [Configuration System](#configuration-system)
- [Diagnostic System](#diagnostic-system)
- [Performance Considerations](#performance-considerations)
- [Extension Points](#extension-points)

---

## Design Philosophy

HexaGlue Engine is built around several core principles:

### 1. Non-Invasive Analysis

HexaGlue **never modifies** your business code. It:

- Analyzes domain types, ports, and services
- Builds an internal representation (IR)
- Generates infrastructure code separately
- Leaves your core business logic untouched

```
Your Domain/Ports (unchanged)
        ↓
    Analysis
        ↓
   IR (internal model)
        ↓
  Generation (plugins)
        ↓
Infrastructure (generated code)
```

### 2. Clean Separation of Concerns

The architecture enforces strict boundaries:

- **engine-core** - Analyzes and builds IR (internal implementation)
- **engine-spi** - Stable plugin API (public contract)
- **Plugins** - Consume IR via SPI, generate code

Neither the core nor plugins touch your business logic.

### 3. Stable Plugin Contract

The **SPI** (`engine-spi`) is the only stable API for plugins:

- Minimal, focused interface
- No internal dependencies
- Semantic versioning (SPI_VERSION = 1)
- JDK-only (no external dependencies)

**Critical rule:** Plugins depend ONLY on `engine-spi`, never on `engine-core` internals.

### 4. Framework Agnostic

HexaGlue core knows nothing about specific frameworks:

- No Spring dependencies
- No JPA dependencies
- No REST framework assumptions

Framework knowledge lives in plugins. Swap plugins, swap stacks.

---

## System Architecture

### High-Level View

```
┌───────────────────────────────────────────────────┐
│            User Application                       │
│  ┌─────────────┐  ┌────────────┐  ┌────────────┐  │
│  │   Domain    │  │   Ports    │  │  Services  │  │
│  │   (pure)    │  │(interfaces)│  │(use cases) │  │
│  └─────────────┘  └────────────┘  └────────────┘  │
└───────────────────────────────────────────────────┘
                       │
                       │ Analyzed during compilation
                       ▼
┌───────────────────────────────────────────────────┐
│           HexaGlue Engine (engine-core)           │
│  ┌───────────────────────────────────────────┐    │
│  │  JSR-269 Annotation Processor             │    │
│  │  - Processes ALL annotations (supports *) │    │
│  │  - Does NOT claim any annotations         │    │
│  │  - Universal compilation listener         │    │
│  └───────────────────────────────────────────┘    │
│                       │                           │
│  ┌───────────────────────────────────────────┐    │
│  │  Analyzers                                │    │
│  │  - Port Analyzer (heuristic discovery)    │    │
│  │  - Domain Analyzer (entity detection)     │    │
│  │  - Service Analyzer (use case detection)  │    │
│  └───────────────────────────────────────────┘    │
│                       │                           │
│  ┌───────────────────────────────────────────┐    │
│  │  Intermediate Representation (IR)         │    │
│  │  - Domain model                           │    │
│  │  - Port model                             │    │
│  │  - Application model                      │    │
│  │  - Type system                            │    │
│  │  - Indexes and resolvers                  │    │
│  └───────────────────────────────────────────┘    │
└───────────────────────────────────────────────────┘
                       │
                       │ Read-only access via SPI
                       ▼
┌───────────────────────────────────────────────────┐
│              Plugin SPI (engine-spi)              │
│  ┌───────────────────────────────────────────┐    │
│  │  HexaGluePlugin Interface                 │    │
│  │  - id(), metadata(), order(), execute()   │    │
│  └───────────────────────────────────────────┘    │
│  ┌───────────────────────────────────────────┐    │
│  │  PluginContext                            │    │
│  │  - getAnalyzedApplication() → IR views    │    │
│  │  - getOptions() → configuration           │    │
│  │  - getDiagnostics() → reporter            │    │
│  │  - getFiler() → code generation           │    │
│  └───────────────────────────────────────────┘    │
└───────────────────────────────────────────────────┘
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
     ┌─────────┐  ┌─────────┐  ┌─────────┐
     │ Plugin  │  │ Plugin  │  │ Plugin  │
     │PortDocs │  │   JPA   │  │  ...    │
     └─────────┘  └─────────┘  └─────────┘
          │            │            │
          └────────────┼────────────┘
                       ▼
          Generated Infrastructure Code
```

---

## Module Structure

HexaGlue Engine is a multi-module Maven project:

### engine-bom

**Bill of Materials** - Centralized dependency management

- No code, pure dependency coordination
- Defines versions for all engine modules
- Used by projects to import consistent versions

**Usage:**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.hexaglue</groupId>
            <artifactId>engine-bom</artifactId>
            <version>0.1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### engine-spi

**Service Provider Interface** - The stable plugin API

- Contains: `HexaGluePlugin`, `PluginContext`, type abstractions, IR views
- Zero external dependencies (JDK-only)
- Semantic versioning (SPI_VERSION = 1)
- Plugins MUST depend only on this module

**Key packages:**
- `io.hexaglue.spi` - Plugin interface and metadata
- `io.hexaglue.spi.context` - Plugin execution context
- `io.hexaglue.spi.codegen` - Code generation API
- `io.hexaglue.spi.diagnostics` - Diagnostic reporting
- `io.hexaglue.spi.ir` - Read-only IR views
- `io.hexaglue.spi.types` - Type system
- `io.hexaglue.spi.options` - Configuration and metadata helpers

**Stability levels:**
- `@StableApi` - Never breaks (source & binary compatible)
- `@EvolvableApi` - May add methods (binary compatible)
- `@ExperimentalApi` - May change or be removed

### engine-core

**Compilation Engine** - The brain of HexaGlue

- JSR-269 annotation processor implementation
- IR implementation (domain, ports, services, types)
- Analyzers (port discovery, domain extraction, type classification)
- Type system (TypeRef hierarchy, comparators, renderers)
- Naming strategies and utilities
- Diagnostic routing
- Plugin orchestration
- Configuration parser (YAML)

**Internal packages (NOT public API):**
- `io.hexaglue.core.internal.*` - Implementation details
- Subject to change without notice
- Never depend on these from plugins

**Public packages:**
- All APIs exposed through `engine-spi`

### engine-testing-harness

**Testing Framework** - For plugin developers

- Compilation test utilities
- In-memory compilation
- Fluent DSL for writing tests
- Diagnostic assertions

[📖 See testing-harness/README.md for complete documentation](engine-testing-harness/README.md)

**Usage:**
```java
CompilationTestCase.run()
    .withSourceFile("CustomerRepository.java", code)
    .withProcessor(MyPlugin.class)
    .expectSuccess()
    .expectGeneratedFile("com.example.CustomerAdapter.java")
    .verify();
```

---

## Compilation Pipeline

### Processing Flow

HexaGlue hooks into Java compilation as a JSR-269 annotation processor:

```
┌─────────────────────────────────────────┐
│  1. DISCOVER                            │
│     javac starts compilation            │
│     HexaGlueProcessor loaded            │
│     Plugins discovered (ServiceLoader)  │
│     Plugins sorted by PluginOrder       │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  2. ANALYZE                             │
│     For each compilation round:         │
│     - PortAnalyzer scans interfaces     │
│     - DomainAnalyzer scans types        │
│     - Classify domain types             │
│     - Detect aggregate roots            │
│     - Resolve nullability               │
│     - Build IR incrementally            │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  3. VALIDATE                            │
│     - Check port structure              │
│     - Validate domain model             │
│     - Verify type relationships         │
│     - Report diagnostics                │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  4. GENERATE (last round only)          │
│     For each plugin (in order):         │
│     - plugin.execute(context)           │
│     - Access IR via context             │
│     - Generate artifacts                │
│     - Report diagnostics                │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  5. WRITE                               │
│     - Write source files (Filer API)    │
│     - Write resource files              │
│     - Write documentation               │
└─────────────────────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  6. FINISH                              │
│     - Emit final diagnostics            │
│     - Compilation completes             │
└─────────────────────────────────────────┘
```

### Annotation Processing Strategy

HexaGlue uses a unique "universal listener" approach:

```java
@SupportedAnnotationTypes("*")  // Process ALL annotations
public class HexaGlueProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                          RoundEnvironment roundEnv) {
        // Analyze ALL root elements, not just annotated ones
        Set<? extends Element> roots = roundEnv.getRootElements();

        // Build IR from structural analysis
        analyzeElements(roots);

        // Return false - don't claim any annotations
        // (allows other processors to work normally)
        return false;
    }
}
```

This approach means:

- No marker annotations required
- Works alongside other processors
- Analyzes full compilation context
- Discovers ports by structure, not annotations

---

## Intermediate Representation

The IR is HexaGlue's internal model of your application architecture.

### IR Components

```
IR (IrSnapshot)
├── Domain Model
│   ├── Entities
│   ├── Value Objects
│   ├── Aggregates
│   └── Domain Services
│
├── Port Model
│   ├── Driving Ports (inbound)
│   ├── Driven Ports (outbound)
│   └── Port Methods
│
├── Application Model
│   └── Application Services (use cases)
│
├── Type System
│   ├── Type Registry
│   ├── Type Hierarchy
│   └── Type References
│
└── Indexes
    ├── By Qualified Name
    ├── By Package
    └── By Role
```

### Port Discovery

Ports are identified by heuristics, NOT annotations:

```java
// PortAnalyzer logic (simplified)
boolean isPort(TypeElement element) {
    return element.getKind() == ElementKind.INTERFACE
        && hasAbstractMethods(element)
        && !isJdkInterface(element)
        && !isLibraryInterface(element)
        && matchesNamingPattern(element);  // *Port, *Repository, etc.
}
```

### Port Direction Resolution

```java
PortDirection resolveDirection(PortView port) {
    // Heuristics:
    // - Package name (*.port.in → DRIVING, *.port.out → DRIVEN)
    // - Naming (*UseCase, *Command → DRIVING, *Repository, *Gateway → DRIVEN)
    // - Method signatures (complex logic → DRIVING, CRUD → DRIVEN)
}
```

### IR Views

Plugins access the IR through read-only views defined in `engine-spi`:

```java
// Plugin code
@Override
public void execute(PluginContext context) {
    // Get analyzed application
    AnalyzedApplication app = context.getAnalyzedApplication();

    // Access ports
    for (PortView port : app.getPorts()) {
        String name = port.qualifiedName();
        PortDirection dir = port.direction();
        List<MethodView> methods = port.methods();
        // ... generate code
    }
}
```

### Semantic Analysis: Where What Lives

**Critical Design Principle:** Semantic analysis happens during ANALYZE phase, NOT in view adapters.

#### ANALYZE Phase Responsibilities

During compilation, the ANALYZE phase:
- Extracts raw structures (types, ports, services)
- **Classifies domain types** (Entity, Value Object, Aggregate Root)
- **Detects aggregate roots** using annotations, ports, package conventions, naming
- **Resolves nullability** from diverse annotation libraries
- **Indexes annotations** for fast lookup
- Stores all semantic decisions in the IR

```java
// Example: AggregateRootClassifier (runs during ANALYZE)
AggregateRootEvidence evidence = classifier.classify(domainType);
if (evidence.isAggregateRoot()) {
    // Store in IR: kind = AGGREGATE_ROOT
    domainType = domainType.withKind(AGGREGATE_ROOT);
}
```

#### View Adapter Responsibilities

IR view adapters (thin mapping layer):
- Convert internal IR models → SPI views
- **NO semantic analysis**
- **NO heuristics or classification**
- **NO recomputation of decisions**
- Pure structural mapping

```java
// IrViewAdapter: DUMB mapping, NO analysis
private static DomainTypeView adaptDomainType(DomainType domainType) {
    // Just map fields, no logic:
    return DomainTypeView.of(
        domainType.qualifiedName(),
        domainType.simpleName(),
        domainType.kind(),           // ← Already computed during ANALYZE
        domainType.type(),
        propertyViews,
        idView,
        domainType.isImmutable(),
        description
    );
}
```

#### Why This Matters

**Performance:** Semantic analysis is expensive. Running it once during ANALYZE (per compilation) is far better than running it repeatedly (per plugin, per access).

**Correctness:** Centralizing classification logic ensures consistency. All plugins see the same aggregate root decisions.

**Debuggability:** Storing evidence allows diagnostics explaining WHY a type was classified.

**SPI Stability:** Adapters don't depend on analyzers/classifiers. SPI views remain stable even if analysis logic evolves.

---

## Type System

HexaGlue uses a platform-agnostic type representation.

### TypeRef Hierarchy

```
TypeRef (abstract)
├── PrimitiveRef (int, boolean, etc.)
├── ArrayRef (T[])
├── ClassRef (simple classes)
├── GenericTypeRef (List<T>)
│   └── TypeArgument
│       ├── ConcreteType
│       ├── WildcardType (? extends T)
│       └── TypeVariable (T)
├── ParameterizedTypeRef
└── VoidRef
```

### Nullability Tracking

Types track nullability from annotations:

```java
TypeRef type = method.returnType();

if (type.isNullable()) {
    // Generate null checks
} else {
    // Assume non-null
}
```

Supported nullability annotations:
- JSR-305: `@Nullable`, `@Nonnull`
- JetBrains: `@Nullable`, `@NotNull`
- Eclipse: `@Nullable`, `@NonNull`
- Checker Framework: `@Nullable`, `@NonNull`
- jMolecules: Infers from `@ValueObject` (immutable → non-null)

### Type Operations

```java
// Type comparison
TypeComparators.areEqual(type1, type2);
TypeComparators.isAssignableFrom(supertype, subtype);

// Type rendering
String qualified = TypeDisplay.qualifiedName(type);
String simple = TypeDisplay.simpleName(type);
String description = TypeDisplay.describe(type);
```

---

## Plugin SPI Architecture

### Plugin Discovery

Plugins are discovered using Java's ServiceLoader:

```
plugin-name.jar
└── META-INF/
    └── services/
        └── io.hexaglue.spi.HexaGluePlugin
            (contains: io.hexaglue.plugin.name.NamePlugin)
```

### Plugin Interface

```java
package io.hexaglue.spi;

public interface HexaGluePlugin {

    /** Unique plugin identifier */
    String id();

    /** Plugin metadata (name, description, version) */
    PluginMetadata metadata();

    /** Execution order (EARLY, NORMAL, LATE) */
    PluginOrder order();

    /** Execute plugin logic */
    void execute(PluginContext context);
}
```

### Plugin Lifecycle

```java
// 1. Discovery (DISCOVER phase)
ServiceLoader<HexaGluePlugin> loader =
    ServiceLoader.load(HexaGluePlugin.class);

// 2. Sorting (by PluginOrder)
List<HexaGluePlugin> sorted = loader.stream()
    .sorted(Comparator.comparing(HexaGluePlugin::order))
    .collect(toList());

// 3. Execution (GENERATE phase only)
for (HexaGluePlugin plugin : sorted) {
    plugin.execute(context);
}
```

### Plugin Isolation

Plugins are isolated from each other:

- No inter-plugin communication
- No shared state
- Each plugin sees same IR snapshot
- Execution order within same `PluginOrder` is undefined

### Plugin Order

```java
public enum PluginOrder {
    EARLY,   // Documentation, validation
    NORMAL,  // Most generation plugins
    LATE     // Aggregation, summary reports
}
```

📖 **For plugin development details**, see the [plugins repository](/plugins).

---

## Configuration System

### Configuration Loading

Configuration is loaded from `hexaglue.yaml`:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.example:
      basePackage: com.example.infrastructure
      enableFeature: true
```

File locations (in priority order):
1. Project root: `./hexaglue.yaml`
2. Resources directory: `src/main/resources/hexaglue.yaml`
3. Any location on the compile-time classpath

### Evidence-Based Resolution

Configuration uses an evidence-based resolution pattern:

```
Priority (highest to lowest):
1. Annotations on source code
2. YAML configuration
3. Heuristics (naming conventions, structure)
4. Defaults
```

Each resolution stores evidence (provenance) explaining WHERE the value came from.

### Property Metadata

The SPI provides helpers for accessing property-level metadata:

```java
// In plugin code
Optional<Integer> columnLength = PropertyMetadataHelper.getPropertyMetadata(
    options,
    "com.example.domain.Customer",
    "name",
    "column",
    "length",
    Integer.class
);
```

This enables hierarchical configuration:

```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.jpa:
      types:
        com.example.domain.Customer:
          properties:
            name:
              column:
                length: 100
                nullable: false
```

---

## Diagnostic System

### Diagnostic Flow

```
Plugin/Core
    │
    │ report(diagnostic)
    ▼
DiagnosticReporter
    │
    │ route by severity
    ▼
JSR-269 Messager
    │
    ▼
Compiler Output
```

### Structured Codes

All diagnostics use structured codes:

```
HG-{COMPONENT}-{NUMBER}

Where:
- COMPONENT: CORE, PORTDOCS, JPA, etc.
- NUMBER:
  - 001-099: INFO
  - 100-199: WARN
  - 200-299: ERROR
```

**Engine diagnostic codes:**
- `HG-CORE-001` to `HG-CORE-099` - Informational messages
- `HG-CORE-100` to `HG-CORE-199` - Warnings
- `HG-CORE-200` to `HG-CORE-299` - Errors

Example diagnostics:
- `HG-CORE-001` - Port discovery complete
- `HG-CORE-002` - Domain types analyzed
- `HG-CORE-200` - Compilation error

---

## Performance Considerations

### Incremental Compilation

HexaGlue supports incremental compilation:

- Only reprocess changed files
- Cache IR between rounds
- Minimize regeneration

### Memory Management

- IR uses efficient data structures
- Plugins should avoid caching
- Large code generation uses streaming

### Compilation Time

Typical overhead:

- Small projects (<50 ports): <1 second
- Medium projects (50-200 ports): 1-3 seconds
- Large projects (>200 ports): 3-10 seconds

---

## Extension Points

### For Plugin Developers

1. **Implement HexaGluePlugin** - Main extension point
2. **Use PluginContext** - Access IR and generation API
3. **Report diagnostics** - Structured error reporting
4. **Generate artifacts** - Source files, resources, docs

📖 **See** [Plugin Development Guide](/plugins)

### For Core Contributors

1. **Add analyzers** - New analysis capabilities (e.g., event detection)
2. **Extend IR** - New model types (e.g., commands, queries)
3. **Add utilities** - Helper classes in `engine-spi`
4. **Improve type system** - Better type inference, resolution

---

## Future Architecture

Planned enhancements:

1. **IR Persistence** - Cache IR across builds
2. **Parallel Plugin Execution** - Run independent plugins concurrently
3. **Incremental Generation** - Only regenerate changed artifacts
4. **Plugin Dependencies** - Allow plugins to declare dependencies
5. **Custom Analyzers** - Plugin-contributed analysis

---

## Related Documentation

- [Engine Testing Harness](engine-testing-harness/README.md) - Testing utilities documentation
- [Plugins Repository](https://github.com/hexaglue/plugins) - Plugin development and available plugins
- [Examples Repository](https://github.com/hexaglue/examples) - Usage examples

---

## Support

- [GitHub Issues](https://github.com/hexaglue/engine/issues) - Bug reports and feature requests
- [GitHub Discussions](https://github.com/hexaglue/engine/discussions) - Architecture questions

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
