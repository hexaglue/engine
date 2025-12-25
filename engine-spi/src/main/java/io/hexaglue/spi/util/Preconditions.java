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

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Small, dependency-free preconditions utility.
 *
 * <p>This is intentionally minimal to keep the SPI stable and avoid external dependencies.
 * Prefer JDK built-ins ({@link Objects#requireNonNull(Object)}) where possible.</p>
 */
public final class Preconditions {

    private Preconditions() {
        // utility class
    }

    /**
     * Ensures a condition is true.
     *
     * @param condition condition to validate
     * @param message error message (non-blank recommended)
     * @throws IllegalArgumentException if condition is false
     */
    public static void checkArgument(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Ensures a condition is true, with a lazy message supplier.
     *
     * @param condition condition to validate
     * @param messageSupplier message supplier (never {@code null})
     * @throws IllegalArgumentException if condition is false
     */
    public static void checkArgument(boolean condition, Supplier<String> messageSupplier) {
        Objects.requireNonNull(messageSupplier, "messageSupplier");
        if (!condition) {
            throw new IllegalArgumentException(messageSupplier.get());
        }
    }

    /**
     * Ensures a state condition is true.
     *
     * @param condition state condition
     * @param message error message
     * @throws IllegalStateException if condition is false
     */
    public static void checkState(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Ensures a state condition is true, with a lazy message supplier.
     *
     * @param condition state condition
     * @param messageSupplier message supplier (never {@code null})
     * @throws IllegalStateException if condition is false
     */
    public static void checkState(boolean condition, Supplier<String> messageSupplier) {
        Objects.requireNonNull(messageSupplier, "messageSupplier");
        if (!condition) {
            throw new IllegalStateException(messageSupplier.get());
        }
    }

    /**
     * Ensures an index is within {@code [0, size)}.
     *
     * @param index index
     * @param size size (must be >= 0)
     * @param label label for error messages
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public static void checkIndex(int index, int size, String label) {
        if (size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        if (index < 0 || index >= size) {
            String l = (label == null || label.isBlank()) ? "index" : label;
            throw new IndexOutOfBoundsException(l + " out of bounds: " + index + " (size=" + size + ")");
        }
    }

    /**
     * Ensures a string is not null and not blank.
     *
     * @param value input value
     * @param label label for error messages
     * @return trimmed value
     * @throws NullPointerException if value is null
     * @throws IllegalArgumentException if value is blank
     */
    public static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String t = value.trim();
        if (t.isEmpty()) {
            throw new IllegalArgumentException(
                    (label == null || label.isBlank()) ? "value" : label + " must not be blank");
        }
        return t;
    }
}
