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
package io.hexaglue.core.context;

import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.context.BuildEnvironment;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.context.GenerationRequest;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.naming.NameStrategySpec;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.Objects;

/**
 * Default {@link GenerationContextSpec} implementation provided by HexaGlue core.
 *
 * <p>
 * This class is a thin, immutable adapter that wires together SPI-facing views produced
 * by the core (IR, type system, naming, options, diagnostics, outputs, environment).
 * </p>
 *
 * <p>
 * All intelligence (resolution, indexing, policies, validation, generation strategy)
 * remains in core internals. This type is intentionally passive and stable.
 * </p>
 *
 * <p>
 * This class must not expose compiler internals (e.g. JSR-269 APIs).
 * </p>
 */
public final class DefaultGenerationContext implements GenerationContextSpec {

    private final NameStrategySpec names;
    private final IrView ir;
    private final TypeSystemSpec types;
    private final OptionsView options;
    private final DiagnosticReporter diagnostics;
    private final ArtifactSink output;
    private final BuildEnvironment environment;
    private final GenerationRequest request;

    /**
     * Creates a generation context.
     *
     * @param names naming strategy, not {@code null}
     * @param ir read-only IR view, not {@code null}
     * @param types type system, not {@code null}
     * @param options options view, not {@code null}
     * @param diagnostics diagnostic reporter, not {@code null}
     * @param output artifact sink, not {@code null}
     * @param environment build environment, not {@code null}
     * @param request generation request metadata, not {@code null}
     */
    public DefaultGenerationContext(
            NameStrategySpec names,
            IrView ir,
            TypeSystemSpec types,
            OptionsView options,
            DiagnosticReporter diagnostics,
            ArtifactSink output,
            BuildEnvironment environment,
            GenerationRequest request) {
        this.names = Objects.requireNonNull(names, "names");
        this.ir = Objects.requireNonNull(ir, "ir");
        this.types = Objects.requireNonNull(types, "types");
        this.options = Objects.requireNonNull(options, "options");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.output = Objects.requireNonNull(output, "output");
        this.environment = Objects.requireNonNull(environment, "environment");
        this.request = Objects.requireNonNull(request, "request");
    }

    @Override
    public NameStrategySpec names() {
        return names;
    }

    @Override
    public IrView model() {
        return ir;
    }

    @Override
    public TypeSystemSpec types() {
        return types;
    }

    @Override
    public OptionsView options() {
        return options;
    }

    @Override
    public DiagnosticReporter diagnostics() {
        return diagnostics;
    }

    @Override
    public ArtifactSink output() {
        return output;
    }

    @Override
    public BuildEnvironment environment() {
        return environment;
    }

    @Override
    public GenerationRequest request() {
        return request;
    }

    @Override
    public String toString() {
        return "DefaultGenerationContext{env=" + environment + ", request=" + request + "}";
    }
}
