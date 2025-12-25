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
 * Frontend abstraction layer for source model access.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides a stable, tool-agnostic abstraction over source code elements
 * and their metadata. It serves as the primary interface between the JSR-269 annotation
 * processing API and the rest of HexaGlue core.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * The frontend package follows these principles:
 * </p>
 * <ul>
 *   <li><strong>Isolation:</strong> Shield the rest of the system from JSR-269 specifics</li>
 *   <li><strong>Simplification:</strong> Provide a simpler API than raw {@link javax.lang.model}</li>
 *   <li><strong>Stability:</strong> Create cacheable, immutable representations</li>
 *   <li><strong>Type Safety:</strong> Integrate with HexaGlue's internal type system</li>
 * </ul>
 *
 * <h2>Core Components</h2>
 *
 * <h3>{@link io.hexaglue.core.frontend.ElementModel}</h3>
 * <p>
 * Represents a stable view of a source element (type, method, field, parameter, etc.).
 * Provides convenient accessors for element properties, modifiers, and annotations.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.frontend.AnnotationModel}</h3>
 * <p>
 * Represents an annotation instance with convenient attribute access. Abstracts the
 * complexity of {@link javax.lang.model.element.AnnotationMirror}.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.frontend.AnnotationIntrospector}</h3>
 * <p>
 * Utility for discovering and inspecting annotations on elements. Provides methods
 * for common annotation queries without verbose mirror manipulation.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.frontend.SourceModelFactory}</h3>
 * <p>
 * Factory for creating element models from JSR-269 elements. Handles type resolution
 * and metadata extraction in a consistent way.
 * </p>
 *
 * <h2>Architecture</h2>
 * <pre>
 *  ┌─────────────────────────────────────────────────────────┐
 *  │          JSR-269 Annotation Processing API              │
 *  │  (Element, TypeElement, AnnotationMirror, TypeMirror)   │
 *  └────────────────────┬────────────────────────────────────┘
 *                       │
 *                       │ bridges
 *                       ▼
 *  ┌─────────────────────────────────────────────────────────┐
 *  │              Frontend Abstraction Layer                 │
 *  │   (ElementModel, AnnotationModel, SourceModelFactory)   │
 *  └────────────────────┬────────────────────────────────────┘
 *                       │
 *                       │ consumed by
 *                       ▼
 *  ┌─────────────────────────────────────────────────────────┐
 *  │              HexaGlue Core Business Logic               │
 *  │     (IR construction, analysis, code generation)        │
 *  └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Frontend vs JSR-269</h2>
 * <table border="1">
 *   <tr>
 *     <th>JSR-269 API</th>
 *     <th>Frontend Abstraction</th>
 *     <th>Purpose</th>
 *   </tr>
 *   <tr>
 *     <td>{@link javax.lang.model.element.Element}</td>
 *     <td>{@link io.hexaglue.core.frontend.ElementModel}</td>
 *     <td>Element representation</td>
 *   </tr>
 *   <tr>
 *     <td>{@link javax.lang.model.element.AnnotationMirror}</td>
 *     <td>{@link io.hexaglue.core.frontend.AnnotationModel}</td>
 *     <td>Annotation representation</td>
 *   </tr>
 *   <tr>
 *     <td>{@link javax.lang.model.type.TypeMirror}</td>
 *     <td>{@link io.hexaglue.core.types.model.BaseTypeRef}</td>
 *     <td>Type representation</td>
 *   </tr>
 *   <tr>
 *     <td>Manual mirror manipulation</td>
 *     <td>{@link io.hexaglue.core.frontend.AnnotationIntrospector}</td>
 *     <td>Annotation queries</td>
 *   </tr>
 * </table>
 *
 * <h2>Integration with Type System</h2>
 * <p>
 * The frontend integrates closely with HexaGlue's internal type system:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.types.TypeResolver} converts {@link javax.lang.model.type.TypeMirror}
 *       to {@link io.hexaglue.core.types.model.BaseTypeRef}</li>
 *   <li>{@link io.hexaglue.core.frontend.ElementModel} exposes element types as
 *       {@link io.hexaglue.core.types.model.BaseTypeRef}</li>
 *   <li>This ensures consistent type representation throughout the system</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <p>
 * Typical usage flow:
 * </p>
 * <pre>{@code
 * // 1. Create factory with type resolver
 * TypeResolver typeResolver = ...;
 * SourceModelFactory factory = SourceModelFactory.create(typeResolver);
 *
 * // 2. Scan source elements
 * Set<? extends Element> elements = roundEnv.getRootElements();
 *
 * // 3. Create stable models
 * List<ElementModel> models = factory.createModels(elements);
 *
 * // 4. Analyze models
 * for (ElementModel model : models) {
 *     if (model.isType() && model.isPublic()) {
 *         // Check for specific annotations
 *         if (model.hasAnnotation("com.example.Entity")) {
 *             AnnotationModel entity = model.findAnnotation("com.example.Entity").get();
 *
 *             // Extract annotation attributes
 *             String tableName = entity.attributeAsString("table")
 *                 .orElse(model.simpleName());
 *
 *             // Process...
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><strong>Decoupling:</strong> Business logic doesn't depend on JSR-269 specifics</li>
 *   <li><strong>Testability:</strong> Models can be constructed without an annotation processor</li>
 *   <li><strong>Simplicity:</strong> Cleaner API than raw Element manipulation</li>
 *   <li><strong>Evolution:</strong> Can support other frontends (e.g., bytecode analysis) in the future</li>
 *   <li><strong>Performance:</strong> Immutable models can be safely cached</li>
 * </ul>
 *
 * <h2>Relationship with jsr269 Package</h2>
 * <p>
 * This package ({@code io.hexaglue.core.frontend}) provides the high-level abstraction,
 * while {@code io.hexaglue.core.frontend.jsr269} contains JSR-269-specific utilities
 * and adapters. Most code should depend only on this frontend package, not directly
 * on the jsr269 subpackage.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All classes in this package produce immutable, thread-safe representations.
 * However, the underlying JSR-269 elements are only valid within the annotation
 * processing round that created them.
 * </p>
 *
 * <h2>Limitations</h2>
 * <ul>
 *   <li>Models retain references to JSR-269 elements for advanced operations,
 *       so they should not be cached across processing rounds</li>
 *   <li>Type resolution may fail for incomplete or malformed types; models
 *       handle this gracefully by returning {@code Optional.empty()}</li>
 *   <li>Package-private and local elements may have limited qualified name information</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 * <p>
 * Potential future enhancements:
 * </p>
 * <ul>
 *   <li>Support for bytecode-based frontends (e.g., ASM, Javassist)</li>
 *   <li>Unified model for source and compiled elements</li>
 *   <li>Lazy loading and caching strategies for large codebases</li>
 *   <li>More specialized models (MethodModel, FieldModel) with targeted APIs</li>
 * </ul>
 *
 * @see javax.lang.model.element
 * @see javax.annotation.processing
 */
package io.hexaglue.core.frontend;
