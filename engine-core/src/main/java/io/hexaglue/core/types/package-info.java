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
 * Type system implementation for HexaGlue.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the core implementation of HexaGlue's type system, which bridges
 * JSR-269 type mirrors with the stable {@link io.hexaglue.spi.types.TypeRef} abstraction
 * exposed to plugins.
 * </p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link io.hexaglue.core.types.DefaultTypeSystem}</h3>
 * <p>
 * Default implementation of {@link io.hexaglue.spi.types.TypeSystemSpec}. Provides:
 * <ul>
 *   <li>Type resolution from qualified names</li>
 *   <li>Assignability checking</li>
 *   <li>Boxing/unboxing operations</li>
 *   <li>Type erasure</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.types.TypeResolver}</h3>
 * <p>
 * Resolves {@link io.hexaglue.spi.types.TypeRef} instances from JSR-269 type mirrors:
 * <ul>
 *   <li>Converts {@link javax.lang.model.type.TypeMirror} to {@link io.hexaglue.spi.types.TypeRef}</li>
 *   <li>Handles all type kinds (primitives, classes, arrays, parameterized, wildcards, type variables)</li>
 *   <li>Preserves nullability information</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.types.TypeRefFactory}</h3>
 * <p>
 * Factory for creating type references:
 * <ul>
 *   <li>Provides constants for common types (Object, String, primitives)</li>
 *   <li>Convenient factory methods for all type kinds</li>
 *   <li>Boxing/unboxing utilities</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.types.TypeComparators}</h3>
 * <p>
 * Comparison and equality utilities:
 * <ul>
 *   <li>Structural equality (ignoring nullability)</li>
 *   <li>Deep equality (including nullability)</li>
 *   <li>Name-based comparators for sorting</li>
 *   <li>Type predicates (isPrimitive, isString, isObject, etc.)</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.types.TypeDisplay}</h3>
 * <p>
 * Rendering and display utilities:
 * <ul>
 *   <li>Qualified rendering (fully-qualified names)</li>
 *   <li>Simple rendering (simple names only)</li>
 *   <li>Type list formatting</li>
 *   <li>Human-readable descriptions</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.types.NullabilityResolver}</h3>
 * <p>
 * Nullability resolution from various sources:
 * <ul>
 *   <li>Recognizes common nullability annotation frameworks</li>
 *   <li>Handles primitives (always non-null)</li>
 *   <li>Provides merging and default strategies</li>
 * </ul>
 * </p>
 *
 * <h2>Design Principles</h2>
 *
 * <h3>Abstraction from JSR-269</h3>
 * <p>
 * The type system intentionally abstracts away JSR-269 internals. This:
 * <ul>
 *   <li>Provides a stable API for plugins (no compiler dependencies)</li>
 *   <li>Allows future support for alternative frontends</li>
 *   <li>Simplifies plugin development</li>
 * </ul>
 * </p>
 *
 * <h3>Immutability</h3>
 * <p>
 * All {@link io.hexaglue.spi.types.TypeRef} instances are immutable. Operations that
 * "modify" a type (like {@code withNullability}) return new instances.
 * </p>
 *
 * <h3>Best-Effort Resolution</h3>
 * <p>
 * Type resolution and assignability checking are best-effort. The system:
 * <ul>
 *   <li>Handles common cases accurately</li>
 *   <li>Falls back to conservative defaults for edge cases</li>
 *   <li>Never throws exceptions during resolution</li>
 * </ul>
 * </p>
 *
 * <h2>Type Hierarchy</h2>
 * <pre>
 * TypeRef (interface)
 *   ├── PrimitiveRef (record)
 *   ├── ClassRef (record)
 *   ├── ArrayRef (record)
 *   ├── ParameterizedRef (record)
 *   ├── WildcardRef (record)
 *   └── TypeVariableRef (record)
 * </pre>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create type system
 * DefaultTypeSystem typeSystem = DefaultTypeSystem.create(
 *     processingEnv.getElementUtils(),
 *     processingEnv.getTypeUtils()
 * );
 *
 * // Resolve types
 * TypeResolver resolver = typeSystem.resolver();
 * TypeRef customerType = resolver.resolve(customerElement.asType());
 *
 * // Create types
 * ClassRef listClass = TypeRefFactory.classRef("java.util.List");
 * TypeRef stringType = TypeRefFactory.STRING;
 * ParameterizedRef listOfString = TypeRefFactory.parameterized(
 *     listClass,
 *     stringType
 * );
 *
 * // Compare types
 * boolean equal = TypeComparators.equalIgnoringNullability(customerType, customerType);
 * boolean isString = TypeComparators.isString(stringType);
 *
 * // Display types
 * String qualified = TypeDisplay.render(listOfString);
 * // → "java.util.List<java.lang.String>"
 *
 * String simple = TypeDisplay.renderSimple(listOfString);
 * // → "List<String>"
 *
 * // Resolve nullability
 * Nullability n = NullabilityResolver.fromElement(fieldElement);
 * TypeRef nullable = customerType.withNullability(Nullability.NULLABLE);
 *
 * // Check assignability
 * boolean assignable = typeSystem.isAssignable(stringType, typeSystem.objectType());
 * // → true
 *
 * // Box/unbox
 * PrimitiveRef intType = TypeRefFactory.INT;
 * TypeRef boxed = typeSystem.box(intType);
 * // → java.lang.Integer
 * }</pre>
 *
 * <h2>Integration with SPI</h2>
 * <p>
 * This package implements the stable SPI defined in {@code io.hexaglue.spi.types}.
 * Plugins receive a {@link io.hexaglue.spi.types.TypeSystemSpec} instance through
 * the {@link io.hexaglue.spi.context.GenerationContextSpec} and should use it for
 * all type operations.
 * </p>
 *
 * <h2>Nullability Annotations</h2>
 * <p>
 * The nullability resolver recognizes annotations from:
 * <ul>
 *   <li>JSR-305 ({@code @Nonnull}, {@code @Nullable})</li>
 *   <li>JetBrains ({@code @NotNull}, {@code @Nullable})</li>
 *   <li>Eclipse ({@code @NonNull}, {@code @Nullable})</li>
 *   <li>Checker Framework ({@code @NonNull}, {@code @Nullable})</li>
 *   <li>Android ({@code @NonNull}, {@code @Nullable})</li>
 *   <li>FindBugs ({@code @Nonnull}, {@code @CheckForNull})</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package are designed for concurrent use within a single
 * annotation processing round:
 * <ul>
 *   <li>{@link io.hexaglue.core.types.DefaultTypeSystem} - thread-safe</li>
 *   <li>{@link io.hexaglue.core.types.TypeResolver} - thread-safe per round</li>
 *   <li>{@link io.hexaglue.core.types.TypeRefFactory} - stateless utility</li>
 *   <li>{@link io.hexaglue.core.types.TypeComparators} - stateless utility</li>
 *   <li>{@link io.hexaglue.core.types.TypeDisplay} - stateless utility</li>
 *   <li>{@link io.hexaglue.core.types.NullabilityResolver} - stateless utility</li>
 * </ul>
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This is a core-internal package. Classes here are <strong>not</strong> part of the stable
 * SPI and may change between versions. Plugins should only depend on
 * {@code io.hexaglue.spi.types}.
 * </p>
 */
package io.hexaglue.core.types;
