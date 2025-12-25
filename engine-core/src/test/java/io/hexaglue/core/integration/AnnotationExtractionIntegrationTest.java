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

import io.hexaglue.spi.ir.domain.AnnotationView;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Integration test for EP-002: Property Annotations.
 *
 * <p>This test validates the AnnotationView SPI interface and its factory methods.
 * The actual extraction from compiled code is tested in the main annotation processor tests.</p>
 */
class AnnotationExtractionIntegrationTest {

    @Test
    void testAnnotationView_factory() {
        // When: Create AnnotationView using factory method
        AnnotationView annotation = AnnotationView.of(
                "jakarta.persistence.Column", Map.of("length", 255, "unique", true, "name", "email_address"));

        // Then: All properties should be accessible
        assertThat(annotation.qualifiedName()).isEqualTo("jakarta.persistence.Column");
        assertThat(annotation.simpleName()).isEqualTo("Column");
        assertThat(annotation.attribute("length", Integer.class)).hasValue(255);
        assertThat(annotation.attribute("unique", Boolean.class)).hasValue(true);
        assertThat(annotation.attribute("name", String.class)).hasValue("email_address");
        assertThat(annotation.attribute("missing")).isEmpty();
    }

    @Test
    void testAnnotationView_is() {
        // Given: An AnnotationView
        AnnotationView nullable = AnnotationView.of("jakarta.annotation.Nullable", Map.of());

        // When/Then: Test is() method
        assertThat(nullable.is("jakarta.annotation.Nullable")).isTrue();
        assertThat(nullable.is("jakarta.annotation.NotNull")).isFalse();
        assertThat(nullable.is("Nullable")).isFalse(); // Must be qualified name
    }

    @Test
    void testAnnotationView_attributes() {
        // Given: An AnnotationView with multiple attributes
        AnnotationView size = AnnotationView.of(
                "jakarta.validation.constraints.Size",
                Map.of("min", 3, "max", 50, "message", "Size must be between 3 and 50"));

        // When/Then: All attributes should be retrievable
        assertThat(size.attribute("min", Integer.class)).hasValue(3);
        assertThat(size.attribute("max", Integer.class)).hasValue(50);
        assertThat(size.attribute("message", String.class)).hasValue("Size must be between 3 and 50");
    }

    @Test
    void testAnnotationView_emptyAttributes() {
        // Given: An AnnotationView without attributes
        AnnotationView deprecated = AnnotationView.of("java.lang.Deprecated", Map.of());

        // When/Then: Attributes map should be empty but not null
        assertThat(deprecated.attributes()).isNotNull();
        assertThat(deprecated.attributes()).isEmpty();
    }

    @Test
    void testAnnotationView_wrongTypeConversion() {
        // Given: An AnnotationView with an Integer attribute
        AnnotationView column = AnnotationView.of("jakarta.persistence.Column", Map.of("length", 255));

        // When/Then: Trying to get as wrong type should return empty
        assertThat(column.attribute("length", Integer.class)).hasValue(255);
        assertThat(column.attribute("length", String.class)).isEmpty();
        assertThat(column.attribute("length", Boolean.class)).isEmpty();
    }

    @Test
    void testAnnotationView_simpleName() {
        // Given: AnnotationViews with various qualified names
        AnnotationView column = AnnotationView.of("jakarta.persistence.Column", Map.of());
        AnnotationView deprecated = AnnotationView.of("java.lang.Deprecated", Map.of());
        AnnotationView custom = AnnotationView.of("MyAnnotation", Map.of());

        // When/Then: Simple names should be extracted correctly
        assertThat(column.simpleName()).isEqualTo("Column");
        assertThat(deprecated.simpleName()).isEqualTo("Deprecated");
        assertThat(custom.simpleName()).isEqualTo("MyAnnotation");
    }

    @Test
    void testAnnotationView_immutability() {
        // Given: An AnnotationView created with a mutable map
        var mutableMap = new java.util.HashMap<String, Object>();
        mutableMap.put("value", "test");
        AnnotationView annotation = AnnotationView.of("com.example.MyAnnotation", mutableMap);

        // When: Modify the original map
        mutableMap.put("value", "modified");
        mutableMap.put("newKey", "newValue");

        // Then: AnnotationView should remain unchanged
        assertThat(annotation.attribute("value", String.class)).hasValue("test");
        assertThat(annotation.attribute("newKey")).isEmpty();
    }
}
