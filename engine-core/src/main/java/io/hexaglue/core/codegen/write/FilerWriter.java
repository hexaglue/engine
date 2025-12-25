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
package io.hexaglue.core.codegen.write;

import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Objects;
import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

/**
 * Low-level abstraction for writing files through the JSR-269 {@link Filer}.
 *
 * <p>
 * This class provides a clean interface to the annotation processing Filer API,
 * handling resource location resolution, encoding, error reporting, and I/O operations.
 * It is used by specialized writers ({@link SourceWriter}, {@link ResourceWriter},
 * {@link DocWriter}) to perform actual file system operations.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Centralizing Filer access enables:
 * </p>
 * <ul>
 *   <li>Consistent error handling across all file types</li>
 *   <li>Unified diagnostic reporting for I/O failures</li>
 *   <li>Testable file writing logic without Filer dependencies</li>
 *   <li>Clear separation between business logic and JSR-269 API</li>
 * </ul>
 *
 * <h2>Filer Locations</h2>
 * <p>
 * The writer supports standard JSR-269 output locations:
 * </p>
 * <ul>
 *   <li><strong>SOURCE_OUTPUT:</strong> Generated source files ({@code .java})</li>
 *   <li><strong>CLASS_OUTPUT:</strong> Generated resources and documentation</li>
 *   <li><strong>NATIVE_HEADER_OUTPUT:</strong> Native headers (rarely used)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * All I/O operations report errors through {@link DiagnosticReporter}:
 * </p>
 * <ul>
 *   <li>File creation failures (permissions, disk space)</li>
 *   <li>Write errors (encoding issues, stream failures)</li>
 *   <li>Filer-specific constraints (duplicate file names)</li>
 * </ul>
 *
 * <h2>Encoding</h2>
 * <p>
 * Text files are written with configurable charset (defaults to UTF-8).
 * Binary files use raw byte streams without encoding.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and thread-safe, but the underlying {@link Filer}
 * is not thread-safe. Callers must ensure single-threaded access to Filer.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * FilerWriter filerWriter = new FilerWriter(filer, diagnostics);
 *
 * // Write a source file
 * boolean success = filerWriter.writeSource(
 *     "com.example.Foo",
 *     "public class Foo {}",
 *     StandardCharsets.UTF_8
 * );
 *
 * // Write a resource file
 * success = filerWriter.writeResource(
 *     StandardLocation.CLASS_OUTPUT,
 *     "",
 *     "config/app.properties",
 *     "key=value".getBytes(StandardCharsets.UTF_8)
 * );
 * }</pre>
 */
public final class FilerWriter {

    private static final DiagnosticCode CODE_SOURCE_WRITE_FAILED = DiagnosticCode.of("HG-WRITE-200");
    private static final DiagnosticCode CODE_RESOURCE_WRITE_FAILED = DiagnosticCode.of("HG-WRITE-201");

    private final Filer filer;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a new filer writer.
     *
     * @param filer JSR-269 filer for file creation (not {@code null})
     * @param diagnostics diagnostic reporter for errors (not {@code null})
     */
    public FilerWriter(Filer filer, DiagnosticReporter diagnostics) {
        this.filer = Objects.requireNonNull(filer, "filer");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Writes a Java source file.
     *
     * @param qualifiedTypeName qualified type name (not {@code null})
     * @param content source content (not {@code null})
     * @param charset character encoding (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean writeSource(String qualifiedTypeName, String content, Charset charset) {
        Objects.requireNonNull(qualifiedTypeName, "qualifiedTypeName");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(charset, "charset");

        try {
            JavaFileObject jfo = filer.createSourceFile(qualifiedTypeName);

            try (Writer writer = jfo.openWriter()) {
                writer.write(content);
            }

            return true;

        } catch (IOException e) {
            reportSourceError(qualifiedTypeName, e);
            return false;
        }
    }

    /**
     * Writes a text resource file.
     *
     * @param location output location (not {@code null})
     * @param packageName package name (empty string for default package, not {@code null})
     * @param relativeName relative file name (not {@code null})
     * @param content text content (not {@code null})
     * @param charset character encoding (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean writeTextResource(
            StandardLocation location, String packageName, String relativeName, String content, Charset charset) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(relativeName, "relativeName");
        Objects.requireNonNull(content, "content");
        Objects.requireNonNull(charset, "charset");

        try {
            FileObject fo = filer.createResource(location, packageName, relativeName);

            try (Writer writer = fo.openWriter()) {
                writer.write(content);
            }

            return true;

        } catch (IOException e) {
            reportResourceError(relativeName, e);
            return false;
        }
    }

    /**
     * Writes a binary resource file.
     *
     * @param location output location (not {@code null})
     * @param packageName package name (empty string for default package, not {@code null})
     * @param relativeName relative file name (not {@code null})
     * @param bytes binary content (not {@code null})
     * @return {@code true} if write succeeded, {@code false} if error occurred
     */
    public boolean writeBinaryResource(
            StandardLocation location, String packageName, String relativeName, byte[] bytes) {
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(relativeName, "relativeName");
        Objects.requireNonNull(bytes, "bytes");

        try {
            FileObject fo = filer.createResource(location, packageName, relativeName);

            try (OutputStream os = fo.openOutputStream()) {
                os.write(bytes);
            }

            return true;

        } catch (IOException e) {
            reportResourceError(relativeName, e);
            return false;
        }
    }

    /**
     * Returns the underlying filer.
     *
     * <p>
     * Exposed for advanced use cases that need direct Filer access.
     * Most code should use the type-safe write methods instead.
     * </p>
     *
     * @return filer (never {@code null})
     */
    public Filer filer() {
        return filer;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Error Reporting
    // ─────────────────────────────────────────────────────────────────────────

    private void reportSourceError(String qualifiedTypeName, IOException cause) {
        diagnostics.report(Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(CODE_SOURCE_WRITE_FAILED)
                .message("Failed to write source file '" + qualifiedTypeName + "': " + cause.getMessage())
                .location(DiagnosticLocation.ofQualifiedName(qualifiedTypeName))
                .cause(cause)
                .build());
    }

    private void reportResourceError(String relativeName, IOException cause) {
        diagnostics.report(Diagnostic.builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(CODE_RESOURCE_WRITE_FAILED)
                .message("Failed to write resource file '" + relativeName + "': " + cause.getMessage())
                .location(DiagnosticLocation.ofPath(relativeName, null, null))
                .cause(cause)
                .build());
    }
}
