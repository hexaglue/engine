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

import io.hexaglue.spi.types.ClassRef;
import io.hexaglue.spi.types.PrimitiveRef;
import io.hexaglue.spi.types.TypeRef;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Default implementation of {@link TypeSystemSpec}.
 *
 * <p>
 * This implementation bridges JSR-269 type utilities ({@link Types} and {@link Elements})
 * with HexaGlue's stable type abstraction.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Type resolution from qualified names</li>
 *   <li>Assignability checking</li>
 *   <li>Boxing and unboxing</li>
 *   <li>Type erasure</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are thread-safe as long as the underlying JSR-269 utilities are used
 * within a single annotation processing round.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * DefaultTypeSystem typeSystem = DefaultTypeSystem.create(
 *     processingEnv.getElementUtils(),
 *     processingEnv.getTypeUtils()
 * );
 *
 * ClassRef stringType = typeSystem.stringType();
 * boolean assignable = typeSystem.isAssignable(stringType, typeSystem.objectType());
 * }</pre>
 */
public final class DefaultTypeSystem implements TypeSystemSpec {

    private final TypeResolver resolver;

    private DefaultTypeSystem(TypeResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    /**
     * Creates a default type system.
     *
     * @param elements element utilities (not {@code null})
     * @param types    type utilities (not {@code null})
     * @return type system (never {@code null})
     */
    public static DefaultTypeSystem create(Elements elements, Types types) {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(types, "types");
        return new DefaultTypeSystem(TypeResolver.create(elements, types));
    }

    @Override
    public ClassRef objectType() {
        return TypeRefFactory.OBJECT;
    }

    @Override
    public ClassRef stringType() {
        return TypeRefFactory.STRING;
    }

    @Override
    public ClassRef classRef(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return TypeRefFactory.classRef(qualifiedName);
    }

    @Override
    public Optional<ClassRef> tryResolveClass(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        try {
            ClassRef ref = resolver.resolveClass(qualifiedName);
            return Optional.of(ref);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public PrimitiveRef primitive(String keyword) {
        Objects.requireNonNull(keyword, "keyword");
        return TypeRefFactory.primitive(keyword);
    }

    @Override
    public TypeRef erasure(TypeRef type) {
        Objects.requireNonNull(type, "type");
        return resolver.erasure(type);
    }

    @Override
    public boolean isAssignable(TypeRef from, TypeRef to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");

        // Exact match (ignoring nullability)
        if (TypeComparators.equalIgnoringNullability(from, to)) {
            return true;
        }

        // Primitives
        if (TypeComparators.isPrimitive(from) && TypeComparators.isPrimitive(to)) {
            return isPrimitiveAssignable((PrimitiveRef) from, (PrimitiveRef) to);
        }

        // Boxing/unboxing
        if (TypeComparators.isPrimitive(from) && to instanceof ClassRef) {
            TypeRef boxed = box(from);
            return TypeComparators.equalIgnoringNullability(boxed, to);
        }
        if (from instanceof ClassRef && TypeComparators.isPrimitive(to)) {
            TypeRef unboxed = unbox(from);
            return TypeComparators.equalIgnoringNullability(unboxed, to);
        }

        // Object is assignable from any reference type
        if (TypeComparators.isObject(to)) {
            return !TypeComparators.isPrimitive(from);
        }

        // Conservative: assume not assignable unless explicitly handled
        return false;
    }

    @Override
    public TypeRef box(TypeRef type) {
        Objects.requireNonNull(type, "type");

        if (type instanceof PrimitiveRef) {
            PrimitiveRef primitive = (PrimitiveRef) type;
            return TypeRefFactory.box(primitive).withNullability(type.nullability());
        }

        return type;
    }

    @Override
    public TypeRef unbox(TypeRef type) {
        Objects.requireNonNull(type, "type");

        if (type instanceof ClassRef) {
            ClassRef classRef = (ClassRef) type;
            PrimitiveRef primitive = TypeRefFactory.unbox(classRef);
            if (primitive != null) {
                return primitive.withNullability(type.nullability());
            }
        }

        return type;
    }

    /**
     * Returns the type resolver backing this type system.
     *
     * <p>
     * This accessor is core-internal and allows direct type resolution when needed.
     * </p>
     *
     * @return type resolver (never {@code null})
     */
    public TypeResolver resolver() {
        return resolver;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isPrimitiveAssignable(PrimitiveRef from, PrimitiveRef to) {
        String fromKeyword = from.name().value();
        String toKeyword = to.name().value();

        // Same type
        if (fromKeyword.equals(toKeyword)) {
            return true;
        }

        // void is not assignable to anything
        if ("void".equals(fromKeyword) || "void".equals(toKeyword)) {
            return false;
        }

        // boolean is not assignable to numeric types
        if ("boolean".equals(fromKeyword) || "boolean".equals(toKeyword)) {
            return false;
        }

        // Numeric widening conversions
        return isNumericWidening(fromKeyword, toKeyword);
    }

    private boolean isNumericWidening(String from, String to) {
        // Java widening primitive conversions
        return switch (from) {
            case "byte" ->
                "short".equals(to)
                        || "int".equals(to)
                        || "long".equals(to)
                        || "float".equals(to)
                        || "double".equals(to);
            case "short", "char" -> "int".equals(to) || "long".equals(to) || "float".equals(to) || "double".equals(to);
            case "int" -> "long".equals(to) || "float".equals(to) || "double".equals(to);
            case "long" -> "float".equals(to) || "double".equals(to);
            case "float" -> "double".equals(to);
            default -> false;
        };
    }
}
