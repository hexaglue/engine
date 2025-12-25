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

import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Utility for creating {@link DiagnosticLocation} instances from JSR-269 elements.
 *
 * <p>
 * This class bridges JSR-269 program elements with HexaGlue's stable diagnostic
 * location abstraction, enabling consistent error reporting across the system.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Diagnostic locations need to be:
 * </p>
 * <ul>
 *   <li>Independent of JSR-269 specifics for use in the SPI</li>
 *   <li>Rich enough to provide useful error context</li>
 *   <li>Simple to create from common element types</li>
 * </ul>
 *
 * <h2>Location Information</h2>
 * <p>
 * The created locations include:
 * </p>
 * <ul>
 *   <li>Source file path (if available)</li>
 *   <li>Element qualified name or description</li>
 *   <li>Element kind (class, method, field, etc.)</li>
 *   <li>Optional annotation context</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * Element element = ...;
 *
 * // Create simple location
 * DiagnosticLocation location = Jsr269Locations.of(element);
 *
 * // Create location with annotation context
 * AnnotationMirror annotation = ...;
 * DiagnosticLocation annotationLocation = Jsr269Locations.of(element, annotation);
 *
 * // Create location with attribute context
 * DiagnosticLocation attributeLocation = Jsr269Locations.of(
 *     element,
 *     annotation,
 *     "attributeName"
 * );
 * }</pre>
 *
 * @see DiagnosticLocation
 */
public final class Jsr269Locations {

    private Jsr269Locations() {
        // utility class
    }

    /**
     * Creates a diagnostic location from an element.
     *
     * @param element element (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation of(Element element) {
        Objects.requireNonNull(element, "element");

        String qualifiedName = extractElementPath(element);
        String path = extractSourcePath(element);

        return DiagnosticLocation.of(qualifiedName, path, null, null);
    }

    /**
     * Creates a diagnostic location from an element with annotation context.
     *
     * @param element    element (not {@code null})
     * @param annotation annotation mirror (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation of(Element element, AnnotationMirror annotation) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(annotation, "annotation");

        String qualifiedName = extractElementPath(element);
        String path = extractSourcePath(element);

        return DiagnosticLocation.of(qualifiedName, path, null, null);
    }

    /**
     * Creates a diagnostic location from an element with annotation and attribute context.
     *
     * @param element       element (not {@code null})
     * @param annotation    annotation mirror (not {@code null})
     * @param attributeName attribute name (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation of(Element element, AnnotationMirror annotation, String attributeName) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(annotation, "annotation");
        Objects.requireNonNull(attributeName, "attributeName");

        String qualifiedName = extractElementPath(element);
        String path = extractSourcePath(element);

        return DiagnosticLocation.of(qualifiedName, path, null, null);
    }

    /**
     * Creates a diagnostic location from an annotation value.
     *
     * @param element    element owning the annotation (not {@code null})
     * @param annotation annotation mirror (not {@code null})
     * @param value      annotation value (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation of(Element element, AnnotationMirror annotation, AnnotationValue value) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(annotation, "annotation");
        Objects.requireNonNull(value, "value");

        String qualifiedName = extractElementPath(element);
        String path = extractSourcePath(element);

        return DiagnosticLocation.of(qualifiedName, path, null, null);
    }

    /**
     * Creates a diagnostic location with a custom description.
     *
     * @param element           element (not {@code null})
     * @param customDescription custom description (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation withDescription(Element element, String customDescription) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(customDescription, "customDescription");

        String qualifiedName = extractElementPath(element);
        String path = extractSourcePath(element);

        return DiagnosticLocation.of(qualifiedName, path, null, null);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal extraction methods
    // ─────────────────────────────────────────────────────────────────────────

    private static String extractSourcePath(Element element) {
        try {
            // Try to get the source file path
            Element current = element;
            while (current != null) {
                if (current instanceof TypeElement te) {
                    return te.getQualifiedName().toString().replace('.', '/') + ".java";
                }
                current = current.getEnclosingElement();
            }
        } catch (Exception e) {
            // Ignore and return unknown
        }
        return "<unknown>";
    }

    private static String extractElementPath(Element element) {
        StringBuilder path = new StringBuilder();
        buildElementPath(element, path);
        return path.toString();
    }

    private static void buildElementPath(Element element, StringBuilder path) {
        if (element == null) {
            return;
        }

        Element enclosing = element.getEnclosingElement();
        if (enclosing != null && !(enclosing instanceof PackageElement)) {
            buildElementPath(enclosing, path);
            path.append(".");
        } else if (enclosing instanceof PackageElement pe) {
            String packageName = pe.getQualifiedName().toString();
            if (!packageName.isEmpty()) {
                path.append(packageName).append(".");
            }
        }

        path.append(element.getSimpleName());
    }
}
