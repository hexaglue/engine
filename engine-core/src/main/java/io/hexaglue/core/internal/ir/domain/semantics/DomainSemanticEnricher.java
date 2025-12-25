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
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.domain.normalize.AnnotationIndex;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Enriches domain models with semantic information from cross-model analysis.
 *
 * <p>This enricher applies semantic classification that requires information from multiple
 * models (domain + ports). It is executed after basic domain and port analysis, but before
 * the final IR snapshot is created.</p>
 *
 * <h2>Enrichment Strategy</h2>
 * <p>The enricher applies:</p>
 * <ul>
 *   <li><strong>Aggregate root reclassification:</strong> Upgrades {@code ENTITY} types
 *       to {@code AGGREGATE_ROOT} based on heuristics (repository ports, packages, naming)</li>
 *   <li><strong>Future:</strong> Additional semantic enrichments as needed</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>This enricher solves the chicken-and-egg problem where aggregate root classification
 * needs port information, but ports are analyzed after domain types. By running after both
 * analyses, we can make informed cross-model decisions.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is safe for concurrent use if constructed with thread-safe dependencies.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // After analyzing domain and ports separately
 * DomainModel initialDomain = domainAnalyzer.analyze(elements);
 * PortModel ports = portAnalyzer.analyze(elements);
 *
 * // Enrich domain with cross-model semantics
 * DomainSemanticEnricher enricher = DomainSemanticEnricher.defaults();
 * DomainModel enrichedDomain = enricher.enrich(initialDomain, ports);
 *
 * // Create IR snapshot with enriched domain
 * IrSnapshot snapshot = IrSnapshot.builder()
 *     .domainModel(enrichedDomain)
 *     .portModel(ports)
 *     .build();
 * }</pre>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal semantic enrichment; not exposed to plugins")
public final class DomainSemanticEnricher {

    private final AggregateRootClassifier aggregateRootClassifier;
    private final RelationshipClassifier relationshipClassifier;

    /**
     * Creates a domain semantic enricher.
     *
     * @param aggregateRootClassifier aggregate root classifier (not {@code null})
     * @param relationshipClassifier relationship classifier (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public DomainSemanticEnricher(
            AggregateRootClassifier aggregateRootClassifier, RelationshipClassifier relationshipClassifier) {
        this.aggregateRootClassifier = Objects.requireNonNull(aggregateRootClassifier, "aggregateRootClassifier");
        this.relationshipClassifier = Objects.requireNonNull(relationshipClassifier, "relationshipClassifier");
    }

    /**
     * Creates a domain semantic enricher with default components (no diagnostics).
     *
     * @return enricher (never {@code null})
     */
    public static DomainSemanticEnricher defaults() {
        return new DomainSemanticEnricher(AggregateRootClassifier.defaults(), RelationshipClassifier.defaults());
    }

    /**
     * Creates a domain semantic enricher with diagnostic reporting.
     *
     * @param diagnostics diagnostic reporter (not {@code null})
     * @return enricher (never {@code null})
     * @throws NullPointerException if diagnostics is null
     */
    public static DomainSemanticEnricher withDiagnostics(DiagnosticReporter diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        return new DomainSemanticEnricher(
                AggregateRootClassifier.withDiagnostics(diagnostics),
                RelationshipClassifier.withDiagnostics(diagnostics));
    }

    /**
     * Enriches a domain model with semantic information from port analysis.
     *
     * <p>This method reclassifies domain types that were initially marked as {@code ENTITY}
     * but should be {@code AGGREGATE_ROOT} based on heuristics that require port information.</p>
     *
     * @param domainModel initial domain model (not {@code null})
     * @param portModel   port model for cross-model analysis (not {@code null})
     * @return enriched domain model (never {@code null})
     * @throws NullPointerException if domainModel or portModel is null
     */
    public DomainModel enrich(DomainModel domainModel, PortModel portModel) {
        Objects.requireNonNull(domainModel, "domainModel");
        Objects.requireNonNull(portModel, "portModel");

        List<Port> allPorts = portModel.ports();
        List<DomainType> enrichedTypes = new ArrayList<>();
        boolean anyChanged = false;

        for (DomainType type : domainModel.types()) {
            DomainType enrichedType = enrichType(type, allPorts, domainModel);
            enrichedTypes.add(enrichedType);
            if (enrichedType != type) {
                anyChanged = true;
            }
        }

        // If nothing changed, return the original model
        if (!anyChanged) {
            return domainModel;
        }

        // Rebuild domain model with enriched types
        return DomainModel.builder()
                .addTypes(enrichedTypes)
                .addServices(domainModel.services())
                .build();
    }

    /**
     * Enriches a single domain type with semantic information.
     *
     * <p>This method applies:</p>
     * <ul>
     *   <li>Aggregate root classification (type-level)</li>
     *   <li>Relationship detection (property-level)</li>
     * </ul>
     *
     * @param type domain type to enrich (not {@code null})
     * @param allPorts all ports for cross-model analysis (not {@code null})
     * @param domainModel complete domain model for relationship classification (not {@code null})
     * @return enriched domain type (may be the same instance if no changes) (never {@code null})
     */
    private DomainType enrichType(DomainType type, List<Port> allPorts, DomainModel domainModel) {
        // Build annotation index for type classification
        AnnotationIndex annotations = AnnotationIndex.of(type.annotations());

        // Step 1: Classify aggregate root (type-level enrichment)
        DomainTypeKind finalKind = type.kind();
        boolean typeKindChanged = false;

        // Only consider ENTITY types for reclassification
        // Types already marked as AGGREGATE_ROOT (via annotations) are left unchanged
        if (type.kind() == DomainTypeKind.ENTITY) {
            AggregateRootEvidence evidence = aggregateRootClassifier.classify(type, annotations, allPorts);
            if (evidence.isAggregateRoot()) {
                finalKind = DomainTypeKind.AGGREGATE_ROOT;
                typeKindChanged = true;
            }
        }

        // Step 2: Enrich properties with relationship metadata
        List<DomainProperty> enrichedProperties = new ArrayList<>();
        boolean anyPropertyChanged = false;

        for (DomainProperty property : type.properties()) {
            DomainProperty enrichedProperty = enrichProperty(property, domainModel);
            enrichedProperties.add(enrichedProperty);
            if (enrichedProperty != property) {
                anyPropertyChanged = true;
            }
        }

        // If nothing changed, return original type
        if (!typeKindChanged && !anyPropertyChanged) {
            return type;
        }

        // Rebuild type with enriched kind and/or properties
        return DomainType.builder()
                .qualifiedName(type.qualifiedName())
                .simpleName(type.simpleName())
                .kind(finalKind)
                .type(type.type())
                .addProperties(enrichedProperties)
                .id(type.id().orElse(null))
                .immutable(type.isImmutable())
                .description(type.description().orElse(null))
                .sourceRef(type.sourceRef().orElse(null))
                .annotations(type.annotations())
                .build();
    }

    /**
     * Enriches a single property with relationship metadata.
     *
     * @param property property to enrich (not {@code null})
     * @param domainModel complete domain model for relationship classification (not {@code null})
     * @return enriched property (may be the same instance if no changes) (never {@code null})
     */
    private DomainProperty enrichProperty(DomainProperty property, DomainModel domainModel) {
        // Skip if property already has relationship metadata
        if (property.relationship().isPresent()) {
            return property;
        }

        // Classify relationship using the relationship classifier
        RelationshipEvidence evidence = relationshipClassifier.classify(property, domainModel);

        // If no relationship detected, return unchanged
        if (!evidence.hasRelationship()) {
            return property;
        }

        // Rebuild property with relationship metadata
        RelationshipMetadata metadata = evidence.relationship();
        return DomainProperty.builder()
                .name(property.name())
                .type(property.type())
                .identity(property.isIdentity())
                .immutable(property.isImmutable())
                .declaringType(property.declaringType().orElse(null))
                .description(property.description().orElse(null))
                .sourceRef(property.sourceRef().orElse(null))
                .annotations(property.annotations())
                .relationshipMetadata(metadata)
                .build();
    }
}
