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
package io.hexaglue.core.integration;

import static com.google.common.truth.Truth.assertThat;

import io.hexaglue.spi.codegen.CustomBlock;
import io.hexaglue.spi.codegen.GeneratedHeader;
import io.hexaglue.spi.codegen.MergeMode;
import io.hexaglue.spi.codegen.SourceFile;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating codegen SPI contracts (MergeMode, CustomBlock, GeneratedHeader, SourceFile).
 *
 * <p>
 * This test ensures that:
 * <ul>
 *   <li>MergeMode enum is accessible</li>
 *   <li>CustomBlock record works correctly</li>
 *   <li>GeneratedHeader provides structured metadata</li>
 *   <li>SourceFile builder pattern works correctly</li>
 * </ul>
 * </p>
 */
class CodegenIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // MergeMode Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testMergeModeEnum() {
        // When: Access MergeMode enum values
        MergeMode overwrite = MergeMode.OVERWRITE;
        MergeMode merge = MergeMode.MERGE_CUSTOM_BLOCKS;
        MergeMode writeOnce = MergeMode.WRITE_ONCE;
        MergeMode failIfExists = MergeMode.FAIL_IF_EXISTS;

        // Then: All values should be accessible
        assertThat(overwrite).isNotNull();
        assertThat(merge).isNotNull();
        assertThat(writeOnce).isNotNull();
        assertThat(failIfExists).isNotNull();
        assertThat(MergeMode.values()).hasLength(4);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CustomBlock Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testCustomBlockCreation() {
        // When: Create custom block with description
        CustomBlock block = new CustomBlock("imports", "User-maintained imports");

        // Then: Should have id and description
        assertThat(block.id()).isEqualTo("imports");
        assertThat(block.description()).isEqualTo("User-maintained imports");
    }

    @Test
    void testCustomBlockOf() {
        // When: Create custom block without description
        CustomBlock block = CustomBlock.of("custom-code");

        // Then: Should have id but no description
        assertThat(block.id()).isEqualTo("custom-code");
        assertThat(block.description()).isNull();
    }

    @Test
    void testCustomBlockTrimsId() {
        // When: Create with whitespace
        CustomBlock block = new CustomBlock("  imports  ", null);

        // Then: Should trim id
        assertThat(block.id()).isEqualTo("imports");
    }

    @Test
    void testCustomBlockRejectsBlankId() {
        // When/Then: Should reject blank id
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> new CustomBlock("   ", null));
    }

    @Test
    void testCustomBlockRejectsNullId() {
        // When/Then: Should reject null id
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> new CustomBlock(null, null));
    }

    @Test
    void testCustomBlockNormalizesBlankDescription() {
        // When: Create with blank description
        CustomBlock block = new CustomBlock("imports", "   ");

        // Then: Should normalize blank to null
        assertThat(block.description()).isNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GeneratedHeader Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testGeneratedHeaderCreation() {
        // Given: Header components
        String toolName = "HexaGlue";
        String license = "MPL-2.0";
        String copyright = "Copyright (c) 2025 Scalastic";
        Instant timestamp = Instant.now();

        // When: Create header
        GeneratedHeader header = GeneratedHeader.of(toolName, license, copyright, timestamp);

        // Then: Should have all properties
        assertThat(header.toolName()).isEqualTo("HexaGlue");
        assertThat(header.license()).isPresent();
        assertThat(header.license().get()).isEqualTo("MPL-2.0");
        assertThat(header.copyright()).isPresent();
        assertThat(header.copyright().get()).isEqualTo("Copyright (c) 2025 Scalastic");
        assertThat(header.generatedAt()).isPresent();
        assertThat(header.generatedAt().get()).isEqualTo(timestamp);
    }

    @Test
    void testGeneratedHeaderMinimalHexaGlue() {
        // When: Create minimal header
        GeneratedHeader header = GeneratedHeader.minimalHexaGlue();

        // Then: Should have tool name but no license/copyright
        assertThat(header.toolName()).isEqualTo("HexaGlue");
        assertThat(header.license()).isEmpty();
        assertThat(header.copyright()).isEmpty();
        assertThat(header.generatedAt()).isEmpty();
    }

    @Test
    void testGeneratedHeaderWithNullOptionals() {
        // When: Create with null optionals
        GeneratedHeader header = GeneratedHeader.of("MyTool", null, null, null);

        // Then: Optional fields should be empty
        assertThat(header.toolName()).isEqualTo("MyTool");
        assertThat(header.license()).isEmpty();
        assertThat(header.copyright()).isEmpty();
        assertThat(header.generatedAt()).isEmpty();
    }

    @Test
    void testGeneratedHeaderRejectsBlankToolName() {
        // When/Then: Should reject blank tool name
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> GeneratedHeader.of("   ", null, null, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SourceFile Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testSourceFileBuilder() {
        // Given: Source file data
        String qualifiedName = "com.example.Customer";
        String content = "package com.example;\npublic class Customer {}";

        // When: Build source file
        SourceFile sourceFile = SourceFile.builder()
                .qualifiedTypeName(qualifiedName)
                .content(content)
                .build();

        // Then: Should have correct properties
        assertThat(sourceFile.qualifiedTypeName()).isEqualTo("com.example.Customer");
        assertThat(sourceFile.content()).isEqualTo(content);
        assertThat(sourceFile.mergeMode()).isEqualTo(MergeMode.MERGE_CUSTOM_BLOCKS); // default
        assertThat(sourceFile.charset()).isEqualTo(StandardCharsets.UTF_8); // default
        assertThat(sourceFile.header()).isEmpty();
        assertThat(sourceFile.customBlocks()).isEmpty();
    }

    @Test
    void testSourceFileWithMergeMode() {
        // When: Build with specific merge mode
        SourceFile sourceFile = SourceFile.builder()
                .qualifiedTypeName("com.example.Customer")
                .content("class Customer {}")
                .mergeMode(MergeMode.OVERWRITE)
                .build();

        // Then: Should have specified merge mode
        assertThat(sourceFile.mergeMode()).isEqualTo(MergeMode.OVERWRITE);
    }

    @Test
    void testSourceFileWithCharset() {
        // When: Build with specific charset
        SourceFile sourceFile = SourceFile.builder()
                .qualifiedTypeName("com.example.Customer")
                .content("class Customer {}")
                .charset(StandardCharsets.ISO_8859_1)
                .build();

        // Then: Should have specified charset
        assertThat(sourceFile.charset()).isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    void testSourceFileWithHeader() {
        // Given: Header
        GeneratedHeader header = GeneratedHeader.minimalHexaGlue();

        // When: Build with header
        SourceFile sourceFile = SourceFile.builder()
                .qualifiedTypeName("com.example.Customer")
                .content("class Customer {}")
                .header(header)
                .build();

        // Then: Should have header
        assertThat(sourceFile.header()).isPresent();
        assertThat(sourceFile.header().get()).isEqualTo(header);
    }

    @Test
    void testSourceFileWithCustomBlocks() {
        // Given: Custom blocks
        CustomBlock imports = CustomBlock.of("imports");
        CustomBlock methods = CustomBlock.of("custom-methods");

        // When: Build with custom blocks
        SourceFile sourceFile = SourceFile.builder()
                .qualifiedTypeName("com.example.Customer")
                .content("class Customer {}")
                .customBlocks(List.of(imports, methods))
                .build();

        // Then: Should have custom blocks
        assertThat(sourceFile.customBlocks()).hasSize(2);
        assertThat(sourceFile.customBlocks()).containsExactly(imports, methods).inOrder();
    }

    @Test
    void testSourceFileRejectsBlankQualifiedTypeName() {
        // When/Then: Should reject blank qualified type name
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SourceFile.builder()
                .qualifiedTypeName("   ")
                .content("code")
                .build());
    }

    @Test
    void testSourceFileRejectsBlankContent() {
        // When/Then: Should reject blank content
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> SourceFile.builder()
                .qualifiedTypeName("com.example.Foo")
                .content("   ")
                .build());
    }

    @Test
    void testSourceFileRejectsNullMergeMode() {
        // When/Then: Should reject null merge mode
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> SourceFile.builder()
                .qualifiedTypeName("com.example.Foo")
                .content("code")
                .mergeMode(null)
                .build());
    }

    @Test
    void testSourceFileRejectsNullCustomBlock() {
        // When/Then: Should reject list containing null
        org.junit.jupiter.api.Assertions.assertThrows(NullPointerException.class, () -> SourceFile.builder()
                .qualifiedTypeName("com.example.Foo")
                .content("code")
                .customBlocks(List.of(CustomBlock.of("valid"), null))
                .build());
    }
}
