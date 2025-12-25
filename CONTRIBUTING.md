# Contributing to HexaGlue Engine

Thank you for your interest in contributing to **HexaGlue Engine**! We welcome contributions from the community.

**HexaGlue Engine** is the core compilation engine that powers infrastructure code generation for Hexagonal Architecture applications.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Types of Contributions](#types-of-contributions)
- [Development Setup](#development-setup)
- [Contributing Code](#contributing-code)
- [Testing](#testing)
- [Code Style](#code-style)
- [Commit Message Convention](#commit-message-convention)
- [Pull Request Process](#pull-request-process)
- [Reporting Issues](#reporting-issues)
- [Related Projects](#related-projects)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors.

### Our Standards

- **Be respectful** - Treat everyone with respect
- **Be collaborative** - Work together constructively
- **Be inclusive** - Welcome diverse perspectives
- **Be professional** - Keep discussions focused on technical merit

### Enforcement

Instances of unacceptable behavior may be reported to info@hexaglue.io.

---

## Getting Started

### Understanding the Project Structure

HexaGlue is organized into three separate repositories:

- **[hexaglue/engine](https://github.com/hexaglue/engine)** (this repository)
  - Core compilation engine
  - Service Provider Interface (SPI)
  - Testing harness for plugin developers

- **[hexaglue/plugins](https://github.com/hexaglue/plugins)**
  - Infrastructure code generators (JPA, Port Documentation, etc.)
  - Plugin development examples

- **[hexaglue/examples](https://github.com/hexaglue/examples)**
  - Working examples and tutorials
  - Hexagonal architecture patterns

### This Repository (engine)

```
engine/
├── engine-bom/              # Bill of Materials for version management
├── engine-spi/              # Service Provider Interface (STABLE API)
├── engine-core/             # Core compilation engine
└── engine-testing-harness/  # Testing utilities for plugin developers
```

📖 **For architecture details**, see [ARCHITECTURE.md](ARCHITECTURE.md)

---

## Types of Contributions

We welcome many types of contributions to the **engine** repository:

### Core Engine

- **Bug fixes** - Fix issues in compilation pipeline, IR analysis, or code generation
- **Performance improvements** - Optimize compilation speed or memory usage
- **New features** - Add capabilities to the core engine (after discussion)
- **Diagnostics** - Improve error messages and diagnostic codes

### SPI (Service Provider Interface)

- **API improvements** - Propose additions to the stable SPI (with backward compatibility)
- **Documentation** - Improve SPI reference documentation
- **Type system** - Enhance type resolution and representation

### Testing Harness

- **Test utilities** - Add helpers for plugin testing
- **Documentation** - Improve testing guide and examples

### Documentation

- **Architecture guides** - Explain compilation pipeline, IR design
- **Contributor guides** - Improve development setup, troubleshooting
- **API documentation** - Enhance Javadocs

📖 **For plugin contributions**, see [hexaglue/plugins](https://github.com/hexaglue/plugins/blob/main/CONTRIBUTING.md)

📖 **For example contributions**, see [hexaglue/examples](https://github.com/hexaglue/examples/blob/main/CONTRIBUTING.md)

---

## Development Setup

### Prerequisites

- **Java 17** or later
- **Maven 3.8+**
- Your favorite IDE (IntelliJ IDEA, Eclipse, VS Code)

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Then clone your fork
git clone https://github.com/YOUR-USERNAME/engine.git
cd engine

# Add upstream remote
git remote add upstream https://github.com/hexaglue/engine.git
```

### 2. Build the Project

```bash
# Build all modules
mvn clean install

# This will:
# 1. Compile engine-spi (the stable SPI)
# 2. Compile engine-core (the compilation engine)
# 3. Compile engine-testing-harness (testing utilities)
# 4. Run all tests
```

### 3. Import into IDE

**IntelliJ IDEA:**
- File → Open → Select `engine/pom.xml`
- Import as Maven project
- Wait for indexing to complete

**Eclipse:**
- File → Import → Maven → Existing Maven Projects
- Select `engine` directory
- Finish

**VS Code:**
- Install Java Extension Pack
- Open `engine` directory
- Maven will auto-detect the project

### 4. Verify Setup

```bash
# Run tests to verify everything works
mvn test

# You should see all tests passing
```

---

## Contributing Code

### 1. Find or Create an Issue

- Check [GitHub Issues](https://github.com/hexaglue/engine/issues)
- Comment on the issue to claim it
- For new features, open a discussion first

### 2. Create a Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/my-feature-name
# or
git checkout -b fix/issue-123-description
```

### 3. Make Your Changes

- Write code following the existing style
- Add tests for new functionality
- Update documentation as needed
- Keep changes focused and atomic

### 4. Document Your Changes

- Add Javadoc to all public APIs
- Update relevant markdown documentation
- Add examples if introducing new features
- Follow existing documentation patterns

---

## Testing

### Running Tests

```bash
# Run all tests
mvn clean test

# Run tests for a specific module
cd engine-core
mvn test

# Run a specific test class
mvn test -Dtest=CompilerTest

# Run with coverage
mvn clean test jacoco:report
```

### Writing Tests

**Unit tests** - Test individual components:
```java
@Test
void shouldDiscoverPortsInPackage() {
    // Arrange
    PortDiscovery discovery = new DefaultPortDiscovery();

    // Act
    List<PortView> ports = discovery.discoverPorts(packageElement);

    // Assert
    assertThat(ports).hasSize(2);
    assertThat(ports.get(0).simpleName()).isEqualTo("CustomerRepository");
}
```

**Integration tests** - Test compilation pipeline:
```java
@Test
void shouldCompileAndGenerateCode() {
    HexaGlueTestHarness.forPlugin(new MyPlugin())
        .withSource("Customer.java", sourceCode)
        .compile()
        .expectSuccess()
        .expectGeneratedSource("CustomerEntity.java");
}
```

### Test Requirements

- All new code must have tests
- Aim for >80% code coverage
- Test both success and error cases
- Use meaningful test names (`shouldDoX_whenY_givenZ`)

---

## Code Style

HexaGlue Engine follows strict code style guidelines:

### Java Style

- **Java 17** language features
- **Palantir Java Format** (enforced via Spotless)
- **Javadoc** on all public APIs
- **Meaningful names** - No abbreviations
- **Clear comments** explaining the "why", not the "what"

### Formatting

```bash
# Format code before committing
mvn spotless:apply

# Check formatting
mvn spotless:check
```

### Package Structure

Follow existing package organization:
```
io.hexaglue.core/
├── discovery/      # Port and domain type discovery
├── ir/             # Intermediate Representation
│   ├── domain/     # Domain model IR
│   ├── ports/      # Ports IR
│   └── app/        # Application services IR
├── codegen/        # Code generation utilities
│   ├── write/      # File writing
│   └── merge/      # Code merging
└── internal/       # Internal implementation (not SPI)
```

### SPI Stability

When modifying the SPI (`engine-spi`):

- **NEVER** break backward compatibility in 1.x releases
- Use default methods to add new interface methods
- Mark new APIs with `@since 1.X.0`
- Deprecate before removal (one MINOR version warning)

📖 **See [engine-spi/SPI_REFERENCE.md](engine-spi/SPI_REFERENCE.md)** for stability guarantees

---

## Commit Message Convention

We follow **Conventional Commits**:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring (no behavior change)
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks (dependencies, build, etc.)

### Scopes (for engine)

- `spi` - SPI changes
- `core` - Core engine changes
- `ir` - Intermediate Representation
- `discovery` - Port/domain discovery
- `codegen` - Code generation
- `merge` - Code merging
- `testing` - Testing harness
- `docs` - Documentation

### Examples

```
feat(spi): add TypeMapper interface for custom conversions

- Added TypeMapper to allow plugins to register custom type mappings
- Updated DomainTypeView to expose type mappers
- Includes tests and updated SPI reference

Closes #123
```

```
fix(core): resolve circular dependency in type resolution

The type resolver was incorrectly handling cyclic references
between domain types, causing infinite loops.

- Added visited set to track resolution path
- Added diagnostic HG-CORE-220 for circular dependencies
- Includes regression test

Fixes #456
```

```
docs(spi): improve Javadoc for PluginContext

Added examples and clarified when methods return Optional vs null.
```

---

## Pull Request Process

### 1. Prepare Your PR

```bash
# Ensure code is formatted
mvn spotless:apply

# Run all tests
mvn clean test

# Build the entire project
mvn clean install

# Commit and push
git add .
git commit -m "feat(scope): your change description"
git push origin feature/your-branch
```

### 2. Create Pull Request

Open a PR on GitHub with a clear description:

**Title:** Follow conventional commits format
```
feat(spi): add TypeMapper interface
```

**Description template:**
```markdown
## Summary
Brief description of what this PR does.

## Motivation
Why is this change needed? What problem does it solve?

## Changes
- Bullet point list of changes
- Be specific and concise

## Testing
- How was this tested?
- What test cases were added?

## Breaking Changes
- List any breaking changes (should be rare for engine)
- Provide migration guide if applicable

## Related Issues
Closes #123
```

### 3. Code Review

- Respond to review comments promptly
- Make requested changes in new commits (don't force-push during review)
- Mark conversations as resolved when addressed
- Request re-review when ready

### 4. Merge

- Maintainers will merge approved PRs
- Commits may be squashed for cleaner history
- Delete your branch after merge

### CI Requirements

All PRs must pass:
- ✅ All tests passing
- ✅ Code formatted with Spotless
- ✅ No new compiler warnings
- ✅ Javadoc builds without errors
- ✅ No new dependency vulnerabilities

---

## Reporting Issues

### Bug Reports

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.yml):

- Clear, descriptive title
- Steps to reproduce
- Expected vs actual behavior
- HexaGlue version (`mvn --version` output)
- Java version (`java --version` output)
- Relevant code snippets or diagnostic codes

### Feature Requests

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.yml):

- Clear use case description
- Proposed solution
- Alternatives considered
- Impact on existing functionality
- Willingness to contribute implementation

### Security Issues

**DO NOT** open public issues for security vulnerabilities.

Email security concerns to: info@hexaglue.io

---

## Related Projects

Contributing to HexaGlue ecosystem:

- **[hexaglue/plugins](https://github.com/hexaglue/plugins)** - Contribute infrastructure generators
  - JPA repository plugin
  - Port documentation plugin
  - Create new plugins for frameworks

- **[hexaglue/examples](https://github.com/hexaglue/examples)** - Contribute examples and tutorials
  - Working applications
  - Architecture patterns
  - Best practices

---

## Questions?

- **General questions**: [GitHub Discussions](https://github.com/hexaglue/engine/discussions)
- **Bug reports**: [GitHub Issues](https://github.com/hexaglue/engine/issues)
- **Security**: info@hexaglue.io

---

## License

By contributing to HexaGlue Engine, you agree that your contributions will be licensed under the **Mozilla Public License 2.0 (MPL-2.0)**.

See [LICENSE](LICENSE) for details.

---

## Additional Resources

- [Architecture Documentation](ARCHITECTURE.md) - Understanding engine internals
- [SPI Reference](engine-spi/SPI_REFERENCE.md) - Service Provider Interface documentation
- [Diagnostic Codes](DIAGNOSTIC_CODES.md) - Error code reference
- [Testing Harness](engine-testing-harness/README.md) - Testing guide
- [Plugin Development](https://github.com/hexaglue/plugins/blob/main/PLUGIN_DEVELOPMENT.md) - Creating plugins

---

<div align="center">

**HexaGlue - Focus on business code, not infrastructure glue.**

Made with ❤️ by Scalastic<br>
Copyright 2025 Scalastic - Released under MPL-2.0

</div>
