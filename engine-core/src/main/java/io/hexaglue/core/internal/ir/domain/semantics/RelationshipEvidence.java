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

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import java.util.Objects;
import java.util.Optional;

/**
 * Evidence explaining why a domain property was classified as a relationship (or not).
 *
 * <p>This class captures the reasoning behind relationship classification decisions,
 * which is essential for diagnostics, debugging, and user transparency.</p>
 *
 * <h2>Design Rationale</h2>
 * <p>Rather than returning a simple boolean or {@link Optional}, we return evidence that includes:</p>
 * <ul>
 *   <li>Whether a relationship was detected</li>
 *   <li>The kind of signal that triggered the classification</li>
 *   <li>The resolved relationship metadata (if applicable)</li>
 *   <li>Optional detail explaining the specific match</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * RelationshipEvidence evidence = classifier.classify(property, domainModel, options);
 *
 * if (evidence.hasRelationship()) {
 *     RelationshipMetadata rel = evidence.relationship();
 *     log.debug("Property {} classified as {} relationship: {}",
 *         property.name(), rel.kind(), evidence.detail());
 *
 *     if (rel.isInterAggregate()) {
 *         log.warn("Inter-aggregate reference detected - ensure ID-only pattern");
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @since 0.4.0
 */
@InternalMarker(reason = "Internal semantics evidence; not exposed to plugins")
public final class RelationshipEvidence {

    /**
     * Kind of evidence that led to the relationship classification.
     */
    public enum Source {
        /**
         * Explicit jMolecules annotation ({@code @Association}, {@code @Entity} on target, etc.).
         */
        JMOLECULES_ANNOTATION,

        /**
         * YAML configuration ({@code types.<fqn>.properties.<name>.relationship.*}).
         */
        YAML_CONFIG,

        /**
         * Heuristic detection (type name ending with "Id", target type kind, etc.).
         */
        HEURISTIC,

        /**
         * No relationship detected (simple property).
         */
        NOT_A_RELATIONSHIP
    }

    private final boolean hasRelationship;
    private final Source source;
    private final RelationshipMetadata metadata;
    private final String detail;

    private RelationshipEvidence(boolean hasRelationship, Source source, RelationshipMetadata metadata, String detail) {
        this.hasRelationship = hasRelationship;
        this.source = Objects.requireNonNull(source, "source");
        this.metadata = metadata;
        this.detail = detail;
    }

    /**
     * Creates evidence for a detected relationship.
     *
     * @param source   source of evidence (not {@code null})
     * @param metadata relationship metadata (not {@code null})
     * @param detail   optional detail explaining the detection (nullable)
     * @return evidence (never {@code null})
     * @throws NullPointerException if source or metadata is null
     */
    public static RelationshipEvidence yes(Source source, RelationshipMetadata metadata, String detail) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(metadata, "metadata");
        if (source == Source.NOT_A_RELATIONSHIP) {
            throw new IllegalArgumentException("Cannot create positive evidence with NOT_A_RELATIONSHIP source");
        }
        return new RelationshipEvidence(true, source, metadata, detail);
    }

    /**
     * Creates evidence for no relationship detected (simple property).
     *
     * @return evidence (never {@code null})
     */
    public static RelationshipEvidence no() {
        return new RelationshipEvidence(false, Source.NOT_A_RELATIONSHIP, null, null);
    }

    /**
     * Creates evidence for no relationship with an explanatory detail.
     *
     * @param detail explanation why no relationship was detected
     * @return evidence (never {@code null})
     */
    public static RelationshipEvidence no(String detail) {
        return new RelationshipEvidence(false, Source.NOT_A_RELATIONSHIP, null, detail);
    }

    /**
     * Returns whether a relationship was detected.
     *
     * @return {@code true} if relationship detected
     */
    public boolean hasRelationship() {
        return hasRelationship;
    }

    /**
     * Returns the source of evidence that led to the classification.
     *
     * @return evidence source (never {@code null})
     */
    public Source source() {
        return source;
    }

    /**
     * Returns the relationship metadata if a relationship was detected.
     *
     * @return relationship metadata
     * @throws IllegalStateException if no relationship was detected
     */
    public RelationshipMetadata relationship() {
        if (!hasRelationship) {
            throw new IllegalStateException("No relationship detected - cannot access metadata");
        }
        return metadata;
    }

    /**
     * Returns the relationship metadata if present.
     *
     * @return relationship metadata or empty
     */
    public Optional<RelationshipMetadata> relationshipOptional() {
        return hasRelationship ? Optional.of(metadata) : Optional.empty();
    }

    /**
     * Returns the optional detail explaining the classification.
     *
     * @return detail if available
     */
    public Optional<String> detail() {
        return Optional.ofNullable(detail);
    }

    @Override
    public String toString() {
        if (hasRelationship) {
            String boundary = metadata.isInterAggregate() ? "inter-aggregate" : "intra-aggregate";
            return "Relationship{source=" + source + ", kind=" + metadata.kind() + ", " + boundary
                    + (detail != null ? ", detail=" + detail : "") + "}";
        }
        return "NoRelationship" + (detail != null ? "{" + detail + "}" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RelationshipEvidence that)) return false;
        return hasRelationship == that.hasRelationship
                && source == that.source
                && Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hasRelationship, source, metadata);
    }
}
