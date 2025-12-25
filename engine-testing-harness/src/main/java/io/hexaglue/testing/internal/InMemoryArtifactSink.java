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
package io.hexaglue.testing.internal;

import static java.util.Objects.requireNonNull;

import io.hexaglue.spi.codegen.ArtifactSink;
import io.hexaglue.spi.codegen.DocFile;
import io.hexaglue.spi.codegen.ResourceFile;
import io.hexaglue.spi.codegen.SourceFile;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link ArtifactSink} used by the testing harness.
 *
 * <p>It captures generated artifacts so tests can assert on them without touching the filesystem.</p>
 *
 * <p><b>Note:</b> merge modes are intentionally ignored for now (last write wins).
 * We can refine this later once merge strategies are stabilized.</p>
 */
public final class InMemoryArtifactSink implements ArtifactSink {

    private final Map<String, String> sourcesByQualifiedType = new LinkedHashMap<>();
    private final Map<String, byte[]> resourcesByPath = new LinkedHashMap<>();
    private final Map<String, String> docsByPath = new LinkedHashMap<>();

    @Override
    public void write(SourceFile file) {
        requireNonNull(file, "file");
        sourcesByQualifiedType.put(file.qualifiedTypeName(), file.content());
    }

    @Override
    public void write(ResourceFile file) {
        requireNonNull(file, "file");

        String path = file.path();

        // Prefer textual content when available
        if (file.text().isPresent()) {
            Charset cs = file.charset();
            resourcesByPath.put(path, file.text().get().getBytes(cs));
            return;
        }

        if (file.bytes().isPresent()) {
            resourcesByPath.put(path, file.bytes().get());
            return;
        }

        // Nothing to write: store empty
        resourcesByPath.put(path, new byte[0]);
    }

    @Override
    public void write(DocFile file) {
        requireNonNull(file, "file");
        docsByPath.put(file.path(), file.content());
    }

    // --- Read API used by CompilationResult ---

    public Optional<String> findGeneratedSource(String qualifiedTypeName) {
        return Optional.ofNullable(sourcesByQualifiedType.get(qualifiedTypeName));
    }

    public Optional<String> findGeneratedDoc(String path) {
        return Optional.ofNullable(docsByPath.get(path));
    }

    public Optional<byte[]> findGeneratedResourceBytes(String path) {
        return Optional.ofNullable(resourcesByPath.get(path));
    }

    public Optional<String> findGeneratedResourceText(String path, Charset charset) {
        byte[] bytes = resourcesByPath.get(path);
        if (bytes == null) return Optional.empty();
        return Optional.of(new String(bytes, charset == null ? Charset.defaultCharset() : charset));
    }

    public Map<String, String> allGeneratedSources() {
        return Map.copyOf(sourcesByQualifiedType);
    }

    public Map<String, String> allGeneratedDocs() {
        return Map.copyOf(docsByPath);
    }

    public Map<String, byte[]> allGeneratedResourcesBytes() {
        return Map.copyOf(resourcesByPath);
    }
}
