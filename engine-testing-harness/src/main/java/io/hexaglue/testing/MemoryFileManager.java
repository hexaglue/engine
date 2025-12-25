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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

/**
 * A {@link JavaFileManager} that captures annotation processor outputs in
 * memory.
 *
 * <p>
 * It intercepts {@code Filer}-based writes (generated sources/resources) so
 * tests can
 * assert on produced artifacts without touching the filesystem.
 * </p>
 */
final class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    /**
     * Generated Java sources (qualified name -> source content).
     */
    private final Map<String, String> generatedSources = new LinkedHashMap<>();

    /**
     * Generated resources (path-like key -> bytes).
     *
     * <p>
     * Key format: {@code <location>/<package>/<relativeName>} (best effort),
     * stable enough for assertions.
     * </p>
     */
    private final Map<String, byte[]> generatedResources = new LinkedHashMap<>();

    MemoryFileManager(StandardJavaFileManager fileManager) {
        super(fileManager);
    }

    /**
     * Returns an immutable snapshot of all captured outputs.
     */
    GeneratedFilesSnapshot snapshot() {
        return new GeneratedFilesSnapshot(Map.copyOf(generatedSources), Map.copyOf(generatedResources));
    }

    // -------------------------------------------------------------------------
    // Interception points used by annotation processors (Filer)
    // -------------------------------------------------------------------------

    @Override
    public JavaFileObject getJavaFileForOutput(
            Location location, String className, JavaFileObject.Kind kind, FileObject sibling) throws IOException {

        // Most processors generate sources into SOURCE_OUTPUT.
        if (kind == JavaFileObject.Kind.SOURCE) {
            return new InMemorySourceOutput(className, location, generatedSources);
        }

        // Class files or other kinds: let the default manager handle them.
        return super.getJavaFileForOutput(location, className, kind, sibling);
    }

    @Override
    public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling)
            throws IOException {

        // Resources (including docs) often go through getFileForOutput.
        String key = resourceKey(location, packageName, relativeName);
        return new InMemoryResourceOutput(key, generatedResources);
    }

    private static String resourceKey(Location location, String packageName, String relativeName) {
        String loc = (location == null) ? "UNKNOWN" : location.getName();
        String pkg = (packageName == null || packageName.isBlank()) ? "" : packageName.replace('.', '/') + "/";
        String rel = (relativeName == null) ? "" : relativeName;
        return loc + "/" + pkg + rel;
    }

    // -------------------------------------------------------------------------
    // Snapshot model (will be used by CompilationResult)
    // -------------------------------------------------------------------------

    static final class GeneratedFilesSnapshot {
        private final Map<String, String> sourcesByQualifiedName;
        private final Map<String, byte[]> resourcesByPath;

        GeneratedFilesSnapshot(Map<String, String> sourcesByQualifiedName, Map<String, byte[]> resourcesByPath) {
            this.sourcesByQualifiedName = sourcesByQualifiedName;
            this.resourcesByPath = resourcesByPath;
        }

        public Map<String, String> sourcesByQualifiedName() {
            return sourcesByQualifiedName;
        }

        public Map<String, byte[]> resourcesByPath() {
            return resourcesByPath;
        }

        public Optional<String> findSource(String qualifiedName) {
            return Optional.ofNullable(sourcesByQualifiedName.get(qualifiedName));
        }

        public Optional<byte[]> findResourceBytes(String pathKey) {
            return Optional.ofNullable(resourcesByPath.get(pathKey));
        }

        public Optional<String> findResourceText(String pathKey) {
            byte[] bytes = resourcesByPath.get(pathKey);
            if (bytes == null) return Optional.empty();
            return Optional.of(new String(bytes, StandardCharsets.UTF_8));
        }
    }

    // -------------------------------------------------------------------------
    // In-memory outputs
    // -------------------------------------------------------------------------

    private static final class InMemorySourceOutput extends SimpleJavaFileObject {

        private final String className;
        private final Map<String, String> sink;

        private final StringWriter buffer = new StringWriter();

        InMemorySourceOutput(String className, Location location, Map<String, String> sink) {
            super(URI.create("mem:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.className = className;
            this.sink = sink;
        }

        @Override
        public Writer openWriter() throws IOException {
            // Wrap to persist on close (reliable, no finalize).
            return new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) {
                    buffer.write(cbuf, off, len);
                }

                @Override
                public void flush() throws IOException {
                    buffer.flush();
                }

                @Override
                public void close() throws IOException {
                    buffer.close();
                    sink.put(className, buffer.toString());
                }
            };
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            // Rare for SOURCE kind, but handle it anyway.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    baos.write(b);
                }

                @Override
                public void close() throws IOException {
                    sink.put(className, baos.toString(StandardCharsets.UTF_8));
                }
            };
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return buffer.toString();
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) {
            return new StringReader(buffer.toString());
        }

        @Override
        public boolean delete() {
            sink.remove(className);
            return true;
        }
    }

    private static final class InMemoryResourceOutput extends SimpleJavaFileObject {

        private final String key;
        private final Map<String, byte[]> sink;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        InMemoryResourceOutput(String key, Map<String, byte[]> sink) {
            super(URI.create("mem:///" + key.replace(":", "_")), Kind.OTHER);
            this.key = key;
            this.sink = sink;
        }

        @Override
        public OutputStream openOutputStream() throws IOException {
            // Persist on close by returning a wrapper stream
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                    buffer.write(b);
                }

                @Override
                public void close() throws IOException {
                    sink.put(key, buffer.toByteArray());
                }
            };
        }

        @Override
        public Writer openWriter() throws IOException {
            return new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) throws IOException {
                    buffer.write(new String(cbuf, off, len).getBytes(StandardCharsets.UTF_8));
                }

                @Override
                public void flush() throws IOException {
                    // no-op
                }

                @Override
                public void close() throws IOException {
                    sink.put(key, buffer.toByteArray());
                }
            };
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
            return buffer.toString(StandardCharsets.UTF_8);
        }

        @Override
        public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
            return new StringReader(buffer.toString(StandardCharsets.UTF_8));
        }
    }
}
