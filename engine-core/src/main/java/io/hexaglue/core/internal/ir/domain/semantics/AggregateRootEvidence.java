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
import java.util.Objects;
import java.util.Optional;

/**
 * Evidence explaining why a domain type was classified as an aggregate root (or not).
 *
 * <p>This class captures the reasoning behind aggregate root classification decisions,
 * which is essential for diagnostics, debugging, and user transparency.</p>
 *
 * <h2>Design Rationale</h2>
 * <p>Rather than returning a simple boolean, we return evidence that includes:</p>
 * <ul>
 *   <li>The classification result (yes/no)</li>
 *   <li>The kind of signal that triggered the classification</li>
 *   <li>Optional detail explaining the specific match</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AggregateRootEvidence evidence = classifier.classify(type, annotations, allPorts);
 * if (evidence.isAggregateRoot()) {
 *     log.debug("Type {} classified as aggregate root: {}",
 *         type.qualifiedName(), evidence.detail());
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal semantics evidence; not exposed to plugins")
public final class AggregateRootEvidence {

    /**
     * Kind of evidence that led to the classification.
     */
    public enum Kind {
        /** Explicit @AggregateRoot or similar annotation */
        EXPLICIT_ANNOTATION,

        /** Type has a corresponding repository port */
        REPOSITORY_PORT,

        /** Type is in an "aggregate" or "aggregates" package */
        PACKAGE_CONVENTION,

        /** Type name ends with "Aggregate" or "AggregateRoot" */
        NAMING_CONVENTION,

        /** Type was declared as aggregate root in configuration (future) */
        CONFIGURATION,

        /** No evidence found (not an aggregate root) */
        NONE
    }

    private final boolean aggregateRoot;
    private final Kind kind;
    private final String detail;

    private AggregateRootEvidence(boolean aggregateRoot, Kind kind, String detail) {
        this.aggregateRoot = aggregateRoot;
        this.kind = Objects.requireNonNull(kind, "kind");
        this.detail = detail;
    }

    /**
     * Creates evidence for a positive classification (is an aggregate root).
     *
     * @param kind   kind of evidence (not {@code null})
     * @param detail optional detail explaining the match (nullable)
     * @return evidence (never {@code null})
     * @throws NullPointerException if kind is null
     */
    public static AggregateRootEvidence yes(Kind kind, String detail) {
        Objects.requireNonNull(kind, "kind");
        return new AggregateRootEvidence(true, kind, detail);
    }

    /**
     * Creates evidence for a negative classification (not an aggregate root).
     *
     * @return evidence (never {@code null})
     */
    public static AggregateRootEvidence no() {
        return new AggregateRootEvidence(false, Kind.NONE, null);
    }

    /**
     * Returns whether the type is classified as an aggregate root.
     *
     * @return {@code true} if aggregate root
     */
    public boolean isAggregateRoot() {
        return aggregateRoot;
    }

    /**
     * Returns the kind of evidence that led to the classification.
     *
     * @return evidence kind (never {@code null})
     */
    public Kind kind() {
        return kind;
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
        if (aggregateRoot) {
            return "AggregateRoot{kind=" + kind + ", detail=" + (detail != null ? detail : "none") + "}";
        }
        return "NotAggregateRoot";
    }
}
