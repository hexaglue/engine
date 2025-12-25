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
package io.hexaglue.core;

import java.util.Objects;

/**
 * Entry point for executing a HexaGlue compilation.
 *
 * <p>
 * This class is a small, stable facade over the internal compilation pipeline.
 * It is <strong>not</strong> intended as a plugin API. Plugins must depend on
 * {@code io.hexaglue.spi} and are executed by the annotation processor.
 * </p>
 *
 * <h2>Usage</h2>
 * <p>
 * In normal operation, HexaGlue is invoked by its JSR-269 annotation processor and
 * users do not call this class directly.
 * </p>
 *
 * <p>
 * This facade exists for internal orchestration and for testing harnesses that may
 * want to drive the compilation pipeline in a controlled way.
 * </p>
 *
 * <h2>Thread-safety</h2>
 * <p>
 * Instances are immutable and thread-safe, but the actual compilation is not required to be
 * concurrent. In typical annotation processing usage, compilation happens on a single thread.
 * </p>
 */
public final class HexaGlueCompiler {

    private final HexaGlueCompilerEngine engine;

    /**
     * Creates a compiler using the default internal engine.
     */
    public HexaGlueCompiler() {
        this.engine = new DefaultHexaGlueCompilerEngine();
    }

    /**
     * Creates a compiler using a provided internal engine.
     *
     * <p>
     * This constructor is intended for tests and internal wiring.
     * </p>
     *
     * @param engine the engine to use, not {@code null}
     */
    public HexaGlueCompiler(HexaGlueCompilerEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    /**
     * Executes a compilation based on the given request.
     *
     * <p>
     * The exact request type is internal to core; external callers should prefer the
     * annotation processor integration.
     * </p>
     *
     * @param request the compilation request, not {@code null}
     * @return the compilation result, never {@code null}
     * @throws IllegalArgumentException if the request is invalid
     * @throws HexaGlueCompilationException if compilation fails
     */
    public HexaGlueCompilationResult compile(HexaGlueCompilationRequest request) {
        Objects.requireNonNull(request, "request");
        return engine.compile(request);
    }

    /**
     * Internal engine abstraction.
     *
     * <p>
     * Kept package-private by design. It is not part of any public API contract.
     * </p>
     */
    interface HexaGlueCompilerEngine {

        /**
         * Executes the compilation.
         *
         * @param request the request, not {@code null}
         * @return the result, never {@code null}
         */
        HexaGlueCompilationResult compile(HexaGlueCompilationRequest request);
    }

    /**
     * Default internal engine implementation.
     */
    static final class DefaultHexaGlueCompilerEngine implements HexaGlueCompilerEngine {

        @Override
        public HexaGlueCompilationResult compile(HexaGlueCompilationRequest request) {
            // NOTE: This is intentionally a small facade.
            // The real pipeline lives in internal packages (lifecycle, diagnostics, IR, codegen).
            // Wiring will be implemented in the core module without leaking internals here.
            return HexaGlueCompilationResult.success();
        }
    }

    /**
     * Internal compilation request.
     *
     * <p>
     * This is a placeholder for the internal request model (e.g. ProcessingEnvironment,
     * compilation options, discovered plugins, etc.). It is package-private to avoid
     * accidental coupling from outside core.
     * </p>
     */
    interface HexaGlueCompilationRequest {
        // intentionally empty in the facade
    }

    /**
     * Internal compilation result.
     *
     * <p>
     * Kept minimal to allow tests to assert coarse success/failure without exposing internals.
     * </p>
     */
    public static final class HexaGlueCompilationResult {

        private static final HexaGlueCompilationResult SUCCESS = new HexaGlueCompilationResult(true);

        private final boolean success;

        private HexaGlueCompilationResult(boolean success) {
            this.success = success;
        }

        /**
         * Returns whether the compilation succeeded.
         *
         * @return {@code true} if successful
         */
        public boolean isSuccess() {
            return success;
        }

        /**
         * Returns a successful result instance.
         *
         * @return a success result, never {@code null}
         */
        public static HexaGlueCompilationResult success() {
            return SUCCESS;
        }
    }

    /**
     * Thrown when a compilation fails for a reason that cannot be represented as a diagnostic alone.
     */
    public static final class HexaGlueCompilationException extends RuntimeException {

        /**
         * Creates an exception with a message.
         *
         * @param message the message, not {@code null}
         */
        public HexaGlueCompilationException(String message) {
            super(Objects.requireNonNull(message, "message"));
        }

        /**
         * Creates an exception with a message and cause.
         *
         * @param message the message, not {@code null}
         * @param cause the cause, may be {@code null}
         */
        public HexaGlueCompilationException(String message, Throwable cause) {
            super(Objects.requireNonNull(message, "message"), cause);
        }
    }
}
