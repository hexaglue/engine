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
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.commons.lang3.Validate;

/**
 * Extended precondition utilities for internal core use.
 *
 * <p>
 * This class wraps Apache Commons Lang {@link Validate} and extends the basic preconditions
 * available in {@code io.hexaglue.spi.util.Preconditions} with additional validation methods
 * needed by the core implementation. It provides:
 * </p>
 * <ul>
 *   <li>Collection and map validation</li>
 *   <li>Range checking</li>
 *   <li>Additional null checks</li>
 *   <li>Composite validations</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While the SPI provides minimal preconditions to avoid dependencies, the core needs more
 * comprehensive validation. This class wraps Apache Commons Lang functionality while
 * delegating to the SPI version where appropriate for consistency.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * List<String> items = Preconditions.checkNotEmpty(list, "items");
 * int value = Preconditions.checkInRange(n, 0, 100, "value");
 * }</pre>
 */
@InternalMarker(reason = "Extended validation utilities wrapping Apache Commons for core implementation only")
public final class Preconditions {

    private Preconditions() {
        // utility class
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegate to SPI for consistency
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Delegates to SPI preconditions for basic argument validation.
     *
     * @param condition condition to validate
     * @param message error message (non-blank recommended)
     * @throws IllegalArgumentException if condition is false
     */
    public static void checkArgument(boolean condition, String message) {
        io.hexaglue.spi.util.Preconditions.checkArgument(condition, message);
    }

    /**
     * Delegates to SPI preconditions for basic argument validation with lazy message.
     *
     * @param condition condition to validate
     * @param messageSupplier message supplier (never {@code null})
     * @throws IllegalArgumentException if condition is false
     */
    public static void checkArgument(boolean condition, Supplier<String> messageSupplier) {
        io.hexaglue.spi.util.Preconditions.checkArgument(condition, messageSupplier);
    }

    /**
     * Delegates to SPI preconditions for state validation.
     *
     * @param condition state condition
     * @param message error message
     * @throws IllegalStateException if condition is false
     */
    public static void checkState(boolean condition, String message) {
        io.hexaglue.spi.util.Preconditions.checkState(condition, message);
    }

    /**
     * Delegates to SPI preconditions for state validation with lazy message.
     *
     * @param condition state condition
     * @param messageSupplier message supplier (never {@code null})
     * @throws IllegalStateException if condition is false
     */
    public static void checkState(boolean condition, Supplier<String> messageSupplier) {
        io.hexaglue.spi.util.Preconditions.checkState(condition, messageSupplier);
    }

    /**
     * Ensures an index is valid for the given size.
     *
     * <p>
     * Delegates to the SPI version for consistency.
     * </p>
     *
     * @param index index
     * @param size size (must be >= 0)
     * @param label label for error messages
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public static void checkIndex(int index, int size, String label) {
        io.hexaglue.spi.util.Preconditions.checkIndex(index, size, label);
    }

    /**
     * Ensures a string is not null and not blank.
     *
     * <p>
     * Delegates to the SPI version for consistency.
     * </p>
     *
     * @param value input value
     * @param label label for error messages
     * @return trimmed value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public static String requireNonBlank(String value, String label) {
        return io.hexaglue.spi.util.Preconditions.requireNonBlank(value, label);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delegates to Apache Commons Lang Validate
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ensures a collection is not null and not empty.
     *
     * <p>Delegates to {@link Validate#notEmpty(Collection, String, Object...)}.</p>
     *
     * @param collection collection to check
     * @param label label for error messages
     * @param <T> collection type
     * @return the collection if not empty
     * @throws NullPointerException if collection is null
     * @throws IllegalArgumentException if collection is empty
     */
    public static <T extends Collection<?>> T checkNotEmpty(T collection, String label) {
        return Validate.notEmpty(collection, "%s must not be empty", label);
    }

    /**
     * Ensures a map is not null and not empty.
     *
     * <p>Delegates to {@link Validate#notEmpty(Map, String, Object...)}.</p>
     *
     * @param map map to check
     * @param label label for error messages
     * @param <T> map type
     * @return the map if not empty
     * @throws NullPointerException if map is null
     * @throws IllegalArgumentException if map is empty
     */
    public static <T extends Map<?, ?>> T checkNotEmpty(T map, String label) {
        return Validate.notEmpty(map, "%s must not be empty", label);
    }

    /**
     * Ensures an array is not null and not empty.
     *
     * <p>Delegates to {@link Validate#notEmpty(Object[], String, Object...)}.</p>
     *
     * @param array array to check
     * @param label label for error messages
     * @param <T> array component type
     * @return the array if not empty
     * @throws NullPointerException if array is null
     * @throws IllegalArgumentException if array is empty
     */
    public static <T> T[] checkNotEmpty(T[] array, String label) {
        return Validate.notEmpty(array, "%s must not be empty", label);
    }

    /**
     * Ensures a value is within the inclusive range [min, max].
     *
     * <p>Delegates to {@link Validate#inclusiveBetween}.</p>
     *
     * @param value value to check
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @param label label for error messages
     * @return the value if in range
     * @throws IllegalArgumentException if value is out of range
     */
    public static int checkInRange(int value, int min, int max, String label) {
        Validate.inclusiveBetween(
                (long) min, (long) max, (long) value, "%s must be in range [%d, %d], got: %d", label, min, max, value);
        return value;
    }

    /**
     * Ensures a value is within the inclusive range [min, max].
     *
     * <p>Delegates to {@link Validate#inclusiveBetween}.</p>
     *
     * @param value value to check
     * @param min minimum value (inclusive)
     * @param max maximum value (inclusive)
     * @param label label for error messages
     * @return the value if in range
     * @throws IllegalArgumentException if value is out of range
     */
    public static long checkInRange(long value, long min, long max, String label) {
        Validate.inclusiveBetween(min, max, value, "%s must be in range [%d, %d], got: %d", label, min, max, value);
        return value;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Extended utilities for core
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ensures a value is positive (greater than zero).
     *
     * @param value value to check
     * @param label label for error messages
     * @return the value if positive
     * @throws IllegalArgumentException if value is not positive
     */
    public static int checkPositive(int value, String label) {
        if (value <= 0) {
            String l = (label == null || label.isBlank()) ? "value" : label;
            throw new IllegalArgumentException(l + " must be positive, got: " + value);
        }
        return value;
    }

    /**
     * Ensures a value is positive (greater than zero).
     *
     * @param value value to check
     * @param label label for error messages
     * @return the value if positive
     * @throws IllegalArgumentException if value is not positive
     */
    public static long checkPositive(long value, String label) {
        if (value <= 0) {
            String l = (label == null || label.isBlank()) ? "value" : label;
            throw new IllegalArgumentException(l + " must be positive, got: " + value);
        }
        return value;
    }

    /**
     * Ensures a value is non-negative (greater than or equal to zero).
     *
     * @param value value to check
     * @param label label for error messages
     * @return the value if non-negative
     * @throws IllegalArgumentException if value is negative
     */
    public static int checkNonNegative(int value, String label) {
        if (value < 0) {
            String l = (label == null || label.isBlank()) ? "value" : label;
            throw new IllegalArgumentException(l + " must be non-negative, got: " + value);
        }
        return value;
    }

    /**
     * Ensures a value is non-negative (greater than or equal to zero).
     *
     * @param value value to check
     * @param label label for error messages
     * @return the value if non-negative
     * @throws IllegalArgumentException if value is negative
     */
    public static long checkNonNegative(long value, String label) {
        if (value < 0) {
            String l = (label == null || label.isBlank()) ? "value" : label;
            throw new IllegalArgumentException(l + " must be non-negative, got: " + value);
        }
        return value;
    }

    /**
     * Ensures an element index is valid for insertion.
     *
     * <p>
     * Valid indices for insertion are [0, size] (inclusive of size for appending).
     * </p>
     *
     * @param index index for insertion
     * @param size current size
     * @param label label for error messages
     * @return the index
     * @throws IndexOutOfBoundsException if index is invalid
     */
    public static int checkElementIndex(int index, int size, String label) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        if (index < 0 || index > size) {
            String l = (label == null || label.isBlank()) ? "index" : label;
            throw new IndexOutOfBoundsException(l + " out of bounds: " + index + " (size=" + size + ")");
        }
        return index;
    }

    /**
     * Ensures no elements in the collection are null.
     *
     * <p>Delegates to {@link Validate#noNullElements(Iterable, String, Object...)}.</p>
     *
     * @param collection collection to check
     * @param label label for error messages
     * @param <T> collection type
     * @return the collection if no nulls present
     * @throws NullPointerException if collection is null or contains null
     */
    public static <T extends Collection<?>> T checkNoNulls(T collection, String label) {
        Validate.noNullElements(collection, "%s contains null element", label);
        return collection;
    }

    /**
     * Ensures no values in the map are null.
     *
     * @param map map to check
     * @param label label for error messages
     * @param <T> map type
     * @return the map if no null values present
     * @throws NullPointerException if map is null or contains null values
     */
    public static <T extends Map<?, ?>> T checkNoNullValues(T map, String label) {
        Objects.requireNonNull(map, label);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getValue() == null) {
                throw new NullPointerException(
                        (label == null || label.isBlank())
                                ? "map"
                                : label + " contains null value for key: " + entry.getKey());
            }
        }
        return map;
    }
}
