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
import io.hexaglue.core.internal.ir.domain.DomainService;
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
 * Efficient index for domain services with multiple lookup strategies.
 *
 * <p>
 * This index provides fast access to domain services using various criteria including qualified
 * names, simple names, and package prefixes. It is optimized for read-heavy workloads typical
 * in code generation scenarios.
 * </p>
 *
 * <h2>Lookup Strategies</h2>
 * <p>
 * The index supports the following lookup patterns:
 * </p>
 * <ul>
 *   <li><strong>By qualified name:</strong> O(1) exact match lookup</li>
 *   <li><strong>By simple name:</strong> Returns all services with matching simple name</li>
 *   <li><strong>By package:</strong> Returns all services in a package or package hierarchy</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Performance:</strong> O(1) lookups for common queries</li>
 *   <li><strong>Immutability:</strong> Index is immutable after construction</li>
 *   <li><strong>Memory Efficiency:</strong> Share service instances across indexes</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainServiceIndex index = DomainServiceIndex.from(domainServices);
 *
 * // Lookup by qualified name
 * Optional<DomainService> pricing = index.findByQualifiedName("com.example.PricingService");
 *
 * // Find all services by simple name
 * List<DomainService> calculators = index.findBySimpleName("Calculator");
 *
 * // Find services in a package
 * List<DomainService> services = index.findByPackage("com.example.domain");
 * }</pre>
 */
@InternalMarker(reason = "Internal domain index; not exposed to plugins")
public final class DomainServiceIndex {

    private final Map<String, DomainService> byQualifiedName;
    private final Map<String, List<DomainService>> bySimpleName;
    private final Map<String, List<DomainService>> byPackage;
    private final List<DomainService> allServices;

    /**
     * Creates a domain service index from the given maps.
     *
     * @param byQualifiedName index by qualified name (not {@code null})
     * @param bySimpleName    index by simple name (not {@code null})
     * @param byPackage       index by package (not {@code null})
     * @param allServices     all services (not {@code null})
     */
    private DomainServiceIndex(
            Map<String, DomainService> byQualifiedName,
            Map<String, List<DomainService>> bySimpleName,
            Map<String, List<DomainService>> byPackage,
            List<DomainService> allServices) {
        this.byQualifiedName = Collections.unmodifiableMap(new HashMap<>(byQualifiedName));
        this.bySimpleName = deepUnmodifiableMap(bySimpleName);
        this.byPackage = deepUnmodifiableMap(byPackage);
        this.allServices = Collections.unmodifiableList(new ArrayList<>(allServices));
    }

    /**
     * Creates an index from a collection of domain services.
     *
     * @param services domain services to index (not {@code null})
     * @return domain service index (never {@code null})
     * @throws NullPointerException if services is null
     */
    public static DomainServiceIndex from(Collection<DomainService> services) {
        Objects.requireNonNull(services, "services");

        Map<String, DomainService> byQualifiedName = new HashMap<>();
        Map<String, List<DomainService>> bySimpleName = new HashMap<>();
        Map<String, List<DomainService>> byPackage = new HashMap<>();

        for (DomainService service : services) {
            // Index by qualified name
            byQualifiedName.put(service.qualifiedName(), service);

            // Index by simple name
            bySimpleName
                    .computeIfAbsent(service.simpleName(), k -> new ArrayList<>())
                    .add(service);

            // Index by package
            String packageName = extractPackageName(service.qualifiedName());
            byPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(service);
        }

        return new DomainServiceIndex(byQualifiedName, bySimpleName, byPackage, new ArrayList<>(services));
    }

    /**
     * Creates an empty index.
     *
     * @return empty index (never {@code null})
     */
    public static DomainServiceIndex empty() {
        return new DomainServiceIndex(Map.of(), Map.of(), Map.of(), List.of());
    }

    /**
     * Finds a domain service by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return domain service if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<DomainService> findByQualifiedName(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return Optional.ofNullable(byQualifiedName.get(qualifiedName));
    }

    /**
     * Finds all domain services with the given simple name.
     *
     * <p>
     * Multiple services may have the same simple name if they are in different packages.
     * </p>
     *
     * @param simpleName simple name (not {@code null})
     * @return list of matching services (never {@code null}, may be empty)
     * @throws NullPointerException if simpleName is null
     */
    public List<DomainService> findBySimpleName(String simpleName) {
        Objects.requireNonNull(simpleName, "simpleName");
        return bySimpleName.getOrDefault(simpleName, List.of());
    }

    /**
     * Finds all domain services in the given package.
     *
     * <p>
     * This method does not search sub-packages. Use {@link #findByPackagePrefix(String)}
     * for hierarchical searches.
     * </p>
     *
     * @param packageName package name (not {@code null})
     * @return list of services in package (never {@code null}, may be empty)
     * @throws NullPointerException if packageName is null
     */
    public List<DomainService> findByPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return byPackage.getOrDefault(packageName, List.of());
    }

    /**
     * Finds all domain services in packages matching the given prefix.
     *
     * <p>
     * This performs a hierarchical search. For example, prefix "com.example" will match
     * services in "com.example", "com.example.domain", "com.example.domain.service", etc.
     * </p>
     *
     * @param packagePrefix package prefix (not {@code null})
     * @return list of matching services (never {@code null}, may be empty)
     * @throws NullPointerException if packagePrefix is null
     */
    public List<DomainService> findByPackagePrefix(String packagePrefix) {
        Objects.requireNonNull(packagePrefix, "packagePrefix");

        return byPackage.entrySet().stream()
                .filter(entry ->
                        entry.getKey().equals(packagePrefix) || entry.getKey().startsWith(packagePrefix + "."))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }

    /**
     * Returns all domain services in this index.
     *
     * @return immutable list of all services (never {@code null})
     */
    public List<DomainService> all() {
        return allServices;
    }

    /**
     * Returns the number of services in this index.
     *
     * @return service count
     */
    public int size() {
        return allServices.size();
    }

    /**
     * Returns whether this index is empty.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return allServices.isEmpty();
    }

    /**
     * Returns whether a service with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if service exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean contains(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return byQualifiedName.containsKey(qualifiedName);
    }

    @Override
    public String toString() {
        return "DomainServiceIndex{size=" + size() + "}";
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
