# HexaGlue Engine Diagnostic Codes

**Structured diagnostic system reference for HexaGlue Engine**

---

## Table of Contents

- [Overview](#overview)
- [Code Format](#code-format)
- [Severity Levels](#severity-levels)
- [Engine Diagnostic Codes](#engine-diagnostic-codes)
- [Plugin Diagnostic Codes](#plugin-diagnostic-codes)
- [Using Diagnostic Codes](#using-diagnostic-codes)
- [Best Practices](#best-practices)

---

## Overview

HexaGlue Engine uses structured diagnostic codes for all messages emitted during compilation. Each diagnostic has:

- **Code** - Unique identifier (e.g., `HG-CORE-001`)
- **Severity** - INFO, WARNING, or ERROR
- **Message** - Human-readable description
- **Location** - Optional source code location (file, line, column)
- **Source** - Component that reported it (engine or plugin ID)

### Why Structured Codes?

1. **Debuggability** - Easier to search for specific errors
2. **Documentation** - Each code can be documented with solutions
3. **Filtering** - Users can suppress specific warnings
4. **Stability** - Code numbers remain stable across versions
5. **Automation** - Tools can react to specific diagnostic codes

---

## Code Format

All diagnostic codes follow this format:

```
HG-{COMPONENT}-{NUMBER}
```

### Components

- **HG** - HexaGlue prefix (all diagnostics)
- **COMPONENT** - Identifies the source
  - `CORE` - Core engine (discovery, analysis, validation)
  - `WRITE` - File writing subsystem
  - `MERGE` - Code merging subsystem
  - `SEMANTICS` - Semantic analysis
  - Plugin-specific identifiers (see [Plugin Diagnostic Codes](#plugin-diagnostic-codes))
- **NUMBER** - 3-digit number indicating severity range

### Number Ranges

```
001-099: Informational messages (INFO)
100-199: Warnings (WARN)
200-299: Errors (ERROR)
```

### Examples

```
HG-CORE-001      Core informational message
HG-CORE-100      Core warning
HG-CORE-200      Core error

HG-WRITE-001     File writing info
HG-MERGE-200     Merge engine error
```

---

## Severity Levels

### INFO (001-099)

Informational messages about normal processing:

- Successful operations
- Statistics and summaries
- Progress updates

**User Impact:** None - just informative

**Example:**
```
[INFO] [HG-CORE-001] Discovered 5 ports
[INFO] [HG-CORE-002] Discovered 8 domain types
```

### WARN (100-199)

Warnings about potential issues that don't prevent compilation:

- Code quality concerns
- Performance warnings
- Deprecation notices
- Non-critical configuration issues

**User Impact:** Should be addressed but not blocking

**Example:**
```
[WARN] [HG-CORE-100] No ports found in compilation
[WARN] [HG-MERGE-100] Orphaned custom blocks detected in file
```

### ERROR (200-299)

Errors that prevent successful code generation:

- Invalid port definitions
- Type resolution failures
- I/O errors during generation
- Merge conflicts

**User Impact:** Must be fixed for compilation to succeed

**Example:**
```
[ERROR] [HG-CORE-200] Port 'CustomerRepository' is a class, must be an interface
[ERROR] [HG-WRITE-200] Failed to write source file: CustomerEntity.java
[ERROR] [HG-MERGE-200] Parse error in custom block markers
```

---

## Engine Diagnostic Codes

### HG-CORE-xxx (Core Engine)

Core engine diagnostics for discovery, analysis, and validation.

#### Informational (001-099)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-001` | Discovered N ports | Successfully discovered ports in codebase |
| `HG-CORE-002` | Discovered N domain types | Successfully discovered domain types |
| `HG-CORE-003` | Discovered N application services | Successfully discovered application services |
| `HG-CORE-010` | Loading plugin: {id} | Plugin being loaded |
| `HG-CORE-011` | Plugin loaded: {name} v{version} | Plugin successfully loaded |
| `HG-CORE-020` | Analysis phase complete | Analysis phase finished |
| `HG-CORE-021` | Validation phase complete | Validation phase finished |
| `HG-CORE-022` | Generation phase complete | Generation phase finished |

#### Warnings (100-199)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-100` | No ports found in compilation | No interfaces matching port heuristics |
| `HG-CORE-101` | No domain types found | No domain types discovered |
| `HG-CORE-110` | Plugin {id} has no metadata | Plugin didn't provide metadata |
| `HG-CORE-111` | Plugin {id} requires HexaGlue {version} | Version incompatibility warning |
| `HG-CORE-120` | Port {name} has no methods | Empty port interface |
| `HG-CORE-121` | Type {name} could not be resolved | Type resolution incomplete |

#### Errors (200-299)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-200` | Port {name} is not an interface | Port must be an interface |
| `HG-CORE-201` | Port {name} has only default methods | Port has no abstract methods |
| `HG-CORE-208` | Validation rule failed: {details} | Validation rule execution failed |
| `HG-CORE-210` | Plugin {id} failed to load | Plugin loading failed |
| `HG-CORE-211` | Plugin {id} execution failed | Plugin threw exception |
| `HG-CORE-220` | Circular dependency detected: {cycle} | Circular type dependencies |
| `HG-CORE-221` | Type {name} not found | Cannot resolve type reference |
| `HG-CORE-230` | I/O error writing {file} | File write failed |

---

### HG-CORE-CODEGEN-xxx (Code Generation)

Code generation subsystem diagnostics.

**Source:** `io.hexaglue.core.codegen.*`

#### Informational (001-099)

*Reserved for future use*

#### Warnings (100-199)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-CODEGEN-101` | Merge mode not supported: {mode} | Merge mode not yet fully supported, using OVERWRITE |

#### Errors (200-299)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-CODEGEN-200` | Internal error: {details} | Internal error in code generation orchestration |
| `HG-CORE-CODEGEN-201` | Generation conflict: {details} | Conflict detected during code generation |
| `HG-CORE-CODEGEN-202` | Plugin generation failed: {details} | Plugin failed during generation phase |
| `HG-CORE-CODEGEN-203` | I/O error: {details} | File I/O error during artifact emission |
| `HG-CORE-CODEGEN-204` | Invalid resource: {details} | Resource file has invalid content |

**Example:**
```java
// From GenerationOrchestrator.java
DiagnosticCode CODE_INTERNAL_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-200");
DiagnosticCode CODE_CONFLICT = DiagnosticCode.of("HG-CORE-CODEGEN-201");
DiagnosticCode CODE_PLUGIN_ERROR = DiagnosticCode.of("HG-CORE-CODEGEN-202");
```

---

### HG-CORE-IR-xxx (Intermediate Representation)

IR analysis and extraction diagnostics.

**Source:** `io.hexaglue.core.internal.ir.*`

#### Informational (001-099)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-IR-001` | JMolecules @Repository annotation detected | JMolecules repository found |


#### Warnings (100-199)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-IR-100` | Domain type extraction failed: {details} | Domain type extraction encountered an issue |
| `HG-CORE-IR-103` | Domain service extraction failed: {details} | Domain service extraction encountered an issue |

#### Errors (200-299)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-IR-200` | JMolecules @AggregateRoot annotation detected | JMolecules aggregate root found |
| `HG-CORE-IR-201` | JMolecules @ValueObject annotation detected | JMolecules value object found |
| `HG-CORE-IR-202` | JMolecules @Entity annotation detected | JMolecules entity found |
| `HG-CORE-IR-203` | JMolecules @DomainEvent annotation detected | JMolecules domain event found |
| `HG-CORE-IR-204` | JMolecules @Identity annotation detected | JMolecules identity found |
| `HG-CORE-IR-205` | Application service extraction failed: {details} | Application service extraction failed |
| `HG-CORE-IR-206` | Application service validation failed: {details} | Application service validation failed |
| `HG-CORE-IR-207` | IR snapshot validation failed: {details} | IR snapshot validation failed - internal error |

---

### HG-CORE-PLUGIN-xxx (Plugin System)

Plugin system diagnostics.

**Source:** `io.hexaglue.core.*`

#### Informational (001-099)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-PLUGIN-001` | Plugin information | Plugin-related informational message |

#### Warnings (100-199)

*Reserved for future use*

#### Errors (200-299)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-PLUGIN-200` | Plugin encountered unexpected error during GENERATE | Plugin failed during generation phase |

---

### HG-CORE-SEMANTICS-xxx (Semantic Analysis - Internal)

Internal semantic analysis diagnostics (core engine).

**Source:** `io.hexaglue.core.internal.ir.domain.semantics.*`

#### Informational (001-099)

| Code | Message | Description |
|------|---------|-------------|
| `HG-CORE-SEMANTICS-001` | Weak signal ignored: {details} | Weak DDD signal ignored during classification |
| `HG-CORE-SEMANTICS-002` | Convention triggered: {details} | Naming convention triggered during classification |

#### Warnings (100-199)

*Reserved for future use*

#### Errors (200-299)

*Reserved for future use*

---

### HG-WRITE-xxx (File Writing)

File writing subsystem diagnostics.

**Source:** `io.hexaglue.core.codegen.write.*`

#### Informational (001-099)

*Reserved for future use*

#### Warnings (100-199)

| Code | Message | Description |
|------|---------|-------------|
| `HG-WRITE-100` | Invalid resource path: {path} | Resource path validation failed |

#### Errors (200-299)

| Code | Message | Description |
|------|---------|-------------|
| `HG-WRITE-200` | Failed to write source file: {file} | Source file write failed |
| `HG-WRITE-201` | Failed to write resource file: {file} | Resource file write failed |

**Example:**
```java
// From FilerWriter.java
DiagnosticCode CODE_SOURCE_WRITE_FAILED = DiagnosticCode.of("HG-WRITE-200");
DiagnosticCode CODE_RESOURCE_WRITE_FAILED = DiagnosticCode.of("HG-WRITE-201");
```

---

### HG-MERGE-xxx (Code Merging)

Code merging subsystem diagnostics for preserving custom code blocks.

**Source:** `io.hexaglue.core.codegen.merge.*`

#### Informational (001-099)

*Reserved for future use*

#### Warnings (100-199)

| Code | Message | Description |
|------|---------|-------------|
| `HG-MERGE-100` | Orphaned custom blocks detected: {blocks} | Custom blocks exist but have no matching markers in new generation |

#### Errors (200-299)

| Code | Message | Description |
|------|---------|-------------|
| `HG-MERGE-200` | Parse error in custom block markers: {error} | Cannot parse custom code block markers |
| `HG-MERGE-201` | Merge operation failed: {error} | Merge engine encountered unrecoverable error |

**Example:**
```java
// From MergeEngine.java
DiagnosticCode CODE_PARSE_ERROR = DiagnosticCode.of("HG-MERGE-200");
DiagnosticCode CODE_ORPHANED_BLOCKS = DiagnosticCode.of("HG-MERGE-100");
DiagnosticCode CODE_MERGE_FAILED = DiagnosticCode.of("HG-MERGE-201");
```

**Merge system overview:**
- Preserves code between `// CUSTOM-START:{id}` and `// CUSTOM-END:{id}` markers
- Detects orphaned blocks when marker IDs no longer exist in new generation
- Reports parse errors when marker format is invalid

📖 **For detailed merge behavior**, see [ARCHITECTURE.md - Code Merging](ARCHITECTURE.md#code-merging)

---

### HG-SEMANTICS-xxx (Semantic Analysis)

Semantic analysis diagnostics for domain modeling validation.

**Source:** `io.hexaglue.core.internal.ir.domain.semantics.*`

#### Informational (001-099)

| Code | Message | Description |
|------|---------|-------------|
| `HG-SEMANTICS-001` | DDD aggregate reference warning: {details} | Cross-aggregate reference detected - should use ID reference |

**DDD Best Practice:**
Aggregates should reference each other by ID, not direct object reference, to maintain consistency boundaries.

**Example:**
```java
// ❌ BAD - Direct aggregate reference
@AggregateRoot
public class Order {
    private Customer customer;  // Direct reference - triggers HG-SEMANTICS-001
}

// ✅ GOOD - ID reference
@AggregateRoot
public class Order {
    private CustomerId customerId;  // ID reference - clean boundary
}
```

---

## Plugin Diagnostic Codes

Plugins define their own diagnostic codes following the same convention:

```
HG-{PLUGIN_ID}-{NUMBER}
```

### Available Plugins

📖 **For plugin-specific diagnostic codes**, see:

- **Port Documentation Plugin** - [plugins/plugin-portdocs](https://github.com/hexaglue/plugins/tree/main/plugin-portdocs)
  - `HG-PORTDOCS-001` to `HG-PORTDOCS-299`
  - Documentation generation diagnostics

- **JPA Repository Plugin** - [plugins/plugin-jpa-repository](https://github.com/hexaglue/plugins/tree/main/plugin-jpa-repository)
  - `HG-JPA-001` to `HG-JPA-299`
  - JPA entity and repository generation diagnostics

### Custom Plugin Codes

When creating your own plugin, follow this convention:

```
HG-{YOUR_PLUGIN_ID}-{NUMBER}

Where:
- YOUR_PLUGIN_ID: Short, uppercase identifier (e.g., MYCOMPANY, CUSTOM)
- NUMBER:
  - 001-099: INFO
  - 100-199: WARN
  - 200-299: ERROR
```

**Example:**
```java
public class MyPlugin implements HexaGluePlugin {
    private static final DiagnosticCode START =
        DiagnosticCode.of("HG-MYPLUGIN-001");
    private static final DiagnosticCode CONFIG_WARNING =
        DiagnosticCode.of("HG-MYPLUGIN-100");
    private static final DiagnosticCode GENERATION_ERROR =
        DiagnosticCode.of("HG-MYPLUGIN-200");
}
```

📖 **For complete plugin development guide**, see [plugins/PLUGIN_DEVELOPMENT.md](https://github.com/hexaglue/plugins/blob/main/PLUGIN_DEVELOPMENT.md)

---

## Using Diagnostic Codes

### In Plugin Code

```java
import io.hexaglue.spi.context.DiagnosticReporter;
import io.hexaglue.spi.diagnostics.DiagnosticCode;

public class MyPlugin implements HexaGluePlugin {

    // Define constants
    private static final DiagnosticCode START =
        DiagnosticCode.of("HG-MYPLUGIN-001");
    private static final DiagnosticCode WARNING =
        DiagnosticCode.of("HG-MYPLUGIN-100");
    private static final DiagnosticCode ERROR =
        DiagnosticCode.of("HG-MYPLUGIN-200");

    @Override
    public void execute(PluginContext context) {
        DiagnosticReporter diagnostics = context.getDiagnostics();

        // Info
        diagnostics.info(START, "Starting plugin execution");

        // Warning
        if (ports.isEmpty()) {
            diagnostics.warn(WARNING, "No ports found");
        }

        // Error
        try {
            generateCode();
        } catch (Exception e) {
            diagnostics.error(ERROR, "Generation failed", e);
        }
    }
}
```

### In Test Code

```java
import io.hexaglue.testing.HexaGlueTestHarness;
import io.hexaglue.spi.diagnostics.DiagnosticCode;

@Test
void shouldWarnWhenNoPortsFound() {
    HexaGlueTestHarness.forPlugin(new MyPlugin())
        .withSource("EmptyClass.java", "public class Empty {}")
        .compile()
        .expectSuccess()
        .expectDiagnostic(
            DiagnosticCode.of("HG-MYPLUGIN-100"),
            "No ports found"
        );
}
```

📖 **For testing examples**, see [engine-testing-harness/README.md](engine-testing-harness/README.md)

---

## Best Practices

### For Plugin Developers

1. **Define all codes as constants** - No magic strings in code
   ```java
   // ✅ GOOD
   private static final DiagnosticCode START = DiagnosticCode.of("HG-MYPLUGIN-001");
   diagnostics.info(START, "Starting");

   // ❌ BAD
   diagnostics.info("HG-MYPLUGIN-001", "Starting");
   ```

2. **Document each code** - Add Javadoc explaining when it's emitted
   ```java
   /**
    * Emitted when plugin starts execution.
    */
   private static final DiagnosticCode START = DiagnosticCode.of("HG-MYPLUGIN-001");
   ```

3. **Use appropriate severity** - Don't make everything an error
   - INFO: Normal operations, statistics
   - WARN: Potential issues, quality concerns, deprecated features
   - ERROR: Must be fixed for compilation to succeed

4. **Provide actionable messages** - Tell users what to do
   ```java
   // ✅ GOOD
   "No @Id field found for Customer. Add @Identity annotation or configure ID field in hexaglue.yaml"

   // ❌ BAD
   "No ID field"
   ```

5. **Include context** - Add relevant names, values, locations
   ```java
   diagnostics.error(
       ERROR_CODE,
       "Cannot generate entity for type '" + domainType.qualifiedName() +
       "': circular dependency detected between " + typeA + " and " + typeB
   );
   ```

6. **Keep codes stable** - Don't reuse or renumber codes across versions
   - Once assigned, a code number is permanent
   - Deprecated codes can be marked as obsolete but never reused

7. **Test diagnostics** - Verify correct codes are emitted
   ```java
   @Test
   void shouldEmitErrorOnInvalidConfig() {
       HexaGlueTestHarness.forPlugin(new MyPlugin())
           .withInvalidConfig()
           .compile()
           .expectFailure()
           .expectDiagnostic(ERROR_CODE, "Invalid configuration");
   }
   ```

### For Tool Integrators

1. **Parse diagnostic codes** - Extract code from messages for automation
2. **Filter by severity** - Allow users to suppress warnings by code
3. **Link to documentation** - Provide URLs to code reference docs
4. **Aggregate diagnostics** - Group related diagnostics together
5. **Track code stability** - Monitor for new or changed codes across versions

---

## Related Documentation

- [SPI Reference](engine-spi/SPI_REFERENCE.md) - Complete SPI documentation
- [Plugin Development](https://github.com/hexaglue/plugins/blob/main/PLUGIN_DEVELOPMENT.md) - Creating plugins
- [Testing Harness](engine-testing-harness/README.md) - Testing diagnostics
- [Architecture](ARCHITECTURE.md) - Engine internals

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
