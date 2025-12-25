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
import io.hexaglue.spi.diagnostics.Diagnostic;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.diagnostics.DiagnosticSeverity;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.RelationshipKind;
import io.hexaglue.spi.ir.domain.RelationshipMetadata;
import java.util.Objects;
import java.util.Optional;

/**
 * Classifies domain properties as relationships using jMolecules annotations and heuristics.
 *
 * <p>This component detects relationships between domain types and determines whether they
 * cross aggregate boundaries (inter-aggregate) or stay within the same aggregate (intra-aggregate).
 * This classification is critical for generating correct JPA mappings that respect DDD principles.</p>
 *
 * <h2>Classification Strategy</h2>
 * <p>The classifier applies signals in priority order:</p>
 * <ol>
 *   <li><strong>jMolecules @Association:</strong> Explicit inter-aggregate marker</li>
 *   <li><strong>Target type annotations:</strong> {@code @AggregateRoot}, {@code @Entity}, {@code @ValueObject}</li>
 *   <li><strong>ID type heuristics:</strong> Property type ends with "Id" or has {@code @Identity}</li>
 *   <li><strong>Collection analysis:</strong> Detect {@code List<Entity>} vs {@code List<String>}</li>
 *   <li><strong>Target type kind:</strong> Check domain model classification</li>
 * </ol>
 *
 * <h2>DDD Compliance</h2>
 * <p>The classifier enforces the DDD rule that <strong>references between aggregates MUST be by ID only</strong>:</p>
 * <ul>
 *   <li>✅ {@code Order → CustomerId} (inter-aggregate, ID-only reference)</li>
 *   <li>✅ {@code Order → List<OrderItem>} (intra-aggregate, full objects)</li>
 *   <li>✅ {@code Customer → Address} (value object, embedded)</li>
 *   <li>❌ {@code Order → Customer} (inter-aggregate with full object - VIOLATION!)</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * RelationshipClassifier classifier = new RelationshipClassifier(
 *     new RelationshipSignals(),
 *     diagnostics  // Pass diagnostic reporter to get DDD violation warnings
 * );
 *
 * for (DomainProperty property : domainType.properties()) {
 *     RelationshipEvidence evidence = classifier.classify(
 *         property,
 *         domainModel
 *     );
 *
 *     if (evidence.hasRelationship()) {
 *         RelationshipMetadata rel = evidence.relationship();
 *         if (rel.isInterAggregate()) {
 *             // Generate FK column only
 *         } else {
 *             // Generate full relationship mapping
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is safe for concurrent use if constructed with thread-safe dependencies.</p>
 *
 * @since 0.4.0
 */
@InternalMarker(reason = "Internal semantic classifier; not exposed to plugins")
public final class RelationshipClassifier {

    private static final DiagnosticCode CODE_DDD_VIOLATION_AGGREGATE_REF = DiagnosticCode.of("HG-SEMANTICS-001");

    private final RelationshipSignals signals;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a relationship classifier.
     *
     * @param signals relationship signals detector (not {@code null})
     * @param diagnostics diagnostic reporter (nullable - no diagnostics if null)
     * @throws NullPointerException if signals is null
     */
    public RelationshipClassifier(RelationshipSignals signals, DiagnosticReporter diagnostics) {
        this.signals = Objects.requireNonNull(signals, "signals");
        this.diagnostics = diagnostics; // nullable
    }

    /**
     * Creates a relationship classifier with default components and no diagnostics.
     *
     * @return classifier (never {@code null})
     */
    public static RelationshipClassifier defaults() {
        return new RelationshipClassifier(new RelationshipSignals(), null);
    }

    /**
     * Creates a relationship classifier with diagnostic reporting.
     *
     * @param diagnostics diagnostic reporter (not {@code null})
     * @return classifier (never {@code null})
     * @throws NullPointerException if diagnostics is null
     */
    public static RelationshipClassifier withDiagnostics(DiagnosticReporter diagnostics) {
        Objects.requireNonNull(diagnostics, "diagnostics");
        return new RelationshipClassifier(new RelationshipSignals(), diagnostics);
    }

    /**
     * Classifies a property as a relationship (or not) and produces evidence.
     *
     * <p>This method analyzes the property's type, annotations, and domain model context
     * to determine if it represents a relationship and what kind.</p>
     *
     * @param property property to classify
     * @param domainModel domain model for target type lookup
     * @return relationship evidence (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public RelationshipEvidence classify(DomainProperty property, DomainModel domainModel) {
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(domainModel, "domainModel");

        AnnotationIndex propertyAnnotations = AnnotationIndex.of(property.annotations());
        String propertyTypeName = property.type().render();

        // Priority 1: Explicit @Association annotation
        if (signals.hasAssociationMarker(propertyAnnotations)) {
            return classifyExplicitAssociation(property, domainModel, propertyTypeName);
        }

        // Priority 2: Check if property type is a domain type in the model
        Optional<DomainType> targetType = domainModel.findType(propertyTypeName);
        if (targetType.isPresent()) {
            return classifyDomainTypeReference(property, targetType.get(), domainModel);
        }

        // Priority 3: Heuristic - Property type name suggests ID reference
        if (signals.typeNameSuggestsId(propertyTypeName)) {
            return classifyIdTypeReference(property, propertyTypeName, domainModel);
        }

        // Priority 4: Check for collections
        if (signals.isCollectionType(propertyTypeName)) {
            return classifyCollectionProperty(property, domainModel);
        }

        // Not a relationship - simple property (String, Integer, LocalDateTime, etc.)
        return RelationshipEvidence.no("Simple property type: " + propertyTypeName);
    }

    // ========================================
    // Private Classification Methods
    // ========================================

    /**
     * Classifies property explicitly marked with @Association.
     */
    private RelationshipEvidence classifyExplicitAssociation(
            DomainProperty property, DomainModel domainModel, String propertyTypeName) {

        // @Association → inter-aggregate reference
        // Try to find the target aggregate (may be an ID type referencing the aggregate)
        String targetTypeName = signals.typeNameSuggestsId(propertyTypeName)
                ? signals.extractEntityNameFromIdType(propertyTypeName)
                : propertyTypeName;

        Optional<DomainType> targetAggregate = domainModel.findType(targetTypeName);

        RelationshipKind kind = determineRelationshipKind(property, targetAggregate.orElse(null));

        RelationshipMetadata metadata = RelationshipMetadata.of(kind, targetTypeName, true /* inter-aggregate */);

        return RelationshipEvidence.yes(
                RelationshipEvidence.Source.JMOLECULES_ANNOTATION,
                metadata,
                "@Association → inter-aggregate to " + targetTypeName);
    }

    /**
     * Classifies property whose type is a known domain type.
     */
    private RelationshipEvidence classifyDomainTypeReference(
            DomainProperty property, DomainType targetType, DomainModel domainModel) {

        AnnotationIndex targetAnnotations = AnnotationIndex.of(targetType.annotations());

        // Check if target is aggregate root → inter-aggregate
        if (targetType.kind() == DomainTypeKind.AGGREGATE_ROOT || signals.targetIsAggregateRoot(targetAnnotations)) {

            // ⚠️ VIOLATION: Full object reference to aggregate root (should be ID-only)
            // Report diagnostic warning about DDD violation
            if (diagnostics != null) {
                diagnostics.report(Diagnostic.builder()
                        .severity(DiagnosticSeverity.WARNING)
                        .code(CODE_DDD_VIOLATION_AGGREGATE_REF)
                        .message("DDD violation: Property '" + property.name()
                                + "' references aggregate root '" + targetType.qualifiedName()
                                + "' directly. Inter-aggregate references should use ID types only (e.g., "
                                + targetType.simpleName() + "Id).")
                        .build());
            }

            // But we still classify it - validation can warn about it
            RelationshipKind kind = determineRelationshipKind(property, targetType);
            RelationshipMetadata metadata =
                    RelationshipMetadata.of(kind, targetType.qualifiedName(), true /* inter-aggregate */);

            return RelationshipEvidence.yes(
                    RelationshipEvidence.Source.HEURISTIC,
                    metadata,
                    "Target is aggregate root (DDD violation: should use ID-only) → " + targetType.qualifiedName());
        }

        // Check if target is value object → embedded
        if (targetType.kind() == DomainTypeKind.VALUE_OBJECT || signals.targetIsValueObject(targetAnnotations)) {

            RelationshipMetadata metadata = RelationshipMetadata.of(
                    RelationshipKind.ONE_TO_ONE, targetType.qualifiedName(), false /* intra-aggregate */);

            return RelationshipEvidence.yes(
                    RelationshipEvidence.Source.JMOLECULES_ANNOTATION,
                    metadata,
                    "@ValueObject target → embedded " + targetType.qualifiedName());
        }

        // Check if target is internal entity → intra-aggregate
        if (targetType.kind() == DomainTypeKind.ENTITY || signals.targetIsInternalEntity(targetAnnotations)) {

            RelationshipKind kind = determineRelationshipKind(property, targetType);
            RelationshipMetadata metadata =
                    RelationshipMetadata.of(kind, targetType.qualifiedName(), false /* intra-aggregate */);

            return RelationshipEvidence.yes(
                    RelationshipEvidence.Source.JMOLECULES_ANNOTATION,
                    metadata,
                    "@Entity target → intra-aggregate " + targetType.qualifiedName());
        }

        // Default: assume intra-aggregate relationship
        RelationshipKind kind = determineRelationshipKind(property, targetType);
        RelationshipMetadata metadata =
                RelationshipMetadata.of(kind, targetType.qualifiedName(), false /* intra-aggregate */);

        return RelationshipEvidence.yes(
                RelationshipEvidence.Source.HEURISTIC,
                metadata,
                "Domain type reference → intra-aggregate " + targetType.qualifiedName());
    }

    /**
     * Classifies property whose type name suggests it's an ID reference.
     */
    private RelationshipEvidence classifyIdTypeReference(
            DomainProperty property, String idTypeName, DomainModel domainModel) {

        // Extract entity name from ID type (CustomerId → Customer)
        String entityName = signals.extractEntityNameFromIdType(idTypeName);

        // Try to find the target aggregate in domain model
        Optional<DomainType> targetAggregate = domainModel.findType(entityName);

        RelationshipKind kind = determineRelationshipKind(property, targetAggregate.orElse(null));

        RelationshipMetadata metadata = RelationshipMetadata.of(kind, entityName, true /* inter-aggregate */);

        return RelationshipEvidence.yes(
                RelationshipEvidence.Source.HEURISTIC,
                metadata,
                "ID type name heuristic (" + idTypeName + ") → inter-aggregate to " + entityName);
    }

    /**
     * Classifies collection property (List, Set, etc.).
     */
    private RelationshipEvidence classifyCollectionProperty(DomainProperty property, DomainModel domainModel) {

        // TODO: Extract element type from generic signature
        // For now, return no relationship - needs TypeRef enhancement to support generics
        return RelationshipEvidence.no("Collection type - element type analysis not yet implemented");
    }

    /**
     * Determines the relationship kind based on property type and target type.
     */
    private RelationshipKind determineRelationshipKind(DomainProperty property, DomainType targetType) {

        // Check if property is a collection
        String typeName = property.type().render();
        if (signals.isCollectionType(typeName)) {
            return RelationshipKind.ONE_TO_MANY;
        }

        // Single reference
        return RelationshipKind.MANY_TO_ONE;
    }
}
