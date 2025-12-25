# HexaGlue Engine

**The core compilation engine and plugin infrastructure for HexaGlue.**

<div align="center">
  <img src="doc/logo-hexaglue.png" alt="HexaGlue" width="400">
</div>

[![CI](https://github.com/hexaglue/engine/actions/workflows/ci.yml/badge.svg)](https://github.com/hexaglue/engine/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://openjdk.java.net/)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

---

## What is HexaGlue Engine?

HexaGlue Engine is a **Java annotation processor** that analyzes your domain model and port interfaces at compile time to generate infrastructure code for applications following **Hexagonal Architecture** (Ports and Adapters pattern).

The engine provides:

1. **Compile-time Analysis** - Discovers and analyzes ports and domain types
2. **Plugin Architecture** - Extensible SPI for infrastructure code generation
3. **Type-Safe Generation** - Full type checking during compilation
4. **Zero Runtime Overhead** - All processing happens at compile time

---

## Module Structure

The engine consists of four main modules:

### `engine-spi`
**Service Provider Interface for plugin developers**

- Stable API for building HexaGlue plugins
- JDK-only dependencies (no external libraries)
- Read-only access to analyzed domain model
- Code generation utilities and diagnostic reporting

### `engine-core`
**Core annotation processor implementation**

- JSR-269 annotation processor
- Domain and port analysis engine
- Intermediate representation (IR) builder
- Plugin discovery and orchestration via ServiceLoader
- Configuration loading from `hexaglue.yaml`

### `engine-testing-harness`
**Testing utilities for plugin developers**

- In-memory compilation testing
- Fluent DSL for writing compilation tests
- Validation of generated code
- Integration with JUnit

[📖 Testing Harness Documentation](engine-testing-harness/README.md)

### `engine-bom`
**Bill of Materials for dependency management**

- Centralized version management for engine modules
- Import into your project for consistent versioning

---

## Installation

### For Application Developers

Add the HexaGlue annotation processor to your Maven build:

```xml
<project>
    <!-- Import BOM for version management -->
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

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.14.1</version>
                <configuration>
                    <annotationProcessorPaths>
                        <!-- HexaGlue Engine -->
                        <path>
                            <groupId>io.hexaglue</groupId>
                            <artifactId>engine-core</artifactId>
                        </path>
                        <!-- Add HexaGlue compatible plugins here -->
                        <!-- And can be mixed with others annotation processors -->
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

Official HexaGlue plugins 

### For Plugin Developers

Depend on the SPI module to build plugins:

```xml
<dependencies>
    <!-- Plugin API -->
    <dependency>
        <groupId>io.hexaglue</groupId>
        <artifactId>engine-spi</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Testing utilities -->
    <dependency>
        <groupId>io.hexaglue</groupId>
        <artifactId>engine-testing-harness</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## How It Works

### 1. Write Clean Ports

Define your application boundaries as plain Java interfaces:

```java
package com.example.ports;

import com.example.domain.customer.Customer;
import com.example.domain.customer.CustomerId;
import java.util.Optional;

/**
 * Driven port (outbound) for customer persistence.
 */
public interface CustomerRepository {
    Optional<Customer> findById(CustomerId id);
    Customer save(Customer customer);
    boolean deleteById(CustomerId id);
}
```

**No annotations required** - HexaGlue discovers ports automatically by analyzing your codebase structure.

### 2. Domain Analysis

The engine analyzes your domain model to understand:

- **Aggregate boundaries** - Identifying aggregate roots and entities
- **Entities vs Value Objects** - Understanding identity and immutability
- **Identity fields** - Detecting ID fields by name or annotation
- **Relationships and collections** - Mapping domain associations

### 3. Plugin Execution

Plugins receive the analyzed model and generate infrastructure code:

```
[INFO] HexaGlue Engine v0.1.0 - Analyzing application...
[INFO] [HG-CORE-001] Discovered 3 ports
[INFO] [HG-CORE-002] Discovered 8 domain types
[INFO]
[INFO] Executing plugins...
[INFO] [HG-PORTDOCS-001] Generating port documentation
[INFO] [HG-JPA-001] Generating JPA entities and repositories
[INFO]
[INFO] Build complete - 3 ports processed successfully
```

---

## Configuration

Configure HexaGlue and its plugins through `hexaglue.yaml` in your project:

```yaml
hexaglue:
  plugins:
    {plugin-id}:
      # Plugin-specific configuration options
```

Place this file at:
- Project root: `./hexaglue.yaml`
- Resources directory: `src/main/resources/hexaglue.yaml`
- Any location on the compile-time classpath

📖 **For plugin-specific configuration options**, see:
- [Plugins Documentation](https://github.com/hexaglue/plugins) - Overview and configuration guide
- [plugin-portdocs README](https://github.com/hexaglue/plugins/tree/main/plugin-portdocs) - Port documentation options
- [plugin-jpa-repository README](https://github.com/hexaglue/plugins/tree/main/plugin-jpa-repository) - JPA plugin configuration

---

## Optional jMolecules Integration

HexaGlue can **optionally read jMolecules annotations** to better understand your domain model:

```java
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

@AggregateRoot
public class Customer {
    @Identity
    private CustomerId id;
    // ...
}
```

> **jMolecules in one sentence**
> jMolecules allows you to express Domain-Driven Design concepts explicitly in Java, without introducing any framework or runtime dependency.

**jMolecules is not required** and **not coupled** to HexaGlue. When present, its annotations simply make domain intent explicit where HexaGlue would otherwise rely on heuristics.

### Supported Annotations

| Annotation | Purpose | Effect |
|------------|---------|--------|
| `@AggregateRoot` | Marks an aggregate root | Removes ambiguity in boundary detection |
| `@Entity` | Marks a domain entity | Distinguishes entities from value objects |
| `@ValueObject` | Marks a value object | Explicitly expresses immutability |
| `@Identity` | Marks an identity field | Overrides naming-based detection |
| `@Repository` | Marks a repository port | Strong signal for driven port detection |

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│              Your Application                       │
│  (Domain + Ports - analyzed, never modified)        │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│           HexaGlue Engine (engine-core)             │
│  - JSR-269 Annotation Processor                     │
│  - Analyzes domain types and ports                  │
│  - Builds intermediate representation (IR)          │
│  - Discovers and orchestrates plugins               │
└─────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────┐
│              Plugin SPI (engine-spi)                │
│  - Stable contract between core and plugins         │
│  - Read-only access to analyzed model               │
│  - Code generation API                              │
│  - Diagnostic reporting                             │
└─────────────────────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
    ┌─────────┐    ┌─────────┐    ┌─────────┐
    │ Plugin  │    │ Plugin  │    │ Plugin  │
    │ PortDocs│    │   JPA   │    │  REST   │
    └─────────┘    └─────────┘    └─────────┘
```

---

## Plugin Development

### Creating a Plugin

1. Depend on `engine-spi`
2. Implement `io.hexaglue.spi.HexaGluePlugin`
3. Register via `META-INF/services/io.hexaglue.spi.HexaGluePlugin`
4. Use `engine-testing-harness` for testing

### Example Plugin Structure

```java
package io.hexaglue.plugin.example;

import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.context.PluginContext;

public class ExamplePlugin implements HexaGluePlugin {

    @Override
    public String getPluginId() {
        return "io.hexaglue.plugin.example";
    }

    @Override
    public void execute(PluginContext context) {
        // Access analyzed model and generate code
        context.getAnalyzedApplication()
               .getPorts()
               .forEach(port -> {
                   // Generate infrastructure code
               });
    }
}
```

### Testing Your Plugin

```java
import io.hexaglue.testing.CompilationTestCase;
import org.junit.jupiter.api.Test;

class ExamplePluginTest {

    @Test
    void shouldGenerateCode() {
        CompilationTestCase.run()
            .withSourceFile("CustomerRepository.java", """
                package com.example;
                public interface CustomerRepository {
                    Customer save(Customer customer);
                }
                """)
            .withProcessor(ExamplePlugin.class)
            .expectSuccess()
            .expectGeneratedFile("com.example.CustomerRepositoryAdapter.java")
            .verify();
    }
}
```

See [engine-testing-harness/README.md](engine-testing-harness/README.md) for complete testing documentation.

📖 **For complete plugin development examples**, see the [plugins repository](https://github.com/hexaglue/plugins):
- [plugin-portdocs](https://github.com/hexaglue/plugins/tree/main/plugin-portdocs) - Reference implementation for documentation generation
- [plugin-jpa-repository](https://github.com/hexaglue/plugins/tree/main/plugin-jpa-repository) - Advanced JPA code generation

---

## Diagnostic Codes

HexaGlue uses structured diagnostic codes for all messages:

```
HG-{COMPONENT}-{NUMBER}
```

**Engine diagnostic ranges:**
- `HG-CORE-001` to `HG-CORE-099` - Informational messages
- `HG-CORE-100` to `HG-CORE-199` - Warnings
- `HG-CORE-200` to `HG-CORE-299` - Errors

Example diagnostics:
- `HG-CORE-001` - Port discovery complete
- `HG-CORE-002` - Domain types analyzed
- `HG-CORE-200` - Compilation error

---

## Building from Source

```bash
# Clone the repository
git clone https://github.com/hexaglue/engine.git
cd engine

# Build all modules
mvn clean install

# Run tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
```

---

## Available Plugins

Official plugins are maintained in the separate [hexaglue/plugins](https://github.com/hexaglue/plugins) repository:

| Plugin | Status | Description |
|--------|--------|-------------|
| **plugin-portdocs** | ✅ Available | Generates Markdown documentation for all ports |
| **plugin-jpa-repository** | ✅ Available | JPA/Hibernate entities, repositories, adapters, and mappers |
| **plugin-spring-rest** | 📅 Planned | Spring MVC REST controllers |
| **plugin-spring-data** | 📅 Planned | Spring Data repositories |
| **plugin-kafka** | 📅 Planned | Kafka producers and consumers |

See the [plugins repository](https://github.com/hexaglue/plugins) for installation and usage instructions.

---

## Examples

📖 **Complete example applications using HexaGlue** are available in the [examples repository](https://github.com/hexaglue/examples).

See the [examples README](https://github.com/hexaglue/examples) for:
- Step-by-step learning path
- Running examples
- Understanding generated code

---

## Key Principles

1. **Your domain stays pure** - HexaGlue never modifies your business code
2. **Zero boilerplate** - Write ports, let plugins generate adapters
3. **Plugin-based** - Extensible architecture for any technology stack
4. **Compile-time safety** - Generation happens during compilation with full type checking
5. **Framework agnostic** - Swap infrastructure without touching business logic

---

## License

HexaGlue Engine is distributed under the **Mozilla Public License 2.0 (MPL-2.0)**.

- ✅ May be used in commercial and proprietary products
- ✅ Modifications to HexaGlue source files must be shared under MPL-2.0
- ✅ Your application code remains your own and may remain proprietary

Learn more: [https://www.mozilla.org/MPL/2.0/](https://www.mozilla.org/MPL/2.0/)

---

## Support

- [GitHub Issues](https://github.com/hexaglue/engine/issues) - Report bugs or request features
- [GitHub Discussions](https://github.com/hexaglue/engine/discussions) - Ask questions and share ideas

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
