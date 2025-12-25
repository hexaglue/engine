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
package io.hexaglue.testing;

import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.options.OptionKey;
import io.hexaglue.spi.options.OptionValue;
import io.hexaglue.spi.options.OptionsView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * High-level compilation test helper.
 *
 * <p>This harness compiles a set of in-memory sources with HexaGlue's
 * annotation processor and captures:</p>
 * <ul>
 *   <li>generated sources/resources/docs</li>
 *   <li>JSR-269 compiler diagnostics</li>
 * </ul>
 *
 * <p>Plugins are provided by the test classpath (ServiceLoader) in real usage,
 * but tests may also register plugins explicitly depending on the core bootstrap.</p>
 *
 * <p>Note: The harness is intentionally "black-box": it does not reach into
 * core internals; it only triggers compilation with the processor.</p>
 */
public final class CompilationTestCase {

    private static final String HEXAGLUE_PROCESSOR = "io.hexaglue.core.processor.HexaGlueProcessor";

    private final List<SourceSpec> sources;
    private final List<HexaGluePlugin> plugins;
    private final Map<OptionKey<?>, OptionValue<?>> options;
    private final List<String> javacOptions;

    private CompilationTestCase(Builder b) {
        this.sources = List.copyOf(b.sources);
        this.plugins = List.copyOf(b.plugins);
        this.options = Map.copyOf(b.options);
        this.javacOptions = List.copyOf(b.javacOptions);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Executes an in-memory compilation with HexaGlue enabled.
     */
    public CompilationResult compile() {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "No system Java compiler available. Are you running tests on a JRE instead of a JDK?");
        }

        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        try (StandardJavaFileManager std = compiler.getStandardFileManager(diagnostics, null, null)) {
            MemoryFileManager fm = new MemoryFileManager(std);

            // Prepare source units
            List<JavaFileObject> units = new ArrayList<>(sources.size());
            for (SourceSpec s : sources) {
                units.add(MemoryJavaFileObject.source(s.qualifiedName(), s.source()));
            }

            // Base javac options: enable processor + specify our processor explicitly.
            List<String> opts = new ArrayList<>();
            opts.add("-proc:only"); // we only need processing + generated outputs
            opts.add("-processor");
            opts.add(HEXAGLUE_PROCESSOR);

            // Allow tests to pass extra options (e.g., -Xlint, etc.)
            opts.addAll(javacOptions);

            // IMPORTANT: pass HexaGlue config to the processor.
            // We keep this harness-side API stable; the actual "wire format" (compiler args, etc.)
            // is handled by core's OptionsResolver.
            //
            // Minimal approach for now:
            // - Provide an OptionsView via a well-known mechanism you already have in core,
            //   or keep empty and evolve later.
            //
            // For v1 of the harness, we expose a test helper file (later) and keep OptionsView for assertions.
            OptionsView resolvedOptions = OptionsView.of(options);

            // Build the compilation task
            JavaCompiler.CompilationTask task = compiler.getTask(
                    /* out */ null,
                    /* fileManager */ fm,
                    /* diagnosticListener */ diagnostics,
                    /* options */ opts,
                    /* classes */ null,
                    /* compilationUnits */ units);

            // If core supports processor instances, we could setProcessors here.
            // But you already have a stable processor FQN; using -processor is simplest.
            boolean ok = Boolean.TRUE.equals(task.call());

            return new CompilationResult(ok, diagnostics.getDiagnostics(), fm.snapshot(), resolvedOptions, plugins);
        } catch (Exception e) {
            return CompilationResult.failedWithException(e);
        }
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {
        private final List<SourceSpec> sources = new ArrayList<>();
        private final List<HexaGluePlugin> plugins = new ArrayList<>();
        private final Map<OptionKey<?>, OptionValue<?>> options = new LinkedHashMap<>();
        private final List<String> javacOptions = new ArrayList<>();

        private Builder() {}

        public Builder addSourceFile(String qualifiedName, String source) {
            this.sources.add(new SourceSpec(qualifiedName, source));
            return this;
        }

        /**
         * Registers a plugin instance for tests that want to pass explicit plugins.
         *
         * <p>Whether core uses this depends on your ProcessorBootstrap. This is here
         * because the harness is meant for plugin devs too (unit/integration tests). :contentReference[oaicite:1]{index=1}</p>
         */
        public Builder addPlugin(HexaGluePlugin plugin) {
            if (plugin != null) this.plugins.add(plugin);
            return this;
        }

        public Builder setOption(OptionKey<?> key, OptionValue<?> value) {
            if (key == null) throw new IllegalArgumentException("key must not be null");
            if (value == null) throw new IllegalArgumentException("value must not be null");
            this.options.put(key, value);
            return this;
        }

        public Builder addJavacOption(String option) {
            if (option != null && !option.isBlank()) this.javacOptions.add(option);
            return this;
        }

        public CompilationTestCase build() {
            return new CompilationTestCase(this);
        }

        public CompilationResult compile() {
            return build().compile();
        }
    }

    // -------------------------------------------------------------------------
    // Internal data holder
    // -------------------------------------------------------------------------

    private record SourceSpec(String qualifiedName, String source) {}
}
