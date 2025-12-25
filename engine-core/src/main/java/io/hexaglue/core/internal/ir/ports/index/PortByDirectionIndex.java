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
import io.hexaglue.spi.ir.ports.PortDirection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Specialized index for ports organized by direction.
 *
 * <p>
 * This index provides fast access to ports based on their direction in Hexagonal Architecture.
 * It enables efficient retrieval of all driving ports (inbound) or all driven ports (outbound)
 * without scanning the entire port collection.
 * </p>
 *
 * <h2>Direction Categories</h2>
 * <ul>
 *   <li><strong>DRIVING:</strong> Inbound ports expressing what the application offers</li>
 *   <li><strong>DRIVEN:</strong> Outbound ports expressing what the application requires</li>
 * </ul>
 *
 * <h2>Lookup Performance</h2>
 * <ul>
 *   <li><strong>By direction:</strong> O(1) lookup to retrieve all ports of a direction</li>
 *   <li><strong>Memory:</strong> Minimal overhead, shares Port instances</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Simplicity:</strong> Single-purpose index focused on direction</li>
 *   <li><strong>Performance:</strong> O(1) lookups for direction-based queries</li>
 *   <li><strong>Immutability:</strong> Index is immutable after construction</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortByDirectionIndex index = PortByDirectionIndex.from(ports);
 *
 * // Get all repositories and gateways (driven ports)
 * List<Port> drivenPorts = index.findByDirection(PortDirection.DRIVEN);
 *
 * // Get all use cases and APIs (driving ports)
 * List<Port> drivingPorts = index.findByDirection(PortDirection.DRIVING);
 * }</pre>
 */
@InternalMarker(reason = "Internal port index; not exposed to plugins")
public final class PortByDirectionIndex {

    private final Map<PortDirection, List<Port>> byDirection;

    /**
     * Creates a port direction index from the given map.
     *
     * @param byDirection index by direction (not {@code null})
     */
    private PortByDirectionIndex(Map<PortDirection, List<Port>> byDirection) {
        this.byDirection = deepUnmodifiableMap(byDirection);
    }

    /**
     * Creates an index from a collection of ports.
     *
     * @param ports ports to index (not {@code null})
     * @return port direction index (never {@code null})
     * @throws NullPointerException if ports is null
     */
    public static PortByDirectionIndex from(Collection<Port> ports) {
        Objects.requireNonNull(ports, "ports");

        Map<PortDirection, List<Port>> byDirection = new HashMap<>();

        for (Port port : ports) {
            byDirection
                    .computeIfAbsent(port.direction(), k -> new ArrayList<>())
                    .add(port);
        }

        return new PortByDirectionIndex(byDirection);
    }

    /**
     * Creates an empty index.
     *
     * @return empty index (never {@code null})
     */
    public static PortByDirectionIndex empty() {
        return new PortByDirectionIndex(Map.of());
    }

    /**
     * Finds all ports with the given direction.
     *
     * @param direction port direction (not {@code null})
     * @return list of matching ports (never {@code null}, may be empty)
     * @throws NullPointerException if direction is null
     */
    public List<Port> findByDirection(PortDirection direction) {
        Objects.requireNonNull(direction, "direction");
        return byDirection.getOrDefault(direction, List.of());
    }

    /**
     * Returns all driving ports (inbound).
     *
     * @return list of driving ports (never {@code null}, may be empty)
     */
    public List<Port> drivingPorts() {
        return findByDirection(PortDirection.DRIVING);
    }

    /**
     * Returns all driven ports (outbound).
     *
     * @return list of driven ports (never {@code null}, may be empty)
     */
    public List<Port> drivenPorts() {
        return findByDirection(PortDirection.DRIVEN);
    }

    /**
     * Returns the count of ports for a given direction.
     *
     * @param direction port direction (not {@code null})
     * @return count of ports
     * @throws NullPointerException if direction is null
     */
    public int countByDirection(PortDirection direction) {
        Objects.requireNonNull(direction, "direction");
        return byDirection.getOrDefault(direction, List.of()).size();
    }

    /**
     * Returns the count of driving ports.
     *
     * @return driving port count
     */
    public int drivingPortCount() {
        return countByDirection(PortDirection.DRIVING);
    }

    /**
     * Returns the count of driven ports.
     *
     * @return driven port count
     */
    public int drivenPortCount() {
        return countByDirection(PortDirection.DRIVEN);
    }

    /**
     * Returns whether the index is empty (no ports).
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return byDirection.values().stream().allMatch(List::isEmpty);
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
        return "PortByDirectionIndex{" + "driving=" + drivingPortCount() + ", driven=" + drivenPortCount() + '}';
    }
}
