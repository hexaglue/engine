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
package io.hexaglue.spi.codegen;

import io.hexaglue.spi.stability.Evolvable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a generated resource file.
 *
 * <p>Resources are typically written to {@code src/main/resources} (or the equivalent output
 * location managed by the compiler).</p>
 */
@Evolvable(since = "1.0.0")
public final class ResourceFile {

    private final String path;
    private final byte[] bytes;
    private final MergeMode mergeMode;
    private final Charset charset;
    private final String text; // optional, for text resources
    private final GeneratedHeader header;

    private ResourceFile(Builder b) {
        this.path = requireNonBlank(b.path, "path");
        this.mergeMode = Objects.requireNonNull(b.mergeMode, "mergeMode");
        this.charset = (b.charset == null) ? StandardCharsets.UTF_8 : b.charset;
        this.header = b.header;

        if (b.bytes != null && b.text != null) {
            throw new IllegalArgumentException("ResourceFile cannot have both bytes and text.");
        }
        if (b.bytes == null && b.text == null) {
            throw new IllegalArgumentException("ResourceFile must have either bytes or text.");
        }

        this.bytes = (b.bytes == null) ? null : b.bytes.clone();
        this.text = b.text;
    }

    /** @return resource path (never blank) */
    public String path() {
        return path;
    }

    /** @return merge mode */
    public MergeMode mergeMode() {
        return mergeMode;
    }

    /** @return charset used when writing text resources */
    public Charset charset() {
        return charset;
    }

    /** @return optional header metadata */
    public Optional<GeneratedHeader> header() {
        return Optional.ofNullable(header);
    }

    /**
     * Returns text content when this resource is text-based.
     *
     * @return text if present
     */
    public Optional<String> text() {
        return Optional.ofNullable(text);
    }

    /**
     * Returns bytes when this resource is binary-based.
     *
     * @return bytes if present
     */
    public Optional<byte[]> bytes() {
        return bytes == null ? Optional.empty() : Optional.of(bytes.clone());
    }

    /** @return builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link ResourceFile}. */
    public static final class Builder {
        private String path;
        private byte[] bytes;
        private String text;
        private MergeMode mergeMode = MergeMode.MERGE_CUSTOM_BLOCKS;
        private Charset charset;
        private GeneratedHeader header;

        private Builder() {}

        /** @param path resource path */
        public Builder path(String path) {
            this.path = path;
            return this;
        }

        /** @param bytes binary content */
        public Builder bytes(byte[] bytes) {
            this.bytes = (bytes == null) ? null : bytes.clone();
            return this;
        }

        /** @param text text content */
        public Builder text(String text) {
            this.text = text;
            return this;
        }

        /** @param mergeMode merge mode */
        public Builder mergeMode(MergeMode mergeMode) {
            this.mergeMode = mergeMode;
            return this;
        }

        /** @param charset charset (defaults to UTF-8) */
        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        /** @param header header metadata */
        public Builder header(GeneratedHeader header) {
            this.header = header;
            return this;
        }

        /** @return built resource file */
        public ResourceFile build() {
            return new ResourceFile(this);
        }
    }

    private static String requireNonBlank(String v, String label) {
        Objects.requireNonNull(v, label);
        String t = v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return t;
    }
}
