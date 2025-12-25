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

import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/**
 * Stable representation of a source element.
 *
 * <p>
 * This class provides a frontend-agnostic view of program elements (types, methods,
 * fields, parameters, etc.) from the source code, abstracting away the complexity
 * of {@link Element} and related JSR-269 APIs.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * The JSR-269 {@link Element} API is powerful but complex. This class provides:
 * </p>
 * <ul>
 *   <li>A simplified, stable API for element inspection</li>
 *   <li>Convenient accessors for common element properties</li>
 *   <li>Integration with HexaGlue's type system ({@link io.hexaglue.core.types.model.BaseTypeRef})</li>
 *   <li>Immutable, cacheable representation</li>
 * </ul>
 *
 * <h2>Element Kinds</h2>
 * <p>
 * This model supports all standard element kinds from {@link ElementKind}:
 * </p>
 * <ul>
 *   <li>{@link ElementKind#CLASS}, {@link ElementKind#INTERFACE}, {@link ElementKind#ENUM}</li>
 *   <li>{@link ElementKind#METHOD}, {@link ElementKind#CONSTRUCTOR}</li>
 *   <li>{@link ElementKind#FIELD}, {@link ElementKind#PARAMETER}</li>
 *   <li>{@link ElementKind#PACKAGE}</li>
 * </ul>
 *
 * <h2>Type Information</h2>
 * <p>
 * The element's type is represented using HexaGlue's stable SPI type system
 * ({@link TypeRef}) rather than {@link javax.lang.model.type.TypeMirror}.
 * This provides a stable, tool-agnostic type representation.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ElementModel element = ...;
 *
 * // Check element kind
 * if (element.kind() == ElementKind.CLASS) {
 *     // Check modifiers
 *     if (element.isPublic() && !element.isAbstract()) {
 *         // Get annotations
 *         List<AnnotationModel> annotations = element.annotations();
 *
 *         // Get type
 *         Optional<TypeRef> type = element.type();
 *     }
 * }
 * }</pre>
 *
 * @see SourceModelFactory
 * @see AnnotationModel
 * @see TypeRef
 */
public final class ElementModel {

    private final String simpleName;
    private final String qualifiedName;
    private final ElementKind kind;
    private final Set<Modifier> modifiers;
    private final List<AnnotationModel> annotations;
    private final TypeRef type;
    private final Element element;

    /**
     * Constructs an element model.
     *
     * @param simpleName    simple name (not {@code null})
     * @param qualifiedName qualified name (may be {@code null} for some element kinds)
     * @param kind          element kind (not {@code null})
     * @param modifiers     modifiers (not {@code null})
     * @param annotations   annotations (not {@code null})
     * @param type          element type (may be {@code null})
     * @param element       underlying element (not {@code null})
     */
    public ElementModel(
            String simpleName,
            String qualifiedName,
            ElementKind kind,
            Set<Modifier> modifiers,
            List<AnnotationModel> annotations,
            TypeRef type,
            Element element) {
        this.simpleName = Objects.requireNonNull(simpleName, "simpleName");
        this.qualifiedName = qualifiedName;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        this.annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations"));
        this.type = type;
        this.element = Objects.requireNonNull(element, "element");
    }

    /**
     * Returns the simple name of this element.
     *
     * @return simple name (never {@code null})
     */
    public String simpleName() {
        return simpleName;
    }

    /**
     * Returns the qualified name of this element if available.
     *
     * <p>
     * The qualified name is typically available for:
     * </p>
     * <ul>
     *   <li>Top-level types</li>
     *   <li>Packages</li>
     * </ul>
     * <p>
     * For other elements (methods, fields, parameters, local classes), the qualified
     * name may not be available.
     * </p>
     *
     * @return qualified name if available
     */
    public Optional<String> qualifiedName() {
        return Optional.ofNullable(qualifiedName);
    }

    /**
     * Returns the element kind.
     *
     * @return element kind (never {@code null})
     */
    public ElementKind kind() {
        return kind;
    }

    /**
     * Returns the modifiers of this element.
     *
     * @return modifiers (never {@code null}, immutable)
     */
    public Set<Modifier> modifiers() {
        return modifiers;
    }

    /**
     * Returns whether this element has the specified modifier.
     *
     * @param modifier modifier to check (not {@code null})
     * @return {@code true} if modifier is present
     */
    public boolean hasModifier(Modifier modifier) {
        Objects.requireNonNull(modifier, "modifier");
        return modifiers.contains(modifier);
    }

    /**
     * Returns whether this element is public.
     *
     * @return {@code true} if public
     */
    public boolean isPublic() {
        return hasModifier(Modifier.PUBLIC);
    }

    /**
     * Returns whether this element is private.
     *
     * @return {@code true} if private
     */
    public boolean isPrivate() {
        return hasModifier(Modifier.PRIVATE);
    }

    /**
     * Returns whether this element is protected.
     *
     * @return {@code true} if protected
     */
    public boolean isProtected() {
        return hasModifier(Modifier.PROTECTED);
    }

    /**
     * Returns whether this element is static.
     *
     * @return {@code true} if static
     */
    public boolean isStatic() {
        return hasModifier(Modifier.STATIC);
    }

    /**
     * Returns whether this element is final.
     *
     * @return {@code true} if final
     */
    public boolean isFinal() {
        return hasModifier(Modifier.FINAL);
    }

    /**
     * Returns whether this element is abstract.
     *
     * @return {@code true} if abstract
     */
    public boolean isAbstract() {
        return hasModifier(Modifier.ABSTRACT);
    }

    /**
     * Returns all annotations present on this element.
     *
     * @return annotations (never {@code null}, immutable)
     */
    public List<AnnotationModel> annotations() {
        return annotations;
    }

    /**
     * Returns whether this element is annotated with the specified annotation.
     *
     * @param qualifiedName fully qualified annotation type name (not {@code null})
     * @return {@code true} if annotation is present
     */
    public boolean hasAnnotation(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return annotations.stream().anyMatch(ann -> ann.isType(qualifiedName));
    }

    /**
     * Finds an annotation by qualified name.
     *
     * @param qualifiedName fully qualified annotation type name (not {@code null})
     * @return annotation if present
     */
    public Optional<AnnotationModel> findAnnotation(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return annotations.stream().filter(ann -> ann.isType(qualifiedName)).findFirst();
    }

    /**
     * Returns the type of this element if available.
     *
     * <p>
     * The type is typically available for:
     * </p>
     * <ul>
     *   <li>Fields</li>
     *   <li>Method return types</li>
     *   <li>Parameters</li>
     *   <li>Type elements (as a self-reference)</li>
     * </ul>
     *
     * @return element type if available
     */
    public Optional<TypeRef> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Returns the underlying JSR-269 element.
     *
     * <p>
     * This provides access to the full annotation processing API when needed.
     * </p>
     *
     * @return element (never {@code null})
     */
    public Element element() {
        return element;
    }

    /**
     * Returns whether this is a type element (class, interface, enum, etc.).
     *
     * @return {@code true} if type element
     */
    public boolean isType() {
        return kind.isClass() || kind.isInterface();
    }

    /**
     * Returns whether this is a method element.
     *
     * @return {@code true} if method
     */
    public boolean isMethod() {
        return kind == ElementKind.METHOD;
    }

    /**
     * Returns whether this is a field element.
     *
     * @return {@code true} if field
     */
    public boolean isField() {
        return kind == ElementKind.FIELD;
    }

    /**
     * Returns whether this is a parameter element.
     *
     * @return {@code true} if parameter
     */
    public boolean isParameter() {
        return kind == ElementKind.PARAMETER;
    }

    /**
     * Returns whether this is a constructor element.
     *
     * @return {@code true} if constructor
     */
    public boolean isConstructor() {
        return kind == ElementKind.CONSTRUCTOR;
    }

    /**
     * Returns whether this is a package element.
     *
     * @return {@code true} if package
     */
    public boolean isPackage() {
        return kind == ElementKind.PACKAGE;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ElementModel other)) return false;
        return element.equals(other.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public String toString() {
        return kind + " " + (qualifiedName != null ? qualifiedName : simpleName);
    }
}
