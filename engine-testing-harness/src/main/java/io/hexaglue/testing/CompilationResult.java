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
import io.hexaglue.spi.options.OptionsView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Result of a compilation test.
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>overall success</li>
 *   <li>javac diagnostics (including annotation processing messages)</li>
 *   <li>captured generated files (sources/resources) produced through Filer</li>
 * </ul>
 *
 * <p>Note: HexaGlue structured diagnostics (HG-xxx) will appear in javac diagnostics
 * if routed through {@code Messager}. If you also want them as typed SPI diagnostics,
 * we will add an optional capture hook once core exposes it.</p>
 */
public final class CompilationResult {

    private final boolean successful;
    private final List<Diagnostic<? extends JavaFileObject>> javacDiagnostics;
    private final MemoryFileManager.GeneratedFilesSnapshot generated;
    private final OptionsView options;
    private final List<HexaGluePlugin> explicitPlugins; // stored for future usage / debugging
    private final Exception exception;

    CompilationResult(
            boolean successful,
            List<Diagnostic<? extends JavaFileObject>> javacDiagnostics,
            MemoryFileManager.GeneratedFilesSnapshot generated,
            OptionsView options,
            List<HexaGluePlugin> explicitPlugins) {
        this.successful = successful;
        this.javacDiagnostics = Collections.unmodifiableList(new ArrayList<>(javacDiagnostics));
        this.generated = generated;
        this.options = options;
        this.explicitPlugins = Collections.unmodifiableList(new ArrayList<>(explicitPlugins));
        this.exception = null;
    }

    private CompilationResult(Exception exception) {
        this.successful = false;
        this.javacDiagnostics = List.of();
        this.generated = new MemoryFileManager.GeneratedFilesSnapshot(java.util.Map.of(), java.util.Map.of());
        this.options = OptionsView.of(java.util.Map.of());
        this.explicitPlugins = List.of();
        this.exception = exception;
    }

    public static CompilationResult failedWithException(Exception e) {
        return new CompilationResult(e);
    }

    public boolean wasSuccessful() {
        return successful;
    }

    /**
     * Raw javac diagnostics (errors, warnings, notes) including annotation processing messages.
     */
    public List<Diagnostic<? extends JavaFileObject>> javacDiagnostics() {
        return javacDiagnostics;
    }

    /**
     * Convenience: returns all diagnostics formatted (kind + message).
     */
    public List<String> formattedDiagnostics() {
        List<String> out = new ArrayList<>(javacDiagnostics.size());
        for (var d : javacDiagnostics) {
            out.add("[" + d.getKind() + "] " + d.getMessage(null));
        }
        return out;
    }

    /**
     * Returns the generated source content for a qualified name (e.g. {@code com.example.Foo}).
     */
    public Optional<String> generatedSourceFile(String qualifiedTypeName) {
        return generated.findSource(qualifiedTypeName);
    }

    /**
     * Returns generated resource bytes by snapshot key.
     *
     * <p>Key format (best effort): {@code <LOCATION>/<packagePath>/<relativeName>}</p>
     * Example: {@code SOURCE_OUTPUT/docs/ports/CustomerRepository.md}</p>
     */
    public Optional<byte[]> generatedResourceBytes(String pathKey) {
        return generated.findResourceBytes(pathKey);
    }

    /**
     * Returns generated resource text (UTF-8) by snapshot key.
     */
    public Optional<String> generatedResourceText(String pathKey) {
        return generated.findResourceText(pathKey);
    }

    public OptionsView options() {
        return options;
    }

    /**
     * Plugins explicitly registered in the test case (may or may not be used depending on core bootstrap).
     */
    public List<HexaGluePlugin> explicitPlugins() {
        return explicitPlugins;
    }

    /**
     * If the harness itself failed (unexpected exception), this is non-empty.
     */
    public Optional<Exception> exception() {
        return Optional.ofNullable(exception);
    }
}
