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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * Utility methods for working with JSR-269 {@link Element} instances.
 *
 * <p>
 * This class provides convenient operations on program elements that are commonly
 * needed but verbose to express using the raw JSR-269 API. It complements the
 * standard {@link Elements} utility with additional helpers.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While {@link Elements} provides basic element operations, many common patterns
 * require multiple API calls or complex traversals. This utility provides:
 * </p>
 * <ul>
 *   <li>Simplified element kind checks and casting</li>
 *   <li>Safe navigation through element hierarchies</li>
 *   <li>Common element property queries</li>
 *   <li>Null-safe operations with {@link Optional} return types</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe. However, {@link Element} instances
 * are only valid within the annotation processing round that created them.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * Element element = ...;
 *
 * // Safe type casting
 * Optional<TypeElement> typeElement = Jsr269Elements.asTypeElement(element);
 *
 * // Check element properties
 * if (Jsr269Elements.isPublicClass(element)) {
 *     String qualifiedName = Jsr269Elements.getQualifiedName(element);
 * }
 *
 * // Navigate hierarchy
 * Optional<TypeElement> enclosingType = Jsr269Elements.getEnclosingTypeElement(element);
 * }</pre>
 *
 * @see Elements
 * @see Element
 */
public final class Jsr269Elements {

    private Jsr269Elements() {
        // utility class
    }

    /**
     * Returns the element as a {@link TypeElement} if it represents a type.
     *
     * @param element element to cast (not {@code null})
     * @return type element if applicable
     */
    public static Optional<TypeElement> asTypeElement(Element element) {
        Objects.requireNonNull(element, "element");
        return element instanceof TypeElement te ? Optional.of(te) : Optional.empty();
    }

    /**
     * Returns the element as an {@link ExecutableElement} if it represents an executable.
     *
     * @param element element to cast (not {@code null})
     * @return executable element if applicable
     */
    public static Optional<ExecutableElement> asExecutableElement(Element element) {
        Objects.requireNonNull(element, "element");
        return element instanceof ExecutableElement ee ? Optional.of(ee) : Optional.empty();
    }

    /**
     * Returns the element as a {@link VariableElement} if it represents a variable.
     *
     * @param element element to cast (not {@code null})
     * @return variable element if applicable
     */
    public static Optional<VariableElement> asVariableElement(Element element) {
        Objects.requireNonNull(element, "element");
        return element instanceof VariableElement ve ? Optional.of(ve) : Optional.empty();
    }

    /**
     * Returns the element as a {@link PackageElement} if it represents a package.
     *
     * @param element element to cast (not {@code null})
     * @return package element if applicable
     */
    public static Optional<PackageElement> asPackageElement(Element element) {
        Objects.requireNonNull(element, "element");
        return element instanceof PackageElement pe ? Optional.of(pe) : Optional.empty();
    }

    /**
     * Returns the qualified name of the element if available.
     *
     * <p>
     * Qualified names are typically available for top-level types and packages.
     * </p>
     *
     * @param element element to query (not {@code null})
     * @return qualified name if available
     */
    public static Optional<String> getQualifiedName(Element element) {
        Objects.requireNonNull(element, "element");

        return switch (element.getKind()) {
            case CLASS, INTERFACE, ENUM, ANNOTATION_TYPE -> {
                if (element instanceof TypeElement te) {
                    yield Optional.of(te.getQualifiedName().toString());
                }
                yield Optional.empty();
            }
            case PACKAGE -> {
                if (element instanceof PackageElement pe) {
                    yield Optional.of(pe.getQualifiedName().toString());
                }
                yield Optional.empty();
            }
            default -> Optional.empty();
        };
    }

    /**
     * Returns the simple name of the element.
     *
     * @param element element to query (not {@code null})
     * @return simple name (never {@code null})
     */
    public static String getSimpleName(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getSimpleName().toString();
    }

    /**
     * Returns the enclosing type element if available.
     *
     * @param element element to query (not {@code null})
     * @return enclosing type element if present
     */
    public static Optional<TypeElement> getEnclosingTypeElement(Element element) {
        Objects.requireNonNull(element, "element");

        Element enclosing = element.getEnclosingElement();
        while (enclosing != null) {
            if (enclosing instanceof TypeElement te) {
                return Optional.of(te);
            }
            enclosing = enclosing.getEnclosingElement();
        }
        return Optional.empty();
    }

    /**
     * Returns the enclosing package element.
     *
     * @param element element to query (not {@code null})
     * @param elements element utilities (not {@code null})
     * @return package element (never {@code null})
     */
    public static PackageElement getPackageElement(Element element, Elements elements) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(elements, "elements");
        return elements.getPackageOf(element);
    }

    /**
     * Returns whether the element is public.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if public
     */
    public static boolean isPublic(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getModifiers().contains(Modifier.PUBLIC);
    }

    /**
     * Returns whether the element is private.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if private
     */
    public static boolean isPrivate(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getModifiers().contains(Modifier.PRIVATE);
    }

    /**
     * Returns whether the element is protected.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if protected
     */
    public static boolean isProtected(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getModifiers().contains(Modifier.PROTECTED);
    }

    /**
     * Returns whether the element is static.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if static
     */
    public static boolean isStatic(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getModifiers().contains(Modifier.STATIC);
    }

    /**
     * Returns whether the element is final.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if final
     */
    public static boolean isFinal(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getModifiers().contains(Modifier.FINAL);
    }

    /**
     * Returns whether the element is abstract.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if abstract
     */
    public static boolean isAbstract(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getModifiers().contains(Modifier.ABSTRACT);
    }

    /**
     * Returns whether the element is a public class.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if public class
     */
    public static boolean isPublicClass(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getKind() == ElementKind.CLASS && isPublic(element);
    }

    /**
     * Returns whether the element is a public interface.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if public interface
     */
    public static boolean isPublicInterface(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getKind() == ElementKind.INTERFACE && isPublic(element);
    }

    /**
     * Returns whether the element is an enum.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if enum
     */
    public static boolean isEnum(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getKind() == ElementKind.ENUM;
    }

    /**
     * Returns whether the element is an annotation type.
     *
     * @param element element to check (not {@code null})
     * @return {@code true} if annotation type
     */
    public static boolean isAnnotationType(Element element) {
        Objects.requireNonNull(element, "element");
        return element.getKind() == ElementKind.ANNOTATION_TYPE;
    }

    /**
     * Returns all enclosed elements of a specific kind.
     *
     * @param element element to query (not {@code null})
     * @param kind    element kind to filter (not {@code null})
     * @return list of enclosed elements of the specified kind (never {@code null})
     */
    public static List<? extends Element> getEnclosedElementsOfKind(Element element, ElementKind kind) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(kind, "kind");

        return element.getEnclosedElements().stream()
                .filter(e -> e.getKind() == kind)
                .toList();
    }

    /**
     * Returns all public methods of a type element.
     *
     * @param typeElement type element to query (not {@code null})
     * @return list of public methods (never {@code null})
     */
    public static List<ExecutableElement> getPublicMethods(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(Jsr269Elements::isPublic)
                .map(e -> (ExecutableElement) e)
                .toList();
    }

    /**
     * Returns all public fields of a type element.
     *
     * @param typeElement type element to query (not {@code null})
     * @return list of public fields (never {@code null})
     */
    public static List<VariableElement> getPublicFields(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        return typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .filter(Jsr269Elements::isPublic)
                .map(e -> (VariableElement) e)
                .toList();
    }

    /**
     * Returns whether an element has any of the specified modifiers.
     *
     * @param element   element to check (not {@code null})
     * @param modifiers modifiers to check (not {@code null})
     * @return {@code true} if any modifier is present
     */
    public static boolean hasAnyModifier(Element element, Modifier... modifiers) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(modifiers, "modifiers");

        Set<Modifier> elementModifiers = element.getModifiers();
        for (Modifier modifier : modifiers) {
            if (elementModifiers.contains(modifier)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether an element has all of the specified modifiers.
     *
     * @param element   element to check (not {@code null})
     * @param modifiers modifiers to check (not {@code null})
     * @return {@code true} if all modifiers are present
     */
    public static boolean hasAllModifiers(Element element, Modifier... modifiers) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(modifiers, "modifiers");

        Set<Modifier> elementModifiers = element.getModifiers();
        for (Modifier modifier : modifiers) {
            if (!elementModifiers.contains(modifier)) {
                return false;
            }
        }
        return true;
    }
}
