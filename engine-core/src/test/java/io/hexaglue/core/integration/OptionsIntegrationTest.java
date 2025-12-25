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

import io.hexaglue.spi.options.OptionKey;
import io.hexaglue.spi.options.OptionScope;
import io.hexaglue.spi.options.OptionValue;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating options SPI contracts (OptionScope, OptionKey, OptionValue).
 */
class OptionsIntegrationTest {

    @Test
    void testOptionScopeEnum() {
        assertThat(OptionScope.values()).hasLength(2);
        assertThat(OptionScope.GLOBAL).isNotNull();
        assertThat(OptionScope.PLUGIN).isNotNull();
    }

    @Test
    void testOptionKeyGlobal() {
        OptionKey<Boolean> key = OptionKey.global("hexaglue.debug", Boolean.class);
        assertThat(key.name()).isEqualTo("hexaglue.debug");
        assertThat(key.type()).isEqualTo(Boolean.class);
        assertThat(key.scope()).isEqualTo(OptionScope.GLOBAL);
        assertThat(key.pluginId()).isEmpty();
    }

    @Test
    void testOptionKeyPlugin() {
        OptionKey<String> key = OptionKey.plugin("my-plugin", "database.url", String.class);
        assertThat(key.name()).isEqualTo("database.url");
        assertThat(key.type()).isEqualTo(String.class);
        assertThat(key.scope()).isEqualTo(OptionScope.PLUGIN);
        assertThat(key.pluginId()).isPresent();
        assertThat(key.pluginId().get()).isEqualTo("my-plugin");
    }

    @Test
    void testOptionKeyPluginRequiresPluginId() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> OptionKey.plugin("", "name", String.class));
    }

    @Test
    void testOptionValueMissing() {
        OptionValue<String> value = OptionValue.missing();
        assertThat(value.value()).isNull();
        assertThat(value.present()).isFalse();
        assertThat(value.source()).isNull();
        assertThat(value.asOptional()).isEmpty();
    }

    @Test
    void testOptionValuePresent() {
        OptionValue<String> value = OptionValue.present("hello", "config-file");
        assertThat(value.value()).isEqualTo("hello");
        assertThat(value.present()).isTrue();
        assertThat(value.source()).isEqualTo("config-file");
        assertThat(value.asOptional()).isPresent();
        assertThat(value.asOptional().get()).isEqualTo("hello");
    }

    @Test
    void testOptionValuePresentWithNullSource() {
        OptionValue<Integer> value = OptionValue.present(42, null);
        assertThat(value.value()).isEqualTo(42);
        assertThat(value.present()).isTrue();
        assertThat(value.source()).isNull();
    }
}
