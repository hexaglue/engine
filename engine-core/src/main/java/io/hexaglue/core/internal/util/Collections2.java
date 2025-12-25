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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;

/**
 * Utility methods for working with {@link Collection} instances.
 *
 * <p>
 * This class wraps Apache Commons Collections and provides additional convenience methods
 * for common collection operations. It focuses on:
 * </p>
 * <ul>
 *   <li>Transforming collections</li>
 *   <li>Filtering collections</li>
 *   <li>Creating immutable collections</li>
 *   <li>Common collection operations</li>
 *   <li>Set operations (union, intersection, difference)</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * This class provides a simplified, focused API that wraps Apache Commons Collections
 * functionality while adding project-specific utilities. It bridges the gap between
 * classical Java collections, Apache Commons, and modern stream operations.
 * </p>
 *
 * <h2>Immutability</h2>
 * <p>
 * Methods prefixed with {@code immutable} return unmodifiable collections. Attempts to
 * modify these collections will throw {@link UnsupportedOperationException}.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless. However, the collections passed as arguments
 * must be thread-safe if used concurrently.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * List<String> names = Collections2.transform(users, User::getName);
 * List<User> active = Collections2.filter(users, User::isActive);
 * Set<Integer> uniqueIds = Collections2.toSet(list);
 * }</pre>
 */
@InternalMarker(reason = "Collection utilities wrapping Apache Commons for core implementation only")
public final class Collections2 {

    private Collections2() {
        // utility class
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates to Apache Commons Collections
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the collection is null or empty.
     *
     * <p>Delegates to {@link CollectionUtils#isEmpty(Collection)}.</p>
     *
     * @param collection collection to check
     * @return {@code true} if null or empty
     */
    public static boolean isEmpty(Collection<?> collection) {
        return CollectionUtils.isEmpty(collection);
    }

    /**
     * Returns {@code true} if the collection is not null and not empty.
     *
     * <p>Delegates to {@link CollectionUtils#isNotEmpty(Collection)}.</p>
     *
     * @param collection collection to check
     * @return {@code true} if not null and not empty
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return CollectionUtils.isNotEmpty(collection);
    }

    /**
     * Returns the size of the collection, or 0 if null.
     *
     * <p>Delegates to {@link CollectionUtils#size(Object)}.</p>
     *
     * @param collection collection to measure
     * @return size (never negative)
     */
    public static int size(Collection<?> collection) {
        return CollectionUtils.size(collection);
    }

    /**
     * Returns the union of two collections.
     *
     * <p>Delegates to {@link CollectionUtils#union(Iterable, Iterable)}.</p>
     *
     * @param a first collection (not {@code null})
     * @param b second collection (not {@code null})
     * @param <T> element type
     * @return union collection (never {@code null})
     */
    public static <T> Collection<T> union(Collection<? extends T> a, Collection<? extends T> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return CollectionUtils.union(a, b);
    }

    /**
     * Returns the intersection of two collections.
     *
     * <p>Delegates to {@link CollectionUtils#intersection}.</p>
     *
     * @param a first collection (not {@code null})
     * @param b second collection (not {@code null})
     * @param <T> element type
     * @return intersection collection (never {@code null})
     */
    public static <T> Collection<T> intersection(Collection<? extends T> a, Collection<? extends T> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return CollectionUtils.intersection(a, b);
    }

    /**
     * Returns the difference of two collections (a - b).
     *
     * <p>Delegates to {@link CollectionUtils#subtract(Iterable, Iterable)}.</p>
     *
     * @param a first collection (not {@code null})
     * @param b second collection (not {@code null})
     * @param <T> element type
     * @return difference collection (never {@code null})
     */
    public static <T> Collection<T> difference(Collection<? extends T> a, Collection<? extends T> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return CollectionUtils.subtract(a, b);
    }

    /**
     * Returns {@code true} if the collections have no common elements.
     *
     * <p>Uses {@link Collections#disjoint(Collection, Collection)} from the JDK.</p>
     *
     * @param a first collection (not {@code null})
     * @param b second collection (not {@code null})
     * @return {@code true} if collections share no elements
     */
    public static boolean disjoint(Collection<?> a, Collection<?> b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");
        return Collections.disjoint(a, b);
    }

    /**
     * Adds all elements from the source to the target collection.
     *
     * <p>Delegates to {@link CollectionUtils#addAll(Collection, Iterable)}.</p>
     *
     * @param target target collection (not {@code null})
     * @param source source iterable (not {@code null})
     * @param <T> element type
     * @return {@code true} if the target was modified
     */
    public static <T> boolean addAll(Collection<T> target, Iterable<? extends T> source) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        return CollectionUtils.addAll(target, source);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extended utilities for core
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Transforms a collection using the given function.
     *
     * @param collection collection to transform (not {@code null})
     * @param mapper transformation function (not {@code null})
     * @param <F> source element type
     * @param <T> target element type
     * @return transformed list (never {@code null}, may be empty)
     */
    public static <F, T> List<T> transform(Collection<F> collection, Function<? super F, ? extends T> mapper) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(mapper, "mapper");
        return collection.stream().map(mapper).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Filters a collection using the given predicate.
     *
     * @param collection collection to filter (not {@code null})
     * @param predicate filter predicate (not {@code null})
     * @param <T> element type
     * @return filtered list (never {@code null}, may be empty)
     */
    public static <T> List<T> filter(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().filter(predicate).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Creates an immutable copy of the given collection.
     *
     * @param collection collection to copy (not {@code null})
     * @param <T> element type
     * @return unmodifiable list (never {@code null})
     */
    public static <T> List<T> immutableList(Collection<? extends T> collection) {
        Objects.requireNonNull(collection, "collection");
        return Collections.unmodifiableList(new ArrayList<>(collection));
    }

    /**
     * Creates an immutable set from the given collection.
     *
     * @param collection collection to copy (not {@code null})
     * @param <T> element type
     * @return unmodifiable set (never {@code null})
     */
    public static <T> Set<T> immutableSet(Collection<? extends T> collection) {
        Objects.requireNonNull(collection, "collection");
        return Collections.unmodifiableSet(new LinkedHashSet<>(collection));
    }

    /**
     * Creates an empty immutable list.
     *
     * @param <T> element type
     * @return empty unmodifiable list (never {@code null})
     */
    public static <T> List<T> emptyList() {
        return Collections.emptyList();
    }

    /**
     * Creates an empty immutable set.
     *
     * @param <T> element type
     * @return empty unmodifiable set (never {@code null})
     */
    public static <T> Set<T> emptySet() {
        return Collections.emptySet();
    }

    /**
     * Converts a collection to a set, removing duplicates.
     *
     * @param collection collection to convert (not {@code null})
     * @param <T> element type
     * @return set (never {@code null}, may be empty)
     */
    public static <T> Set<T> toSet(Collection<T> collection) {
        Objects.requireNonNull(collection, "collection");
        return new LinkedHashSet<>(collection);
    }

    /**
     * Converts a collection to a list.
     *
     * @param collection collection to convert (not {@code null})
     * @param <T> element type
     * @return list (never {@code null}, may be empty)
     */
    public static <T> List<T> toList(Collection<T> collection) {
        Objects.requireNonNull(collection, "collection");
        return new ArrayList<>(collection);
    }

    /**
     * Finds the first element matching the predicate.
     *
     * @param collection collection to search (not {@code null})
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
     * Returns {@code true} if any element matches the predicate.
     *
     * @param collection collection to search (not {@code null})
     * @param predicate search predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if any element matches
     */
    public static <T> boolean any(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().anyMatch(predicate);
    }

    /**
     * Returns {@code true} if all elements match the predicate.
     *
     * @param collection collection to check (not {@code null})
     * @param predicate check predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if all elements match
     */
    public static <T> boolean all(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().allMatch(predicate);
    }

    /**
     * Returns {@code true} if no element matches the predicate.
     *
     * @param collection collection to check (not {@code null})
     * @param predicate check predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if no element matches
     */
    public static <T> boolean none(Collection<T> collection, Predicate<? super T> predicate) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(predicate, "predicate");
        return collection.stream().noneMatch(predicate);
    }

    /**
     * Creates a new list by concatenating multiple collections.
     *
     * @param collections collections to concatenate (not {@code null})
     * @param <T> element type
     * @return concatenated list (never {@code null})
     */
    @SafeVarargs
    public static <T> List<T> concat(Collection<? extends T>... collections) {
        Objects.requireNonNull(collections, "collections");

        List<T> result = new ArrayList<>();
        for (Collection<? extends T> collection : collections) {
            if (collection != null) {
                result.addAll(collection);
            }
        }
        return result;
    }

    /**
     * Returns a sorted list of the collection.
     *
     * @param collection collection to sort (not {@code null})
     * @param <T> element type (must be comparable)
     * @return sorted list (never {@code null})
     */
    public static <T extends Comparable<? super T>> List<T> sorted(Collection<T> collection) {
        Objects.requireNonNull(collection, "collection");
        List<T> result = new ArrayList<>(collection);
        Collections.sort(result);
        return result;
    }

    /**
     * Returns a sorted list of the collection using a comparator.
     *
     * @param collection collection to sort (not {@code null})
     * @param comparator comparator (not {@code null})
     * @param <T> element type
     * @return sorted list (never {@code null})
     */
    public static <T> List<T> sorted(Collection<T> collection, Comparator<? super T> comparator) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(comparator, "comparator");
        List<T> result = new ArrayList<>(collection);
        result.sort(comparator);
        return result;
    }

    /**
     * Returns a collection with duplicate elements removed, preserving order.
     *
     * @param collection collection to deduplicate (not {@code null})
     * @param <T> element type
     * @return list without duplicates (never {@code null})
     */
    public static <T> List<T> distinct(Collection<T> collection) {
        Objects.requireNonNull(collection, "collection");
        return new ArrayList<>(new LinkedHashSet<>(collection));
    }

    /**
     * Returns the first n elements of the collection.
     *
     * @param collection collection (not {@code null})
     * @param n number of elements to take (must be >= 0)
     * @param <T> element type
     * @return list with at most n elements (never {@code null})
     */
    public static <T> List<T> limit(Collection<T> collection, int n) {
        Objects.requireNonNull(collection, "collection");
        Preconditions.checkNonNegative(n, "n");
        return collection.stream().limit(n).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Skips the first n elements and returns the rest.
     *
     * @param collection collection (not {@code null})
     * @param n number of elements to skip (must be >= 0)
     * @param <T> element type
     * @return list without first n elements (never {@code null})
     */
    public static <T> List<T> skip(Collection<T> collection, int n) {
        Objects.requireNonNull(collection, "collection");
        Preconditions.checkNonNegative(n, "n");
        return collection.stream().skip(n).collect(Collectors.toCollection(ArrayList::new));
    }
}
