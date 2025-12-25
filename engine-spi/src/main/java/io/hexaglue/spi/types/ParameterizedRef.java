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

import io.hexaglue.spi.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Parameterized type reference (e.g., {@code List<String>}).
 *
 * @param rawType raw class type (must be {@link ClassRef})
 * @param typeArguments type arguments (non-empty)
 * @param nullability nullability marker for the parameterized reference
 */
public record ParameterizedRef(ClassRef rawType, List<TypeRef> typeArguments, Nullability nullability)
        implements TypeRef {

    public ParameterizedRef {
        Objects.requireNonNull(rawType, "rawType");
        Objects.requireNonNull(typeArguments, "typeArguments");
        Objects.requireNonNull(nullability, "nullability");
        if (typeArguments.isEmpty()) {
            throw new IllegalArgumentException("typeArguments must not be empty");
        }
        typeArguments = List.copyOf(typeArguments);
        for (TypeRef arg : typeArguments) {
            Objects.requireNonNull(arg, "typeArguments contains null");
        }
    }

    /**
     * Creates a parameterized reference.
     *
     * @param rawType raw type
     * @param typeArguments type arguments
     * @return parameterized ref with {@link Nullability#UNSPECIFIED}
     */
    public static ParameterizedRef of(ClassRef rawType, List<TypeRef> typeArguments) {
        return new ParameterizedRef(rawType, typeArguments, Nullability.UNSPECIFIED);
    }

    @Override
    public TypeKind kind() {
        return TypeKind.PARAMETERIZED;
    }

    @Override
    public TypeName name() {
        return rawType.name();
    }

    @Override
    public ParameterizedRef withNullability(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return new ParameterizedRef(rawType, typeArguments, nullability);
    }

    @Override
    public Optional<CollectionMetadata> collectionMetadata() {
        String qualifiedName = rawType.name().value();
        Optional<CollectionKind> kind = Collections.detectKind(qualifiedName);

        if (kind.isEmpty()) {
            return Optional.empty();
        }

        // Determine element and key types based on collection kind
        CollectionKind collectionKind = kind.get();

        if (collectionKind == CollectionKind.MAP) {
            // Map<K, V> - needs both key and value types
            if (typeArguments.size() >= 2) {
                TypeRef keyType = typeArguments.get(0);
                TypeRef valueType = typeArguments.get(1);
                return Optional.of(SimpleCollectionMetadata.ofMap(keyType, valueType));
            }
            // Raw Map without type arguments - cannot extract metadata
            return Optional.empty();
        } else {
            // List<E>, Set<E>, Collection<E> - single element type
            if (typeArguments.size() >= 1) {
                TypeRef elementType = typeArguments.get(0);
                return Optional.of(SimpleCollectionMetadata.of(collectionKind, elementType));
            }
            // Raw collection without type arguments - cannot extract metadata
            return Optional.empty();
        }
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
}
