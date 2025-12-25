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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a generated Java source file.
 *
 * <p>This is a structured model for a source file that can be written by the compiler.
 * It does not prescribe a particular code generation library.</p>
 */
@Evolvable(since = "1.0.0")
public final class SourceFile {

    private final String qualifiedTypeName;
    private final String content;
    private final MergeMode mergeMode;
    private final Charset charset;
    private final GeneratedHeader header;
    private final List<CustomBlock> customBlocks;

    private SourceFile(Builder b) {
        this.qualifiedTypeName = requireNonBlank(b.qualifiedTypeName, "qualifiedTypeName");
        this.content = requireNonBlank(b.content, "content");
        this.mergeMode = Objects.requireNonNull(b.mergeMode, "mergeMode");
        this.charset = (b.charset == null) ? StandardCharsets.UTF_8 : b.charset;
        this.header = b.header;
        this.customBlocks = (b.customBlocks == null) ? List.of() : List.copyOf(b.customBlocks);
        for (CustomBlock cb : customBlocks) Objects.requireNonNull(cb, "customBlocks contains null");
    }

    /**
     * Qualified type name of the top-level type in this source file.
     *
     * <p>Example: {@code "com.example.infrastructure.persistence.CustomerEntity"}.</p>
     *
     * @return qualified type name (never blank)
     */
    public String qualifiedTypeName() {
        return qualifiedTypeName;
    }

    /**
     * Source content (without any guarantee of formatting).
     *
     * @return content (never blank)
     */
    public String content() {
        return content;
    }

    /** @return merge mode (never {@code null}) */
    public MergeMode mergeMode() {
        return mergeMode;
    }

    /** @return charset used for writing */
    public Charset charset() {
        return charset;
    }

    /** @return optional header metadata */
    public Optional<GeneratedHeader> header() {
        return Optional.ofNullable(header);
    }

    /** @return custom blocks declared for merge-aware regeneration */
    public List<CustomBlock> customBlocks() {
        return customBlocks;
    }

    /** @return builder */
    public static Builder builder() {
        return new Builder();
    }

    /** Builder for {@link SourceFile}. */
    public static final class Builder {
        private String qualifiedTypeName;
        private String content;
        private MergeMode mergeMode = MergeMode.MERGE_CUSTOM_BLOCKS;
        private Charset charset;
        private GeneratedHeader header;
        private List<CustomBlock> customBlocks;

        private Builder() {}

        /** @param qualifiedTypeName qualified type name */
        public Builder qualifiedTypeName(String qualifiedTypeName) {
            this.qualifiedTypeName = qualifiedTypeName;
            return this;
        }

        /** @param content source content */
        public Builder content(String content) {
            this.content = content;
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

        /** @param customBlocks custom blocks list */
        public Builder customBlocks(List<CustomBlock> customBlocks) {
            this.customBlocks = customBlocks;
            return this;
        }

        /** @return built source file */
        public SourceFile build() {
            return new SourceFile(this);
        }
    }

    private static String requireNonBlank(String v, String label) {
        Objects.requireNonNull(v, label);
        String t = v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return t;
    }
}
