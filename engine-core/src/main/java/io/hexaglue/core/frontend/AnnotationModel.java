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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;

/**
 * Stable representation of an annotation instance.
 *
 * <p>
 * This class provides a frontend-agnostic view of annotations, abstracting away
 * the complexity of {@link AnnotationMirror} and providing convenient accessors
 * for annotation attributes.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While {@link AnnotationMirror} is powerful, it is also complex and tightly coupled
 * to the annotation processing API. This class provides:
 * </p>
 * <ul>
 *   <li>A simpler, more intuitive API for annotation inspection</li>
 *   <li>Type-safe attribute access with default value handling</li>
 *   <li>Immutable, cacheable representation</li>
 *   <li>Clear separation between frontend concerns and business logic</li>
 * </ul>
 *
 * <h2>Annotation Attributes</h2>
 * <p>
 * Annotation attributes are exposed as a map of names to {@link AnnotationValue} objects.
 * The underlying {@link AnnotationMirror} is also accessible for advanced operations.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * AnnotationModel annotation = ...;
 *
 * // Check annotation type
 * if (annotation.qualifiedName().equals("com.example.MyAnnotation")) {
 *     // Get attribute value
 *     Optional<AnnotationValue> value = annotation.attribute("value");
 *
 *     // Access underlying mirror for advanced operations
 *     AnnotationMirror mirror = annotation.mirror();
 * }
 * }</pre>
 *
 * @see AnnotationIntrospector
 * @see AnnotationMirror
 */
public final class AnnotationModel {

    private final String qualifiedName;
    private final AnnotationMirror mirror;
    private final Map<String, AnnotationValue> attributes;

    /**
     * Constructs an annotation model.
     *
     * @param qualifiedName qualified name of the annotation type (not {@code null})
     * @param mirror        underlying annotation mirror (not {@code null})
     * @param attributes    map of attribute names to values (not {@code null})
     */
    public AnnotationModel(String qualifiedName, AnnotationMirror mirror, Map<String, AnnotationValue> attributes) {
        this.qualifiedName = Objects.requireNonNull(qualifiedName, "qualifiedName");
        this.mirror = Objects.requireNonNull(mirror, "mirror");
        this.attributes = Map.copyOf(Objects.requireNonNull(attributes, "attributes"));
    }

    /**
     * Creates an annotation model from an annotation mirror.
     *
     * <p>
     * This factory method extracts the qualified name and all attribute values
     * (including defaults) from the mirror.
     * </p>
     *
     * @param mirror annotation mirror (not {@code null})
     * @return annotation model (never {@code null})
     */
    public static AnnotationModel of(AnnotationMirror mirror) {
        Objects.requireNonNull(mirror, "mirror");

        String qualifiedName = mirror.getAnnotationType().toString();

        // Extract all attribute values (including defaults)
        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = mirror.getElementValues();

        Map<String, AnnotationValue> attributes = elementValues.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        e -> e.getKey().getSimpleName().toString(), Map.Entry::getValue));

        return new AnnotationModel(qualifiedName, mirror, attributes);
    }

    /**
     * Returns the qualified name of the annotation type.
     *
     * <p>
     * For example, {@code "java.lang.Override"} or {@code "com.example.MyAnnotation"}.
     * </p>
     *
     * @return qualified annotation type name (never {@code null})
     */
    public String qualifiedName() {
        return qualifiedName;
    }

    /**
     * Returns the simple name of the annotation type.
     *
     * <p>
     * For example, {@code "Override"} or {@code "MyAnnotation"}.
     * </p>
     *
     * @return simple annotation type name (never {@code null})
     */
    public String simpleName() {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }

    /**
     * Returns the underlying annotation mirror.
     *
     * <p>
     * This provides access to the full JSR-269 annotation API for advanced operations.
     * </p>
     *
     * @return annotation mirror (never {@code null})
     */
    public AnnotationMirror mirror() {
        return mirror;
    }

    /**
     * Returns all annotation attributes.
     *
     * <p>
     * The returned map contains all explicitly specified attribute values.
     * Default values are NOT included in this map unless explicitly overridden.
     * </p>
     *
     * @return attribute map (never {@code null}, immutable)
     */
    public Map<String, AnnotationValue> attributes() {
        return attributes;
    }

    /**
     * Returns the value of the specified attribute.
     *
     * @param name attribute name (not {@code null})
     * @return attribute value if present
     */
    public Optional<AnnotationValue> attribute(String name) {
        Objects.requireNonNull(name, "name");
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * Returns the value of the specified attribute as a string.
     *
     * @param name attribute name (not {@code null})
     * @return attribute value as string if present
     */
    public Optional<String> attributeAsString(String name) {
        return attribute(name).map(av -> av.getValue().toString());
    }

    /**
     * Returns whether this annotation has the specified attribute.
     *
     * @param name attribute name (not {@code null})
     * @return {@code true} if the attribute is present
     */
    public boolean hasAttribute(String name) {
        Objects.requireNonNull(name, "name");
        return attributes.containsKey(name);
    }

    /**
     * Returns whether this annotation is of the specified type.
     *
     * @param qualifiedName qualified annotation type name (not {@code null})
     * @return {@code true} if types match
     */
    public boolean isType(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return this.qualifiedName.equals(qualifiedName);
    }

    /**
     * Returns whether this annotation's simple name matches the given name.
     *
     * <p>
     * This is useful for quick checks when the fully qualified name is unknown
     * or not important.
     * </p>
     *
     * @param simpleName simple annotation name (not {@code null})
     * @return {@code true} if simple names match
     */
    public boolean hasSimpleName(String simpleName) {
        Objects.requireNonNull(simpleName, "simpleName");
        return simpleName().equals(simpleName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof AnnotationModel other)) return false;
        return qualifiedName.equals(other.qualifiedName) && attributes.equals(other.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName, attributes);
    }

    @Override
    public String toString() {
        return "@" + qualifiedName;
    }
}
