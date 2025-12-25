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

import java.util.Objects;
import java.util.Optional;

/**
 * Simple immutable implementation of {@link CollectionMetadata}.
 *
 * <p>This class provides a straightforward implementation of collection metadata
 * suitable for use in plugins and SPI consumers.</p>
 *
 * @since 0.4.0
 */
public final class SimpleCollectionMetadata implements CollectionMetadata {

    private final CollectionKind kind;
    private final TypeRef elementType;
    private final TypeRef internalKeyType;

    /**
     * Private constructor.
     *
     * @param kind           collection kind (not {@code null})
     * @param elementType    element/value type (not {@code null})
     * @param internalKeyType key type for maps (may be {@code null})
     */
    private SimpleCollectionMetadata(CollectionKind kind, TypeRef elementType, TypeRef internalKeyType) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        this.internalKeyType = internalKeyType;

        if (kind == CollectionKind.MAP && internalKeyType == null) {
            throw new IllegalArgumentException("keyType must be provided for MAP collections");
        }
    }

    @Override
    public CollectionKind kind() {
        return kind;
    }

    @Override
    public TypeRef elementType() {
        return elementType;
    }

    /**
     * Creates collection metadata for List, Set, or generic Collection types.
     *
     * @param kind        collection kind (LIST, SET, or COLLECTION, not MAP)
     * @param elementType element type (not {@code null})
     * @return collection metadata
     * @throws IllegalArgumentException if kind is MAP
     */
    public static SimpleCollectionMetadata of(CollectionKind kind, TypeRef elementType) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(elementType, "elementType");

        if (kind == CollectionKind.MAP) {
            throw new IllegalArgumentException("Use ofMap(keyType, valueType) for MAP collections");
        }

        return new SimpleCollectionMetadata(kind, elementType, null);
    }

    /**
     * Creates collection metadata for Map types.
     *
     * @param keyType   key type (not {@code null})
     * @param valueType value type (not {@code null})
     * @return collection metadata with kind=MAP
     */
    public static SimpleCollectionMetadata ofMap(TypeRef keyType, TypeRef valueType) {
        Objects.requireNonNull(keyType, "keyType");
        Objects.requireNonNull(valueType, "valueType");
        return new SimpleCollectionMetadata(CollectionKind.MAP, valueType, keyType);
    }

    @Override
    public Optional<TypeRef> keyType() {
        return Optional.ofNullable(internalKeyType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SimpleCollectionMetadata other)) return false;
        return kind == other.kind
                && elementType.equals(other.elementType)
                && Objects.equals(internalKeyType, other.internalKeyType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, elementType, internalKeyType);
    }

    @Override
    public String toString() {
        if (kind == CollectionKind.MAP) {
            return String.format(
                    "CollectionMetadata{kind=%s, keyType=%s, valueType=%s}", kind, internalKeyType, elementType);
        }
        return String.format("CollectionMetadata{kind=%s, elementType=%s}", kind, elementType);
    }
}
