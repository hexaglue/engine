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
package io.hexaglue.core.internal.ir.domain.semantics;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.hexaglue.spi.ir.domain.RelationshipKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link RelationshipEvidence}.
 *
 * <p>These tests verify the Evidence pattern implementation for relationship classification,
 * including factory methods, traceability, and error handling.</p>
 *
 * @since 0.4.0
 */
class RelationshipEvidenceTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Evidence Factories - Positive Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void yes_createsPositiveEvidence() {
        // Given: Relationship metadata
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.MANY_TO_ONE, "com.example.domain.Customer", true);

        // When: Create positive evidence
        RelationshipEvidence evidence = RelationshipEvidence.yes(
                RelationshipEvidence.Source.JMOLECULES_ANNOTATION, metadata, "@Association detected");

        // Then: Evidence is positive
        assertThat(evidence.hasRelationship()).isTrue();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.JMOLECULES_ANNOTATION);
        assertThat(evidence.relationship()).isEqualTo(metadata);
        assertThat(evidence.detail()).hasValue("@Association detected");
    }

    @Test
    void yes_fromYamlConfig_createsPositiveEvidence() {
        // Given: Relationship from YAML configuration
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.ONE_TO_MANY, "com.example.domain.OrderItem", false);

        // When: Create evidence from YAML
        RelationshipEvidence evidence = RelationshipEvidence.yes(
                RelationshipEvidence.Source.YAML_CONFIG, metadata, "Configured in hexaglue.yaml");

        // Then: Evidence tracks YAML source
        assertThat(evidence.hasRelationship()).isTrue();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.YAML_CONFIG);
        assertThat(evidence.detail()).hasValue("Configured in hexaglue.yaml");
    }

    @Test
    void yes_fromHeuristic_createsPositiveEvidence() {
        // Given: Relationship detected via heuristic
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.MANY_TO_ONE, "com.example.domain.Customer", true);

        // When: Create evidence from heuristic
        RelationshipEvidence evidence = RelationshipEvidence.yes(
                RelationshipEvidence.Source.HEURISTIC, metadata, "ID type name suggests Customer reference");

        // Then: Evidence tracks heuristic source
        assertThat(evidence.hasRelationship()).isTrue();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.HEURISTIC);
        assertThat(evidence.detail()).hasValue("ID type name suggests Customer reference");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evidence Factories - Negative Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void no_createsNegativeEvidence() {
        // When: Create negative evidence without detail
        RelationshipEvidence evidence = RelationshipEvidence.no();

        // Then: Evidence is negative
        assertThat(evidence.hasRelationship()).isFalse();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.NOT_A_RELATIONSHIP);
        assertThat(evidence.detail()).isEmpty();
    }

    @Test
    void no_withDetail_createsNegativeEvidenceWithReason() {
        // When: Create negative evidence with reason
        RelationshipEvidence evidence = RelationshipEvidence.no("Simple property type");

        // Then: Evidence is negative with detail
        assertThat(evidence.hasRelationship()).isFalse();
        assertThat(evidence.source()).isEqualTo(RelationshipEvidence.Source.NOT_A_RELATIONSHIP);
        assertThat(evidence.detail()).hasValue("Simple property type");
    }

    @Test
    void no_withDetailedReason_preservesFullExplanation() {
        // When: Create negative evidence with detailed reason
        String detailedReason = "Property 'email' has type 'java.lang.String' which is not a domain type";
        RelationshipEvidence evidence = RelationshipEvidence.no(detailedReason);

        // Then: Full reason is preserved
        assertThat(evidence.hasRelationship()).isFalse();
        assertThat(evidence.detail()).hasValue(detailedReason);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Error Cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void relationship_whenNoRelationship_throwsException() {
        // Given: Negative evidence
        RelationshipEvidence evidence = RelationshipEvidence.no();

        // When/Then: Accessing relationship throws
        assertThrows(IllegalStateException.class, evidence::relationship);
    }

    @Test
    void yes_withNotARelationshipSource_throwsException() {
        // Given: Metadata for a relationship
        RelationshipMetadata metadata = RelationshipMetadata.of(RelationshipKind.ONE_TO_ONE, "Target", false);

        // When/Then: Creating positive evidence with NOT_A_RELATIONSHIP source throws
        assertThrows(IllegalArgumentException.class, () -> {
            RelationshipEvidence.yes(RelationshipEvidence.Source.NOT_A_RELATIONSHIP, metadata, "detail");
        });
    }

    @Test
    void yes_withNullMetadata_throwsException() {
        // When/Then: Creating evidence with null metadata throws
        assertThrows(NullPointerException.class, () -> {
            RelationshipEvidence.yes(RelationshipEvidence.Source.HEURISTIC, null, "detail");
        });
    }

    @Test
    void yes_withNullSource_throwsException() {
        // Given: Valid metadata
        RelationshipMetadata metadata = RelationshipMetadata.of(RelationshipKind.ONE_TO_ONE, "Target", false);

        // When/Then: Creating evidence with null source throws
        assertThrows(NullPointerException.class, () -> {
            RelationshipEvidence.yes(null, metadata, "detail");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Traceability and Debugging
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void toString_providesUsefulDebugInfo_positiveEvidence() {
        // Given: Positive evidence
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.MANY_TO_ONE, "com.example.domain.Customer", true);
        RelationshipEvidence evidence =
                RelationshipEvidence.yes(RelationshipEvidence.Source.HEURISTIC, metadata, "ID heuristic");

        // When: Convert to string
        String result = evidence.toString();

        // Then: Contains useful debug info
        assertThat(result).contains("HEURISTIC");
        assertThat(result).contains("MANY_TO_ONE");
        assertThat(result).contains("inter-aggregate");
    }

    @Test
    void toString_providesUsefulDebugInfo_negativeEvidence() {
        // Given: Negative evidence
        RelationshipEvidence evidence = RelationshipEvidence.no("Simple type");

        // When: Convert to string
        String result = evidence.toString();

        // Then: Contains useful debug info
        assertThat(result).contains("NoRelationship");
        assertThat(result).contains("Simple type");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Evidence Immutability
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void evidence_isImmutable() {
        // Given: Evidence with metadata
        RelationshipMetadata metadata =
                RelationshipMetadata.of(RelationshipKind.ONE_TO_ONE, "com.example.domain.Address", false);
        RelationshipEvidence evidence =
                RelationshipEvidence.yes(RelationshipEvidence.Source.JMOLECULES_ANNOTATION, metadata, "test");

        // When: Access multiple times
        RelationshipMetadata first = evidence.relationship();
        RelationshipMetadata second = evidence.relationship();

        // Then: Same instance returned (immutable)
        assertThat(first).isSameInstanceAs(second);
    }
}
