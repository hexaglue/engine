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
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.collections4.IterableUtils;

/**
 * Utility methods for working with {@link Iterable} instances.
 *
 * <p>
 * This class wraps Apache Commons Collections {@link IterableUtils} and provides additional
 * convenience methods. Unlike {@link Collections2}, this class works with any {@link Iterable},
 * including lazy or infinite sequences.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While {@link java.util.Collection} is the most common iterable type, many data sources
 * are iterable but not collections (e.g., result sets, file lines, custom iterators).
 * This class provides utilities that work with any iterable source.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless. However, the iterables passed as arguments
 * must be thread-safe if used concurrently.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * String first = Iterables2.getFirst(iterable);
 * int count = Iterables2.size(iterable);
 * boolean empty = Iterables2.isEmpty(iterable);
 * }</pre>
 */
@InternalMarker(reason = "Iterable utilities wrapping Apache Commons for core implementation only")
public final class Iterables2 {

    private Iterables2() {
        // utility class
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates to Apache Commons Collections
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if the iterable is empty.
     *
     * <p>Delegates to {@link IterableUtils#isEmpty(Iterable)}.</p>
     *
     * @param iterable iterable to check (not {@code null})
     * @return {@code true} if empty
     */
    public static boolean isEmpty(Iterable<?> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.isEmpty(iterable);
    }

    /**
     * Returns the number of elements in the iterable.
     *
     * <p>Delegates to {@link IterableUtils#size(Iterable)}.</p>
     *
     * @param iterable iterable to measure (not {@code null})
     * @return number of elements (never negative)
     */
    public static int size(Iterable<?> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.size(iterable);
    }

    /**
     * Returns {@code true} if the iterable contains the specified element.
     *
     * <p>Delegates to {@link IterableUtils#contains(Iterable, Object)}.</p>
     *
     * @param iterable iterable to search (not {@code null})
     * @param element element to find
     * @param <T> element type
     * @return {@code true} if element is found
     */
    public static <T> boolean contains(Iterable<T> iterable, T element) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.contains(iterable, element);
    }

    /**
     * Converts an iterable to a list.
     *
     * <p>Delegates to {@link IterableUtils#toList(Iterable)}.</p>
     *
     * @param iterable iterable to convert (not {@code null})
     * @param <T> element type
     * @return list (never {@code null})
     */
    public static <T> List<T> toList(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.toList(iterable);
    }

    /**
     * Returns the first element of the iterable, or {@code null} if empty.
     *
     * <p>Delegates to {@link IterableUtils#first(Iterable)}.</p>
     *
     * @param iterable iterable to query (not {@code null})
     * @param <T> element type
     * @return first element, or {@code null} if empty
     */
    public static <T> T getFirst(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.first(iterable);
    }

    /**
     * Returns the element at the specified index.
     *
     * <p>Delegates to {@link IterableUtils#get(Iterable, int)}.</p>
     *
     * @param iterable iterable to query (not {@code null})
     * @param index index of element to return (must be >= 0)
     * @param <T> element type
     * @return element at index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public static <T> T get(Iterable<T> iterable, int index) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.get(iterable, index);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extended utilities for core
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the first element of the iterable, or the default value if empty.
     *
     * @param iterable iterable to query (not {@code null})
     * @param defaultValue default value
     * @param <T> element type
     * @return first element, or default value if empty
     */
    public static <T> T getFirst(Iterable<T> iterable, T defaultValue) {
        T first = getFirst(iterable);
        return first != null ? first : defaultValue;
    }

    /**
     * Returns the last element of the iterable.
     *
     * @param iterable iterable to query (not {@code null})
     * @param <T> element type
     * @return last element, or {@code null} if empty
     */
    public static <T> T getLast(Iterable<T> iterable) {
        Objects.requireNonNull(iterable, "iterable");

        if (iterable instanceof List) {
            List<T> list = (List<T>) iterable;
            return list.isEmpty() ? null : list.get(list.size() - 1);
        }

        Iterator<T> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            return null;
        }

        T last = iterator.next();
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        return last;
    }

    /**
     * Returns the last element of the iterable, or the default value if empty.
     *
     * @param iterable iterable to query (not {@code null})
     * @param defaultValue default value
     * @param <T> element type
     * @return last element, or default value if empty
     */
    public static <T> T getLast(Iterable<T> iterable, T defaultValue) {
        T last = getLast(iterable);
        return last != null ? last : defaultValue;
    }

    /**
     * Returns the element at the specified index, or the default value if out of bounds.
     *
     * @param iterable iterable to query (not {@code null})
     * @param index index of element to return (must be >= 0)
     * @param defaultValue default value
     * @param <T> element type
     * @return element at index, or default value if out of bounds
     */
    public static <T> T get(Iterable<T> iterable, int index, T defaultValue) {
        try {
            return get(iterable, index);
        } catch (IndexOutOfBoundsException e) {
            return defaultValue;
        }
    }

    /**
     * Returns {@code true} if any element matches the predicate.
     *
     * @param iterable iterable to search (not {@code null})
     * @param predicate search predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if any element matches
     */
    public static <T> boolean any(Iterable<T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(iterable, "iterable");
        Objects.requireNonNull(predicate, "predicate");

        for (T element : iterable) {
            if (predicate.test(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if all elements match the predicate.
     *
     * @param iterable iterable to check (not {@code null})
     * @param predicate check predicate (not {@code null})
     * @param <T> element type
     * @return {@code true} if all elements match
     */
    public static <T> boolean all(Iterable<T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(iterable, "iterable");
        Objects.requireNonNull(predicate, "predicate");

        for (T element : iterable) {
            if (!predicate.test(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Finds the first element matching the predicate.
     *
     * @param iterable iterable to search (not {@code null})
     * @param predicate search predicate (not {@code null})
     * @param <T> element type
     * @return first matching element, or {@code null} if not found
     */
    public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(iterable, "iterable");
        Objects.requireNonNull(predicate, "predicate");

        for (T element : iterable) {
            if (predicate.test(element)) {
                return element;
            }
        }
        return null;
    }

    /**
     * Finds the first element matching the predicate, or returns the default value.
     *
     * @param iterable iterable to search (not {@code null})
     * @param predicate search predicate (not {@code null})
     * @param defaultValue default value
     * @param <T> element type
     * @return first matching element, or default value if not found
     */
    public static <T> T find(Iterable<T> iterable, Predicate<? super T> predicate, T defaultValue) {
        T found = find(iterable, predicate);
        return found != null ? found : defaultValue;
    }

    /**
     * Transforms an iterable using the given function.
     *
     * @param iterable iterable to transform (not {@code null})
     * @param mapper transformation function (not {@code null})
     * @param <F> source element type
     * @param <T> target element type
     * @return transformed list (never {@code null})
     */
    public static <F, T> List<T> transform(Iterable<F> iterable, Function<? super F, ? extends T> mapper) {
        Objects.requireNonNull(iterable, "iterable");
        Objects.requireNonNull(mapper, "mapper");

        List<T> result = new ArrayList<>();
        for (F element : iterable) {
            result.add(mapper.apply(element));
        }
        return result;
    }

    /**
     * Filters an iterable using the given predicate.
     *
     * @param iterable iterable to filter (not {@code null})
     * @param predicate filter predicate (not {@code null})
     * @param <T> element type
     * @return filtered list (never {@code null})
     */
    public static <T> List<T> filter(Iterable<T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(iterable, "iterable");
        Objects.requireNonNull(predicate, "predicate");

        List<T> result = new ArrayList<>();
        for (T element : iterable) {
            if (predicate.test(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Counts the number of elements matching the predicate.
     *
     * @param iterable iterable to search (not {@code null})
     * @param predicate count predicate (not {@code null})
     * @param <T> element type
     * @return count (never negative)
     */
    public static <T> int count(Iterable<T> iterable, Predicate<? super T> predicate) {
        Objects.requireNonNull(iterable, "iterable");
        Objects.requireNonNull(predicate, "predicate");

        int count = 0;
        for (T element : iterable) {
            if (predicate.test(element)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns a string representation of the iterable.
     *
     * @param iterable iterable to format (not {@code null})
     * @return string representation (never {@code null})
     */
    public static String toString(Iterable<?> iterable) {
        Objects.requireNonNull(iterable, "iterable");
        return IterableUtils.toString(iterable);
    }
}
