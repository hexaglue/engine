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
package io.hexaglue.core.internal.spi;

import io.hexaglue.core.context.DefaultBuildEnvironment;
import io.hexaglue.core.context.DefaultGenerationRequest;
import io.hexaglue.core.diagnostics.DiagnosticEngine;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.IrSnapshot;
import io.hexaglue.core.lifecycle.PluginExecutionPlan;
import io.hexaglue.core.naming.DefaultNameStrategy;
import io.hexaglue.core.types.DefaultTypeSystem;
import io.hexaglue.spi.HexaGlueVersion;
import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.context.BuildEnvironment;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.context.GenerationRequest;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.naming.NameStrategySpec;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Builder that constructs {@link GenerationContextSpec} instances for plugin execution.
 *
 * <p>
 * This builder assembles all the components required by the SPI contract, bridging
 * between HexaGlue's internal models and the stable plugin-facing API.
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Creates SPI views from internal IR models</li>
 *   <li>Instantiates default implementations of SPI specs</li>
 *   <li>Wires diagnostic reporting between core and plugins</li>
 *   <li>Provides file output capabilities via artifact sink</li>
 * </ul>
 *
 * <h2>Design Strategy</h2>
 * <p>
 * The builder uses default implementations for all SPI specs, ensuring consistent
 * behavior across all plugins. More sophisticated implementations can be swapped
 * in later as needed.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This builder is stateless and thread-safe. Each invocation creates a new context.
 * </p>
 */
@InternalMarker(reason = "Internal context builder; not exposed to plugins")
public final class GenerationContextBuilder {

    private GenerationContextBuilder() {
        // Utility class - prevent instantiation
    }

    /**
     * Builds a {@link GenerationContextSpec} for plugin execution.
     *
     * <p>
     * This method creates all the required SPI components and assembles them into
     * an immutable generation context that plugins can use to generate artifacts.
     * </p>
     *
     * @param snapshot IR snapshot from analysis phase (not {@code null})
     * @param diagnosticEngine diagnostic engine for error reporting (not {@code null})
     * @param processingEnv annotation processing environment (not {@code null})
     * @param pluginPlan plugin execution plan (not {@code null})
     * @param artifactSink artifact sink for collecting generated artifacts (not {@code null})
     * @return generation context for plugins (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public static GenerationContextSpec build(
            IrSnapshot snapshot,
            DiagnosticEngine diagnosticEngine,
            ProcessingEnvironment processingEnv,
            PluginExecutionPlan pluginPlan,
            ArtifactSink artifactSink,
            OptionsView resolvedOptions) {

        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(diagnosticEngine, "diagnosticEngine");
        Objects.requireNonNull(processingEnv, "processingEnv");
        Objects.requireNonNull(pluginPlan, "pluginPlan");
        Objects.requireNonNull(artifactSink, "artifactSink");

        // 1. Create IR view from snapshot
        IrView irView = IrViewAdapter.from(snapshot);

        // 2. Get diagnostic reporter from engine
        DiagnosticReporter diagnosticReporter = diagnosticEngine.reporter();

        // 3. Create naming strategy
        NameStrategySpec nameStrategy = DefaultNameStrategy.of("generated"); // Default base package

        // 4. Create type system
        TypeSystemSpec typeSystem =
                DefaultTypeSystem.create(processingEnv.getElementUtils(), processingEnv.getTypeUtils());

        // 5. Use resolved options passed from core (YAML, defaults, etc.)
        OptionsView options = Objects.requireNonNull(resolvedOptions, "resolvedOptions");

        // 6. Create build environment
        BuildEnvironment environment = DefaultBuildEnvironment.fromProcessing(
                processingEnv,
                io.hexaglue.spi.context.ExecutionMode.DEVELOPMENT,
                false, // debugEnabled - can be enhanced later
                Locale.getDefault(),
                "maven", // buildTool
                null // host
                );

        // 7. Create generation request
        GenerationRequest request = createGenerationRequest(pluginPlan);

        // 8. Assemble and return context
        return GenerationContextSpec.of(
                nameStrategy, irView, typeSystem, options, diagnosticReporter, artifactSink, environment, request);
    }

    /**
     * Creates a generation request from the plugin execution plan.
     *
     * @param pluginPlan plugin plan (not {@code null})
     * @return generation request (never {@code null})
     */
    private static GenerationRequest createGenerationRequest(PluginExecutionPlan pluginPlan) {
        // Get HexaGlue version (0.1.0-SNAPSHOT)
        HexaGlueVersion coreVersion = HexaGlueVersion.of(0, 1, 0);

        // Get active plugin IDs
        Set<String> activePluginIds =
                pluginPlan.plugins().stream().map(plugin -> plugin.id()).collect(Collectors.toSet());

        // Create request with default values
        return new DefaultGenerationRequest(
                coreVersion,
                "hexaglue-project", // Default project ID
                17, // Default to Java 17
                activePluginIds);
    }
}
