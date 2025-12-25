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
package io.hexaglue.spi.ir.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of an annotation on a domain element.
 *
 * <p>This abstraction intentionally simplifies annotation representation to provide
 * a platform-agnostic view to plugins:
 * <ul>
 *   <li>No access to annotation mirrors or compiler-specific types</li>
 *   <li>Attribute values are decoded to simple Java types</li>
 *   <li>Complex attribute types may be represented as strings</li>
 * </ul>
 *
 * <p><strong>Supported attribute value types:</strong>
 * <ul>
 *   <li>Primitives and wrappers: {@code Integer}, {@code Boolean}, {@code Long}, {@code Double}, etc.</li>
 *   <li>Strings: {@code String}</li>
 *   <li>Enums: {@code String} (qualified name, e.g., "jakarta.persistence.FetchType.LAZY")</li>
 *   <li>Classes: {@code String} (qualified name, e.g., "java.lang.String")</li>
 *   <li>Arrays: {@code List<?>} containing elements of the above types</li>
 *   <li>Nested annotations: {@code AnnotationView}</li>
 * </ul>
 *
 * <p><strong>Usage example:</strong>
 * <pre>{@code
 * // Reading JPA @Column annotation
 * Optional<AnnotationView> column = property.annotations().stream()
 *     .filter(a -> a.is("jakarta.persistence.Column"))
 *     .findFirst();
 *
 * if (column.isPresent()) {
 *     int length = column.get()
 *         .attribute("length", Integer.class)
 *         .orElse(255);
 *     boolean unique = column.get()
 *         .attribute("unique", Boolean.class)
 *         .orElse(false);
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
public interface AnnotationView {

    /**
     * Qualified name of the annotation type.
     *
     * <p>Example: {@code "jakarta.persistence.Column"}
     *
     * @return annotation type qualified name (never blank)
     */
    String qualifiedName();

    /**
     * Simple name of the annotation type.
     *
     * <p>Example: {@code "Column"} for {@code jakarta.persistence.Column}
     *
     * @return annotation simple name (never blank)
     */
    String simpleName();

    /**
     * Annotation attributes as key-value pairs.
     *
     * <p>Attribute values are decoded to simple types as documented in the class javadoc.
     * Only explicitly specified attributes are included; defaults declared in the annotation
     * definition are not resolved.</p>
     *
     * <p><strong>Examples:</strong>
     * <ul>
     *   <li>{@code @Column(length = 100)} → {@code {"length": 100}}</li>
     *   <li>{@code @Size(min = 1, max = 100)} → {@code {"min": 1, "max": 100}}</li>
     *   <li>{@code @OneToMany(cascade = ALL)} → {@code {"cascade": ["jakarta.persistence.CascadeType.ALL"]}}</li>
     * </ul>
     *
     * @return immutable map of attributes (never {@code null}, may be empty)
     */
    Map<String, Object> attributes();

    /**
     * Gets a single attribute value, if present.
     *
     * @param name attribute name (non-blank)
     * @return attribute value or empty if not present
     * @throws NullPointerException if name is null
     */
    default Optional<Object> attribute(String name) {
        Objects.requireNonNull(name, "name");
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(attributes().get(trimmedName));
    }

    /**
     * Gets a typed attribute value.
     *
     * <p>This method attempts to cast the attribute value to the specified type.
     * If the attribute is not present or cannot be cast to the specified type,
     * an empty Optional is returned.</p>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * Optional<Integer> length = annotation.attribute("length", Integer.class);
     * Optional<String> name = annotation.attribute("name", String.class);
     * }</pre>
     *
     * @param name attribute name
     * @param type expected type
     * @param <T> attribute type
     * @return typed value or empty if not present or wrong type
     * @throws NullPointerException if type is null
     */
    @SuppressWarnings("unchecked")
    default <T> Optional<T> attribute(String name, Class<T> type) {
        Objects.requireNonNull(type, "type");
        return attribute(name).filter(type::isInstance).map(v -> (T) v);
    }

    /**
     * Checks if this annotation has a specific qualified name.
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * if (annotation.is("jakarta.persistence.Column")) {
     *     // Handle @Column annotation
     * }
     * }</pre>
     *
     * @param qualifiedName annotation qualified name
     * @return true if this annotation's qualified name matches
     * @throws NullPointerException if qualifiedName is null
     */
    default boolean is(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        String trimmed = qualifiedName.trim();
        return !trimmed.isEmpty() && this.qualifiedName().equals(trimmed);
    }

    /**
     * Factory method for creating simple annotation views.
     *
     * <p>This factory is intended for tests, tooling, and plugin implementations
     * that need to create annotation views programmatically.</p>
     *
     * <p><strong>Example:</strong>
     * <pre>{@code
     * AnnotationView column = AnnotationView.of(
     *     "jakarta.persistence.Column",
     *     Map.of("length", 100, "unique", true)
     * );
     * }</pre>
     *
     * @param qualifiedName annotation type qualified name (non-blank)
     * @param attributes attribute map (non-null, may be empty)
     * @return annotation view instance
     * @throws NullPointerException if qualifiedName or attributes is null
     * @throws IllegalArgumentException if qualifiedName is blank
     */
    static AnnotationView of(String qualifiedName, Map<String, Object> attributes) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(attributes, "attributes");

        String qn = qualifiedName.trim();
        if (qn.isEmpty()) {
            throw new IllegalArgumentException("qualifiedName must not be blank");
        }

        // Compute simple name
        String sn = qn.contains(".") ? qn.substring(qn.lastIndexOf('.') + 1) : qn;

        // Create immutable copy of attributes
        Map<String, Object> attrs = Map.copyOf(attributes);

        return new AnnotationView() {
            @Override
            public String qualifiedName() {
                return qn;
            }

            @Override
            public String simpleName() {
                return sn;
            }

            @Override
            public Map<String, Object> attributes() {
                return attrs;
            }

            @Override
            public String toString() {
                return "@" + sn + (attrs.isEmpty() ? "" : attrs);
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof AnnotationView)) return false;
                AnnotationView other = (AnnotationView) obj;
                return qn.equals(other.qualifiedName()) && attrs.equals(other.attributes());
            }

            @Override
            public int hashCode() {
                return Objects.hash(qn, attrs);
            }
        };
    }
}
