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
package io.hexaglue.core.internal.ir.ports.index;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.ir.ports.PortDirection;
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
 * Unified index providing efficient access to all port contracts.
 *
 * <p>
 * This is the main entry point for querying the port model. It aggregates multiple specialized
 * indexes and provides a unified API for accessing ports by various criteria including qualified
 * names, simple names, directions, and packages.
 * </p>
 *
 * <h2>Index Structure</h2>
 * <p>
 * The port index is composed of:
 * </p>
 * <ul>
 *   <li><strong>Direction Index:</strong> Fast lookups by port direction (DRIVING vs DRIVEN)</li>
 *   <li><strong>Name Indexes:</strong> Fast lookups by qualified and simple names</li>
 *   <li><strong>Package Index:</strong> Fast lookups by package</li>
 * </ul>
 *
 * <h2>Common Queries</h2>
 * <p>
 * The index optimizes the following query patterns:
 * </p>
 * <ul>
 *   <li>Find a specific port by qualified name</li>
 *   <li>List all ports of a specific direction (driving or driven)</li>
 *   <li>List all ports in a package</li>
 *   <li>Search by simple name across all ports</li>
 * </ul>
 *
 * <h2>Lookup Performance</h2>
 * <ul>
 *   <li><strong>By qualified name:</strong> O(1) hash-based lookup</li>
 *   <li><strong>By direction:</strong> O(1) lookup</li>
 *   <li><strong>By package:</strong> O(1) lookup</li>
 *   <li><strong>By simple name:</strong> O(1) lookup (may return multiple results)</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Unified API:</strong> Single access point for all port queries</li>
 *   <li><strong>Performance:</strong> O(1) lookups for common queries</li>
 *   <li><strong>Immutability:</strong> Index is immutable after construction</li>
 *   <li><strong>Memory Efficiency:</strong> Share Port instances across indexes</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortIndex index = PortIndex.from(portModel);
 *
 * // Access specialized indexes
 * PortByDirectionIndex directionIndex = index.byDirection();
 *
 * // Direct lookups
 * Optional<Port> repository = index.findPort("com.example.CustomerRepository");
 * List<Port> drivenPorts = index.drivenPorts();
 * List<Port> portsInPackage = index.findByPackage("com.example.domain.ports");
 * }</pre>
 */
@InternalMarker(reason = "Internal port index; not exposed to plugins")
public final class PortIndex {

    private final Map<String, Port> byQualifiedName;
    private final Map<String, List<Port>> bySimpleName;
    private final Map<String, List<Port>> byPackage;
    private final PortByDirectionIndex directionIndex;
    private final List<Port> allPorts;

    /**
     * Creates a port index from the given maps and specialized indexes.
     *
     * @param byQualifiedName index by qualified name (not {@code null})
     * @param bySimpleName    index by simple name (not {@code null})
     * @param byPackage       index by package (not {@code null})
     * @param directionIndex  direction index (not {@code null})
     * @param allPorts        all ports (not {@code null})
     */
    private PortIndex(
            Map<String, Port> byQualifiedName,
            Map<String, List<Port>> bySimpleName,
            Map<String, List<Port>> byPackage,
            PortByDirectionIndex directionIndex,
            List<Port> allPorts) {
        this.byQualifiedName = Collections.unmodifiableMap(new HashMap<>(byQualifiedName));
        this.bySimpleName = deepUnmodifiableMap(bySimpleName);
        this.byPackage = deepUnmodifiableMap(byPackage);
        this.directionIndex = Objects.requireNonNull(directionIndex, "directionIndex");
        this.allPorts = Collections.unmodifiableList(new ArrayList<>(allPorts));
    }

    /**
     * Creates an index from a port model.
     *
     * @param model port model to index (not {@code null})
     * @return port index (never {@code null})
     * @throws NullPointerException if model is null
     */
    public static PortIndex from(PortModel model) {
        Objects.requireNonNull(model, "model");
        return from(model.ports());
    }

    /**
     * Creates an index from a collection of ports.
     *
     * @param ports ports to index (not {@code null})
     * @return port index (never {@code null})
     * @throws NullPointerException if ports is null
     */
    public static PortIndex from(Collection<Port> ports) {
        Objects.requireNonNull(ports, "ports");

        Map<String, Port> byQualifiedName = new HashMap<>();
        Map<String, List<Port>> bySimpleName = new HashMap<>();
        Map<String, List<Port>> byPackage = new HashMap<>();

        for (Port port : ports) {
            // Index by qualified name
            byQualifiedName.put(port.qualifiedName(), port);

            // Index by simple name
            bySimpleName
                    .computeIfAbsent(port.simpleName(), k -> new ArrayList<>())
                    .add(port);

            // Index by package
            String packageName = port.packageName();
            byPackage.computeIfAbsent(packageName, k -> new ArrayList<>()).add(port);
        }

        // Build direction index
        PortByDirectionIndex directionIndex = PortByDirectionIndex.from(ports);

        return new PortIndex(byQualifiedName, bySimpleName, byPackage, directionIndex, new ArrayList<>(ports));
    }

    /**
     * Creates an empty index.
     *
     * @return empty index (never {@code null})
     */
    public static PortIndex empty() {
        return new PortIndex(Map.of(), Map.of(), Map.of(), PortByDirectionIndex.empty(), List.of());
    }

    /**
     * Returns the direction index.
     *
     * <p>
     * Use this to access specialized direction-based lookups.
     * </p>
     *
     * @return direction index (never {@code null})
     */
    public PortByDirectionIndex byDirection() {
        return directionIndex;
    }

    /**
     * Finds a port by its qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return port if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<Port> findPort(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return Optional.ofNullable(byQualifiedName.get(qualifiedName));
    }

    /**
     * Finds all ports with the given simple name.
     *
     * <p>
     * Multiple ports may have the same simple name if they are in different packages.
     * </p>
     *
     * @param simpleName simple name (not {@code null})
     * @return list of matching ports (never {@code null}, may be empty)
     * @throws NullPointerException if simpleName is null
     */
    public List<Port> findBySimpleName(String simpleName) {
        Objects.requireNonNull(simpleName, "simpleName");
        return bySimpleName.getOrDefault(simpleName, List.of());
    }

    /**
     * Finds all ports in the given package.
     *
     * <p>
     * This method does not search sub-packages. Use {@link #findByPackagePrefix(String)}
     * for hierarchical searches.
     * </p>
     *
     * @param packageName package name (not {@code null})
     * @return list of ports in package (never {@code null}, may be empty)
     * @throws NullPointerException if packageName is null
     */
    public List<Port> findByPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        return byPackage.getOrDefault(packageName, List.of());
    }

    /**
     * Finds all ports in packages starting with the given prefix.
     *
     * <p>
     * This enables hierarchical package searches. For example, searching for "com.example"
     * will return ports in "com.example", "com.example.domain", "com.example.application", etc.
     * </p>
     *
     * @param packagePrefix package prefix (not {@code null})
     * @return list of ports in matching packages (never {@code null}, may be empty)
     * @throws NullPointerException if packagePrefix is null
     */
    public List<Port> findByPackagePrefix(String packagePrefix) {
        Objects.requireNonNull(packagePrefix, "packagePrefix");

        return byPackage.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(packagePrefix))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }

    /**
     * Finds all ports with the given direction.
     *
     * <p>
     * This is a convenience method that delegates to {@link PortByDirectionIndex#findByDirection(PortDirection)}.
     * </p>
     *
     * @param direction port direction (not {@code null})
     * @return list of matching ports (never {@code null}, may be empty)
     * @throws NullPointerException if direction is null
     */
    public List<Port> findByDirection(PortDirection direction) {
        return directionIndex.findByDirection(direction);
    }

    /**
     * Returns all driving ports (inbound).
     *
     * @return list of driving ports (never {@code null}, may be empty)
     */
    public List<Port> drivingPorts() {
        return directionIndex.drivingPorts();
    }

    /**
     * Returns all driven ports (outbound).
     *
     * @return list of driven ports (never {@code null}, may be empty)
     */
    public List<Port> drivenPorts() {
        return directionIndex.drivenPorts();
    }

    /**
     * Returns all ports.
     *
     * @return immutable list of all ports (never {@code null})
     */
    public List<Port> all() {
        return allPorts;
    }

    /**
     * Returns the total number of ports.
     *
     * @return port count
     */
    public int size() {
        return allPorts.size();
    }

    /**
     * Returns the number of driving ports.
     *
     * @return driving port count
     */
    public int drivingPortCount() {
        return directionIndex.drivingPortCount();
    }

    /**
     * Returns the number of driven ports.
     *
     * @return driven port count
     */
    public int drivenPortCount() {
        return directionIndex.drivenPortCount();
    }

    /**
     * Returns whether a port with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if port exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean contains(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return byQualifiedName.containsKey(qualifiedName);
    }

    /**
     * Returns whether the index is empty (no ports).
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return allPorts.isEmpty();
    }

    /**
     * Makes a map with unmodifiable lists as values.
     *
     * @param map source map (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map with unmodifiable list values
     */
    private static <K, V> Map<K, List<V>> deepUnmodifiableMap(Map<K, List<V>> map) {
        Map<K, List<V>> result = new HashMap<>();
        for (Map.Entry<K, List<V>> entry : map.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public String toString() {
        return "PortIndex{" + "ports="
                + size() + ", driving="
                + drivingPortCount() + ", driven="
                + drivenPortCount() + '}';
    }
}
