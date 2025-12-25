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
package io.hexaglue.core.types.model;

import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeName;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

/**
 * Internal representation of a non-parameterized class or interface type reference.
 *
 * <p>
 * Represents types such as:
 * <ul>
 *   <li>{@code java.lang.String}</li>
 *   <li>{@code java.util.List} (raw, without type parameters)</li>
 *   <li>{@code com.example.Customer}</li>
 * </ul>
 * </p>
 *
 * <h2>Qualified vs Simple Names</h2>
 * <p>
 * The type name should typically be fully qualified (e.g., {@code "java.util.List"}).
 * However, the SPI allows simple names in contexts where qualification is not available.
 * Use {@link #qualifiedName()} to check if the name is qualified.
 * </p>
 *
 * <h2>Raw Types</h2>
 * <p>
 * This class represents raw class types. For parameterized types (e.g., {@code List<String>}),
 * use {@link ParameterizedTypeRef}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
 * ClassTypeRef customerType = ClassTypeRef.of("com.example.Customer", typeMirror);
 * }</pre>
 */
public final class ClassTypeRef extends BaseTypeRef {

    /**
     * Constructs a class type reference.
     *
     * @param name        type name (not {@code null}, typically qualified)
     * @param nullability nullability marker (not {@code null})
     * @param typeMirror  optional underlying type mirror (may be {@code null})
     */
    public ClassTypeRef(TypeName name, Nullability nullability, TypeMirror typeMirror) {
        super(name, nullability, typeMirror);
    }

    /**
     * Creates a class type reference from a name.
     *
     * @param qualifiedOrSimpleName class name (not blank)
     * @return class type reference with {@link Nullability#UNSPECIFIED}
     */
    public static ClassTypeRef of(String qualifiedOrSimpleName) {
        Objects.requireNonNull(qualifiedOrSimpleName, "qualifiedOrSimpleName");
        return new ClassTypeRef(TypeName.of(qualifiedOrSimpleName.trim()), Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a class type reference with a type mirror.
     *
     * @param qualifiedOrSimpleName class name (not blank)
     * @param typeMirror            underlying type mirror (may be {@code null})
     * @return class type reference with {@link Nullability#UNSPECIFIED}
     */
    public static ClassTypeRef of(String qualifiedOrSimpleName, TypeMirror typeMirror) {
        Objects.requireNonNull(qualifiedOrSimpleName, "qualifiedOrSimpleName");
        return new ClassTypeRef(TypeName.of(qualifiedOrSimpleName.trim()), Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Creates a class type reference with nullability.
     *
     * @param qualifiedOrSimpleName class name (not blank)
     * @param nullability           nullability marker (not {@code null})
     * @param typeMirror            underlying type mirror (may be {@code null})
     * @return class type reference
     */
    public static ClassTypeRef of(String qualifiedOrSimpleName, Nullability nullability, TypeMirror typeMirror) {
        Objects.requireNonNull(qualifiedOrSimpleName, "qualifiedOrSimpleName");
        Objects.requireNonNull(nullability, "nullability");
        return new ClassTypeRef(TypeName.of(qualifiedOrSimpleName.trim()), nullability, typeMirror);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.CLASS;
    }

    @Override
    public ClassTypeRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        if (this.nullability().equals(nullability)) {
            return this;
        }
        return new ClassTypeRef(name(), nullability, typeMirror().orElse(null));
    }

    /**
     * Returns the qualified name if this name is qualified.
     *
     * @return qualified name if available
     */
    public Optional<String> qualifiedName() {
        return name().isQualified() ? Optional.of(name().value()) : Optional.empty();
    }

    /**
     * Returns the simple name (without package).
     *
     * @return simple name (never blank)
     */
    public String simpleName() {
        return name().simpleName();
    }

    /**
     * Returns the package name if this is a qualified name.
     *
     * @return package name if present
     */
    public Optional<String> packageName() {
        return name().packageName();
    }

    @Override
    public TypeRef toSpiType() {
        return io.hexaglue.spi.types.ClassRef.of(name().value()).withNullability(nullability());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ClassTypeRef other)) return false;
        return name().equals(other.name()) && nullability().equals(other.nullability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), nullability());
    }
}
