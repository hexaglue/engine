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
package io.hexaglue.core.types;

import io.hexaglue.spi.types.ArrayRef;
import io.hexaglue.spi.types.ClassRef;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.ParameterizedRef;
import io.hexaglue.spi.types.PrimitiveRef;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.WildcardRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Resolves {@link TypeRef} instances from JSR-269 type mirrors.
 *
 * <p>
 * This class acts as a bridge between the JSR-269 type system and HexaGlue's
 * stable {@link TypeRef} abstraction.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are thread-safe as long as the underlying {@link Elements} and {@link Types}
 * utilities are used in a single annotation processing round.
 * </p>
 */
public final class TypeResolver {

    private final Elements elements;

    private TypeResolver(Elements elements, Types types) {
        this.elements = Objects.requireNonNull(elements, "elements");
    }

    /**
     * Creates a type resolver.
     *
     * @param elements element utilities (not {@code null})
     * @param types    type utilities (not {@code null})
     * @return type resolver (never {@code null})
     */
    public static TypeResolver create(Elements elements, Types types) {
        return new TypeResolver(elements, types);
    }

    /**
     * Resolves a type reference from a type mirror.
     *
     * @param typeMirror type mirror (not {@code null})
     * @return type reference (never {@code null})
     */
    public TypeRef resolve(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");

        Nullability nullability = NullabilityResolver.fromTypeMirror(typeMirror);

        return switch (typeMirror.getKind()) {
            case VOID -> TypeRefFactory.VOID.withNullability(nullability);
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE ->
                resolvePrimitive((PrimitiveType) typeMirror, nullability);
            case DECLARED -> resolveDeclared((DeclaredType) typeMirror, nullability);
            case ARRAY -> resolveArray((ArrayType) typeMirror, nullability);
            case TYPEVAR -> resolveTypeVariable((TypeVariable) typeMirror, nullability);
            case WILDCARD -> resolveWildcard((WildcardType) typeMirror, nullability);
            default -> TypeRefFactory.OBJECT.withNullability(nullability);
        };
    }

    /**
     * Resolves a type reference from an element.
     *
     * @param element element (not {@code null})
     * @return type reference (never {@code null})
     */
    public TypeRef resolveFromElement(Element element) {
        Objects.requireNonNull(element, "element");
        return resolve(element.asType());
    }

    /**
     * Resolves a class reference from a qualified name.
     *
     * @param qualifiedName qualified name (not blank)
     * @return class reference (never {@code null})
     */
    public ClassRef resolveClass(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        TypeElement typeElement = elements.getTypeElement(qualifiedName);
        if (typeElement == null) {
            // Fallback: create unresolved reference
            return ClassRef.of(qualifiedName);
        }

        return (ClassRef) resolve(typeElement.asType());
    }

    /**
     * Returns the erasure of a type reference.
     *
     * @param typeRef type reference (not {@code null})
     * @return erased type (never {@code null})
     */
    public TypeRef erasure(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        return switch (typeRef.kind()) {
            case PRIMITIVE, CLASS -> typeRef;
            case ARRAY -> {
                ArrayRef arrayRef = (ArrayRef) typeRef;
                TypeRef erasedComponent = erasure(arrayRef.componentType());
                yield TypeRefFactory.arrayOf(erasedComponent, arrayRef.nullability());
            }
            case PARAMETERIZED -> {
                ParameterizedRef paramRef = (ParameterizedRef) typeRef;
                yield paramRef.rawType().withNullability(paramRef.nullability());
            }
            case WILDCARD -> TypeRefFactory.OBJECT.withNullability(typeRef.nullability());
            case TYPE_VARIABLE -> TypeRefFactory.OBJECT.withNullability(typeRef.nullability());
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal resolution methods
    // ─────────────────────────────────────────────────────────────────────────

    private PrimitiveRef resolvePrimitive(PrimitiveType primitiveType, Nullability nullability) {
        String keyword = primitiveType.getKind().name().toLowerCase();
        return TypeRefFactory.primitive(keyword).withNullability(nullability);
    }

    private TypeRef resolveDeclared(DeclaredType declaredType, Nullability nullability) {
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return TypeRefFactory.OBJECT.withNullability(nullability);
        }

        TypeElement typeElement = (TypeElement) element;
        String qualifiedName = typeElement.getQualifiedName().toString();
        ClassRef classRef = ClassRef.of(qualifiedName).withNullability(nullability);

        // Check if parameterized
        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
        if (typeArguments.isEmpty()) {
            return classRef;
        }

        // Resolve type arguments
        List<TypeRef> resolvedArgs = new ArrayList<>();
        for (TypeMirror arg : typeArguments) {
            resolvedArgs.add(resolve(arg));
        }

        return TypeRefFactory.parameterized(classRef, resolvedArgs).withNullability(nullability);
    }

    private ArrayRef resolveArray(ArrayType arrayType, Nullability nullability) {
        TypeRef component = resolve(arrayType.getComponentType());
        return TypeRefFactory.arrayOf(component, nullability);
    }

    private io.hexaglue.spi.types.TypeVariableRef resolveTypeVariable(
            TypeVariable typeVariable, Nullability nullability) {
        String name = typeVariable.toString();

        // Resolve bounds
        TypeMirror upperBound = typeVariable.getUpperBound();
        List<TypeRef> bounds = new ArrayList<>();
        if (upperBound != null && !isJavaLangObject(upperBound)) {
            bounds.add(resolve(upperBound));
        }

        return TypeRefFactory.typeVariable(name, bounds);
    }

    private WildcardRef resolveWildcard(WildcardType wildcardType, Nullability nullability) {
        TypeMirror extendsBound = wildcardType.getExtendsBound();
        TypeMirror superBound = wildcardType.getSuperBound();

        if (extendsBound != null) {
            TypeRef bound = resolve(extendsBound);
            return TypeRefFactory.wildcardExtends(bound).withNullability(nullability);
        }

        if (superBound != null) {
            TypeRef bound = resolve(superBound);
            return TypeRefFactory.wildcardSuper(bound).withNullability(nullability);
        }

        return TypeRefFactory.wildcard().withNullability(nullability);
    }

    private boolean isJavaLangObject(TypeMirror typeMirror) {
        if (typeMirror.getKind() != TypeKind.DECLARED) {
            return false;
        }
        DeclaredType declaredType = (DeclaredType) typeMirror;
        Element element = declaredType.asElement();
        if (!(element instanceof TypeElement)) {
            return false;
        }
        TypeElement typeElement = (TypeElement) element;
        return "java.lang.Object".equals(typeElement.getQualifiedName().toString());
    }
}
