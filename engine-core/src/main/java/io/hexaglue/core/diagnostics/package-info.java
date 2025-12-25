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
 * Core diagnostic and validation infrastructure for HexaGlue.
 *
 * <h2>Overview</h2>
 * <p>
 * This package provides the implementation of HexaGlue's diagnostic system, which is responsible
 * for collecting, validating, rendering, and reporting issues during compilation.
 * </p>
 *
 * <h2>Key Components</h2>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.DiagnosticEngine}</h3>
 * <p>
 * The central orchestrator that:
 * <ul>
 *   <li>Collects diagnostics from core and plugins</li>
 *   <li>Routes diagnostics to the JSR-269 messager</li>
 *   <li>Provides query capabilities</li>
 *   <li>Manages diagnostic lifecycle</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.DiagnosticSink}</h3>
 * <p>
 * Thread-safe collector for diagnostics. Provides:
 * <ul>
 *   <li>Concurrent diagnostic collection</li>
 *   <li>Filtering by severity</li>
 *   <li>Query methods</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.DefaultDiagnosticReporter}</h3>
 * <p>
 * Implementation of the SPI {@link io.hexaglue.spi.diagnostics.DiagnosticReporter} interface.
 * Acts as a bridge between plugins and the internal diagnostic collection mechanism.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.DiagnosticFactory}</h3>
 * <p>
 * Factory for creating common diagnostic patterns. Reduces boilerplate and ensures consistency.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.DiagnosticRenderer}</h3>
 * <p>
 * Formats diagnostics for human consumption. Supports:
 * <ul>
 *   <li>Compact single-line format</li>
 *   <li>Detailed multi-line format</li>
 *   <li>Summary statistics</li>
 * </ul>
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.Locations}</h3>
 * <p>
 * Utility for creating {@link io.hexaglue.spi.diagnostics.DiagnosticLocation} instances from
 * various sources (elements, paths, qualified names).
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.ValidationEngine}</h3>
 * <p>
 * Executes validation plans and collects validation issues. Handles rule execution failures
 * gracefully.
 * </p>
 *
 * <h3>{@link io.hexaglue.core.diagnostics.ValidationPlan}</h3>
 * <p>
 * Immutable plan describing validation rules to execute. Supports composable validation
 * logic.
 * </p>
 *
 * <h2>Design Principles</h2>
 *
 * <h3>Separation of Concerns</h3>
 * <p>
 * The SPI ({@code io.hexaglue.spi.diagnostics}) defines stable, minimal interfaces. This
 * package provides the implementation without exposing internal details to plugins.
 * </p>
 *
 * <h3>Thread Safety</h3>
 * <p>
 * All components are designed for concurrent use. Multiple threads may report diagnostics
 * simultaneously.
 * </p>
 *
 * <h3>Fail-Safe Execution</h3>
 * <p>
 * Validation execution is resilient. If a rule fails, the engine records the error and
 * continues with remaining rules.
 * </p>
 *
 * <h3>Immutability</h3>
 * <p>
 * Data structures ({@link io.hexaglue.core.diagnostics.ValidationPlan}, results) are immutable
 * after construction, ensuring stability during multi-phase compilation.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Initialize during processor setup
 * DiagnosticEngine engine = DiagnosticEngine.create(processingEnv.getMessager());
 *
 * // Pass reporter to plugins
 * DiagnosticReporter reporter = engine.reporter();
 * GenerationContextSpec context = buildContext(reporter, ...);
 *
 * // Plugins report issues
 * reporter.error(DiagnosticCode.of("INVALID_PORT"), "Port must be an interface");
 *
 * // Execute validation
 * ValidationPlan plan = ValidationPlan.builder()
 *     .rule("domain-types", () -> validateDomainTypes())
 *     .build();
 * engine.executeValidation(plan, null);
 *
 * // Flush to compiler
 * engine.flushToMessager();
 *
 * // Check for errors
 * if (engine.hasErrors()) {
 *   // Abort generation
 * }
 * }</pre>
 *
 * <h2>Stability</h2>
 * <p>
 * This is a core-internal package. Classes here are <strong>not</strong> part of the stable
 * SPI and may change between versions. Plugins should only depend on
 * {@code io.hexaglue.spi.diagnostics}.
 * </p>
 */
package io.hexaglue.core.diagnostics;
