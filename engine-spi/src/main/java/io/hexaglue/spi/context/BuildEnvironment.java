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
package io.hexaglue.spi.context;

import io.hexaglue.spi.stability.Evolvable;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes the build and runtime environment in which HexaGlue is executing.
 *
 * <p>This is an informational, stable view intended for plugins. It must not expose
 * compiler internals (e.g., annotation processing APIs) directly.</p>
 *
 * <p>All values are optional unless explicitly documented otherwise.</p>
 *
 * <p>This SPI type is deliberately minimal and JDK-only.</p>
 */
@Evolvable(since = "1.0.0")
public interface BuildEnvironment {

    /**
     * Returns the execution mode (development, CI, release).
     *
     * @return execution mode (never {@code null})
     */
    ExecutionMode mode();

    /**
     * Returns whether debug mode is enabled for this compilation.
     *
     * <p>Debug mode is intended for additional logs and diagnostics to help plugin authors
     * troubleshoot generation. Plugins should avoid generating different artifacts solely
     * based on debug mode.</p>
     *
     * @return {@code true} if debug mode is enabled
     */
    boolean isDebugEnabled();

    /**
     * Returns the effective locale used by the compiler for messages and formatting.
     *
     * <p>Plugins should not assume this is the user's locale, only that it is the compiler's
     * effective locale for this run.</p>
     *
     * @return locale (never {@code null})
     */
    Locale locale();

    /**
     * Returns a stable identifier for the build tool, if known.
     *
     * <p>Examples: {@code "maven"}, {@code "gradle"}, {@code "bazel"}.</p>
     *
     * @return build tool id, if known
     */
    Optional<String> buildTool();

    /**
     * Returns a stable identifier for the host environment, if known.
     *
     * <p>Examples: {@code "idea"}, {@code "eclipse"}, {@code "cli"}, {@code "daemon"}.</p>
     *
     * @return host id, if known
     */
    Optional<String> host();

    /**
     * Returns arbitrary environment attributes provided by the compiler.
     *
     * <p>This is intended for forward-compatible extensions. Keys must be stable and
     * documented by the compiler.</p>
     *
     * @return immutable map of attributes (never {@code null})
     */
    Map<String, String> attributes();

    /**
     * Creates a simple immutable {@link BuildEnvironment} instance.
     *
     * <p>This factory is provided for tests and lightweight tooling.</p>
     *
     * @param mode execution mode
     * @param debugEnabled debug flag
     * @param locale effective locale
     * @param buildTool build tool id (nullable)
     * @param host host id (nullable)
     * @param attributes attributes map (nullable)
     * @return immutable build environment
     */
    static BuildEnvironment of(
            ExecutionMode mode,
            boolean debugEnabled,
            Locale locale,
            String buildTool,
            String host,
            Map<String, String> attributes) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(locale, "locale");

        final Map<String, String> attrs = (attributes == null) ? Map.of() : Map.copyOf(attributes);
        final Optional<String> bt =
                Optional.ofNullable(buildTool).map(String::trim).filter(s -> !s.isEmpty());
        final Optional<String> h = Optional.ofNullable(host).map(String::trim).filter(s -> !s.isEmpty());

        return new BuildEnvironment() {
            @Override
            public ExecutionMode mode() {
                return mode;
            }

            @Override
            public boolean isDebugEnabled() {
                return debugEnabled;
            }

            @Override
            public Locale locale() {
                return locale;
            }

            @Override
            public Optional<String> buildTool() {
                return bt;
            }

            @Override
            public Optional<String> host() {
                return h;
            }

            @Override
            public Map<String, String> attributes() {
                return attrs;
            }
        };
    }
}
