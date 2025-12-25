# HexaGlue SPI Reference

**Service Provider Interface for plugin developers**

> **Status:** This document defines the stability guarantees for the HexaGlue SPI.
> **Current SPI Version:** 1.0.0

---

## Overview

The **engine-spi** module is the **only stable contract** between HexaGlue Engine and plugins. It provides read-only views of analyzed domain models, ports, and application services, along with utilities for code generation, diagnostics, and naming conventions.

**Key Principles:**
- Plugins MUST depend only on `engine-spi`, never on `engine-core` internals
- Zero external dependencies (JDK-only)
- Semantic versioning with explicit stability guarantees
- Forward compatibility through interface default methods

---

## Version Policy

HexaGlue SPI follows **Semantic Versioning 2.0**:

- **MAJOR** version (X.0.0): Breaking changes to stable APIs
- **MINOR** version (1.X.0): New features, evolvable APIs extended (backward compatible)
- **PATCH** version (1.0.X): Bug fixes only

**Current Version:** `HexaGlueVersion.SPI_VERSION = 1`

**Compatibility Promise:**
- Code compiled against SPI 1.0.y will run on all 1.x.y versions
- No breaking changes within 1.x.y
- A deprecation warning will be issued at least one major version before removal

---

## Stability Levels

### STABLE - Never breaks in 1.x

APIs marked as **STABLE** are guaranteed to:
- Never change method signatures
- Never remove types, methods, or fields
- Never change semantic behavior
- Maintain binary and source compatibility across all 1.x releases

**Evolution strategy:**
- New methods added with default implementations (interfaces)
- New optional fields added via builders (classes)
- Deprecation warnings before removal in MAJOR version

### EVOLVABLE - Can be extended (non-breaking)

APIs marked as **EVOLVABLE** are stable but may evolve:
- New methods may be added (with defaults or new overloads)
- New fields may be added (via builders)
- New subtypes may be introduced
- Existing methods/fields never removed or changed

**When to use:**
- Read-only, use returned values
- Do NOT implement these interfaces yourself (engine-core provides implementations)
- Do NOT extend/subclass these types

### EXPERIMENTAL - May change or be removed

APIs marked as **EXPERIMENTAL**:
- May change signature or semantics in MINOR versions
- May be removed in MINOR versions
- Use at your own risk for early access to features

---

## API Stability Map

### Plugin Contract (🔒 STABLE)

Core plugin interface and lifecycle:

| Type | Package | Description |
|------|---------|-------------|
| `HexaGluePlugin` | `io.hexaglue.spi` | Main plugin interface |
| `PluginMetadata` | `io.hexaglue.spi` | Plugin description (name, version) |
| `PluginOrder` | `io.hexaglue.spi` | Execution order (EARLY, NORMAL, LATE) |
| `HexaGlueVersion` | `io.hexaglue.spi` | Version information |

**Example:**
```java
public class MyPlugin implements HexaGluePlugin {
    @Override
    public String id() {
        return "io.hexaglue.plugin.myname";
    }

    @Override
    public PluginMetadata metadata() {
        return PluginMetadata.of("MyPlugin", "Description",
            HexaGlueVersion.of(0, 1, 0));
    }

    @Override
    public PluginOrder order() {
        return PluginOrder.NORMAL;
    }

    @Override
    public void execute(PluginContext context) {
        // Plugin logic here
    }
}
```

---

### Plugin Context (🔒 STABLE)

Access to analyzed model and generation APIs:

| Type | Package | Description |
|------|---------|-------------|
| `PluginContext` | `io.hexaglue.spi.context` | Root entry point for plugins |
| `AnalyzedApplication` | `io.hexaglue.spi.context` | Access to analyzed IR |

**PluginContext methods:**
```java
public interface PluginContext {
    AnalyzedApplication getAnalyzedApplication();  // IR views
    PluginOptionsView getOptions();                // Configuration
    DiagnosticReporter getDiagnostics();           // Error reporting
    Filer getFiler();                              // Code generation
    ProcessingEnvironment getProcessingEnv();      // JSR-269 environment
}
```

---

### IR Views - Domain Model

#### DomainTypeView (🔄 EVOLVABLE)

Represents analyzed domain types:

```java
public interface DomainTypeView {
    String qualifiedName();                    // 🔒 STABLE
    String simpleName();                       // 🔒 STABLE
    String packageName();                      // 🔒 STABLE

    DomainTypeKind kind();                     // 🔒 STABLE
    boolean isAggregateRoot();                 // 🔒 STABLE
    boolean isEntity();                        // 🔒 STABLE
    boolean isValueObject();                   // 🔒 STABLE
    boolean isImmutable();                     // 🔒 STABLE

    List<PropertyView> properties();           // 🔒 STABLE
    Optional<PropertyView> identity();         // 🔒 STABLE

    TypeRef type();                            // 🔒 STABLE
    Optional<String> documentation();          // 🔒 STABLE

    List<AnnotationView> annotations();        // 🔄 EVOLVABLE (may add methods)
}
```

**DomainTypeKind:**
```java
public enum DomainTypeKind {
    AGGREGATE_ROOT,   // Root of consistency boundary
    ENTITY,           // Object with identity
    VALUE_OBJECT,     // Immutable descriptive object
    DOMAIN_SERVICE,   // Stateless domain operation
    UNKNOWN           // Not classified
}
```

#### PropertyView (🔄 EVOLVABLE)

Represents domain type properties:

```java
public interface PropertyView {
    String name();                             // 🔒 STABLE
    TypeRef type();                            // 🔒 STABLE
    boolean isNullable();                      // 🔒 STABLE
    boolean isCollection();                    // 🔒 STABLE
    Optional<String> documentation();          // 🔒 STABLE
    List<AnnotationView> annotations();        // 🔄 EVOLVABLE
}
```

---

### IR Views - Ports

#### PortView (🔄 EVOLVABLE)

Represents application ports (interfaces):

```java
public interface PortView {
    String qualifiedName();                    // 🔒 STABLE
    String simpleName();                       // 🔒 STABLE
    String packageName();                      // 🔒 STABLE

    PortDirection direction();                 // 🔒 STABLE

    List<MethodView> methods();                // 🔒 STABLE
    Optional<String> documentation();          // 🔒 STABLE
    List<AnnotationView> annotations();        // 🔄 EVOLVABLE
}
```

**PortDirection:**
```java
public enum PortDirection {
    DRIVING,    // Inbound - what application offers
    DRIVEN      // Outbound - what application requires
}
```

#### MethodView (🔄 EVOLVABLE)

Represents port methods:

```java
public interface MethodView {
    String name();                             // 🔒 STABLE
    TypeRef returnType();                      // 🔒 STABLE
    List<ParameterView> parameters();          // 🔒 STABLE
    Optional<String> documentation();          // 🔒 STABLE
    List<AnnotationView> annotations();        // 🔄 EVOLVABLE
}
```

---

### Type System (🔒 STABLE)

Platform-agnostic type representation:

#### TypeRef Hierarchy

```java
TypeRef (abstract)
├── PrimitiveRef        // int, boolean, etc.
├── ArrayRef            // T[]
├── ClassRef            // Simple classes
├── GenericTypeRef      // List<T>
└── VoidRef            // void
```

#### TypeRef API

```java
public interface TypeRef {
    String qualifiedName();                    // e.g., "java.util.List"
    String simpleName();                       // e.g., "List"
    boolean isPrimitive();                     // true for int, boolean, etc.
    boolean isArray();                         // true for T[]
    boolean isVoid();                          // true for void
    boolean isNullable();                      // Based on annotations

    // For generic types
    boolean isGeneric();
    List<TypeRef> typeArguments();             // For List<String>, returns [String]
}
```

**Type rendering utilities:**
```java
// In plugin code
String qualified = TypeDisplay.qualifiedName(type);
// → "java.util.List<com.example.Customer>"

String simple = TypeDisplay.simpleName(type);
// → "List<Customer>"
```

**Nullability detection:**

HexaGlue detects nullability from annotations:
- JSR-305: `@Nullable`, `@Nonnull`
- JetBrains: `@Nullable`, `@NotNull`
- Eclipse: `@Nullable`, `@NonNull`
- Checker Framework: `@Nullable`, `@NonNull`

```java
TypeRef type = method.returnType();
if (type.isNullable()) {
    // Generate null checks
}
```

---

### Annotations (🔄 EVOLVABLE)

#### AnnotationView

Access to source annotations:

```java
public interface AnnotationView {
    String qualifiedName();                    // 🔒 STABLE

    // Get attribute value
    <T> Optional<T> attribute(String name, Class<T> type);  // 🔒 STABLE

    // Get all attributes
    Map<String, Object> attributes();          // 🔒 STABLE
}
```

**Example:**
```java
// Find @Column annotation
Optional<AnnotationView> column = property.annotations()
    .stream()
    .filter(a -> a.qualifiedName().equals("jakarta.persistence.Column"))
    .findFirst();

// Get length attribute
Integer length = column.flatMap(a -> a.attribute("length", Integer.class))
    .orElse(255);  // Default
```

---

### Configuration (🔒 STABLE)

#### PluginOptionsView

Access to plugin configuration from `hexaglue.yaml`:

```java
public interface PluginOptionsView {
    Optional<String> getString(String key);
    Optional<Boolean> getBoolean(String key);
    Optional<Integer> getInteger(String key);
    Optional<Map<String, Object>> getMap(String key);
    Optional<List<Object>> getList(String key);
}
```

**Example:**
```java
// Get basePackage option
String basePackage = context.getOptions()
    .getString("basePackage")
    .orElse("generated.infrastructure");

// Get nested configuration
Optional<Map<String, Object>> typesConfig =
    context.getOptions().getMap("types");
```

**Configuration structure:**
```yaml
hexaglue:
  plugins:
    io.hexaglue.plugin.myname:
      basePackage: com.example.infrastructure
      enableFeature: true
      types:
        com.example.domain.Customer:
          tableName: customers
```

#### PropertyMetadataHelper (🔄 EVOLVABLE)

Helper for accessing hierarchical property metadata:

```java
// Get property-level metadata
Optional<Integer> length = PropertyMetadataHelper.getPropertyMetadata(
    options,
    "com.example.domain.Customer",  // type
    "name",                          // property
    "column",                        // category
    "length",                        // key
    Integer.class                    // expected type
);

// Get entire category configuration
Map<String, Object> columnConfig = PropertyMetadataHelper.getPropertyConfig(
    options,
    "com.example.domain.Customer",
    "name",
    "column"
);
```

---

### Code Generation (🔒 STABLE)

#### Filer

Generate source and resource files:

```java
public interface Filer {
    void createSourceFile(String qualifiedName, String content);
    void createResource(String path, String content);
}
```

**Example:**
```java
Filer filer = context.getFiler();

// Generate Java source file
filer.createSourceFile(
    "com.example.infrastructure.CustomerAdapter",
    javaCode
);

// Generate resource file
filer.createResource(
    "docs/ports/CustomerRepository.md",
    markdownContent
);
```

---

### Diagnostics (🔒 STABLE)

#### DiagnosticReporter

Report structured diagnostics:

```java
public interface DiagnosticReporter {
    void info(String code, String message);
    void warn(String code, String message);
    void error(String code, String message);
    void error(String code, String message, Throwable cause);
}
```

**Diagnostic code convention:**
```
HG-{PLUGIN}-{NUMBER}

Where:
- PLUGIN: Your plugin identifier (uppercase)
- NUMBER:
  - 001-099: INFO
  - 100-199: WARN
  - 200-299: ERROR
```

**Example:**
```java
DiagnosticReporter diagnostics = context.getDiagnostics();

// Info
diagnostics.info("HG-MYPLUGIN-001", "Starting generation");

// Warning
diagnostics.warn("HG-MYPLUGIN-100", "No config found, using defaults");

// Error
diagnostics.error("HG-MYPLUGIN-200", "Failed to generate adapter for " + port.simpleName());
```

---

## Optional jMolecules Integration

HexaGlue can read jMolecules annotations to understand domain models better:

```java
import org.jmolecules.ddd.annotation.*;

@AggregateRoot
public class Customer {
    @Identity
    private CustomerId id;
    // ...
}
```

**Supported annotations:**
- `@AggregateRoot` - Marks aggregate roots
- `@Entity` - Marks entities
- `@ValueObject` - Marks value objects
- `@Identity` - Marks identity fields
- `@Repository` - Marks repository ports

**Benefits:**
- Removes ambiguity in classification
- Explicit domain semantics
- Better diagnostics

**Important:** jMolecules is **optional** - HexaGlue works without it using heuristics.

---

## Best Practices

### 1. Depend Only on SPI

```xml
<!-- GOOD -->
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>engine-spi</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- BAD - Will break -->
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>engine-core</artifactId>
</dependency>
```

### 2. Use Stability Annotations as Guidance

- **STABLE APIs** - Use freely, won't change
- **EVOLVABLE APIs** - Use but expect new methods may be added
- **EXPERIMENTAL APIs** - Use with caution, may change

### 3. Handle Evolution Gracefully

When new methods are added to evolvable interfaces:
```java
// Your plugin compiled against SPI 1.0
// will run on SPI 1.1 even if new methods were added
// (interfaces have default implementations)
```

### 4. Provide Defaults

```java
// Always provide sensible defaults
String basePackage = options.getString("basePackage")
    .orElse("generated.infrastructure");
```

### 5. Validate Assumptions

```java
// Check if expected annotations exist
Optional<AnnotationView> entity = type.annotations()
    .stream()
    .filter(a -> a.qualifiedName().equals("jakarta.persistence.Entity"))
    .findFirst();

if (entity.isEmpty()) {
    diagnostics.warn("HG-MYPLUGIN-100",
        "No @Entity annotation found, using heuristics");
}
```

---

## Related Documentation

- [Engine Architecture](../ARCHITECTURE.md) - Understanding IR and compilation pipeline
- [Plugin Development Guide](../../plugins/PLUGIN_DEVELOPMENT.md) - How to create plugins
- [Examples](../../examples/) - Working examples

---

## Support

- [GitHub Issues](https://github.com/hexaglue/engine/issues) - Bug reports
- [GitHub Discussions](https://github.com/hexaglue/engine/discussions) - API questions

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
