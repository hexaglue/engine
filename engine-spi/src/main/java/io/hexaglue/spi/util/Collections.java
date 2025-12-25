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
package io.hexaglue.spi.util;

import io.hexaglue.spi.types.CollectionKind;
import java.util.Optional;
import java.util.Set;

/**
 * Utilities for working with collection types.
 *
 * <p>This class provides helper methods for identifying and working with Java collection
 * types in a type-safe manner. It is primarily used by plugins to handle collection
 * properties during code generation.</p>
 *
 * <p>All methods are null-safe and thread-safe.</p>
 *
 * @since 0.4.0
 */
public final class Collections {

    private static final Set<String> LIST_TYPES = Set.of(
            "java.util.List",
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.Vector",
            "java.util.Stack",
            "java.util.concurrent.CopyOnWriteArrayList");

    private static final Set<String> SET_TYPES = Set.of(
            "java.util.Set",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.TreeSet",
            "java.util.EnumSet",
            "java.util.concurrent.ConcurrentSkipListSet",
            "java.util.concurrent.CopyOnWriteArraySet");

    private static final Set<String> MAP_TYPES = Set.of(
            "java.util.Map",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.TreeMap",
            "java.util.Hashtable",
            "java.util.IdentityHashMap",
            "java.util.WeakHashMap",
            "java.util.EnumMap",
            "java.util.concurrent.ConcurrentHashMap",
            "java.util.concurrent.ConcurrentSkipListMap");

    private Collections() {
        // Utility class
    }

    /**
     * Detects the collection kind from a qualified type name.
     *
     * <p>This method analyzes the fully-qualified class name to determine if it represents
     * a known collection type and returns the appropriate {@link CollectionKind}.</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "java.util.List"} → {@code Optional.of(CollectionKind.LIST)}</li>
     *   <li>{@code "java.util.HashSet"} → {@code Optional.of(CollectionKind.SET)}</li>
     *   <li>{@code "java.util.HashMap"} → {@code Optional.of(CollectionKind.MAP)}</li>
     *   <li>{@code "java.util.Collection"} → {@code Optional.of(CollectionKind.COLLECTION)}</li>
     *   <li>{@code "java.lang.String"} → {@code Optional.empty()}</li>
     *   <li>{@code null} → {@code Optional.empty()}</li>
     * </ul>
     *
     * @param qualifiedName fully-qualified class name (may be {@code null})
     * @return collection kind or empty if not a known collection type
     */
    public static Optional<CollectionKind> detectKind(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return Optional.empty();
        }

        // Check specific collection types first (most specific to least specific)
        if (LIST_TYPES.contains(qualifiedName)) {
            return Optional.of(CollectionKind.LIST);
        }

        if (SET_TYPES.contains(qualifiedName)) {
            return Optional.of(CollectionKind.SET);
        }

        if (MAP_TYPES.contains(qualifiedName)) {
            return Optional.of(CollectionKind.MAP);
        }

        // Check for generic Collection interface
        if ("java.util.Collection".equals(qualifiedName)) {
            return Optional.of(CollectionKind.COLLECTION);
        }

        // Not a recognized collection type
        return Optional.empty();
    }

    /**
     * Checks if a qualified name represents a collection type.
     *
     * @param qualifiedName fully-qualified class name (may be {@code null})
     * @return {@code true} if the type is a known collection, {@code false} otherwise
     */
    public static boolean isCollection(String qualifiedName) {
        return detectKind(qualifiedName).isPresent();
    }

    /**
     * Checks if a qualified name represents a List type.
     *
     * @param qualifiedName fully-qualified class name (may be {@code null})
     * @return {@code true} if the type is a List, {@code false} otherwise
     */
    public static boolean isList(String qualifiedName) {
        return qualifiedName != null && LIST_TYPES.contains(qualifiedName);
    }

    /**
     * Checks if a qualified name represents a Set type.
     *
     * @param qualifiedName fully-qualified class name (may be {@code null})
     * @return {@code true} if the type is a Set, {@code false} otherwise
     */
    public static boolean isSet(String qualifiedName) {
        return qualifiedName != null && SET_TYPES.contains(qualifiedName);
    }

    /**
     * Checks if a qualified name represents a Map type.
     *
     * @param qualifiedName fully-qualified class name (may be {@code null})
     * @return {@code true} if the type is a Map, {@code false} otherwise
     */
    public static boolean isMap(String qualifiedName) {
        return qualifiedName != null && MAP_TYPES.contains(qualifiedName);
    }
}
