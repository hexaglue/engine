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
 * Internal utilities for the HexaGlue testing harness.
 *
 * <p><b>Warning:</b> This package contains internal implementation details that may change
 * without notice. Do not depend on these types in external code.</p>
 *
 * <h2>Contents</h2>
 *
 * <ul>
 *   <li>{@link io.hexaglue.testing.internal.InMemoryArtifactSink} - In-memory implementation
 *       of {@link io.hexaglue.spi.codegen.ArtifactSink} for hook-based testing (Mode B)</li>
 * </ul>
 *
 * <h2>Hook-based Testing (Mode B - Future)</h2>
 *
 * <p>The {@code InMemoryArtifactSink} enables a future testing mode where HexaGlue core
 * can be configured to write outputs to an injected sink rather than the compiler's
 * {@code Filer}. This provides:</p>
 *
 * <ul>
 *   <li>Direct access to HexaGlue-native artifacts ({@code SourceFile}, {@code DocFile},
 *       {@code ResourceFile})</li>
 *   <li>Typed diagnostic assertions using {@link io.hexaglue.spi.diagnostics.Diagnostic}</li>
 *   <li>Better plugin unit testing ergonomics</li>
 * </ul>
 *
 * <p><b>Current Status:</b> Mode B requires core integration support via
 * {@code HexaGlueTestHooks} which is not yet implemented. The components in this
 * package serve as scaffolding for future functionality.</p>
 *
 * @see io.hexaglue.testing
 */
package io.hexaglue.testing.internal;
