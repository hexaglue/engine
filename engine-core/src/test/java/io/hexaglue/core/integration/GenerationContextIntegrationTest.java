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

import io.hexaglue.spi.context.BuildEnvironment;
import io.hexaglue.spi.context.ExecutionMode;
import io.hexaglue.spi.context.GenerationRequest;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating SPI contracts for BuildEnvironment and GenerationRequest.
 *
 * <p>
 * This test ensures that:
 * <ul>
 *   <li>SPI factory methods work correctly</li>
 *   <li>All accessors return expected values</li>
 *   <li>The contracts are stable and usable by plugins</li>
 * </ul>
 *
 * <p>
 * Note: Full GenerationContextSpec integration requires core implementations
 * that are not yet fully accessible. This test focuses on what can be validated
 * using SPI-only APIs.
 * </p>
 */
class GenerationContextIntegrationTest {

    @Test
    void testBuildEnvironmentContract() {
        // When: Create BuildEnvironment using SPI factory
        BuildEnvironment env = BuildEnvironment.of(
                ExecutionMode.DEVELOPMENT, true, Locale.US, "maven", "idea", Map.of("custom.key", "custom.value"));

        // Then: All accessors should return the injected values
        assertThat(env.mode()).isEqualTo(ExecutionMode.DEVELOPMENT);
        assertThat(env.isDebugEnabled()).isTrue();
        assertThat(env.locale()).isEqualTo(Locale.US);
        assertThat(env.buildTool()).isPresent();
        assertThat(env.buildTool().get()).isEqualTo("maven");
        assertThat(env.host()).isPresent();
        assertThat(env.host().get()).isEqualTo("idea");
        assertThat(env.attributes()).isNotNull();
        assertThat(env.attributes()).containsEntry("custom.key", "custom.value");
    }

    @Test
    void testBuildEnvironmentWithNullOptionals() {
        // When: Create BuildEnvironment with null optionals
        BuildEnvironment env = BuildEnvironment.of(ExecutionMode.CI, false, Locale.getDefault(), null, null, null);

        // Then: Optional fields should be empty
        assertThat(env.mode()).isEqualTo(ExecutionMode.CI);
        assertThat(env.isDebugEnabled()).isFalse();
        assertThat(env.buildTool()).isEmpty();
        assertThat(env.host()).isEmpty();
        assertThat(env.attributes()).isNotNull();
        assertThat(env.attributes()).isEmpty();
    }

    @Test
    void testExecutionModeEnum() {
        // When: Access ExecutionMode enum values
        ExecutionMode dev = ExecutionMode.DEVELOPMENT;
        ExecutionMode ci = ExecutionMode.CI;
        ExecutionMode release = ExecutionMode.RELEASE;

        // Then: All values should be accessible
        assertThat(dev).isNotNull();
        assertThat(ci).isNotNull();
        assertThat(release).isNotNull();
        assertThat(ExecutionMode.values()).hasLength(3);
    }

    @Test
    void testGenerationRequestContract() {
        // When: Create GenerationRequest using SPI factory
        GenerationRequest request = GenerationRequest.of(
                null, // coreVersion
                "com.example:my-project:1.0.0", // projectId
                17, // targetJavaRelease
                Set.of("plugin1", "plugin2") // activePluginIds
                );

        // Then: All accessors should return the injected values
        assertThat(request.coreVersion()).isEmpty();
        assertThat(request.projectId()).isPresent();
        assertThat(request.projectId().get()).isEqualTo("com.example:my-project:1.0.0");
        assertThat(request.targetJavaRelease()).isPresent();
        assertThat(request.targetJavaRelease().get()).isEqualTo(17);
        assertThat(request.activePluginIds()).isNotNull();
        assertThat(request.activePluginIds()).containsExactly("plugin1", "plugin2");
    }

    @Test
    void testGenerationRequestWithNullOptionals() {
        // When: Create GenerationRequest with null optionals
        GenerationRequest request = GenerationRequest.of(null, null, null, null);

        // Then: Optional fields should be empty
        assertThat(request.coreVersion()).isEmpty();
        assertThat(request.projectId()).isEmpty();
        assertThat(request.targetJavaRelease()).isEmpty();
        assertThat(request.activePluginIds()).isNotNull();
        assertThat(request.activePluginIds()).isEmpty();
    }

    @Test
    void testBuildEnvironmentRequiredFields() {
        // When/Then: Should require mode
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class, () -> BuildEnvironment.of(null, false, Locale.US, null, null, null));

        // When/Then: Should require locale
        org.junit.jupiter.api.Assertions.assertThrows(
                NullPointerException.class,
                () -> BuildEnvironment.of(ExecutionMode.DEVELOPMENT, false, null, null, null, null));
    }
}
