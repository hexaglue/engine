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
package io.hexaglue.core.diagnostics;

import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import java.nio.file.Path;
import java.util.Objects;
import javax.lang.model.element.Element;

/**
 * Utility for creating {@link DiagnosticLocation} instances from various sources.
 *
 * <p>
 * This class provides convenient factory methods to construct diagnostic locations from:
 * <ul>
 *   <li>JSR-269 {@link Element} instances</li>
 *   <li>File paths</li>
 *   <li>Qualified names</li>
 * </ul>
 * </p>
 *
 * <p>
 * The utility intentionally shields the SPI from direct exposure to {@code javax.lang.model.*}
 * internals, maintaining a stable abstraction boundary.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class Locations {

    private Locations() {
        // utility class
    }

    /**
     * Creates a location from a JSR-269 element.
     *
     * <p>
     * This method attempts to extract the qualified name and source position from the element.
     * If the element does not have a source position (e.g., it is a synthetic element), the
     * location will contain only the qualified name.
     * </p>
     *
     * @param element the element (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation fromElement(Element element) {
        Objects.requireNonNull(element, "element");

        String qualifiedName = extractQualifiedName(element);
        String path = null;

        // Extract source file path if available
        try {
            Element enclosing = element;
            while (enclosing != null
                    && enclosing.getKind() != javax.lang.model.element.ElementKind.CLASS
                    && enclosing.getKind() != javax.lang.model.element.ElementKind.INTERFACE
                    && enclosing.getKind() != javax.lang.model.element.ElementKind.ENUM
                    && enclosing.getKind() != javax.lang.model.element.ElementKind.ANNOTATION_TYPE
                    && enclosing.getKind() != javax.lang.model.element.ElementKind.RECORD) {
                enclosing = enclosing.getEnclosingElement();
            }

            if (enclosing != null) {
                javax.lang.model.element.TypeElement typeElement = (javax.lang.model.element.TypeElement) enclosing;
                path = extractPath(typeElement);
            }
        } catch (Exception e) {
            // Best-effort: if extraction fails, use qualified name only
        }

        return DiagnosticLocation.of(qualifiedName, path, null, null);
    }

    /**
     * Creates a location from a file path.
     *
     * @param path the file path (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation fromPath(Path path) {
        Objects.requireNonNull(path, "path");
        return DiagnosticLocation.ofPath(path.toString(), null, null);
    }

    /**
     * Creates a location from a file path with line information.
     *
     * @param path the file path (not {@code null})
     * @param line 1-based line number (nullable)
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation fromPath(Path path, Integer line) {
        Objects.requireNonNull(path, "path");
        return DiagnosticLocation.ofPath(path.toString(), line, null);
    }

    /**
     * Creates a location from a file path with line and column information.
     *
     * @param path   the file path (not {@code null})
     * @param line   1-based line number (nullable)
     * @param column 1-based column number (nullable)
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation fromPath(Path path, Integer line, Integer column) {
        Objects.requireNonNull(path, "path");
        return DiagnosticLocation.ofPath(path.toString(), line, column);
    }

    /**
     * Creates a location from a qualified name.
     *
     * @param qualifiedName the qualified name (not {@code null})
     * @return diagnostic location (never {@code null})
     */
    public static DiagnosticLocation fromQualifiedName(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return DiagnosticLocation.ofQualifiedName(qualifiedName);
    }

    /**
     * Returns the unknown location singleton.
     *
     * @return unknown location (never {@code null})
     */
    public static DiagnosticLocation unknown() {
        return DiagnosticLocation.unknown();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String extractQualifiedName(Element element) {
        Element e = element;
        while (e != null) {
            if (e instanceof javax.lang.model.element.QualifiedNameable) {
                javax.lang.model.element.QualifiedNameable qn = (javax.lang.model.element.QualifiedNameable) e;
                String name = qn.getQualifiedName().toString();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
            e = e.getEnclosingElement();
        }
        return element.getSimpleName().toString();
    }

    /**
     * Extracts source file path from a TypeElement.
     *
     * <p>
     * This method derives the source file path from the qualified name by converting
     * package separators to path separators. This is a best-effort portable approach
     * using only JSR-269 standard APIs.
     * </p>
     *
     * <h2>Implementation Notes</h2>
     * <p>
     * To obtain the exact source file path, one would need to use
     * {@code com.sun.source.util.Trees}, which is not part of JSR-269 and is
     * javac-specific. For portability, we use the qualified name approach.
     * </p>
     *
     * <p>
     * This limitation is acceptable because diagnostics are ultimately reported via
     * JSR-269 {@code Messager} which uses {@code Element} instances directly, allowing
     * IDEs and build tools to navigate to the precise source location.
     * </p>
     *
     * @param typeElement type element (not {@code null})
     * @return derived source file path, or {@code null} if extraction fails
     */
    private static String extractPath(javax.lang.model.element.TypeElement typeElement) {
        try {
            // Derive path from qualified name (standard JSR-269 approach)
            String qn = typeElement.getQualifiedName().toString();
            return qn.replace('.', '/') + ".java";

        } catch (Exception e) {
            return null;
        }
    }
}
