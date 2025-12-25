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
package io.hexaglue.core.internal.util;

import io.hexaglue.core.internal.InternalMarker;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import org.apache.commons.collections4.MapUtils;

/**
 * Utility methods for working with {@link Map} instances.
 *
 * <p>
 * This class wraps Apache Commons Collections {@link MapUtils} and provides additional
 * convenience methods for common map operations. It focuses on:
 * </p>
 * <ul>
 *   <li>Creating and copying maps</li>
 *   <li>Transforming maps</li>
 *   <li>Filtering maps</li>
 *   <li>Map interrogation and manipulation</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * This class provides a simplified API for map operations that complements the JDK's
 * {@link Map} interface and wraps Apache Commons utilities.
 * </p>
 *
 * <h2>Immutability</h2>
 * <p>
 * Methods prefixed with {@code immutable} return unmodifiable maps. Attempts to
 * modify these maps will throw {@link UnsupportedOperationException}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless. However, the maps passed as arguments
 * must be thread-safe if used concurrently.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * Map<String, Integer> map = Maps2.of("a", 1, "b", 2);
 * Map<String, String> transformed = Maps2.transformValues(map, Object::toString);
 * boolean empty = Maps2.isEmpty(map);
 * }</pre>
 */
@InternalMarker(reason = "Map utilities wrapping Apache Commons for core implementation only")
public final class Maps2 {

    private Maps2() {
        // utility class
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates to Apache Commons Collections
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the map is null or empty.
     *
     * <p>Delegates to {@link MapUtils#isEmpty(Map)}.</p>
     *
     * @param map map to check
     * @return {@code true} if null or empty
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return MapUtils.isEmpty(map);
    }

    /**
     * Returns {@code true} if the map is not null and not empty.
     *
     * <p>Delegates to {@link MapUtils#isNotEmpty(Map)}.</p>
     *
     * @param map map to check
     * @return {@code true} if not null and not empty
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return MapUtils.isNotEmpty(map);
    }

    /**
     * Returns the size of the map, or 0 if null.
     *
     * <p>Delegates to {@link MapUtils#size(Map)}.</p>
     *
     * @param map map to measure
     * @return size (never negative)
     */
    public static int size(Map<?, ?> map) {
        return MapUtils.size(map);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extended utilities for core
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates an immutable empty map.
     *
     * @param <K> key type
     * @param <V> value type
     * @return empty unmodifiable map (never {@code null})
     */
    public static <K, V> Map<K, V> emptyMap() {
        return Collections.emptyMap();
    }

    /**
     * Creates an immutable map with a single entry.
     *
     * @param key key (not {@code null})
     * @param value value (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return singleton map (never {@code null})
     */
    public static <K, V> Map<K, V> of(K key, V value) {
        Objects.requireNonNull(key, "key");
        return Collections.singletonMap(key, value);
    }

    /**
     * Creates an immutable map with two entries.
     *
     * @param k1 first key (not {@code null})
     * @param v1 first value (not {@code null})
     * @param k2 second key (not {@code null})
     * @param v2 second value (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map (never {@code null})
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates an immutable map with three entries.
     *
     * @param k1 first key (not {@code null})
     * @param v1 first value (not {@code null})
     * @param k2 second key (not {@code null})
     * @param v2 second value (not {@code null})
     * @param k3 third key (not {@code null})
     * @param v3 third value (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map (never {@code null})
     */
    public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates an immutable copy of the given map.
     *
     * @param map map to copy (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return unmodifiable map (never {@code null})
     */
    public static <K, V> Map<K, V> immutableMap(Map<? extends K, ? extends V> map) {
        Objects.requireNonNull(map, "map");
        return Collections.unmodifiableMap(new LinkedHashMap<>(map));
    }

    /**
     * Creates a new mutable map.
     *
     * @param <K> key type
     * @param <V> value type
     * @return new mutable map (never {@code null})
     */
    public static <K, V> Map<K, V> newMap() {
        return new HashMap<>();
    }

    /**
     * Creates a new mutable map with initial capacity.
     *
     * @param initialCapacity initial capacity
     * @param <K> key type
     * @param <V> value type
     * @return new mutable map (never {@code null})
     */
    public static <K, V> Map<K, V> newMap(int initialCapacity) {
        return new HashMap<>(initialCapacity);
    }

    /**
     * Creates a new linked hash map (preserves insertion order).
     *
     * @param <K> key type
     * @param <V> value type
     * @return new mutable linked map (never {@code null})
     */
    public static <K, V> Map<K, V> newLinkedMap() {
        return new LinkedHashMap<>();
    }

    /**
     * Transforms the values of a map using the given function.
     *
     * @param map map to transform (not {@code null})
     * @param valueMapper value transformation function (not {@code null})
     * @param <K> key type
     * @param <V1> source value type
     * @param <V2> target value type
     * @return transformed map (never {@code null})
     */
    public static <K, V1, V2> Map<K, V2> transformValues(
            Map<K, V1> map, Function<? super V1, ? extends V2> valueMapper) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(valueMapper, "valueMapper");

        Map<K, V2> result = new LinkedHashMap<>();
        for (Map.Entry<K, V1> entry : map.entrySet()) {
            result.put(entry.getKey(), valueMapper.apply(entry.getValue()));
        }
        return result;
    }

    /**
     * Transforms the keys of a map using the given function.
     *
     * @param map map to transform (not {@code null})
     * @param keyMapper key transformation function (not {@code null})
     * @param <K1> source key type
     * @param <K2> target key type
     * @param <V> value type
     * @return transformed map (never {@code null})
     */
    public static <K1, K2, V> Map<K2, V> transformKeys(Map<K1, V> map, Function<? super K1, ? extends K2> keyMapper) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(keyMapper, "keyMapper");

        Map<K2, V> result = new LinkedHashMap<>();
        for (Map.Entry<K1, V> entry : map.entrySet()) {
            result.put(keyMapper.apply(entry.getKey()), entry.getValue());
        }
        return result;
    }

    /**
     * Transforms both keys and values of a map.
     *
     * @param map map to transform (not {@code null})
     * @param transformer entry transformation function (not {@code null})
     * @param <K1> source key type
     * @param <V1> source value type
     * @param <K2> target key type
     * @param <V2> target value type
     * @return transformed map (never {@code null})
     */
    public static <K1, V1, K2, V2> Map<K2, V2> transform(
            Map<K1, V1> map, BiFunction<? super K1, ? super V1, Map.Entry<K2, V2>> transformer) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(transformer, "transformer");

        Map<K2, V2> result = new LinkedHashMap<>();
        for (Map.Entry<K1, V1> entry : map.entrySet()) {
            Map.Entry<K2, V2> transformed = transformer.apply(entry.getKey(), entry.getValue());
            if (transformed != null) {
                result.put(transformed.getKey(), transformed.getValue());
            }
        }
        return result;
    }

    /**
     * Filters a map based on a predicate.
     *
     * @param map map to filter (not {@code null})
     * @param predicate filter predicate (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return filtered map (never {@code null})
     */
    public static <K, V> Map<K, V> filter(Map<K, V> map, BiPredicate<? super K, ? super V> predicate) {
        Objects.requireNonNull(map, "map");
        Objects.requireNonNull(predicate, "predicate");

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry.getKey(), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Returns a safe value from the map, or the default value if key is absent.
     *
     * @param map map to query (not {@code null})
     * @param key key to lookup
     * @param defaultValue default value
     * @param <K> key type
     * @param <V> value type
     * @return value or default
     */
    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        Objects.requireNonNull(map, "map");
        return map.getOrDefault(key, defaultValue);
    }

    /**
     * Merges multiple maps into one.
     *
     * <p>
     * Later maps override earlier ones for duplicate keys.
     * </p>
     *
     * @param maps maps to merge (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return merged map (never {@code null})
     */
    @SafeVarargs
    public static <K, V> Map<K, V> merge(Map<K, V>... maps) {
        Objects.requireNonNull(maps, "maps");

        Map<K, V> result = new LinkedHashMap<>();
        for (Map<K, V> map : maps) {
            if (map != null) {
                result.putAll(map);
            }
        }
        return result;
    }

    /**
     * Inverts a map (swaps keys and values).
     *
     * <p>
     * Note: If multiple keys map to the same value, only one will be retained.
     * </p>
     *
     * @param map map to invert (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return inverted map (never {@code null})
     */
    public static <K, V> Map<V, K> invert(Map<K, V> map) {
        Objects.requireNonNull(map, "map");

        Map<V, K> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    /**
     * Creates a simple immutable entry.
     *
     * @param key key (not {@code null})
     * @param value value
     * @param <K> key type
     * @param <V> value type
     * @return map entry (never {@code null})
     */
    public static <K, V> Map.Entry<K, V> entry(K key, V value) {
        Objects.requireNonNull(key, "key");
        return new AbstractMapEntry<>(key, value);
    }

    /**
     * Simple immutable map entry implementation.
     */
    private static final class AbstractMapEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private final V value;

        AbstractMapEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException("Entry is immutable");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return Objects.equals(key, entry.getKey()) && Objects.equals(value, entry.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
