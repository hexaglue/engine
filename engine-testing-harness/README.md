# hexaglue-testing-harness

**Compile-time testing toolkit for HexaGlue core and plugins.**

This module provides utilities for testing annotation processors, particularly HexaGlue and its plugins, through **in-memory compilation** without filesystem dependencies.

---

## Table of Contents

- [Purpose](#purpose)
- [Key Features](#key-features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Core Components](#core-components)
- [Usage Examples](#usage-examples)
- [Testing Modes](#testing-modes)
- [Design Principles](#design-principles)
- [Development Guide](#development-guide)

---

## Purpose

HexaGlue is a **compile-time tool** based on JSR-269 annotation processing. The most valuable tests for such tools are **compile-time integration tests** that:

1. Provide input Java sources
2. Run the annotation processor (javac + HexaGlue)
3. Validate produced artifacts and diagnostics

`hexaglue-testing-harness` makes this testing workflow **simple**, **deterministic**, and **filesystem-free**.

---

## Key Features

- **In-memory compilation** - No temporary projects, no file system fixtures
- **Black-box testing** - Tests validate behavior as seen by users at compile-time
- **Diagnostic capture** - Access to all javac diagnostics (ERROR, WARNING, NOTE)
- **Generated file inspection** - Retrieve generated sources, resources, and docs
- **Deterministic** - Consistent results across platforms and environments
- **Fluent API** - Builder pattern for readable test scenarios

---

## Architecture

The harness operates in two conceptual layers:

### 1. **Input Layer** - Provide sources to compile

- `CompilationTestCase` - Main entry point with fluent builder API
- `MemoryJavaFileObject` - In-memory representation of `.java` sources

### 2. **Output Layer** - Capture compilation results

- `MemoryFileManager` - Intercepts `Filer` outputs (sources, resources, docs)
- `CompilationResult` - Immutable view of compilation outcome
- `GeneratedFilesSnapshot` - Captured generated files

### Execution Flow

```
┌─────────────────────────────────────────────────────────────┐
│  CompilationTestCase.builder()                              │
│    .addSourceFile("com.example.Port", "...")                │
│    .compile()                                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  JavaCompiler (javac)                                       │
│    - Reads MemoryJavaFileObject inputs                      │
│    - Runs HexaGlueProcessor                                 │
│    - Writes outputs to MemoryFileManager                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│  CompilationResult                                          │
│    - wasSuccessful()                                        │
│    - javacDiagnostics()                                     │
│    - generatedSourceFile("com.example.Gen")                 │
│    - generatedResourceText("docs/Port.md")                  │
└─────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### Add Dependency

```xml
<dependency>
    <groupId>io.hexaglue</groupId>
    <artifactId>hexaglue-testing-harness</artifactId>
    <version>${hexaglue.version}</version>
    <scope>test</scope>
</dependency>
```

### Write Your First Test

```java
import io.hexaglue.testing.CompilationTestCase;
import io.hexaglue.testing.CompilationResult;
import static com.google.common.truth.Truth.assertThat;
import org.junit.jupiter.api.Test;

class MyPortTest {

    @Test
    void shouldCompileValidPort() {
        CompilationResult result = CompilationTestCase.builder()
            .addSourceFile("com.example.port.CustomerRepository", """
                package com.example.port;

                public interface CustomerRepository {
                    void save(Customer customer);
                    Customer findById(String id);
                }
                """)
            .compile();

        // Assert compilation succeeded
        assertThat(result.wasSuccessful()).isTrue();

        // Assert no errors
        assertThat(result.javacDiagnostics()).isEmpty();
    }
}
```

---

## Core Components

### `CompilationTestCase`

**Main entry point** for writing compilation tests.

**Key methods:**
- `builder()` - Creates a new builder
- `addSourceFile(qualifiedName, content)` - Adds a Java source to compile
- `addJavacOption(option)` - Adds javac options (e.g., `-Xlint:all`)
- `addPlugin(plugin)` - Registers a plugin instance (for plugin unit tests)
- `setOption(key, value)` - Sets HexaGlue options
- `compile()` - Executes compilation and returns `CompilationResult`

**Example:**
```java
CompilationResult result = CompilationTestCase.builder()
    .addSourceFile("com.example.Port", sourceCode)
    .addJavacOption("-Xlint:deprecation")
    .compile();
```

---

### `CompilationResult`

**Immutable view** of compilation outcome.

**Key methods:**
- `wasSuccessful()` - Overall compilation success
- `javacDiagnostics()` - List of `javax.tools.Diagnostic` (ERROR, WARNING, NOTE)
- `formattedDiagnostics()` - Human-readable diagnostic messages
- `generatedSourceFile(qualifiedName)` - Retrieve generated Java source
- `generatedResourceText(pathKey)` - Retrieve generated resource as text
- `generatedResourceBytes(pathKey)` - Retrieve generated resource as bytes
- `options()` - Resolved `OptionsView` for assertions
- `exception()` - Harness exception if internal failure occurred

**Example:**
```java
// Check compilation succeeded
assertThat(result.wasSuccessful()).isTrue();

// Check generated adapter exists
Optional<String> adapter = result.generatedSourceFile("com.example.CustomerRepositoryImpl");
assertThat(adapter).isPresent();
assertThat(adapter.get()).contains("class CustomerRepositoryImpl");

// Check diagnostics
assertThat(result.javacDiagnostics()).hasSize(0);
```

---

### `MemoryFileManager`

**Intercepts `Filer` outputs** during annotation processing.

- Captures generated Java sources via `getJavaFileForOutput()`
- Captures resources/docs via `getFileForOutput()`
- Stores everything in-memory (no filesystem I/O)

**Resource key format:**
```
<LOCATION>/<packagePath>/<relativeName>
```

Examples:
- `SOURCE_OUTPUT/docs/ports/CustomerRepository.md`
- `CLASS_OUTPUT/META-INF/services/io.hexaglue.spi.HexaGluePlugin`

**Note:** Tests should match resources by suffix (e.g., `endsWith("docs/hello.md")`) to avoid overcoupling to exact key format.

---

### `MemoryJavaFileObject`

**In-memory representation** of Java source files.

Provides sources to `javac` without touching disk.

**Internal usage only** - tests use `CompilationTestCase.addSourceFile()` instead.

---

## Usage Examples

### Example 1: Test Successful Compilation

```java
@Test
void shouldCompilePortWithMultipleMethods() {
    CompilationResult result = CompilationTestCase.builder()
        .addSourceFile("com.example.port.OrderService", """
            package com.example.port;

            public interface OrderService {
                void createOrder(Order order);
                Order findById(String id);
                List<Order> findByCustomer(String customerId);
            }
            """)
        .compile();

    assertThat(result.wasSuccessful()).isTrue();
}
```

---

### Example 2: Test Compilation Failure

```java
@Test
void shouldRejectInvalidSyntax() {
    CompilationResult result = CompilationTestCase.builder()
        .addSourceFile("com.example.Invalid", """
            package com.example;

            public class Invalid {
                // Missing semicolon
                public String hello() { return "world" }
            }
            """)
        .compile();

    assertThat(result.wasSuccessful()).isFalse();

    // Check we have an ERROR diagnostic
    boolean hasError = result.javacDiagnostics().stream()
        .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
    assertThat(hasError).isTrue();
}
```

---

### Example 3: Test Diagnostic Messages

```java
@Test
void shouldCaptureWarnings() {
    CompilationResult result = CompilationTestCase.builder()
        .addSourceFile("com.example.Deprecated", """
            package com.example;

            @Deprecated
            public class Deprecated {
                @SuppressWarnings("unused")
                private String field;
            }
            """)
        .addJavacOption("-Xlint:deprecation")
        .compile();

    assertThat(result.wasSuccessful()).isTrue();

    // Compilation succeeds with warnings
    List<String> formatted = result.formattedDiagnostics();
    assertThat(formatted).isNotEmpty();
}
```

---

### Example 4: Test Generated Sources

```java
@Test
void shouldGenerateAdapter() {
    CompilationResult result = CompilationTestCase.builder()
        .addSourceFile("com.example.port.CustomerRepository", """
            package com.example.port;

            public interface CustomerRepository {
                Customer findById(String id);
            }
            """)
        .compile();

    assertThat(result.wasSuccessful()).isTrue();

    // Check generated adapter exists
    Optional<String> adapter = result.generatedSourceFile("com.example.adapters.CustomerRepositoryImpl");
    assertThat(adapter).isPresent();
    assertThat(adapter.get()).contains("implements CustomerRepository");
}
```

---

### Example 5: Test Generated Resources

```java
@Test
void shouldGenerateDocumentation() {
    CompilationResult result = CompilationTestCase.builder()
        .addSourceFile("com.example.port.OrderService", """
            package com.example.port;

            /**
             * Service for managing orders.
             */
            public interface OrderService {
                void createOrder(Order order);
            }
            """)
        .compile();

    assertThat(result.wasSuccessful()).isTrue();

    // Find doc resource by suffix
    Optional<String> doc = result.generatedResourceText("docs/ports/OrderService.md");
    assertThat(doc).isPresent();
    assertThat(doc.get()).contains("# OrderService");
}
```

---

### Example 6: Test Multiple Sources

```java
@Test
void shouldCompileMultipleRelatedSources() {
    CompilationResult result = CompilationTestCase.builder()
        .addSourceFile("com.example.Customer", """
            package com.example;

            public record Customer(String id, String name) {}
            """)
        .addSourceFile("com.example.port.CustomerRepository", """
            package com.example.port;
            import com.example.Customer;

            public interface CustomerRepository {
                void save(Customer customer);
            }
            """)
        .compile();

    assertThat(result.wasSuccessful()).isTrue();
}
```

---

## Testing Modes

### Mode A: Filer Capture (Current)

**Status**: ✅ **Production-ready**

The harness intercepts `javax.annotation.processing.Filer` outputs through a custom `JavaFileManager`. This provides realistic end-to-end testing as experienced by users.

**Captures:**
- Generated `.java` sources
- Resource files (docs, configs, etc.)
- javac diagnostics (ERROR, WARNING, NOTE)

**Use cases:**
- Integration tests for HexaGlue core
- Plugin end-to-end tests
- Verification of processor behavior

---

### Mode B: Hook-based HexaGlue-native Capture (Future)

**Status**: 🔮 **Planned** (requires core integration)

Will provide direct access to HexaGlue SPI artifacts and structured diagnostics.

**Would capture:**
- `io.hexaglue.spi.codegen.SourceFile` (with merge modes)
- `io.hexaglue.spi.codegen.DocFile`
- `io.hexaglue.spi.codegen.ResourceFile`
- Typed `io.hexaglue.spi.diagnostics.Diagnostic` (not just javac messages)

**Use cases:**
- Plugin unit tests with fine-grained assertions
- Testing merge strategies
- Diagnostic code validation

**Requirements:**
- Core support for `HexaGlueTestHooks`
- Injectable `ArtifactSink` and `DiagnosticReporter`
- Plugin instance injection (bypass ServiceLoader)

**Scaffolding:** `io.hexaglue.testing.internal.InMemoryArtifactSink` is already present but inactive.

---

## Design Principles

### 1. **Black-box Testing**

Tests validate processor behavior **as seen by users at compile-time**, not internal implementation details.

- ✅ Tests interact with `javac` API
- ✅ Tests assert on generated outputs
- ✅ Tests check diagnostics
- ❌ Tests do NOT reach into core internals

---

### 2. **In-memory Execution**

Everything stays in memory - no temporary projects, no filesystem fixtures.

- **Inputs**: Provided as `JavaFileObject`s backed by strings
- **Outputs**: Intercepted in-memory from `Filer`
- **Benefits**: Fast, deterministic, no cleanup required

---

### 3. **Deterministic Behavior**

Tests must not depend on:
- Filesystem layout
- Build directory behavior
- OS-dependent file separators
- System-specific paths

**Solution**: Use string-based keys and suffix matching for resources.

---

### 4. **Readable Test Scenarios**

A test should read like a scenario:

```java
// 1. Add sources
CompilationTestCase.builder()
    .addSourceFile("com.example.Port", "...")

// 2. Compile
    .compile()

// 3. Assert on outputs
assertThat(result.wasSuccessful()).isTrue();
```

---

## Development Guide

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CompilationTestCaseTest

# Run with verbose output
mvn test -X
```

---

### Adding New Tests

1. **Create test class** in `src/test/java/io/hexaglue/testing/`
2. **Use JUnit 5**: `@org.junit.jupiter.api.Test`
3. **Follow naming convention**: `*Test.java`
4. **Use Truth assertions**: `assertThat(...)`

**Example:**
```java
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;

class MyNewTest {

    @Test
    void shouldTestSomething() {
        CompilationResult result = CompilationTestCase.builder()
            .addSourceFile("com.example.Test", "...")
            .compile();

        assertThat(result.wasSuccessful()).isTrue();
    }
}
```

---

### Code Style

- **Formatting**: Palantir Java Format (via Spotless)
- **Applied automatically**: During `mvn process-sources`
- **Manual application**: `mvn spotless:apply`
- **Verification**: `mvn spotless:check`

---

### Package Structure

```
io.hexaglue.testing/
├── CompilationTestCase.java     (Public API - main entry point)
├── CompilationResult.java        (Public API - result view)
├── MemoryFileManager.java        (Package-private - Filer capture)
├── MemoryJavaFileObject.java     (Package-private - input sources)
├── package-info.java             (Package documentation)
└── internal/
    ├── InMemoryArtifactSink.java (Mode B scaffolding)
    └── package-info.java         (Internal package doc)
```

**Public API:**
- `CompilationTestCase` - Main DSL
- `CompilationResult` - Result view

**Internal:**
- All other classes (subject to change without notice)

---

## Dependencies

### Runtime Dependencies

- `hexaglue-spi` - SPI types for plugins
- `hexaglue-core` - Annotation processor being tested
- `com.google.testing.compile:compile-testing` - Google's compile-testing engine
- `com.google.truth:truth` - Fluent assertions

### Test Dependencies

- `org.junit.jupiter:junit-jupiter` (JUnit 5)

---

## Known Limitations

### 1. No Direct Plugin Injection (Mode A)

Currently, plugins are discovered via ServiceLoader from the test classpath.

- ✅ **Works**: Plugins in `src/test/java` with `META-INF/services/` registration
- ❌ **Doesn't work**: Direct plugin instance injection

**Workaround**: Mode B (future) will support this.

---

### 2. Resource Key Format

Resource keys use best-effort format: `<LOCATION>/<packagePath>/<relativeName>`

**Recommendation**: Use suffix matching in tests:
```java
// ✅ GOOD - suffix matching
result.generatedResourceText("docs/hello.md")

// ❌ BAD - exact key coupling
result.generatedResourceText("SOURCE_OUTPUT/docs/hello.md")
```

---

### 3. Empty Source List

Compiling with no sources will fail with exception:
```java
CompilationResult result = CompilationTestCase.builder()
    .compile(); // ❌ Throws: "error: no source files or class names"
```

**Workaround**: Always provide at least one source file.

---

## Troubleshooting

### Compilation fails with "No system Java compiler available"

**Cause**: Running tests on JRE instead of JDK.

**Solution**: Ensure tests run on a JDK (not JRE):
```bash
# Check Java version
java -version

# Ensure JAVA_HOME points to JDK
echo $JAVA_HOME
```

---

### Diagnostics show errors but test doesn't fail

**Cause**: Compilation can "succeed" even with processor errors if javac itself succeeds.

**Solution**: Check `result.javacDiagnostics()` explicitly:
```java
assertThat(result.javacDiagnostics()).isEmpty();

// Or check for specific diagnostic kinds
boolean hasErrors = result.javacDiagnostics().stream()
    .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR);
assertThat(hasErrors).isFalse();
```

---

### Generated files not captured

**Cause**: Processor might not be writing through `Filer`.

**Solution**:
1. Verify processor uses `processingEnv.getFiler()` API
2. Enable debug output:
```java
if (!result.wasSuccessful()) {
    result.formattedDiagnostics().forEach(System.out::println);
}
```

---

## Related Documentation

- [Module Architecture Plan](../doc/internal/work_in_progress/plan.md) - Detailed architectural design
- [HexaGlue Core](../hexaglue-core/README.md) - Annotation processor being tested
- [HexaGlue SPI](../hexaglue-spi/README.md) - Plugin interfaces

---

## License

Mozilla Public License 2.0 (MPL-2.0)

---

## Contributing

This module is part of the HexaGlue project. For contribution guidelines, see the main repository documentation.

**Key principles when modifying this module:**
- Maintain black-box testing philosophy
- Preserve deterministic behavior
- Keep API simple and readable
- Add tests for new features
- Document public APIs thoroughly

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
