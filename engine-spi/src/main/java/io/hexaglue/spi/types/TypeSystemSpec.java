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
package io.hexaglue.spi.types;

import io.hexaglue.spi.stability.Evolvable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Type system access exposed to plugins.
 *
 * <p>This SPI abstracts the underlying compiler model and provides stable operations
 * needed for generation, validation and mapping.</p>
 *
 * <p>Implementations are provided by HexaGlue core. Plugins should not assume any
 * particular implementation strategy.</p>
 *
 * <h2>Stability</h2>
 * <p>This interface should evolve by adding new methods with default implementations.
 * Avoid changing existing semantics.</p>
 */
@Evolvable(since = "1.0.0")
public interface TypeSystemSpec {

    /**
     * Returns a reference to {@code java.lang.Object}.
     *
     * @return object type (never {@code null})
     */
    ClassRef objectType();

    /**
     * Returns a reference to {@code java.lang.String}.
     *
     * @return string type (never {@code null})
     */
    ClassRef stringType();

    /**
     * Creates a class reference from a qualified name.
     *
     * @param qualifiedName qualified name (non-blank)
     * @return class reference (never {@code null})
     */
    ClassRef classRef(String qualifiedName);

    /**
     * Resolves a class reference if it exists/ is visible to the compilation.
     *
     * <p>Visibility rules are implementation-defined (classpath/modulepath).</p>
     *
     * @param qualifiedName qualified name (non-blank)
     * @return resolved class reference if available
     */
    Optional<ClassRef> tryResolveClass(String qualifiedName);

    /**
     * Creates a primitive reference by keyword.
     *
     * @param keyword primitive keyword (e.g., {@code "int"}, {@code "boolean"}, {@code "void"})
     * @return primitive reference (never {@code null})
     */
    PrimitiveRef primitive(String keyword);

    /**
     * Creates an array reference.
     *
     * @param component component type
     * @return array reference
     */
    default ArrayRef arrayOf(TypeRef component) {
        Objects.requireNonNull(component, "component");
        return ArrayRef.of(component);
    }

    /**
     * Creates a parameterized type reference.
     *
     * @param raw raw class type
     * @param args type arguments
     * @return parameterized reference
     */
    default ParameterizedRef parameterized(ClassRef raw, List<TypeRef> args) {
        Objects.requireNonNull(raw, "raw");
        Objects.requireNonNull(args, "args");
        return ParameterizedRef.of(raw, args);
    }

    /**
     * Creates an unbounded wildcard {@code ?}.
     *
     * @return wildcard
     */
    default WildcardRef wildcard() {
        return WildcardRef.unbounded();
    }

    /**
     * Creates a wildcard {@code ? extends <upper>}.
     *
     * @param upper upper bound
     * @return wildcard
     */
    default WildcardRef wildcardExtends(TypeRef upper) {
        return WildcardRef.extendsBound(Objects.requireNonNull(upper, "upper"));
    }

    /**
     * Creates a wildcard {@code ? super <lower>}.
     *
     * @param lower lower bound
     * @return wildcard
     */
    default WildcardRef wildcardSuper(TypeRef lower) {
        return WildcardRef.superBound(Objects.requireNonNull(lower, "lower"));
    }

    /**
     * Creates a type variable reference.
     *
     * @param name variable name
     * @param bounds bounds (may be empty)
     * @return type variable ref
     */
    default TypeVariableRef typeVariable(String name, List<TypeRef> bounds) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(bounds, "bounds");
        return new TypeVariableRef(TypeName.of(name.trim()), bounds, Nullability.UNSPECIFIED);
    }

    /**
     * Returns the erasure of a type (best-effort).
     *
     * <p>For parameterized types, this typically returns the raw type.
     * For others, it may return the input unchanged.</p>
     *
     * @param type type reference
     * @return erasure (never {@code null})
     */
    TypeRef erasure(TypeRef type);

    /**
     * Returns whether {@code from} is assignable to {@code to} (best-effort).
     *
     * <p>This operation is implementation-defined but should follow Java assignability rules
     * as closely as possible for the types supported by the compilation.</p>
     *
     * @param from source type
     * @param to target type
     * @return {@code true} if assignable
     */
    boolean isAssignable(TypeRef from, TypeRef to);

    /**
     * Boxes a primitive type into its wrapper if applicable.
     *
     * <p>If {@code type} is not primitive (or is {@code void}), the input may be returned unchanged.</p>
     *
     * @param type input type
     * @return boxed type (never {@code null})
     */
    TypeRef box(TypeRef type);

    /**
     * Unboxes a wrapper type into its primitive if applicable.
     *
     * <p>If {@code type} is not a known wrapper, the input may be returned unchanged.</p>
     *
     * @param type input type
     * @return unboxed type (never {@code null})
     */
    TypeRef unbox(TypeRef type);
}
