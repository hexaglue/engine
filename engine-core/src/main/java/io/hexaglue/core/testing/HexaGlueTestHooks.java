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
package io.hexaglue.core.testing;

import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.options.OptionsView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Test-only hook used by hexaglue-testing-harness to inject dependencies into the compiler.
 *
 * <p>Rationale: the real processor is discovered/bootstrapped by javac; in tests we want to
 * provide plugins, options and in-memory sinks without touching ServiceLoader, filesystem or yaml.</p>
 *
 * <p>Implementation note: uses a ThreadLocal to keep test execution isolated.</p>
 */
public final class HexaGlueTestHooks {

    private HexaGlueTestHooks() {}

    private static final ThreadLocal<Overrides> TL = new ThreadLocal<>();

    public static Optional<Overrides> current() {
        return Optional.ofNullable(TL.get());
    }

    public static Scope install(Overrides overrides) {
        Objects.requireNonNull(overrides, "overrides");
        TL.set(overrides);
        return new Scope();
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed = false;

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            TL.remove();
        }
    }

    /**
     * What tests can override/inject into core execution.
     */
    public record Overrides(
            List<HexaGluePlugin> plugins, OptionsView options, DiagnosticReporter diagnostics, ArtifactSink output) {
        public Overrides {
            Objects.requireNonNull(plugins, "plugins");
            Objects.requireNonNull(options, "options");
            Objects.requireNonNull(diagnostics, "diagnostics");
            Objects.requireNonNull(output, "output");
        }
    }
}
