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
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for managing {@link Closeable} and {@link AutoCloseable} resources.
 *
 * <p>
 * This class provides safe resource management patterns that handle exceptions gracefully
 * and prevent resource leaks. It focuses on:
 * </p>
 * <ul>
 *   <li>Null-safe closing</li>
 *   <li>Exception suppression</li>
 *   <li>Logging close failures</li>
 *   <li>Multi-resource closing</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While try-with-resources is the preferred pattern for managing closeables, there are
 * cases where explicit closing is needed (e.g., cleaning up collections of resources,
 * optional cleanup in finally blocks, or handling resources created conditionally).
 * This class provides safe patterns for these scenarios.
 * </p>
 *
 * <h2>Exception Handling</h2>
 * <p>
 * Methods in this class never throw exceptions from the {@code close()} operation.
 * Instead, they either:
 * </p>
 * <ul>
 *   <li>Suppress the exception silently (for {@code closeQuietly} variants)</li>
 *   <li>Log the exception (for {@code close} variants)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless. However, the closeables themselves must
 * be thread-safe if accessed concurrently.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * InputStream stream = null;
 * try {
 *     stream = openStream();
 *     // use stream
 * } finally {
 *     Closeables.closeQuietly(stream);
 * }
 * }</pre>
 */
@InternalMarker(reason = "Resource management utilities for core implementation only")
public final class Closeables {

    private static final Logger LOGGER = Logger.getLogger(Closeables.class.getName());

    private Closeables() {
        // utility class
    }

    /**
     * Closes a closeable, suppressing any {@link IOException}.
     *
     * <p>
     * This method is null-safe and will not throw any exceptions. Use this when
     * you don't care about close failures (e.g., in finally blocks).
     * </p>
     *
     * @param closeable closeable to close (may be {@code null})
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                // Suppressed intentionally
            }
        }
    }

    /**
     * Closes an auto-closeable, suppressing any exception.
     *
     * <p>
     * This method is null-safe and will not throw any exceptions. Use this when
     * you don't care about close failures (e.g., in finally blocks).
     * </p>
     *
     * @param closeable closeable to close (may be {@code null})
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                // Suppressed intentionally
            }
        }
    }

    /**
     * Closes a closeable, logging any {@link IOException}.
     *
     * <p>
     * This method is null-safe and will not throw any exceptions. Close failures
     * are logged at WARNING level.
     * </p>
     *
     * @param closeable closeable to close (may be {@code null})
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to close resource: " + closeable, e);
            }
        }
    }

    /**
     * Closes an auto-closeable, logging any exception.
     *
     * <p>
     * This method is null-safe and will not throw any exceptions. Close failures
     * are logged at WARNING level.
     * </p>
     *
     * @param closeable closeable to close (may be {@code null})
     */
    public static void close(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to close resource: " + closeable, e);
            }
        }
    }

    /**
     * Closes a closeable with a custom logger.
     *
     * <p>
     * This method is null-safe and will not throw any exceptions. Close failures
     * are logged to the provided logger at WARNING level.
     * </p>
     *
     * @param closeable closeable to close (may be {@code null})
     * @param logger logger to use (not {@code null})
     */
    public static void close(Closeable closeable, Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Failed to close resource: " + closeable, e);
            }
        }
    }

    /**
     * Closes an auto-closeable with a custom logger.
     *
     * <p>
     * This method is null-safe and will not throw any exceptions. Close failures
     * are logged to the provided logger at WARNING level.
     * </p>
     *
     * @param closeable closeable to close (may be {@code null})
     * @param logger logger to use (not {@code null})
     */
    public static void close(AutoCloseable closeable, Logger logger) {
        Objects.requireNonNull(logger, "logger");
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to close resource: " + closeable, e);
            }
        }
    }

    /**
     * Closes multiple closeables, suppressing exceptions.
     *
     * <p>
     * All closeables are closed even if some fail. Exceptions are suppressed.
     * </p>
     *
     * @param closeables closeables to close (not {@code null}, elements may be {@code null})
     */
    public static void closeAllQuietly(Closeable... closeables) {
        Objects.requireNonNull(closeables, "closeables");
        for (Closeable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Closes multiple auto-closeables, suppressing exceptions.
     *
     * <p>
     * All closeables are closed even if some fail. Exceptions are suppressed.
     * </p>
     *
     * @param closeables closeables to close (not {@code null}, elements may be {@code null})
     */
    public static void closeAllQuietly(AutoCloseable... closeables) {
        Objects.requireNonNull(closeables, "closeables");
        for (AutoCloseable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Closes multiple closeables, logging exceptions.
     *
     * <p>
     * All closeables are closed even if some fail. Exceptions are logged.
     * </p>
     *
     * @param closeables closeables to close (not {@code null}, elements may be {@code null})
     */
    public static void closeAll(Closeable... closeables) {
        Objects.requireNonNull(closeables, "closeables");
        for (Closeable closeable : closeables) {
            close(closeable);
        }
    }

    /**
     * Closes multiple auto-closeables, logging exceptions.
     *
     * <p>
     * All closeables are closed even if some fail. Exceptions are logged.
     * </p>
     *
     * @param closeables closeables to close (not {@code null}, elements may be {@code null})
     */
    public static void closeAll(AutoCloseable... closeables) {
        Objects.requireNonNull(closeables, "closeables");
        for (AutoCloseable closeable : closeables) {
            close(closeable);
        }
    }

    /**
     * Closes an iterable of closeables, suppressing exceptions.
     *
     * <p>
     * All closeables are closed even if some fail. Exceptions are suppressed.
     * </p>
     *
     * @param closeables closeables to close (not {@code null}, elements may be {@code null})
     * @param <T> closeable type
     */
    public static <T extends Closeable> void closeAllQuietly(Iterable<T> closeables) {
        Objects.requireNonNull(closeables, "closeables");
        for (T closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Closes an iterable of closeables, logging exceptions.
     *
     * <p>
     * All closeables are closed even if some fail. Exceptions are logged.
     * </p>
     *
     * @param closeables closeables to close (not {@code null}, elements may be {@code null})
     * @param <T> closeable type
     */
    public static <T extends Closeable> void closeAll(Iterable<T> closeables) {
        Objects.requireNonNull(closeables, "closeables");
        for (T closeable : closeables) {
            close(closeable);
        }
    }
}
