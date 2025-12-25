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

/**
 * Kind of collection type.
 *
 * <p>This enumeration classifies collection types based on their primary characteristics:
 * ordering, uniqueness, and key-value structure.</p>
 *
 * @since 0.4.0
 */
public enum CollectionKind {

    /**
     * Ordered collection allowing duplicates (java.util.List and subtypes).
     *
     * <p>Examples: {@code List<String>}, {@code ArrayList<Customer>}</p>
     */
    LIST,

    /**
     * Unordered collection with unique elements (java.util.Set and subtypes).
     *
     * <p>Examples: {@code Set<String>}, {@code HashSet<Customer>}</p>
     */
    SET,

    /**
     * Key-value pairs (java.util.Map and subtypes).
     *
     * <p>Examples: {@code Map<String, Integer>}, {@code HashMap<Long, Customer>}</p>
     */
    MAP,

    /**
     * Generic collection (java.util.Collection or other unspecified collection types).
     *
     * <p>This is used as a fallback when the specific collection type cannot be determined
     * or for custom collection types that don't clearly fit the other categories.</p>
     *
     * <p>Examples: {@code Collection<String>}, custom collection implementations</p>
     */
    COLLECTION
}
