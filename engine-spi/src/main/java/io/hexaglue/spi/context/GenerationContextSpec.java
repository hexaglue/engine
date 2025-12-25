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

import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.naming.NameStrategySpec;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.stability.Stable;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.Objects;

/**
 * Root SPI entry-point provided to plugins for a single compilation run.
 *
 * <p>This interface is the only supported way for plugins to access:
 * <ul>
 *   <li>read-only IR views (domain, ports, etc.)</li>
 *   <li>type information</li>
 *   <li>naming conventions</li>
 *   <li>options/configuration</li>
 *   <li>diagnostics reporting</li>
 *   <li>artifact output (sources/resources/docs)</li>
 * </ul>
 *
 * <p><strong>Stability rule:</strong> This interface must remain stable across versions.
 * New capabilities should be added using new methods with default implementations when possible,
 * or by extending nested views rather than breaking signatures.</p>
 *
 * <p>Implementation notes:
 * <ul>
 *   <li>Provided by HexaGlue core.</li>
 *   <li>Must not leak compiler internals (e.g., JSR-269 types) through the SPI.</li>
 * </ul>
 */
@Stable(since = "1.0.0")
public interface GenerationContextSpec {

    /**
     * Naming conventions shared across plugins.
     *
     * @return naming strategy view (never {@code null})
     */
    NameStrategySpec names();

    /**
     * Read-only access to the analyzed architecture model.
     *
     * <p>This provides access to the domain model, ports, and application services
     * discovered during compilation.</p>
     *
     * @return architecture model view (never {@code null})
     * @since 0.2.0
     */
    IrView model();

    /**
     * Type system access used by plugins for consistent code generation.
     *
     * @return type system (never {@code null})
     */
    TypeSystemSpec types();

    /**
     * Configuration options (global and per-plugin).
     *
     * @return options view (never {@code null})
     */
    OptionsView options();

    /**
     * Diagnostics reporting API.
     *
     * <p>Plugins should prefer reporting diagnostics over throwing exceptions for user-caused errors.</p>
     *
     * @return diagnostic reporter (never {@code null})
     */
    DiagnosticReporter diagnostics();

    /**
     * Artifact output sink used to write generated sources, resources and documentation.
     *
     * @return artifact sink (never {@code null})
     */
    ArtifactSink output();

    /**
     * Build and host environment information.
     *
     * @return build environment (never {@code null})
     */
    BuildEnvironment environment();

    /**
     * High-level request metadata (project id, target Java release, etc.).
     *
     * @return generation request (never {@code null})
     */
    GenerationRequest request();

    /**
     * Convenience factory for building a simple {@link GenerationContextSpec} instance.
     *
     * <p>This is intended for tests and lightweight tooling. HexaGlue core is free to provide
     * richer implementations.</p>
     *
     * @param names naming view
     * @param ir architecture model view (returned by {@link #model()})
     * @param types type system
     * @param options options view
     * @param diagnostics diagnostics reporter
     * @param output artifact sink
     * @param environment build environment
     * @param request request metadata
     * @return immutable context
     */
    static GenerationContextSpec of(
            NameStrategySpec names,
            IrView ir,
            TypeSystemSpec types,
            OptionsView options,
            DiagnosticReporter diagnostics,
            ArtifactSink output,
            BuildEnvironment environment,
            GenerationRequest request) {
        Objects.requireNonNull(names, "names");
        Objects.requireNonNull(ir, "ir");
        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(diagnostics, "diagnostics");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(request, "request");

        return new GenerationContextSpec() {
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
        };
    }
}
