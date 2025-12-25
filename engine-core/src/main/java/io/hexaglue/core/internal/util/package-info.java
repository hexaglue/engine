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
/**
 * Internal utility classes for the HexaGlue core implementation.
 *
 * <p>
 * This package provides a comprehensive set of utility classes that wrap and extend
 * functionality from Apache Commons Collections, Apache Commons Lang, and the JDK.
 * These utilities are designed exclusively for internal use within hexaglue-core and
 * are <strong>not part of the public API</strong>.
 * </p>
 *
 * <h2>Package Organization</h2>
 *
 * <h3>Validation and Preconditions</h3>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.util.Preconditions} - Extended argument and state validation</li>
 * </ul>
 *
 * <h3>String Manipulation</h3>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.util.Strings} - Extended string operations and transformations</li>
 * </ul>
 *
 * <h3>Collection Utilities</h3>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.util.Collections2} - Collection operations wrapping Apache Commons</li>
 *   <li>{@link io.hexaglue.core.internal.util.Iterables2} - Iterable operations for lazy sequences</li>
 *   <li>{@link io.hexaglue.core.internal.util.Maps2} - Map creation, transformation, and manipulation</li>
 * </ul>
 *
 * <h3>Stream Processing</h3>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.util.Streams2} - Stream creation and terminal operation shortcuts</li>
 * </ul>
 *
 * <h3>Resource Management</h3>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.util.Closeables} - Safe resource closing patterns</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 *
 * <h3>1. Wrapping External Dependencies</h3>
 * <p>
 * Classes in this package wrap Apache Commons functionality to:
 * </p>
 * <ul>
 *   <li>Provide a consistent, simplified API</li>
 *   <li>Add project-specific conveniences</li>
 *   <li>Isolate external dependencies (making future migrations easier)</li>
 *   <li>Add null-safety and validation where appropriate</li>
 * </ul>
 *
 * <h3>2. Null Safety</h3>
 * <p>
 * All utility methods perform null checks on required parameters and throw
 * {@link NullPointerException} with descriptive messages. Methods that accept
 * nullable parameters are explicitly documented.
 * </p>
 *
 * <h3>3. Immutability</h3>
 * <p>
 * Methods that return immutable collections are prefixed with {@code immutable}.
 * These collections are truly unmodifiable - attempts to modify them will throw
 * {@link UnsupportedOperationException}.
 * </p>
 *
 * <h3>4. Fail-Fast Behavior</h3>
 * <p>
 * Utilities validate arguments eagerly and fail fast with clear error messages.
 * This helps catch bugs early in development.
 * </p>
 *
 * <h3>5. Thread Safety</h3>
 * <p>
 * All utility classes are stateless and thread-safe. However, the objects they
 * operate on must be thread-safe if used concurrently.
 * </p>
 *
 * <h2>Relationship to SPI Utilities</h2>
 *
 * <p>
 * The {@code hexaglue-spi} module provides minimal utilities ({@code Preconditions},
 * {@code Strings}) to avoid external dependencies. This package extends those
 * utilities for core implementation needs:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.util.Preconditions} delegates to
 *       {@link io.hexaglue.spi.util.Preconditions} and adds collection/range checks</li>
 *   <li>{@link io.hexaglue.core.internal.util.Strings} delegates to
 *       {@link io.hexaglue.spi.util.Strings} and adds advanced operations</li>
 * </ul>
 *
 * <h2>External Dependencies</h2>
 *
 * <p>
 * This package depends on:
 * </p>
 * <ul>
 *   <li>Apache Commons Collections 4 - for collection utilities</li>
 *   <li>Apache Commons Lang 3 - for general utilities</li>
 *   <li>JDK 17+ - for streams, optionals, and functional interfaces</li>
 * </ul>
 *
 * <h2>Usage Guidelines</h2>
 *
 * <h3>For Core Developers</h3>
 * <ul>
 *   <li>Use these utilities instead of duplicating common patterns</li>
 *   <li>Prefer utilities over direct Apache Commons calls for consistency</li>
 *   <li>Add new utilities here when patterns are repeated 3+ times</li>
 *   <li>Document all new utilities with Javadoc and usage examples</li>
 * </ul>
 *
 * <h3>For Plugin Developers</h3>
 * <ul>
 *   <li><strong>DO NOT USE</strong> - These utilities are internal only</li>
 *   <li>Use {@code hexaglue-spi} utilities instead</li>
 *   <li>Or depend on Apache Commons directly in your plugin</li>
 * </ul>
 *
 * <h2>Stability</h2>
 *
 * <p>
 * Classes in this package are <strong>not</strong> part of the public API and may
 * change in any release without notice. The {@link io.hexaglue.core.internal.InternalMarker}
 * annotation marks all classes as internal.
 * </p>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal utility package for core implementation only")
package io.hexaglue.core.internal.util;
