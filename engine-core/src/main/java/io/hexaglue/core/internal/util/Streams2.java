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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utility methods for working with {@link Stream} instances.
 *
 * <p>
 * This class provides convenient operations for creating and manipulating streams.
 * While the JDK's Stream API is powerful, this class adds commonly-needed helpers
 * and shortcuts.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Stream operations are fundamental to modern Java but some common patterns are
 * verbose. This class provides:
 * </p>
 * <ul>
 *   <li>Stream creation helpers</li>
 *   <li>Terminal operation shortcuts</li>
 *   <li>Specialized collectors</li>
 *   <li>Stream combination utilities</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless. Streams themselves should not be
 * shared across threads unless the underlying source is thread-safe and the
 * stream is configured for parallel processing.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * List<String> names = Streams2.map(users, User::getName);
 * List<User> active = Streams2.filter(users, User::isActive);
 * Map<String, User> byId = Streams2.toMap(users, User::getId);
 * }</pre>
 */
@InternalMarker(reason = "Stream utilities for core implementation only")
public final class Streams2 {

    private Streams2() {
        // utility class
    }

    /**
     * Creates a stream from an iterable.
     *
     * @param iterable iterable source (not {@code null})
     * @param <T> element type
     * @return stream (never {@code null})
     */
    public static <T> Stream<T> stream(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    /**
     * Creates a parallel stream from an iterable.
     *
     * @param iterable iterable source (not {@code null})
     * @param <T> element type
     * @return parallel stream (never {@code null})
     */
    public static <T> Stream<T> parallelStream(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return StreamSupport.stream(iterable.spliterator(), true);
    }

    /**
     * Maps elements from a collection using the given function.
     *
     * @param collection source collection (not {@code null})
     * @param mapper mapping function (not {@code null})
     * @param <T> source type
     * @param <R> result type
     * @return mapped list (never {@code null})
     */
    public static <T, R> List<R> map(Collection<T> collection, Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(mapper, "mapper");
        return collection.stream().map(mapper).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Flat-maps elements from a collection.
     *
     * @param collection source collection (not {@code null})
     * @param mapper flat-mapping function (not {@code null})
     * @param <T> source type
     * @param <R> result type
     * @return flat-mapped list (never {@code null})
     */
    public static <T, R> List<R> flatMap(
            Collection<T> collection, Function<? super T, ? extends Stream<? extends R>> mapper) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(mapper, "mapper");
        return collection.stream().flatMap(mapper).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Filters elements from a collection.
     *
     * @param collection source collection (not {@code null})
     * @param predicate filter predicate (not {@code null})
     * @param <T> element type
     * @return filtered list (never {@code null})
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Collects a stream to a list.
     *
     * @param stream stream to collect (not {@code null})
     * @param <T> element type
     * @return list (never {@code null})
     */
    public static <T> List<T> toList(Stream<T> stream) {
        Objects.requireNonNull(stream, "stream");
        return stream.collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Collects elements to a map by key.
     *
     * @param collection source collection (not {@code null})
     * @param keyMapper key extraction function (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return map (never {@code null})
     */
    public static <K, V> Map<K, V> toMap(Collection<V> collection, Function<? super V, ? extends K> keyMapper) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(keyMapper, "keyMapper");
        return collection.stream().collect(Collectors.toMap(keyMapper, Function.identity()));
    }

    /**
     * Collects elements to a map.
     *
     * @param collection source collection (not {@code null})
     * @param keyMapper key extraction function (not {@code null})
     * @param valueMapper value extraction function (not {@code null})
     * @param <T> source type
     * @param <K> key type
     * @param <V> value type
     * @return map (never {@code null})
     */
    public static <T, K, V> Map<K, V> toMap(
            Collection<T> collection,
            Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(keyMapper, "keyMapper");
        Objects.requireNonNull(valueMapper, "valueMapper");
        return collection.stream().collect(Collectors.toMap(keyMapper, valueMapper));
    }

    /**
     * Groups elements by a classifier function.
     *
     * @param collection source collection (not {@code null})
     * @param classifier grouping function (not {@code null})
     * @param <K> key type
     * @param <V> value type
     * @return grouped map (never {@code null})
     */
    public static <K, V> Map<K, List<V>> groupBy(
            Collection<V> collection, Function<? super V, ? extends K> classifier) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(classifier, "classifier");
        return collection.stream().collect(Collectors.groupingBy(classifier));
    }

    /**
     * Partitions elements by a predicate.
     *
     * @param collection source collection (not {@code null})
     * @param predicate partition predicate (not {@code null})
     * @param <T> element type
     * @return map with {@code true} and {@code false} keys (never {@code null})
     */
    public static <T> Map<Boolean, List<T>> partition(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().collect(Collectors.partitioningBy(predicate));
    }

    /**
     * Counts elements matching the predicate.
     *
     * @param collection source collection (not {@code null})
     * @param predicate count predicate (not {@code null})
     * @param <T> element type
     * @return count (never negative)
     */
    public static <T> long count(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().filter(predicate).count();
    }

    /**
     * Returns {@code true} if any element matches the predicate.
     *
     * @param collection source collection (not {@code null})
     * @param predicate test predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if any element matches
     */
    public static <T> boolean anyMatch(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().anyMatch(predicate);
    }

    /**
     * Returns {@code true} if all elements match the predicate.
     *
     * @param collection source collection (not {@code null})
     * @param predicate test predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if all elements match
     */
    public static <T> boolean allMatch(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().allMatch(predicate);
    }

    /**
     * Returns {@code true} if no element matches the predicate.
     *
     * @param collection source collection (not {@code null})
     * @param predicate test predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if no element matches
     */
    public static <T> boolean noneMatch(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().noneMatch(predicate);
    }

    /**
     * Finds the first element matching the predicate.
     *
     * @param collection source collection (not {@code null})
     * @param predicate search predicate (not {@code null})
     * @param <T> element type
     * @return first matching element, or {@code null} if not found
     */
    public static <T> T findFirst(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Concatenates multiple streams.
     *
     * @param streams streams to concatenate (not {@code null})
     * @param <T> element type
     * @return concatenated stream (never {@code null})
     */
    @SafeVarargs
    public static <T> Stream<T> concat(Stream<T>... streams) {
        Objects.requireNonNull(streams, "streams");
        return Stream.of(streams).flatMap(Function.identity());
    }

    /**
     * Collects a stream using the given collector.
     *
     * @param stream stream to collect (not {@code null})
     * @param collector collector to use (not {@code null})
     * @param <T> element type
     * @param <R> result type
     * @return collected result
     */
    public static <T, R> R collect(Stream<T> stream, Collector<? super T, ?, R> collector) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(collector, "collector");
        return stream.collect(collector);
    }

    /**
     * Joins stream elements into a string with a delimiter.
     *
     * @param stream stream to join (not {@code null})
     * @param delimiter delimiter (not {@code null})
     * @param <T> element type
     * @return joined string (never {@code null})
     */
    public static <T> String join(Stream<T> stream, String delimiter) {
        Objects.requireNonNull(stream, "stream");
        Objects.requireNonNull(delimiter, "delimiter");
        return stream.map(Object::toString).collect(Collectors.joining(delimiter));
    }

    /**
     * Joins collection elements into a string with a delimiter.
     *
     * @param collection collection to join (not {@code null})
     * @param delimiter delimiter (not {@code null})
     * @param <T> element type
     * @return joined string (never {@code null})
     */
    public static <T> String join(Collection<T> collection, String delimiter) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(delimiter, "delimiter");
        return collection.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }
}
