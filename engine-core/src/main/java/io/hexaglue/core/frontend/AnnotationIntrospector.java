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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Utility for introspecting annotations on source elements.
 *
 * <p>
 * This class provides convenient methods for discovering and inspecting annotations
 * on {@link Element} instances from the JSR-269 annotation processing API.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Annotation introspection with {@link Element#getAnnotationMirrors()} and
 * {@link Element#getAnnotation(Class)} can be verbose and error-prone. This utility
 * provides:
 * </p>
 * <ul>
 *   <li>Simplified API for common annotation queries</li>
 *   <li>Support for both runtime and source-only annotations</li>
 *   <li>Convenient conversion to {@link AnnotationModel}</li>
 *   <li>Null-safe operations with {@link Optional} return types</li>
 * </ul>
 *
 * <h2>Annotation Visibility</h2>
 * <p>
 * This introspector can discover annotations regardless of their retention policy:
 * </p>
 * <ul>
 *   <li>{@code @Retention(SOURCE)} - Available during annotation processing</li>
 *   <li>{@code @Retention(CLASS)} - Available during annotation processing</li>
 *   <li>{@code @Retention(RUNTIME)} - Available during annotation processing and at runtime</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * TypeElement typeElement = ...;
 *
 * // Check if annotated
 * if (AnnotationIntrospector.hasAnnotation(typeElement, "com.example.Entity")) {
 *     // Find annotation
 *     Optional<AnnotationModel> entity = AnnotationIntrospector.findAnnotation(
 *         typeElement,
 *         "com.example.Entity"
 *     );
 *
 *     // Get all annotations
 *     List<AnnotationModel> allAnnotations = AnnotationIntrospector.getAnnotations(typeElement);
 * }
 * }</pre>
 *
 * @see AnnotationModel
 */
public final class AnnotationIntrospector {

    private AnnotationIntrospector() {
        // utility class
    }

    /**
     * Returns all annotations present on the element.
     *
     * @param element element to inspect (not {@code null})
     * @return list of annotations (never {@code null}, immutable)
     */
    public static List<AnnotationModel> getAnnotations(Element element) {
        Objects.requireNonNull(element, "element");

        return element.getAnnotationMirrors().stream().map(AnnotationModel::of).toList();
    }

    /**
     * Finds an annotation by qualified name.
     *
     * @param element       element to inspect (not {@code null})
     * @param qualifiedName fully qualified annotation type name (not {@code null})
     * @return annotation if present
     */
    public static Optional<AnnotationModel> findAnnotation(Element element, String qualifiedName) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return element.getAnnotationMirrors().stream()
                .filter(mirror ->
                        qualifiedName.equals(mirror.getAnnotationType().toString()))
                .findFirst()
                .map(AnnotationModel::of);
    }

    /**
     * Finds an annotation by type element.
     *
     * @param element        element to inspect (not {@code null})
     * @param annotationType annotation type element (not {@code null})
     * @return annotation if present
     */
    public static Optional<AnnotationModel> findAnnotation(Element element, TypeElement annotationType) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(annotationType, "annotationType");

        String qualifiedName = annotationType.getQualifiedName().toString();
        return findAnnotation(element, qualifiedName);
    }

    /**
     * Finds an annotation by simple name.
     *
     * <p>
     * <strong>Warning:</strong> This method matches by simple name only and may return
     * incorrect results if multiple annotations with the same simple name exist.
     * Prefer {@link #findAnnotation(Element, String)} with a qualified name when possible.
     * </p>
     *
     * @param element    element to inspect (not {@code null})
     * @param simpleName simple annotation type name (not {@code null})
     * @return annotation if present
     */
    public static Optional<AnnotationModel> findAnnotationBySimpleName(Element element, String simpleName) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(simpleName, "simpleName");

        return element.getAnnotationMirrors().stream()
                .map(AnnotationModel::of)
                .filter(model -> model.hasSimpleName(simpleName))
                .findFirst();
    }

    /**
     * Returns whether the element is annotated with the specified annotation.
     *
     * @param element       element to inspect (not {@code null})
     * @param qualifiedName fully qualified annotation type name (not {@code null})
     * @return {@code true} if annotation is present
     */
    public static boolean hasAnnotation(Element element, String qualifiedName) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return element.getAnnotationMirrors().stream()
                .anyMatch(mirror ->
                        qualifiedName.equals(mirror.getAnnotationType().toString()));
    }

    /**
     * Returns whether the element is annotated with the specified annotation.
     *
     * @param element        element to inspect (not {@code null})
     * @param annotationType annotation type element (not {@code null})
     * @return {@code true} if annotation is present
     */
    public static boolean hasAnnotation(Element element, TypeElement annotationType) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(annotationType, "annotationType");

        String qualifiedName = annotationType.getQualifiedName().toString();
        return hasAnnotation(element, qualifiedName);
    }

    /**
     * Returns whether the element has at least one annotation.
     *
     * @param element element to inspect (not {@code null})
     * @return {@code true} if element has any annotations
     */
    public static boolean hasAnyAnnotation(Element element) {
        Objects.requireNonNull(element, "element");
        return !element.getAnnotationMirrors().isEmpty();
    }

    /**
     * Returns whether the element is annotated with any of the specified annotations.
     *
     * @param element        element to inspect (not {@code null})
     * @param qualifiedNames qualified annotation type names (not {@code null})
     * @return {@code true} if any annotation is present
     */
    public static boolean hasAnyAnnotation(Element element, String... qualifiedNames) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(qualifiedNames, "qualifiedNames");

        List<String> names = List.of(qualifiedNames);
        return element.getAnnotationMirrors().stream()
                .anyMatch(mirror -> names.contains(mirror.getAnnotationType().toString()));
    }

    /**
     * Returns whether the element is annotated with all of the specified annotations.
     *
     * @param element        element to inspect (not {@code null})
     * @param qualifiedNames qualified annotation type names (not {@code null})
     * @return {@code true} if all annotations are present
     */
    public static boolean hasAllAnnotations(Element element, String... qualifiedNames) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(qualifiedNames, "qualifiedNames");

        List<String> presentAnnotations = element.getAnnotationMirrors().stream()
                .map(mirror -> mirror.getAnnotationType().toString())
                .toList();

        for (String name : qualifiedNames) {
            if (!presentAnnotations.contains(name)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the underlying annotation mirror for the specified annotation.
     *
     * <p>
     * This provides direct access to the JSR-269 API when needed.
     * </p>
     *
     * @param element       element to inspect (not {@code null})
     * @param qualifiedName fully qualified annotation type name (not {@code null})
     * @return annotation mirror if present
     */
    public static Optional<AnnotationMirror> findAnnotationMirror(Element element, String qualifiedName) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : mirrors) {
            if (qualifiedName.equals(mirror.getAnnotationType().toString())) {
                return Optional.of(mirror);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns annotations matching a package prefix.
     *
     * <p>
     * This is useful for finding all annotations from a specific package or framework.
     * </p>
     *
     * @param element       element to inspect (not {@code null})
     * @param packagePrefix package prefix (not {@code null})
     * @return list of matching annotations (never {@code null}, immutable)
     */
    public static List<AnnotationModel> findAnnotationsByPackage(Element element, String packagePrefix) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(packagePrefix, "packagePrefix");

        return element.getAnnotationMirrors().stream()
                .filter(mirror -> mirror.getAnnotationType().toString().startsWith(packagePrefix))
                .map(AnnotationModel::of)
                .toList();
    }
}
