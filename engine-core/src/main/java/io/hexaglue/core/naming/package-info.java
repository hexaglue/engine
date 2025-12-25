/**
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2025 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

/**
 * Naming strategy implementation and utilities for HexaGlue.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the core implementation of HexaGlue's naming system, which ensures
 * consistent, predictable, and valid Java identifiers across all generated code.
 * </p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link io.hexaglue.core.naming.DefaultNameStrategy}</h3>
 * <p>
 * Default implementation of {@link io.hexaglue.spi.naming.NameStrategySpec}. Provides:
 * <ul>
 *   <li>Configurable base package for generated code</li>
 *   <li>Optional domain package mirroring</li>
 *   <li>Role-based naming conventions</li>
 *   <li>Hint-driven customization</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.naming.NameSanitizer}</h3>
 * <p>
 * Utilities for sanitizing and normalizing identifiers:
 * <ul>
 *   <li>Validation of Java identifiers</li>
 *   <li>Keyword escaping</li>
 *   <li>Case conversion (camelCase, PascalCase, CONSTANT_CASE)</li>
 *   <li>Character sanitization</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.naming.NameRules}</h3>
 * <p>
 * Centralized naming rules and conventions:
 * <ul>
 *   <li>Standard suffixes and prefixes (Entity, Dto, Mapper, etc.)</li>
 *   <li>Package segment conventions (persistence, rest, graphql, etc.)</li>
 *   <li>Role-based name formatting</li>
 *   <li>Getter/setter/builder name suggestions</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.naming.QualifiedNames}</h3>
 * <p>
 * Utilities for working with {@link io.hexaglue.spi.naming.QualifiedName}:
 * <ul>
 *   <li>Conversion from JSR-269 elements</li>
 *   <li>Package manipulation (replace, append, prepend)</li>
 *   <li>Name transformation (suffix, prefix, sibling, nested)</li>
 *   <li>Path utilities</li>
 * </ul>
 * </p>
 *
 * <h2>Design Principles</h2>
 *
 * <h3>Determinism</h3>
 * <p>
 * All naming operations are deterministic. Given the same inputs, the strategy always
 * produces the same outputs. This ensures:
 * <ul>
 *   <li>Reproducible builds</li>
 *   <li>Predictable generated code</li>
 *   <li>Stable references across compilation rounds</li>
 * </ul>
 * </p>
 *
 * <h3>Validity</h3>
 * <p>
 * All generated names are guaranteed to be valid Java identifiers. The sanitizer:
 * <ul>
 *   <li>Replaces invalid characters</li>
 *   <li>Escapes keywords</li>
 *   <li>Ensures correct start characters</li>
 *   <li>Handles edge cases (empty strings, special characters)</li>
 * </ul>
 * </p>
 *
 * <h3>Consistency</h3>
 * <p>
 * Centralized rules ensure consistency across:
 * <ul>
 *   <li>All plugins (they share the same strategy)</li>
 *   <li>All generated artifacts (types, fields, methods, resources)</li>
 *   <li>Multiple compilation rounds</li>
 * </ul>
 * </p>
 *
 * <h3>Extensibility</h3>
 * <p>
 * While providing sensible defaults, the system allows customization through:
 * <ul>
 *   <li>Strategy configuration (base package, mirroring)</li>
 *   <li>Hints passed to naming methods</li>
 *   <li>Custom strategy implementations (via SPI)</li>
 * </ul>
 * </p>
 *
 * <h2>Naming Conventions</h2>
 *
 * <h3>Type Names</h3>
 * <p>
 * Generated types follow Java conventions:
 * <ul>
 *   <li>PascalCase</li>
 *   <li>Descriptive suffixes (CustomerEntity, OrderDto, CustomerMapper)</li>
 *   <li>No underscores or special characters</li>
 * </ul>
 * </p>
 *
 * <h3>Member Names</h3>
 * <p>
 * Fields and methods follow Java conventions:
 * <ul>
 *   <li>Fields: camelCase (customerId, orderTotal)</li>
 *   <li>Methods: camelCase (findById, createOrder)</li>
 *   <li>Constants: CONSTANT_CASE (DEFAULT_TIMEOUT, MAX_RETRIES)</li>
 * </ul>
 * </p>
 *
 * <h3>Package Names</h3>
 * <p>
 * Generated packages follow established patterns:
 * <ul>
 *   <li>All lowercase</li>
 *   <li>Dot-separated segments</li>
 *   <li>Role-based organization (persistence, rest, mappers)</li>
 *   <li>Optional domain mirroring</li>
 * </ul>
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Configure strategy
 * DefaultNameStrategy strategy = DefaultNameStrategy.builder()
 *     .basePackage("com.example.infrastructure")
 *     .mirrorDomainPackages(false)
 *     .build();
 *
 * // Generate package name
 * String pkg = strategy.packageName(
 *     "com.example.domain.customer",
 *     NameRole.PACKAGE,
 *     "persistence"
 * );
 * // → "com.example.infrastructure.persistence"
 *
 * // Generate type name
 * String typeName = strategy.simpleName(
 *     "Customer",
 *     NameRole.TYPE,
 *     "Entity"
 * );
 * // → "CustomerEntity"
 *
 * // Create qualified name
 * QualifiedName qn = strategy.qualifiedTypeName(pkg, typeName);
 * // → "com.example.infrastructure.persistence.CustomerEntity"
 *
 * // Manipulate qualified names
 * QualifiedName mapper = QualifiedNames.appendSuffix(qn, "Mapper");
 * // → "com.example.infrastructure.persistence.CustomerEntityMapper"
 *
 * // Sanitize user input
 * String field = NameSanitizer.toCamelCase("customer-id");
 * // → "customerId"
 *
 * // Suggest method names
 * String getter = NameRules.suggestGetter("active", "boolean");
 * // → "isActive"
 * }</pre>
 *
 * <h2>Integration with SPI</h2>
 * <p>
 * This package implements the stable SPI defined in {@code io.hexaglue.spi.naming}.
 * Plugins receive a {@link io.hexaglue.spi.naming.NameStrategySpec} instance through
 * the {@link io.hexaglue.spi.context.GenerationContextSpec} and should use it for
 * all naming decisions to ensure consistency.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package are designed for concurrent use:
 * <ul>
 *   <li>{@link io.hexaglue.core.naming.DefaultNameStrategy} - immutable after construction</li>
 *   <li>{@link io.hexaglue.core.naming.NameSanitizer} - stateless utility</li>
 *   <li>{@link io.hexaglue.core.naming.NameRules} - stateless utility</li>
 *   <li>{@link io.hexaglue.core.naming.QualifiedNames} - stateless utility</li>
 * </ul>
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This is a core-internal package. Classes here are <strong>not</strong> part of the stable
 * SPI and may change between versions. Plugins should only depend on
 * {@code io.hexaglue.spi.naming}.
 * </p>
 */
package io.hexaglue.core.naming;
