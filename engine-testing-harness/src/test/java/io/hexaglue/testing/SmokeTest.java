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

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class SmokeTest {

    @Test
    void shouldCompileSimplePort() {
        CompilationResult result = CompilationTestCase.builder()
                .addSourceFile("com.example.port.CustomerRepository", """
            package com.example.port;

            public interface CustomerRepository {
              void save(String id);
            }
            """)
                .compile();

        // Print diagnostics if compilation failed to help debugging
        if (!result.wasSuccessful()) {
            System.out.println("Compilation failed with diagnostics:");
            result.formattedDiagnostics().forEach(System.out::println);
        }

        assertThat(result.wasSuccessful()).isTrue();
    }
}
