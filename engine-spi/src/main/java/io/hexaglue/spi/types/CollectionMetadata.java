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

import java.util.Optional;

/**
 * Metadata about a collection type.
 *
 * <p>This interface provides access to information about collection types that is useful
 * for code generation, particularly for persistence layer adapters (JPA, etc.).</p>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code List<String>} → kind=LIST, elementType=String, keyType=empty</li>
 *   <li>{@code Set<Customer>} → kind=SET, elementType=Customer, keyType=empty</li>
 *   <li>{@code Map<Long, Order>} → kind=MAP, elementType=Order, keyType=Long</li>
 * </ul>
 *
 * @since 0.4.0
 */
public interface CollectionMetadata {

    /**
     * Kind of collection (List, Set, Map, etc.).
     *
     * @return collection kind (never {@code null})
     */
    CollectionKind kind();

    /**
     * Element type for List/Set, or value type for Map.
     *
     * <p>For {@link CollectionKind#MAP MAP}, this represents the value type (V in {@code Map<K,V>}).
     * For other collection kinds, this represents the element type (E in {@code Collection<E>}).</p>
     *
     * @return element/value type (never {@code null})
     */
    TypeRef elementType();

    /**
     * Key type for Map collections.
     *
     * <p>This is only present when {@link #kind()} returns {@link CollectionKind#MAP MAP}.
     * For all other collection kinds, this returns {@link Optional#empty()}.</p>
     *
     * @return key type or empty if not a Map
     */
    Optional<TypeRef> keyType();
}
