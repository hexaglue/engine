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
import io.hexaglue.spi.context.ExecutionMode;
import io.hexaglue.spi.context.GenerationContextSpec;
import io.hexaglue.spi.context.GenerationRequest;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.naming.NameStrategySpec;
import io.hexaglue.spi.options.OptionsView;
import io.hexaglue.spi.types.TypeSystemSpec;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Factory for core context objects.
 *
 * <p>
 * This factory wires core internal services into SPI-facing context objects. It is designed to
 * keep the SPI stable and prevent leakage of compiler internals (JSR-269) into plugins.
 * </p>
 *
 * <p>
 * The created objects are immutable and safe to pass to plugins for the duration of a compilation.
 * </p>
 */
public final class ContextFactory {

    private ContextFactory() {
        // static-only
    }

    /**
     * Creates a {@link BuildEnvironment} for the current compilation.
     *
     * <p>
     * This method is best-effort. Unknown values are returned as empty {@link java.util.Optional}s
     * in the {@link BuildEnvironment} implementation.
     * </p>
     *
     * @param processingEnv processing environment, not {@code null}
     * @param mode execution mode, not {@code null}
     * @param debugEnabled debug flag
     * @param locale effective locale, not {@code null}
     * @param buildTool build tool id (nullable)
     * @param host host id (nullable)
     * @param attributes additional attributes (nullable)
     * @return build environment, never {@code null}
     */
    public static DefaultBuildEnvironment buildEnvironment(
            ProcessingEnvironment processingEnv,
            ExecutionMode mode,
            boolean debugEnabled,
            Locale locale,
            String buildTool,
            String host,
            Map<String, String> attributes) {
        Objects.requireNonNull(processingEnv, "processingEnv");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(locale, "locale");

        if (attributes == null || attributes.isEmpty()) {
            return DefaultBuildEnvironment.fromProcessing(processingEnv, mode, debugEnabled, locale, buildTool, host);
        }
        return DefaultBuildEnvironment.of(mode, debugEnabled, locale, buildTool, host, attributes);
    }

    /**
     * Creates a best-effort {@link GenerationRequest}.
     *
     * <p>
     * The request is informational and must not expose compiler internals. Core should enrich the
     * request with project id, target release and active plugin ids when available.
     * </p>
     *
     * @param processingEnv processing environment, not {@code null}
     * @return request, never {@code null}
     */
    public static DefaultGenerationRequest request(ProcessingEnvironment processingEnv) {
        return DefaultGenerationRequest.fromProcessingEnvironment(processingEnv);
    }

    /**
     * Creates a {@link GenerationRequest} enriched with best-effort metadata.
     *
     * @param base base request (typically from {@link #request(ProcessingEnvironment)}), not {@code null}
     * @param projectId project id (nullable/blank allowed)
     * @param targetJavaRelease target Java release (nullable)
     * @param activePluginIds active plugin ids (nullable)
     * @return enriched request, never {@code null}
     */
    public static DefaultGenerationRequest request(
            DefaultGenerationRequest base, String projectId, Integer targetJavaRelease, Set<String> activePluginIds) {
        Objects.requireNonNull(base, "base");
        DefaultGenerationRequest r = base;
        if (projectId != null && !projectId.trim().isEmpty()) {
            r = r.withProjectId(projectId);
        }
        if (targetJavaRelease != null && targetJavaRelease > 0) {
            r = new DefaultGenerationRequest(
                    r.coreVersion().orElse(null), r.projectId().orElse(null), targetJavaRelease, r.activePluginIds());
        }
        if (activePluginIds != null) {
            r = r.withActivePluginIds(activePluginIds);
        }
        return r;
    }

    /**
     * Creates a {@link GenerationContextSpec} for plugin execution.
     *
     * <p>
     * Callers provide SPI-facing implementations. These are typically adapters backed by core internals.
     * </p>
     *
     * @param names naming strategy, not {@code null}
     * @param ir IR view, not {@code null}
     * @param types type system, not {@code null}
     * @param options options view, not {@code null}
     * @param diagnostics diagnostics reporter, not {@code null}
     * @param output artifact sink, not {@code null}
     * @param environment build environment, not {@code null}
     * @param request request metadata, not {@code null}
     * @return generation context, never {@code null}
     */
    public static DefaultGenerationContext context(
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

        return new DefaultGenerationContext(names, ir, types, options, diagnostics, output, environment, request);
    }

    /**
     * Creates a {@link DebugLog} for the current compilation.
     *
     * @param processingEnv processing environment, not {@code null}
     * @param debugEnabled debug flag
     * @return debug logger, never {@code null}
     */
    public static DebugLog debugLog(ProcessingEnvironment processingEnv, boolean debugEnabled) {
        Objects.requireNonNull(processingEnv, "processingEnv");
        return new DebugLog(processingEnv.getMessager(), debugEnabled, "[HexaGlue]");
    }
}
