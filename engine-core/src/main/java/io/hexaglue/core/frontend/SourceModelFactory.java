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
package io.hexaglue.core.frontend;

import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Factory for creating stable source element models.
 *
 * <p>
 * This factory bridges the JSR-269 annotation processing API ({@link Element})
 * with HexaGlue's internal element model ({@link ElementModel}). It handles
 * the conversion of element metadata, annotations, and type information.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Direct usage of {@link Element} throughout the codebase would create tight
 * coupling to the annotation processing API. This factory provides:
 * </p>
 * <ul>
 *   <li>Centralized element model creation</li>
 *   <li>Consistent type resolution via {@link TypeResolver}</li>
 *   <li>Simplified annotation extraction</li>
 *   <li>Clear separation between frontend and business logic</li>
 * </ul>
 *
 * <h2>Type Resolution</h2>
 * <p>
 * The factory requires a {@link TypeResolver} to convert {@link TypeMirror} instances
 * to HexaGlue's stable {@link TypeRef} representation. This ensures consistent
 * type handling across the system.
 * </p>
 *
 * <h2>Supported Element Kinds</h2>
 * <p>
 * The factory can create models for all standard element kinds:
 * </p>
 * <ul>
 *   <li>Types ({@link TypeElement})</li>
 *   <li>Methods and constructors ({@link ExecutableElement})</li>
 *   <li>Fields and parameters ({@link VariableElement})</li>
 *   <li>Packages ({@link PackageElement})</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are thread-safe as long as the underlying {@link TypeResolver} is.
 * The factory itself maintains no mutable state.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * TypeResolver resolver = ...;
 * SourceModelFactory factory = SourceModelFactory.create(resolver);
 *
 * TypeElement typeElement = ...;
 * ElementModel model = factory.createModel(typeElement);
 *
 * // Access element properties
 * String name = model.simpleName();
 * List<AnnotationModel> annotations = model.annotations();
 * }</pre>
 *
 * @see ElementModel
 * @see TypeResolver
 */
public final class SourceModelFactory {

    private final TypeResolver typeResolver;

    private SourceModelFactory(TypeResolver typeResolver) {
        this.typeResolver = Objects.requireNonNull(typeResolver, "typeResolver");
    }

    /**
     * Creates a source model factory.
     *
     * @param typeResolver type resolver (not {@code null})
     * @return factory (never {@code null})
     */
    public static SourceModelFactory create(TypeResolver typeResolver) {
        Objects.requireNonNull(typeResolver, "typeResolver");
        return new SourceModelFactory(typeResolver);
    }

    /**
     * Creates an element model from a JSR-269 element.
     *
     * @param element element to model (not {@code null})
     * @return element model (never {@code null})
     */
    public ElementModel createModel(Element element) {
        Objects.requireNonNull(element, "element");

        String simpleName = extractSimpleName(element);
        String qualifiedName = extractQualifiedName(element);
        ElementKind kind = element.getKind();
        Set<Modifier> modifiers = element.getModifiers();
        List<AnnotationModel> annotations = AnnotationIntrospector.getAnnotations(element);
        TypeRef type = extractType(element);

        return new ElementModel(simpleName, qualifiedName, kind, modifiers, annotations, type, element);
    }

    /**
     * Creates element models from a collection of JSR-269 elements.
     *
     * @param elements elements to model (not {@code null})
     * @return element models (never {@code null})
     */
    public List<ElementModel> createModels(Iterable<? extends Element> elements) {
        Objects.requireNonNull(elements, "elements");

        return java.util.stream.StreamSupport.stream(elements.spliterator(), false)
                .map(this::createModel)
                .toList();
    }

    /**
     * Returns the type resolver backing this factory.
     *
     * @return type resolver (never {@code null})
     */
    public TypeResolver typeResolver() {
        return typeResolver;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal extraction methods
    // ─────────────────────────────────────────────────────────────────────────

    private String extractSimpleName(Element element) {
        return element.getSimpleName().toString();
    }

    private String extractQualifiedName(Element element) {
        return switch (element.getKind()) {
            case CLASS, INTERFACE, ENUM, ANNOTATION_TYPE -> {
                if (element instanceof TypeElement te) {
                    yield te.getQualifiedName().toString();
                }
                yield null;
            }
            case PACKAGE -> {
                if (element instanceof PackageElement pe) {
                    yield pe.getQualifiedName().toString();
                }
                yield null;
            }
            default -> null;
        };
    }

    private TypeRef extractType(Element element) {
        TypeMirror typeMirror =
                switch (element.getKind()) {
                    case CLASS, INTERFACE, ENUM, ANNOTATION_TYPE -> {
                        if (element instanceof TypeElement te) {
                            yield te.asType();
                        }
                        yield null;
                    }
                    case FIELD, PARAMETER, LOCAL_VARIABLE, ENUM_CONSTANT, EXCEPTION_PARAMETER, RESOURCE_VARIABLE -> {
                        if (element instanceof VariableElement ve) {
                            yield ve.asType();
                        }
                        yield null;
                    }
                    case METHOD, CONSTRUCTOR -> {
                        if (element instanceof ExecutableElement ee) {
                            yield ee.getReturnType();
                        }
                        yield null;
                    }
                    default -> null;
                };

        if (typeMirror == null) {
            return null;
        }

        try {
            return typeResolver.resolve(typeMirror);
        } catch (Exception e) {
            // If type resolution fails, return null rather than failing the entire model
            // This allows partial models to be created even when some type information
            // is unavailable or malformed
            return null;
        }
    }
}
