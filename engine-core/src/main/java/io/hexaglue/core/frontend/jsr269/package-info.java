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
 * Low-level utilities for working with the JSR-269 annotation processing API.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides utility classes that simplify common operations on JSR-269
 * types ({@link javax.lang.model.element.Element}, {@link javax.lang.model.type.TypeMirror},
 * {@link javax.lang.model.element.AnnotationMirror}). These utilities serve as the
 * foundation for the higher-level frontend abstractions in the parent package.
 * </p>
 *
 * <h2>Design Philosophy</h2>
 * <p>
 * The JSR-269 API is powerful but verbose. This package follows these principles:
 * </p>
 * <ul>
 *   <li><strong>Simplification:</strong> Reduce boilerplate for common operations</li>
 *   <li><strong>Safety:</strong> Provide null-safe operations with {@link java.util.Optional}</li>
 *   <li><strong>Clarity:</strong> Offer clear, purpose-driven methods</li>
 *   <li><strong>Statelessness:</strong> All utilities are stateless and thread-safe</li>
 * </ul>
 *
 * <h2>Core Utilities</h2>
 *
 * <h3>{@link io.hexaglue.core.frontend.jsr269.Jsr269Elements}</h3>
 * <p>
 * Utilities for working with {@link javax.lang.model.element.Element} instances:
 * </p>
 * <ul>
 *   <li>Safe type casting (asTypeElement, asExecutableElement, etc.)</li>
 *   <li>Element property queries (isPublic, isStatic, etc.)</li>
 *   <li>Hierarchy navigation (getEnclosingTypeElement, getPackageElement)</li>
 *   <li>Member filtering (getPublicMethods, getPublicFields)</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.frontend.jsr269.Jsr269Types}</h3>
 * <p>
 * Utilities for working with {@link javax.lang.model.type.TypeMirror} instances:
 * </p>
 * <ul>
 *   <li>Safe type casting (asDeclaredType, asPrimitiveType, etc.)</li>
 *   <li>Type kind checks (isPrimitive, isArray, etc.)</li>
 *   <li>Type component access (getArrayComponentType, getTypeArguments)</li>
 *   <li>Type relationships (isAssignable, isSubtype, isSameType)</li>
 *   <li>Boxing/unboxing operations</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.frontend.jsr269.Jsr269Mirrors}</h3>
 * <p>
 * Utilities for working with {@link javax.lang.model.element.AnnotationMirror} instances:
 * </p>
 * <ul>
 *   <li>Annotation type queries (getAnnotationType, getAnnotationSimpleName)</li>
 *   <li>Attribute value extraction (getAttribute, getAttributeAsString, etc.)</li>
 *   <li>Type-safe attribute access (getAttributeAsInt, getAttributeAsBoolean, etc.)</li>
 *   <li>List attribute handling (getAttributeAsStringList, getAttributeAsTypeList)</li>
 *   <li>Annotation searching (findAnnotation)</li>
 * </ul>
 *
 * <h3>{@link io.hexaglue.core.frontend.jsr269.Jsr269Locations}</h3>
 * <p>
 * Utilities for creating {@link io.hexaglue.spi.diagnostics.DiagnosticLocation} instances:
 * </p>
 * <ul>
 *   <li>Location creation from elements</li>
 *   <li>Location enrichment with annotation context</li>
 *   <li>Location enrichment with attribute context</li>
 *   <li>Custom location descriptions</li>
 * </ul>
 *
 * <h2>Usage Patterns</h2>
 *
 * <h3>Element Introspection</h3>
 * <pre>{@code
 * Element element = ...;
 *
 * // Safe casting
 * Optional<TypeElement> typeElement = Jsr269Elements.asTypeElement(element);
 *
 * // Property checks
 * if (Jsr269Elements.isPublicClass(element)) {
 *     String qualifiedName = Jsr269Elements.getQualifiedName(element).orElse("unknown");
 *     List<ExecutableElement> methods = Jsr269Elements.getPublicMethods((TypeElement) element);
 * }
 * }</pre>
 *
 * <h3>Type Analysis</h3>
 * <pre>{@code
 * TypeMirror typeMirror = ...;
 *
 * // Type checks and casting
 * if (Jsr269Types.isDeclared(typeMirror)) {
 *     Optional<TypeElement> typeElement = Jsr269Types.getTypeElement(typeMirror);
 *     List<? extends TypeMirror> typeArgs = Jsr269Types.getTypeArguments(typeMirror);
 * }
 *
 * // Type relationships
 * if (Jsr269Types.isAssignable(type1, type2, types)) {
 *     // type1 can be assigned to type2
 * }
 * }</pre>
 *
 * <h3>Annotation Extraction</h3>
 * <pre>{@code
 * AnnotationMirror mirror = ...;
 *
 * // Get annotation info
 * String annotationType = Jsr269Mirrors.getAnnotationType(mirror);
 *
 * // Extract attribute values
 * Optional<String> stringValue = Jsr269Mirrors.getAttributeAsString(mirror, "value");
 * Optional<Boolean> boolValue = Jsr269Mirrors.getAttributeAsBoolean(mirror, "enabled");
 * List<String> names = Jsr269Mirrors.getAttributeAsStringList(mirror, "names");
 *
 * // Type attribute
 * Optional<TypeMirror> classValue = Jsr269Mirrors.getAttributeAsType(mirror, "targetClass");
 * }</pre>
 *
 * <h3>Diagnostic Locations</h3>
 * <pre>{@code
 * Element element = ...;
 * AnnotationMirror annotation = ...;
 *
 * // Simple location
 * DiagnosticLocation location = Jsr269Locations.of(element);
 *
 * // Location with annotation context
 * DiagnosticLocation annotationLoc = Jsr269Locations.of(element, annotation);
 *
 * // Location with attribute context
 * DiagnosticLocation attributeLoc = Jsr269Locations.of(element, annotation, "attributeName");
 * }</pre>
 *
 * <h2>Integration with Frontend Package</h2>
 * <p>
 * This package serves as the foundation for higher-level abstractions:
 * </p>
 * <pre>
 *  ┌─────────────────────────────────────────────────────────┐
 *  │              JSR-269 API (javax.lang.model)             │
 *  │   (Element, TypeMirror, AnnotationMirror, Types...)    │
 *  └────────────────────┬────────────────────────────────────┘
 *                       │
 *                       │ utilities
 *                       ▼
 *  ┌─────────────────────────────────────────────────────────┐
 *  │           JSR-269 Utilities (this package)              │
 *  │  (Jsr269Elements, Jsr269Types, Jsr269Mirrors, ...)     │
 *  └────────────────────┬────────────────────────────────────┘
 *                       │
 *                       │ used by
 *                       ▼
 *  ┌─────────────────────────────────────────────────────────┐
 *  │          Frontend Abstractions (parent package)         │
 *  │  (ElementModel, AnnotationModel, SourceModelFactory)    │
 *  └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h2>When to Use These Utilities</h2>
 * <ul>
 *   <li><strong>Direct JSR-269 work:</strong> When implementing low-level element analysis</li>
 *   <li><strong>Type resolution:</strong> When converting TypeMirror to TypeRef</li>
 *   <li><strong>Annotation processing:</strong> When extracting annotation metadata</li>
 *   <li><strong>Diagnostics:</strong> When creating diagnostic locations</li>
 * </ul>
 *
 * <h2>When NOT to Use These Utilities</h2>
 * <p>
 * Prefer the higher-level frontend abstractions when:
 * </p>
 * <ul>
 *   <li>You need stable, cacheable element representations</li>
 *   <li>You're working with business logic that shouldn't depend on JSR-269</li>
 *   <li>You need plugin-safe APIs (use SPI types instead)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All utility classes in this package are stateless and thread-safe. However, the
 * JSR-269 objects they operate on (Element, TypeMirror, etc.) are only valid within
 * the annotation processing round that created them.
 * </p>
 *
 * <h2>Error Handling</h2>
 * <p>
 * These utilities follow a consistent error handling pattern:
 * </p>
 * <ul>
 *   <li>Required parameters are validated with {@link java.util.Objects#requireNonNull}</li>
 *   <li>Optional results are returned via {@link java.util.Optional}</li>
 *   <li>List results return empty lists rather than null</li>
 *   <li>Exceptions from JSR-269 are generally not caught unless documented</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li>All methods are lightweight wrappers with minimal overhead</li>
 *   <li>No caching is performed (utilities are stateless)</li>
 *   <li>Repeated calls may re-traverse element hierarchies</li>
 *   <li>For expensive operations, consider caching results in calling code</li>
 * </ul>
 *
 * <h2>Future Extensions</h2>
 * <p>
 * Potential future enhancements:
 * </p>
 * <ul>
 *   <li>Additional type relationship queries (captures, wildcards)</li>
 *   <li>More specialized element filtering methods</li>
 *   <li>Enhanced location information (line numbers, columns)</li>
 *   <li>Utilities for module and package elements</li>
 * </ul>
 *
 * @see javax.lang.model.element
 * @see javax.lang.model.type
 * @see javax.annotation.processing
 */
package io.hexaglue.core.frontend.jsr269;
