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

import io.hexaglue.spi.HexaGlueVersion;
import io.hexaglue.spi.context.BuildEnvironment;
import io.hexaglue.spi.context.ExecutionMode;
import io.hexaglue.spi.context.GenerationRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating context SPI contracts.
 *
 * <p>Tests the build context system including execution modes, build environment,
 * and generation requests.</p>
 */
class ContextIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // ExecutionMode Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testExecutionModeEnum() {
        // When: Access ExecutionMode enum values
        assertThat(ExecutionMode.values()).hasLength(3);
        assertThat(ExecutionMode.DEVELOPMENT).isNotNull();
        assertThat(ExecutionMode.CI).isNotNull();
        assertThat(ExecutionMode.RELEASE).isNotNull();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BuildEnvironment Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testBuildEnvironmentFactory() {
        // Given: Build environment components
        ExecutionMode mode = ExecutionMode.DEVELOPMENT;
        boolean debugEnabled = true;
        Locale locale = Locale.US;
        String buildTool = "maven";
        String host = "idea";
        Map<String, String> attributes = Map.of("key1", "value1", "key2", "value2");

        // When: Create build environment
        BuildEnvironment env = BuildEnvironment.of(mode, debugEnabled, locale, buildTool, host, attributes);

        // Then: Should have all properties
        assertThat(env.mode()).isEqualTo(ExecutionMode.DEVELOPMENT);
        assertThat(env.isDebugEnabled()).isTrue();
        assertThat(env.locale()).isEqualTo(Locale.US);
        assertThat(env.buildTool().isPresent()).isTrue();
        assertThat(env.buildTool().get()).isEqualTo("maven");
        assertThat(env.host().isPresent()).isTrue();
        assertThat(env.host().get()).isEqualTo("idea");
        assertThat(env.attributes()).hasSize(2);
        assertThat(env.attributes().get("key1")).isEqualTo("value1");
    }

    @Test
    void testBuildEnvironmentWithNulls() {
        // When: Create build environment with nulls
        BuildEnvironment env = BuildEnvironment.of(ExecutionMode.CI, false, Locale.getDefault(), null, null, null);

        // Then: Should have defaults
        assertThat(env.mode()).isEqualTo(ExecutionMode.CI);
        assertThat(env.isDebugEnabled()).isFalse();
        assertThat(env.buildTool().isPresent()).isFalse();
        assertThat(env.host().isPresent()).isFalse();
        assertThat(env.attributes()).isEmpty();
    }

    @Test
    void testBuildEnvironmentNormalizesBlankStrings() {
        // When: Create environment with blank strings
        BuildEnvironment env = BuildEnvironment.of(ExecutionMode.RELEASE, true, Locale.FRANCE, "   ", "   ", Map.of());

        // Then: Should normalize to empty optionals
        assertThat(env.buildTool().isPresent()).isFalse();
        assertThat(env.host().isPresent()).isFalse();
    }

    @Test
    void testBuildEnvironmentDifferentModes() {
        // When: Create environments with different modes
        BuildEnvironment dev = BuildEnvironment.of(ExecutionMode.DEVELOPMENT, false, Locale.US, null, null, null);
        BuildEnvironment ci = BuildEnvironment.of(ExecutionMode.CI, false, Locale.US, null, null, null);
        BuildEnvironment release = BuildEnvironment.of(ExecutionMode.RELEASE, false, Locale.US, null, null, null);

        // Then: Should have correct modes
        assertThat(dev.mode()).isEqualTo(ExecutionMode.DEVELOPMENT);
        assertThat(ci.mode()).isEqualTo(ExecutionMode.CI);
        assertThat(release.mode()).isEqualTo(ExecutionMode.RELEASE);
    }

    @Test
    void testBuildEnvironmentRejectsNullMode() {
        // When/Then: Should reject null mode
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> BuildEnvironment.of(null, false, Locale.US, null, null, null));
    }

    @Test
    void testBuildEnvironmentRejectsNullLocale() {
        // When/Then: Should reject null locale
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> BuildEnvironment.of(ExecutionMode.DEVELOPMENT, false, null, null, null, null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GenerationRequest Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testGenerationRequestFactory() {
        // Given: Request components
        HexaGlueVersion version = HexaGlueVersion.of(0, 1, 0);
        String projectId = "com.example:my-project:1.0.0";
        Integer targetRelease = 17;
        Set<String> pluginIds = Set.of("plugin-a", "plugin-b");

        // When: Create generation request
        GenerationRequest request = GenerationRequest.of(version, projectId, targetRelease, pluginIds);

        // Then: Should have all properties
        assertThat(request.coreVersion().isPresent()).isTrue();
        assertThat(request.coreVersion().get()).isEqualTo(version);
        assertThat(request.projectId().isPresent()).isTrue();
        assertThat(request.projectId().get()).isEqualTo("com.example:my-project:1.0.0");
        assertThat(request.targetJavaRelease().isPresent()).isTrue();
        assertThat(request.targetJavaRelease().get()).isEqualTo(17);
        assertThat(request.activePluginIds()).hasSize(2);
        assertThat(request.activePluginIds()).contains("plugin-a");
        assertThat(request.activePluginIds()).contains("plugin-b");
    }

    @Test
    void testGenerationRequestWithNulls() {
        // When: Create request with nulls
        GenerationRequest request = GenerationRequest.of(null, null, null, null);

        // Then: Should have defaults
        assertThat(request.coreVersion().isPresent()).isFalse();
        assertThat(request.projectId().isPresent()).isFalse();
        assertThat(request.targetJavaRelease().isPresent()).isFalse();
        assertThat(request.activePluginIds()).isEmpty();
    }

    @Test
    void testGenerationRequestNormalizesBlankProjectId() {
        // When: Create request with blank project id
        GenerationRequest request = GenerationRequest.of(null, "   ", null, null);

        // Then: Should normalize to empty
        assertThat(request.projectId().isPresent()).isFalse();
    }

    @Test
    void testGenerationRequestFiltersInvalidJavaRelease() {
        // When: Create request with zero or negative Java release
        GenerationRequest request1 = GenerationRequest.of(null, null, 0, null);
        GenerationRequest request2 = GenerationRequest.of(null, null, -1, null);

        // Then: Should filter out invalid values
        assertThat(request1.targetJavaRelease().isPresent()).isFalse();
        assertThat(request2.targetJavaRelease().isPresent()).isFalse();
    }

    @Test
    void testGenerationRequestNormalizesPluginIds() {
        // When: Create request with blank/null plugin ids
        Set<String> pluginIds = Set.of("plugin-a", "   ", "plugin-b");
        GenerationRequest request = GenerationRequest.of(null, null, null, pluginIds);

        // Then: Should filter out blank values
        assertThat(request.activePluginIds()).hasSize(2);
        assertThat(request.activePluginIds()).contains("plugin-a");
        assertThat(request.activePluginIds()).contains("plugin-b");
    }

    @Test
    void testGenerationRequestValidJavaReleases() {
        // When: Create requests with valid Java releases
        GenerationRequest java8 = GenerationRequest.of(null, null, 8, null);
        GenerationRequest java11 = GenerationRequest.of(null, null, 11, null);
        GenerationRequest java17 = GenerationRequest.of(null, null, 17, null);
        GenerationRequest java21 = GenerationRequest.of(null, null, 21, null);

        // Then: Should preserve valid releases
        assertThat(java8.targetJavaRelease().get()).isEqualTo(8);
        assertThat(java11.targetJavaRelease().get()).isEqualTo(11);
        assertThat(java17.targetJavaRelease().get()).isEqualTo(17);
        assertThat(java21.targetJavaRelease().get()).isEqualTo(21);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HexaGlueVersion Tests (Bonus)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testHexaGlueVersionCreation() {
        // When: Create version
        HexaGlueVersion version = HexaGlueVersion.of(1, 2, 3);

        // Then: Should have correct components
        assertThat(version).isNotNull();
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(2);
        assertThat(version.patch()).isEqualTo(3);
    }

    @Test
    void testHexaGlueVersionParsing() {
        // When: Parse version string
        HexaGlueVersion version = HexaGlueVersion.parse("1.2.3");

        // Then: Should parse correctly
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(2);
        assertThat(version.patch()).isEqualTo(3);
    }

    @Test
    void testHexaGlueVersionParsingWithWhitespace() {
        // When: Parse version with whitespace
        HexaGlueVersion version = HexaGlueVersion.parse("  1.2.3  ");

        // Then: Should trim and parse
        assertThat(version.major()).isEqualTo(1);
        assertThat(version.minor()).isEqualTo(2);
        assertThat(version.patch()).isEqualTo(3);
    }

    @Test
    void testHexaGlueVersionToString() {
        // When: Convert version to string
        HexaGlueVersion version = HexaGlueVersion.of(1, 2, 3);

        // Then: Should format correctly
        assertThat(version.toString()).isEqualTo("1.2.3");
    }

    @Test
    void testHexaGlueVersionEquality() {
        // Given: Two versions with same components
        HexaGlueVersion v1 = HexaGlueVersion.of(1, 2, 3);
        HexaGlueVersion v2 = HexaGlueVersion.of(1, 2, 3);

        // Then: Should be equal
        assertThat(v1).isEqualTo(v2);
        assertThat(v1.hashCode()).isEqualTo(v2.hashCode());
    }

    @Test
    void testHexaGlueVersionComparison() {
        // Given: Different versions
        HexaGlueVersion v1_0_0 = HexaGlueVersion.of(1, 0, 0);
        HexaGlueVersion v1_1_0 = HexaGlueVersion.of(1, 1, 0);
        HexaGlueVersion v1_1_1 = HexaGlueVersion.of(1, 1, 1);
        HexaGlueVersion v2_0_0 = HexaGlueVersion.of(2, 0, 0);

        // Then: Should compare correctly
        assertThat(v1_0_0.compareTo(v1_1_0)).isLessThan(0);
        assertThat(v1_1_0.compareTo(v1_1_1)).isLessThan(0);
        assertThat(v1_1_1.compareTo(v2_0_0)).isLessThan(0);
        assertThat(v2_0_0.compareTo(v1_0_0)).isGreaterThan(0);
        assertThat(v1_1_0.compareTo(v1_1_0)).isEqualTo(0);
    }

    @Test
    void testHexaGlueVersionRejectsNegative() {
        // When/Then: Should reject negative components
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.of(-1, 0, 0));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.of(0, -1, 0));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.of(0, 0, -1));
    }

    @Test
    void testHexaGlueVersionParseRejectsInvalid() {
        // When/Then: Should reject invalid formats
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.parse("1.2"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.parse("1.2.3.4"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.parse("a.b.c"));
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> HexaGlueVersion.parse("1.2.x"));
    }

    @Test
    void testHexaGlueVersionSpiVersionConstant() {
        // When: Access SPI_VERSION constant
        int spiVersion = HexaGlueVersion.SPI_VERSION;

        // Then: Should be accessible and positive
        assertThat(spiVersion).isGreaterThan(0);
    }
}
