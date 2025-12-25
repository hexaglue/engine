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
 * Internal type reference model for HexaGlue core.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the internal representation of Java types used throughout
 * HexaGlue core. These implementations extend the stable SPI abstractions with
 * additional metadata and functionality needed for code generation and analysis.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While the SPI ({@code io.hexaglue.spi.types}) provides stable, record-based type
 * references for plugin consumption, the core needs richer representations that:
 * </p>
 * <ul>
 *   <li>Maintain references to compiler-specific {@link javax.lang.model.type.TypeMirror}
 *       objects for advanced type operations</li>
 *   <li>Support efficient type resolution and caching</li>
 *   <li>Enable internal optimizations without breaking SPI stability</li>
 *   <li>Bridge between JSR-269 annotation processing and the stable type system</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * The package follows a class hierarchy:
 * </p>
 * <pre>
 * {@link io.hexaglue.core.types.model.BaseTypeRef} (abstract)
 *   ├── {@link io.hexaglue.core.types.model.PrimitiveTypeRef}
 *   ├── {@link io.hexaglue.core.types.model.ClassTypeRef}
 *   ├── {@link io.hexaglue.core.types.model.ArrayTypeRef}
 *   ├── {@link io.hexaglue.core.types.model.ParameterizedTypeRef}
 *   ├── {@link io.hexaglue.core.types.model.WildcardTypeRef}
 *   └── {@link io.hexaglue.core.types.model.TypeVariableTypeRef}
 * </pre>
 *
 * <h2>Core vs SPI Types</h2>
 * <p>
 * The relationship between core and SPI types:
 * </p>
 * <table border="1">
 *   <tr>
 *     <th>Core Type (Internal)</th>
 *     <th>SPI Type (Public)</th>
 *     <th>Purpose</th>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.BaseTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.TypeRef}</td>
 *     <td>Common abstraction</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.PrimitiveTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.PrimitiveRef}</td>
 *     <td>Primitive types</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.ClassTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.ClassRef}</td>
 *     <td>Class/interface types</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.ArrayTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.ArrayRef}</td>
 *     <td>Array types</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.ParameterizedTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.ParameterizedRef}</td>
 *     <td>Generic types</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.WildcardTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.WildcardRef}</td>
 *     <td>Wildcard arguments</td>
 *   </tr>
 *   <tr>
 *     <td>{@link io.hexaglue.core.types.model.TypeVariableTypeRef}</td>
 *     <td>{@link io.hexaglue.spi.types.TypeVariableRef}</td>
 *     <td>Type variables</td>
 *   </tr>
 * </table>
 *
 * <h2>Conversion</h2>
 * <p>
 * All internal types implement {@link io.hexaglue.core.types.model.BaseTypeRef#toSpiType()}
 * to convert to their stable SPI representation. This conversion strips internal metadata
 * (like {@link javax.lang.model.type.TypeMirror} references) to ensure plugins only see
 * stable, documented types.
 * </p>
 *
 * <h2>Type Mirror Integration</h2>
 * <p>
 * Internal types optionally maintain references to {@link javax.lang.model.type.TypeMirror}
 * objects from the JSR-269 annotation processing API. These references enable:
 * </p>
 * <ul>
 *   <li>Precise type assignability checks via {@link javax.lang.model.util.Types}</li>
 *   <li>Element resolution via {@link javax.lang.model.util.Elements}</li>
 *   <li>Generic type introspection and bounds checking</li>
 *   <li>Annotation metadata extraction</li>
 * </ul>
 * <p>
 * <strong>Important:</strong> {@link javax.lang.model.type.TypeMirror} objects are only
 * valid within the annotation processing round that created them. Do not cache these
 * references across rounds.
 * </p>
 *
 * <h2>Immutability and Thread Safety</h2>
 * <p>
 * All classes in this package are immutable and thread-safe. Methods that appear to
 * modify a type (like {@code withNullability()}) return new instances rather than
 * mutating existing ones.
 * </p>
 *
 * <h2>Equality Semantics</h2>
 * <p>
 * Type equality is based on structural equality, not identity:
 * </p>
 * <ul>
 *   <li>Two types are equal if they represent the same type structure</li>
 *   <li>Nullability is included in equality checks</li>
 *   <li>The underlying {@link javax.lang.model.type.TypeMirror} is NOT included in
 *       equality checks (structural equality only)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create internal types
 * ClassTypeRef listType = ClassTypeRef.of("java.util.List");
 * ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
 * ParameterizedTypeRef listOfStrings = ParameterizedTypeRef.of(
 *     listType,
 *     List.of(stringType)
 * );
 *
 * // Convert to SPI for plugin consumption
 * TypeRef spiType = listOfStrings.toSpiType();
 *
 * // Access type mirror if available
 * listOfStrings.typeMirror().ifPresent(mirror -> {
 *     // Perform advanced type operations
 * });
 * }</pre>
 *
 * <h2>Package Visibility</h2>
 * <p>
 * This package is internal to HexaGlue core and is not part of the public API.
 * External plugins should use only the SPI types from {@code io.hexaglue.spi.types}.
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * While this package is internal and not covered by SPI stability guarantees, it is
 * designed with stability in mind:
 * </p>
 * <ul>
 *   <li>Public constructors and factory methods provide stable creation patterns</li>
 *   <li>The class hierarchy is designed to be extended if needed</li>
 *   <li>All public APIs are documented and validated</li>
 * </ul>
 *
 * @see io.hexaglue.core.types.TypeRefFactory
 * @see io.hexaglue.core.types.TypeResolver
 * @see javax.lang.model.type.TypeMirror
 */
package io.hexaglue.core.types.model;
