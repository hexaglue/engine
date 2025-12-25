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
package io.hexaglue.core.frontend.jsr269;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

/**
 * Utility methods for working with JSR-269 {@link AnnotationMirror} instances.
 *
 * <p>
 * This class provides convenient operations for extracting and manipulating
 * annotation metadata from the JSR-269 annotation processing API.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Working with {@link AnnotationMirror} can be verbose and error-prone. This utility
 * provides:
 * </p>
 * <ul>
 *   <li>Simplified attribute value extraction</li>
 *   <li>Type-safe attribute access with proper casting</li>
 *   <li>Null-safe operations with {@link Optional} return types</li>
 *   <li>Common annotation queries</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe. However, {@link AnnotationMirror}
 * instances are only valid within the annotation processing round that created them.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * AnnotationMirror mirror = ...;
 *
 * // Get annotation type
 * String qualifiedName = Jsr269Mirrors.getAnnotationType(mirror);
 *
 * // Extract attribute values
 * Optional<String> value = Jsr269Mirrors.getAttributeAsString(mirror, "value");
 * Optional<TypeMirror> classValue = Jsr269Mirrors.getAttributeAsType(mirror, "targetClass");
 * List<String> names = Jsr269Mirrors.getAttributeAsStringList(mirror, "names");
 * }</pre>
 *
 * @see AnnotationMirror
 * @see AnnotationValue
 */
public final class Jsr269Mirrors {

    private Jsr269Mirrors() {
        // utility class
    }

    /**
     * Returns the qualified name of the annotation type.
     *
     * @param mirror annotation mirror (not {@code null})
     * @return qualified annotation type name (never {@code null})
     */
    public static String getAnnotationType(AnnotationMirror mirror) {
        Objects.requireNonNull(mirror, "mirror");
        return mirror.getAnnotationType().toString();
    }

    /**
     * Returns the simple name of the annotation type.
     *
     * @param mirror annotation mirror (not {@code null})
     * @return simple annotation type name (never {@code null})
     */
    public static String getAnnotationSimpleName(AnnotationMirror mirror) {
        Objects.requireNonNull(mirror, "mirror");
        TypeElement element = (TypeElement) mirror.getAnnotationType().asElement();
        return element.getSimpleName().toString();
    }

    /**
     * Returns all attribute values including defaults.
     *
     * @param mirror annotation mirror (not {@code null})
     * @return map of attribute names to values (never {@code null})
     */
    public static Map<? extends ExecutableElement, ? extends AnnotationValue> getAttributeValues(
            AnnotationMirror mirror) {
        Objects.requireNonNull(mirror, "mirror");
        return mirror.getElementValues();
    }

    /**
     * Returns the value of the specified attribute.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value if present
     */
    public static Optional<AnnotationValue> getAttribute(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        Map<? extends ExecutableElement, ? extends AnnotationValue> values = mirror.getElementValues();
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : values.entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(attributeName)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the value of the specified attribute as a string.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as string if present and convertible
     */
    public static Optional<String> getAttributeAsString(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(av -> av.getValue().toString())
                .map(s -> s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s);
    }

    /**
     * Returns the value of the specified attribute as an integer.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as integer if present and convertible
     */
    public static Optional<Integer> getAttributeAsInt(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(AnnotationValue::getValue)
                .filter(v -> v instanceof Integer)
                .map(v -> (Integer) v);
    }

    /**
     * Returns the value of the specified attribute as a boolean.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as boolean if present and convertible
     */
    public static Optional<Boolean> getAttributeAsBoolean(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(AnnotationValue::getValue)
                .filter(v -> v instanceof Boolean)
                .map(v -> (Boolean) v);
    }

    /**
     * Returns the value of the specified attribute as a type mirror.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as type mirror if present and convertible
     */
    public static Optional<TypeMirror> getAttributeAsType(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(AnnotationValue::getValue)
                .filter(v -> v instanceof TypeMirror)
                .map(v -> (TypeMirror) v);
    }

    /**
     * Returns the value of the specified attribute as an enum constant.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as enum constant if present and convertible
     */
    public static Optional<VariableElement> getAttributeAsEnum(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(AnnotationValue::getValue)
                .filter(v -> v instanceof VariableElement)
                .map(v -> (VariableElement) v);
    }

    /**
     * Returns the value of the specified attribute as an annotation mirror.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as annotation mirror if present and convertible
     */
    public static Optional<AnnotationMirror> getAttributeAsAnnotation(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(AnnotationValue::getValue)
                .filter(v -> v instanceof AnnotationMirror)
                .map(v -> (AnnotationMirror) v);
    }

    /**
     * Returns the value of the specified attribute as a list.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as list if present and convertible
     */
    @SuppressWarnings("unchecked")
    public static Optional<List<? extends AnnotationValue>> getAttributeAsList(
            AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttribute(mirror, attributeName)
                .map(AnnotationValue::getValue)
                .filter(v -> v instanceof List)
                .map(v -> (List<? extends AnnotationValue>) v);
    }

    /**
     * Returns the value of the specified attribute as a list of strings.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as string list (never {@code null})
     */
    public static List<String> getAttributeAsStringList(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttributeAsList(mirror, attributeName)
                .map(list -> list.stream()
                        .map(AnnotationValue::getValue)
                        .map(Object::toString)
                        .map(s -> s.startsWith("\"") && s.endsWith("\"") ? s.substring(1, s.length() - 1) : s)
                        .toList())
                .orElse(List.of());
    }

    /**
     * Returns the value of the specified attribute as a list of type mirrors.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return attribute value as type mirror list (never {@code null})
     */
    public static List<TypeMirror> getAttributeAsTypeList(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");

        return getAttributeAsList(mirror, attributeName)
                .map(list -> list.stream()
                        .map(AnnotationValue::getValue)
                        .filter(v -> v instanceof TypeMirror)
                        .map(v -> (TypeMirror) v)
                        .toList())
                .orElse(List.of());
    }

    /**
     * Returns whether the annotation has the specified attribute.
     *
     * @param mirror        annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return {@code true} if attribute is present
     */
    public static boolean hasAttribute(AnnotationMirror mirror, String attributeName) {
        Objects.requireNonNull(mirror, "mirror");
        Objects.requireNonNull(attributeName, "attributeName");
        return getAttribute(mirror, attributeName).isPresent();
    }

    /**
     * Returns the type element of the annotation.
     *
     * @param mirror annotation mirror (not {@code null})
     * @return annotation type element (never {@code null})
     */
    public static TypeElement getAnnotationTypeElement(AnnotationMirror mirror) {
        Objects.requireNonNull(mirror, "mirror");
        return (TypeElement) mirror.getAnnotationType().asElement();
    }

    /**
     * Returns whether two annotation mirrors represent the same annotation type.
     *
     * @param mirror1 first annotation mirror (not {@code null})
     * @param mirror2 second annotation mirror (not {@code null})
     * @return {@code true} if same annotation type
     */
    public static boolean isSameAnnotationType(AnnotationMirror mirror1, AnnotationMirror mirror2) {
        Objects.requireNonNull(mirror1, "mirror1");
        Objects.requireNonNull(mirror2, "mirror2");
        return getAnnotationType(mirror1).equals(getAnnotationType(mirror2));
    }

    /**
     * Finds an annotation mirror on an element by qualified name.
     *
     * @param element       element to search (not {@code null})
     * @param qualifiedName qualified annotation name (not {@code null})
     * @return annotation mirror if present
     */
    public static Optional<AnnotationMirror> findAnnotation(Element element, String qualifiedName) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : mirrors) {
            if (qualifiedName.equals(getAnnotationType(mirror))) {
                return Optional.of(mirror);
            }
        }
        return Optional.empty();
    }
}
