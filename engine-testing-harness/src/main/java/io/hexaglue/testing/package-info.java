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
 * Compile-time testing toolkit for HexaGlue.
 *
 * <p>This package provides utilities for testing annotation processors, particularly HexaGlue
 * and its plugins, through in-memory compilation without filesystem dependencies.</p>
 *
 * <h2>Core Components</h2>
 *
 * <ul>
 *   <li>{@link io.hexaglue.testing.CompilationTestCase} - Main entry point for writing
 *       compilation tests using a fluent builder API</li>
 *   <li>{@link io.hexaglue.testing.CompilationResult} - Immutable view of compilation outcomes,
 *       including success status, diagnostics, and generated files</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>{@code
 * CompilationResult result = CompilationTestCase.builder()
 *     .addSourceFile("com.example.port.CustomerRepository", """
 *         package com.example.port;
 *         public interface CustomerRepository {
 *             void save(Customer customer);
 *         }
 *         """)
 *     .addJavacOption("-Xlint:all")
 *     .compile();
 *
 * assertThat(result.wasSuccessful()).isTrue();
 * assertThat(result.generatedSourceFile("com.example.adapters.CustomerRepositoryImpl"))
 *     .isPresent();
 * }</pre>
 *
 * <h2>Design Principles</h2>
 *
 * <ul>
 *   <li><b>Black-box testing:</b> Tests validate processor behavior as seen by users at
 *       compile-time, not internal implementation details</li>
 *   <li><b>In-memory execution:</b> No filesystem I/O required; sources and outputs are
 *       handled entirely in memory</li>
 *   <li><b>Deterministic:</b> Tests produce consistent results across platforms and
 *       build environments</li>
 * </ul>
 *
 * <h2>Testing Modes</h2>
 *
 * <p><b>Mode A (Current): Filer Capture</b></p>
 * <p>The harness intercepts {@code javax.annotation.processing.Filer} outputs through
 * a custom {@code JavaFileManager}. This mode provides realistic end-to-end testing
 * as experienced by users.</p>
 *
 * <p><b>Mode B (Future): Hook-based HexaGlue-native Capture</b></p>
 * <p>Will provide direct access to HexaGlue SPI artifacts ({@code SourceFile},
 * {@code DocFile}, {@code ResourceFile}) and structured diagnostics. Requires
 * core integration support.</p>
 *
 * @see io.hexaglue.testing.CompilationTestCase
 * @see io.hexaglue.testing.CompilationResult
 */
package io.hexaglue.testing;
