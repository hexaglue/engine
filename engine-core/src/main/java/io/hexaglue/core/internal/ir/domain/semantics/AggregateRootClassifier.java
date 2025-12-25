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
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.domain.normalize.AnnotationIndex;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import java.util.List;
import java.util.Objects;

/**
 * Classifies aggregate roots using explicit annotations and conservative heuristics.
 *
 * <p>This component is part of the ANALYZE phase. Its output must be stored in the IR
 * (e.g., {@link DomainTypeKind} or internal flags). SPI adapters must not re-run this logic.</p>
 *
 * <h2>Classification Strategy</h2>
 * <p>The classifier applies multiple signals in priority order:</p>
 * <ol>
 *   <li><strong>Pre-classified types:</strong> If {@code kind == AGGREGATE_ROOT}, accept immediately</li>
 *   <li><strong>Only entities:</strong> Only {@code ENTITY} types can become aggregate roots</li>
 *   <li><strong>Strong markers:</strong> Explicit {@code @AggregateRoot} annotations</li>
 *   <li><strong>Weak marker + confirmation:</strong> JPA {@code @Entity} with repository port</li>
 *   <li><strong>Package convention:</strong> Types in "aggregate" or "aggregates" packages</li>
 *   <li><strong>Naming convention:</strong> Types ending with "Aggregate" or "AggregateRoot"</li>
 * </ol>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Single decision point:</strong> All classification logic is centralized here</li>
 *   <li><strong>Explainable:</strong> Returns {@link AggregateRootEvidence} for diagnostics</li>
 *   <li><strong>Conservative:</strong> Prefers false negatives over false positives</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is safe for concurrent use if constructed with thread-safe dependencies.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AggregateRootClassifier classifier = new AggregateRootClassifier(
 *     new AggregateRootSignals(),
 *     new RepositoryPortMatcher()
 * );
 *
 * AggregateRootEvidence evidence = classifier.classify(
 *     domainType,
 *     AnnotationIndex.of(domainType.annotations()),
 *     allPorts
 * );
 *
 * if (evidence.isAggregateRoot()) {
 *     // Upgrade to AGGREGATE_ROOT kind
 *     kind = DomainTypeKind.AGGREGATE_ROOT;
 * }
 * }</pre>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal semantic classifier; not exposed to plugins")
public final class AggregateRootClassifier {

    private static final DiagnosticCode CODE_WEAK_SIGNAL_IGNORED = DiagnosticCode.of("HG-CORE-SEMANTICS-001");
    private static final DiagnosticCode CODE_CONVENTION_TRIGGERED = DiagnosticCode.of("HG-CORE-SEMANTICS-002");

    private final AggregateRootSignals signals;
    private final RepositoryPortMatcher repositoryPorts;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates an aggregate root classifier.
     *
     * @param signals         signals detector (not {@code null})
     * @param repositoryPorts repository port matcher (not {@code null})
     * @param diagnostics     diagnostic reporter (nullable - no diagnostics if null)
     * @throws NullPointerException if signals or repositoryPorts is null
     */
    public AggregateRootClassifier(
            AggregateRootSignals signals, RepositoryPortMatcher repositoryPorts, DiagnosticReporter diagnostics) {
        this.signals = Objects.requireNonNull(signals, "signals");
        this.repositoryPorts = Objects.requireNonNull(repositoryPorts, "repositoryPorts");
        this.diagnostics = diagnostics; // nullable
    }

    /**
     * Creates an aggregate root classifier with default components and no diagnostics.
     *
     * @return classifier (never {@code null})
     */
    public static AggregateRootClassifier defaults() {
        return new AggregateRootClassifier(new AggregateRootSignals(), new RepositoryPortMatcher(), null);
    }

    /**
     * Creates an aggregate root classifier with default components and diagnostic reporting.
     *
     * @param diagnostics diagnostic reporter (not {@code null})
     * @return classifier (never {@code null})
     * @throws NullPointerException if diagnostics is null
     */
    public static AggregateRootClassifier withDiagnostics(DiagnosticReporter diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        return new AggregateRootClassifier(new AggregateRootSignals(), new RepositoryPortMatcher(), diagnostics);
    }

    /**
     * Classifies a domain type as aggregate root or not.
     *
     * <p>This method applies the full classification strategy and returns evidence
     * explaining the decision. The evidence can be used for diagnostics, debugging,
     * and user feedback.</p>
     *
     * @param type        domain type (not {@code null})
     * @param annotations annotations index for this type (not {@code null})
     * @param allPorts    all project ports (not {@code null})
     * @return evidence (never {@code null})
     * @throws NullPointerException if type, annotations, or allPorts is null
     */
    public AggregateRootEvidence classify(DomainType type, AnnotationIndex annotations, List<Port> allPorts) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(annotations, "annotations");
        Objects.requireNonNull(allPorts, "allPorts");

        // 1. If already classified as AGGREGATE_ROOT, return true immediately
        if (type.kind() == DomainTypeKind.AGGREGATE_ROOT) {
            return AggregateRootEvidence.yes(AggregateRootEvidence.Kind.EXPLICIT_ANNOTATION, "kind=AGGREGATE_ROOT");
        }

        // 2. Only ENTITY types can be aggregate roots
        if (type.kind() != DomainTypeKind.ENTITY) {
            return AggregateRootEvidence.no();
        }

        // 3. Strong explicit markers (jMolecules @AggregateRoot, Spring @Document, etc.)
        if (signals.hasStrongAggregateMarker(annotations)) {
            return AggregateRootEvidence.yes(
                    AggregateRootEvidence.Kind.EXPLICIT_ANNOTATION, "Strong aggregate marker present");
        }

        // 4. Weak marker + repository port confirmation (JPA @Entity + repository)
        boolean hasJpaEntity = signals.hasJpaEntityMarker(annotations);
        boolean hasRepoPort = repositoryPorts.hasRepositoryPort(type, allPorts);

        if (hasJpaEntity && hasRepoPort) {
            return AggregateRootEvidence.yes(
                    AggregateRootEvidence.Kind.REPOSITORY_PORT, "JPA @Entity + repository port");
        }

        // Emit diagnostic if JPA @Entity present but no repository port (weak signal ignored)
        if (hasJpaEntity && !hasRepoPort && diagnostics != null) {
            diagnostics.info(
                    CODE_WEAK_SIGNAL_IGNORED,
                    "Type '" + type.simpleName()
                            + "' has JPA @Entity annotation but no repository port detected. "
                            + "Treating as internal entity. Add a repository port or use @AggregateRoot to mark as aggregate root.");
        }

        // 5. Convention-based fallbacks (conservative)

        // Package convention: types in "aggregate" or "aggregates" packages
        if (signals.isInAggregatePackage(type.qualifiedName())) {
            if (diagnostics != null) {
                diagnostics.info(
                        CODE_CONVENTION_TRIGGERED,
                        "Type '" + type.simpleName()
                                + "' classified as aggregate root based on package convention (*.aggregate(s).*)");
            }
            return AggregateRootEvidence.yes(
                    AggregateRootEvidence.Kind.PACKAGE_CONVENTION, "Package matches *.aggregate(s).*");
        }

        // Naming convention: types ending with "Aggregate" or "AggregateRoot"
        if (signals.hasAggregateRootName(type.simpleName())) {
            if (diagnostics != null) {
                diagnostics.info(
                        CODE_CONVENTION_TRIGGERED,
                        "Type '" + type.simpleName()
                                + "' classified as aggregate root based on naming convention (ends with Aggregate/AggregateRoot)");
            }
            return AggregateRootEvidence.yes(
                    AggregateRootEvidence.Kind.NAMING_CONVENTION, "Name ends with Aggregate/AggregateRoot");
        }

        // Default: For ENTITY types without clear signals, assume internal entity
        return AggregateRootEvidence.no();
    }
}
