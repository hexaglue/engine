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

import io.hexaglue.core.types.CollectionTypeDetector;
import io.hexaglue.spi.types.CollectionKind;
import io.hexaglue.spi.types.CollectionMetadata;
import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

/**
 * Internal representation of a parameterized (generic) type reference.
 *
 * <p>
 * Represents types such as:
 * <ul>
 *   <li>{@code List<String>}</li>
 *   <li>{@code Map<String, Integer>}</li>
 *   <li>{@code Optional<Customer>}</li>
 *   <li>{@code Supplier<List<String>>}</li>
 * </ul>
 * </p>
 *
 * <h2>Structure</h2>
 * <p>
 * A parameterized type consists of:
 * <ul>
 *   <li>A raw type (the base class or interface, e.g., {@code List})</li>
 *   <li>One or more type arguments (e.g., {@code String}, {@code Integer})</li>
 * </ul>
 * </p>
 *
 * <h2>Type Arguments</h2>
 * <p>
 * Type arguments can be:
 * <ul>
 *   <li>Concrete types ({@link ClassTypeRef}, {@link PrimitiveTypeRef})</li>
 *   <li>Wildcards ({@link WildcardTypeRef})</li>
 *   <li>Type variables ({@link TypeVariableTypeRef})</li>
 *   <li>Other parameterized types (nested generics)</li>
 * </ul>
 * </p>
 *
 * <h2>Validation</h2>
 * <p>
 * The constructor validates that:
 * <ul>
 *   <li>The raw type is a {@link ClassTypeRef}</li>
 *   <li>Type arguments list is non-empty</li>
 *   <li>All type arguments are non-null</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe. The type arguments list is defensively copied.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ClassTypeRef listType = ClassTypeRef.of("java.util.List");
 * ClassTypeRef stringType = ClassTypeRef.of("java.lang.String");
 * ParameterizedTypeRef listOfStrings = ParameterizedTypeRef.of(listType, List.of(stringType));
 * }</pre>
 */
public final class ParameterizedTypeRef extends BaseTypeRef {

    private final ClassTypeRef rawType;
    private final List<BaseTypeRef> typeArguments;

    /**
     * Constructs a parameterized type reference.
     *
     * @param rawType       raw class type (not {@code null})
     * @param typeArguments type arguments (non-empty, all elements non-null)
     * @param nullability   nullability marker (not {@code null})
     * @param typeMirror    optional underlying type mirror (may be {@code null})
     * @throws IllegalArgumentException if typeArguments is empty
     */
    public ParameterizedTypeRef(
            ClassTypeRef rawType, List<BaseTypeRef> typeArguments, Nullability nullability, TypeMirror typeMirror) {
        super(rawType.name(), nullability, typeMirror);
        this.rawType = Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(typeArguments, "typeArguments");

        if (typeArguments.isEmpty()) {
            throw new IllegalArgumentException("typeArguments must not be empty");
        }

        this.typeArguments = List.copyOf(typeArguments);
        for (BaseTypeRef arg : this.typeArguments) {
            Objects.requireNonNull(arg, "typeArguments contains null");
        }
    }

    /**
     * Creates a parameterized type reference.
     *
     * @param rawType       raw class type (not {@code null})
     * @param typeArguments type arguments (non-empty)
     * @return parameterized type reference with {@link Nullability#UNSPECIFIED}
     */
    public static ParameterizedTypeRef of(ClassTypeRef rawType, List<BaseTypeRef> typeArguments) {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(typeArguments, "typeArguments");
        return new ParameterizedTypeRef(rawType, typeArguments, Nullability.UNSPECIFIED, null);
    }

    /**
     * Creates a parameterized type reference with a type mirror.
     *
     * @param rawType       raw class type (not {@code null})
     * @param typeArguments type arguments (non-empty)
     * @param typeMirror    underlying type mirror (may be {@code null})
     * @return parameterized type reference with {@link Nullability#UNSPECIFIED}
     */
    public static ParameterizedTypeRef of(
            ClassTypeRef rawType, List<BaseTypeRef> typeArguments, TypeMirror typeMirror) {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(typeArguments, "typeArguments");
        return new ParameterizedTypeRef(rawType, typeArguments, Nullability.UNSPECIFIED, typeMirror);
    }

    /**
     * Creates a parameterized type reference with nullability.
     *
     * @param rawType       raw class type (not {@code null})
     * @param typeArguments type arguments (non-empty)
     * @param nullability   nullability marker (not {@code null})
     * @param typeMirror    underlying type mirror (may be {@code null})
     * @return parameterized type reference
     */
    public static ParameterizedTypeRef of(
            ClassTypeRef rawType, List<BaseTypeRef> typeArguments, Nullability nullability, TypeMirror typeMirror) {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(typeArguments, "typeArguments");
        Objects.requireNonNull(nullability, "nullability");
        return new ParameterizedTypeRef(rawType, typeArguments, nullability, typeMirror);
    }

    /**
     * Returns the raw type (the base class or interface without type parameters).
     *
     * @return raw type (never {@code null})
     */
    public ClassTypeRef rawType() {
        return rawType;
    }

    /**
     * Returns the type arguments.
     *
     * @return type arguments (never empty, immutable)
     */
    public List<BaseTypeRef> typeArguments() {
        return typeArguments;
    }

    @Override
    public TypeKind kind() {
        return TypeKind.PARAMETERIZED;
    }

    @Override
    public ParameterizedTypeRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        if (this.nullability().equals(nullability)) {
            return this;
        }
        return new ParameterizedTypeRef(
                rawType, typeArguments, nullability, typeMirror().orElse(null));
    }

    @Override
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(rawType.render()).append("<");
        for (int i = 0; i < typeArguments.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(typeArguments.get(i).render());
        }
        sb.append(">");
        return sb.toString();
    }

    @Override
    public Optional<CollectionMetadata> collectionMetadata() {
        String qualifiedName = rawType.name().value();
        Optional<CollectionKind> kind = CollectionTypeDetector.detectCollectionKind(qualifiedName);

        if (kind.isEmpty()) {
            return Optional.empty();
        }

        // Determine element and key types based on collection kind
        CollectionKind collectionKind = kind.get();

        if (collectionKind == CollectionKind.MAP) {
            // Map<K, V> - needs both key and value types
            if (typeArguments.size() >= 2) {
                TypeRef keyType = typeArguments.get(0).toSpiType();
                TypeRef valueType = typeArguments.get(1).toSpiType();
                return Optional.of(CollectionMetadataImpl.ofMap(keyType, valueType));
            }
            // Raw Map without type arguments - cannot extract metadata
            return Optional.empty();
        } else {
            // List<E>, Set<E>, Collection<E> - single element type
            if (typeArguments.size() >= 1) {
                TypeRef elementType = typeArguments.get(0).toSpiType();
                return Optional.of(CollectionMetadataImpl.of(collectionKind, elementType));
            }
            // Raw collection without type arguments - cannot extract metadata
            return Optional.empty();
        }
    }

    @Override
    public TypeRef toSpiType() {
        io.hexaglue.spi.types.ClassRef spiRawType = (io.hexaglue.spi.types.ClassRef) rawType.toSpiType();

        List<TypeRef> spiTypeArgs =
                typeArguments.stream().map(BaseTypeRef::toSpiType).toList();

        return io.hexaglue.spi.types.ParameterizedRef.of(spiRawType, spiTypeArgs)
                .withNullability(nullability());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ParameterizedTypeRef other)) return false;
        return rawType.equals(other.rawType)
                && typeArguments.equals(other.typeArguments)
                && nullability().equals(other.nullability());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawType, typeArguments, nullability());
    }
}
