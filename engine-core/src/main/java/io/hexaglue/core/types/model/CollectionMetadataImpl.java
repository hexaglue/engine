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

import io.hexaglue.spi.types.CollectionKind;
import io.hexaglue.spi.types.CollectionMetadata;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal implementation of collection metadata.
 *
 * <p>This implementation provides information about collection types extracted from
 * parameterized type references during type analysis.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>Instances are immutable and thread-safe.</p>
 *
 * @see CollectionMetadata
 * @see CollectionKind
 */
public final class CollectionMetadataImpl implements CollectionMetadata {

    private final CollectionKind kind;
    private final TypeRef elementType;
    private final TypeRef keyType;

    /**
     * Constructs collection metadata.
     *
     * @param kind        collection kind (not {@code null})
     * @param elementType element/value type (not {@code null})
     * @param keyType     key type for maps (may be {@code null})
     */
    private CollectionMetadataImpl(CollectionKind kind, TypeRef elementType, TypeRef keyType) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.elementType = Objects.requireNonNull(elementType, "elementType");
        this.keyType = keyType;
    }

    /**
     * Creates collection metadata for List or Set types.
     *
     * @param kind        collection kind (LIST, SET, or COLLECTION)
     * @param elementType element type (not {@code null})
     * @return collection metadata
     * @throws IllegalArgumentException if kind is MAP
     */
    public static CollectionMetadataImpl of(CollectionKind kind, TypeRef elementType) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(elementType, "elementType");

        if (kind == CollectionKind.MAP) {
            throw new IllegalArgumentException("Use of(kind, keyType, valueType) for MAP collections");
        }

        return new CollectionMetadataImpl(kind, elementType, null);
    }

    /**
     * Creates collection metadata for Map types.
     *
     * @param keyType   key type (not {@code null})
     * @param valueType value type (not {@code null})
     * @return collection metadata with kind=MAP
     */
    public static CollectionMetadataImpl ofMap(TypeRef keyType, TypeRef valueType) {
        Objects.requireNonNull(keyType, "keyType");
        Objects.requireNonNull(valueType, "valueType");
        return new CollectionMetadataImpl(CollectionKind.MAP, valueType, keyType);
    }

    @Override
    public CollectionKind kind() {
        return kind;
    }

    @Override
    public TypeRef elementType() {
        return elementType;
    }

    @Override
    public Optional<TypeRef> keyType() {
        return Optional.ofNullable(keyType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CollectionMetadataImpl other)) return false;
        return kind == other.kind && elementType.equals(other.elementType) && Objects.equals(keyType, other.keyType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, elementType, keyType);
    }

    @Override
    public String toString() {
        if (kind == CollectionKind.MAP) {
            return String.format("CollectionMetadata{kind=%s, keyType=%s, valueType=%s}", kind, keyType, elementType);
        }
        return String.format("CollectionMetadata{kind=%s, elementType=%s}", kind, elementType);
    }
}
