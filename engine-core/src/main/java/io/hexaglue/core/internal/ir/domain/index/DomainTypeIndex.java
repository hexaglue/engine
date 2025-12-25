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
package io.hexaglue.core.internal.ir.domain.index;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Efficient index for domain types with multiple lookup strategies.
 *
 * <p>
 * This index provides fast access to domain types using various criteria including qualified
 * names, simple names, type kinds, and package prefixes. It is optimized for read-heavy
 * workloads typical in code generation scenarios.
 * </p>
 *
 * <h2>Lookup Strategies</h2>
 * <p>
 * The index supports the following lookup patterns:
 * </p>
 * <ul>
 *   <li><strong>By qualified name:</strong> O(1) exact match lookup</li>
 *   <li><strong>By simple name:</strong> Returns all types with matching simple name</li>
 *   <li><strong>By kind:</strong> Returns all types of a specific kind (entity, value object, etc.)</li>
 *   <li><strong>By package:</strong> Returns all types in a package or package hierarchy</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Performance:</strong> O(1) or O(log n) lookups for common queries</li>
 *   <li><strong>Immutability:</strong> Index is immutable after construction</li>
 *   <li><strong>Memory Efficiency:</strong> Share type instances across indexes</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainTypeIndex index = DomainTypeIndex.from(domainTypes);
 *
 * // Lookup by qualified name
 * Optional<DomainType> customer = index.findByQualifiedName("com.example.Customer");
 *
 * // Find all entities
 * List<DomainType> entities = index.findByKind(DomainTypeKind.ENTITY);
 *
 * // Find types in a package
 * List<DomainType> domainTypes = index.findByPackage("com.example.domain");
 * }</pre>
 */
@InternalMarker(reason = "Internal domain index; not exposed to plugins")
public final class DomainTypeIndex {

    private final Map<String, DomainType> byQualifiedName;
    private final Map<String, List<DomainType>> bySimpleName;
    private final Map<DomainTypeKind, List<DomainType>> byKind;
    private final Map<String, List<DomainType>> byPackage;
    private final List<DomainType> allTypes;

    /**
     * Creates a domain type index from the given maps.
     *
     * @param byQualifiedName index by qualified name (not {@code null})
     * @param bySimpleName    index by simple name (not {@code null})
     * @param byKind          index by kind (not {@code null})
     * @param byPackage       index by package (not {@code null})
     * @param allTypes        all types (not {@code null})
     */
    private DomainTypeIndex(
            Map<String, DomainType> byQualifiedName,
            Map<String, List<DomainType>> bySimpleName,
            Map<DomainTypeKind, List<DomainType>> byKind,
            Map<String, List<DomainType>> byPackage,
            List<DomainType> allTypes) {
        this.byQualifiedName = Collections.unmodifiableMap(new HashMap<>(byQualifiedName));
        this.bySimpleName = deepUnmodifiableMap(bySimpleName);
        this.byKind = deepUnmodifiableMap(byKind);
        this.byPackage = deepUnmodifiableMap(byPackage);
        this.allTypes = Collections.unmodifiableList(new ArrayList<>(allTypes));
    }

    /**
     * Creates an index from a collection of domain types.
     *
     * @param types domain types to index (not {@code null})
     * @return domain type index (never {@code null})
     * @throws NullPointerException if types is null
     */
    public static DomainTypeIndex from(Collection<DomainType> types) {
        Objects.requireNonNull(types, "types");

        Map<String, DomainType> byQualifiedName = new HashMap<>();
        Map<String, List<DomainType>> bySimpleName = new HashMap<>();
        Map<DomainTypeKind, List<DomainType>> byKind = new HashMap<>();
        Map<String, List<DomainType>> byPackage = new HashMap<>();

        for (DomainType type : types) {
            // Index by qualified name
            byQualifiedName.put(type.qualifiedName(), type);

            // Index by simple name
            bySimpleName
                    .computeIfAbsent(type.simpleName(), k -> new ArrayList<>())
                    .add(type);

            // Index by kind
            byKind.computeIfAbsent(type.kind(), k -> new ArrayList<>()).add(type);

            // Index by package
            String packageName = extractPackageName(type.qualifiedName());
            byPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(type);
        }

        return new DomainTypeIndex(byQualifiedName, bySimpleName, byKind, byPackage, new ArrayList<>(types));
    }

    /**
     * Creates an empty index.
     *
     * @return empty index (never {@code null})
     */
    public static DomainTypeIndex empty() {
        return new DomainTypeIndex(Map.of(), Map.of(), Map.of(), Map.of(), List.of());
    }

    /**
     * Finds a domain type by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return domain type if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<DomainType> findByQualifiedName(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return Optional.ofNullable(byQualifiedName.get(qualifiedName));
    }

    /**
     * Finds all domain types with the given simple name.
     *
     * <p>
     * Multiple types may have the same simple name if they are in different packages.
     * </p>
     *
     * @param simpleName simple name (not {@code null})
     * @return list of matching types (never {@code null}, may be empty)
     * @throws NullPointerException if simpleName is null
     */
    public List<DomainType> findBySimpleName(String simpleName) {
        Objects.requireNonNull(simpleName, "simpleName");
        return bySimpleName.getOrDefault(simpleName, List.of());
    }

    /**
     * Finds all domain types of the given kind.
     *
     * @param kind domain type kind (not {@code null})
     * @return list of matching types (never {@code null}, may be empty)
     * @throws NullPointerException if kind is null
     */
    public List<DomainType> findByKind(DomainTypeKind kind) {
        Objects.requireNonNull(kind, "kind");
        return byKind.getOrDefault(kind, List.of());
    }

    /**
     * Finds all domain types in the given package.
     *
     * <p>
     * This method does not search sub-packages. Use {@link #findByPackagePrefix(String)}
     * for hierarchical searches.
     * </p>
     *
     * @param packageName package name (not {@code null})
     * @return list of types in package (never {@code null}, may be empty)
     * @throws NullPointerException if packageName is null
     */
    public List<DomainType> findByPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return byPackage.getOrDefault(packageName, List.of());
    }

    /**
     * Finds all domain types in packages matching the given prefix.
     *
     * <p>
     * This performs a hierarchical search. For example, prefix "com.example" will match
     * types in "com.example", "com.example.domain", "com.example.domain.model", etc.
     * </p>
     *
     * @param packagePrefix package prefix (not {@code null})
     * @return list of matching types (never {@code null}, may be empty)
     * @throws NullPointerException if packagePrefix is null
     */
    public List<DomainType> findByPackagePrefix(String packagePrefix) {
        Objects.requireNonNull(packagePrefix, "packagePrefix");

        return byPackage.entrySet().stream()
                .filter(entry ->
                        entry.getKey().equals(packagePrefix) || entry.getKey().startsWith(packagePrefix + "."))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns all domain types in this index.
     *
     * @return immutable list of all types (never {@code null})
     */
    public List<DomainType> all() {
        return allTypes;
    }

    /**
     * Returns the number of types in this index.
     *
     * @return type count
     */
    public int size() {
        return allTypes.size();
    }

    /**
     * Returns whether this index is empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return allTypes.isEmpty();
    }

    /**
     * Returns whether a type with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if type exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean contains(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return byQualifiedName.containsKey(qualifiedName);
    }

    @Override
    public String toString() {
        return "DomainTypeIndex{size=" + size() + "}";
    }

    /**
     * Extracts the package name from a qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return package name or empty string
     */
    private static String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot > 0 ? qualifiedName.substring(0, lastDot) : "";
    }

    /**
     * Creates an unmodifiable copy of a map with unmodifiable list values.
     *
     * @param map source map (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map with unmodifiable values
     */
    private static <K, V> Map<K, List<V>> deepUnmodifiableMap(Map<K, List<V>> map) {
        Map<K, List<V>> result = new HashMap<>();
        for (Map.Entry<K, List<V>> entry : map.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }
}
