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
package io.hexaglue.core.internal.ir.domain.analysis;

import io.hexaglue.core.diagnostics.DiagnosticFactory;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainService;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Main orchestrator for domain model analysis.
 *
 * <p>
 * The domain analyzer is the entry point for extracting and validating the complete domain
 * model from source code. It coordinates specialized extractors, applies validation rules,
 * and constructs the final {@link DomainModel} instance.
 * </p>
 *
 * <h2>Analysis Pipeline</h2>
 * <p>
 * The analyzer follows a multi-phase process:
 * </p>
 * <ol>
 *   <li><strong>Discovery:</strong> Identify domain type and service candidates</li>
 *   <li><strong>Extraction:</strong> Extract detailed information using specialized extractors</li>
 *   <li><strong>Validation:</strong> Apply domain rules and constraints</li>
 *   <li><strong>Construction:</strong> Build the immutable domain model</li>
 *   <li><strong>Indexing:</strong> Create lookup indexes for efficient querying</li>
 * </ol>
 *
 * <h2>Coordination</h2>
 * <p>
 * The analyzer coordinates the following components:
 * </p>
 * <ul>
 *   <li>{@link DomainTypeExtractor} - extracts domain types</li>
 *   <li>{@link DomainServiceExtractor} - extracts domain services</li>
 *   <li>{@link DomainPropertyExtractor} - extracts properties</li>
 *   <li>{@link DomainTypeKindResolver} - classifies types</li>
 *   <li>{@link DomainRules} - validates domain model</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The analyzer does not throw exceptions for domain modeling errors. Instead, it:
 * </p>
 * <ul>
 *   <li>Collects validation violations</li>
 *   <li>Reports them through the diagnostic system</li>
 *   <li>Continues analysis where possible</li>
 *   <li>Returns a best-effort domain model</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Extract all relevant domain information</li>
 *   <li><strong>Resilience:</strong> Handle partial or malformed models gracefully</li>
 *   <li><strong>Performance:</strong> Minimize redundant analysis</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are safe for concurrent use if constructed with thread-safe dependencies.
 * Each analysis produces an independent {@link DomainModel}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create analyzer with dependencies
 * DomainTypeKindResolver kindResolver = new DomainTypeKindResolver();
 * DomainPropertyExtractor propertyExtractor = new DomainPropertyExtractor();
 * DomainTypeExtractor typeExtractor = new DomainTypeExtractor(kindResolver, propertyExtractor);
 * DomainServiceExtractor serviceExtractor = new DomainServiceExtractor();
 * DomainRules rules = new DomainRules();
 *
 * DomainAnalyzer analyzer = new DomainAnalyzer(
 *     typeExtractor,
 *     serviceExtractor,
 *     rules
 * );
 *
 * // Analyze domain
 * Set<TypeElement> domainElements = ...;
 * DomainModel model = analyzer.analyze(domainElements);
 * }</pre>
 */
@InternalMarker(reason = "Internal domain analysis; not exposed to plugins")
public final class DomainAnalyzer {

    private static final DiagnosticCode CODE_DOMAIN_TYPE_EXTRACTION_FAILED = DiagnosticCode.of("HG-CORE-IR-100");
    private static final DiagnosticCode CODE_DOMAIN_SERVICE_EXTRACTION_FAILED = DiagnosticCode.of("HG-CORE-IR-103");

    // jMolecules annotation detection diagnostics (200 series)
    static final DiagnosticCode CODE_JMOLECULES_AGGREGATE_ROOT = DiagnosticCode.of("HG-CORE-IR-200");
    static final DiagnosticCode CODE_JMOLECULES_VALUE_OBJECT = DiagnosticCode.of("HG-CORE-IR-201");
    static final DiagnosticCode CODE_JMOLECULES_ENTITY = DiagnosticCode.of("HG-CORE-IR-202");
    static final DiagnosticCode CODE_JMOLECULES_DOMAIN_EVENT = DiagnosticCode.of("HG-CORE-IR-203");
    static final DiagnosticCode CODE_JMOLECULES_IDENTITY = DiagnosticCode.of("HG-CORE-IR-204");

    private final DomainTypeExtractor typeExtractor;
    private final DomainServiceExtractor serviceExtractor;
    private final DomainRules rules;
    private final TypeResolver typeResolver;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates a domain analyzer with the given dependencies.
     *
     * @param typeExtractor    domain type extractor (not {@code null})
     * @param serviceExtractor domain service extractor (not {@code null})
     * @param rules            domain validation rules (not {@code null})
     * @param typeResolver     type resolver (not {@code null})
     * @param diagnostics      diagnostic reporter for error reporting (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public DomainAnalyzer(
            DomainTypeExtractor typeExtractor,
            DomainServiceExtractor serviceExtractor,
            DomainRules rules,
            TypeResolver typeResolver,
            DiagnosticReporter diagnostics) {
        this.typeExtractor = Objects.requireNonNull(typeExtractor, "typeExtractor");
        this.serviceExtractor = Objects.requireNonNull(serviceExtractor, "serviceExtractor");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.typeResolver = Objects.requireNonNull(typeResolver, "typeResolver");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Analyzes a set of type elements and constructs the domain model.
     *
     * <p>
     * This implementation analyzes all provided elements, extracts domain types and services,
     * validates them, and builds a complete domain model.
     * </p>
     *
     * @param elements source elements to analyze (not {@code null}, must contain {@code TypeElement} instances)
     * @return analyzed domain model (never {@code null}, may be empty)
     * @throws NullPointerException if elements is null
     */
    public DomainModel analyze(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        // Extract domain types and services
        List<DomainType> types = analyzeDomainTypes(elements);
        List<DomainService> services = analyzeDomainServices(elements);

        // Build domain model
        return DomainModel.builder().addTypes(types).addServices(services).build();
    }

    /**
     * Analyzes domain types from a set of elements.
     *
     * <p>
     * This method filters and extracts only domain type elements, delegating to
     * {@link DomainTypeExtractor} for detailed extraction.
     * </p>
     *
     * @param elements source elements (not {@code null})
     * @return list of extracted domain types (never {@code null})
     */
    public List<DomainType> analyzeDomainTypes(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        List<DomainType> types = new ArrayList<>();

        for (Object element : elements) {
            if (element instanceof TypeElement te) {
                // Check if it's a valid domain type candidate
                if (typeExtractor.isDomainType(te)) {
                    try {
                        // Create TypeRef for this element
                        TypeRef typeRef = typeResolver.resolveFromElement(te);

                        // Extract domain type
                        Optional<DomainType> domainType = typeExtractor.extract(te, typeRef);

                        // Add if successfully extracted
                        domainType.ifPresent(types::add);
                    } catch (Exception e) {
                        // Report extraction failure as diagnostic
                        diagnostics.report(DiagnosticFactory.errorWithCause(
                                CODE_DOMAIN_TYPE_EXTRACTION_FAILED,
                                "Failed to extract domain type '" + te.getSimpleName() + "': " + e.getMessage(),
                                te,
                                "io.hexaglue.core",
                                e));
                        // Continue processing other elements
                    }
                }
            }
        }

        return types;
    }

    /**
     * Analyzes domain services from a set of elements.
     *
     * <p>
     * This method filters and extracts only domain service elements, delegating to
     * {@link DomainServiceExtractor} for detailed extraction.
     * </p>
     *
     * @param elements source elements (not {@code null})
     * @return list of extracted domain services (never {@code null})
     */
    public List<DomainService> analyzeDomainServices(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        List<DomainService> services = new ArrayList<>();

        for (Object element : elements) {
            if (element instanceof TypeElement te) {
                try {
                    // Extract domain service
                    Optional<DomainService> domainService = serviceExtractor.extract(te);

                    // Add if successfully extracted
                    domainService.ifPresent(services::add);
                } catch (Exception e) {
                    // Report extraction failure as diagnostic
                    diagnostics.report(DiagnosticFactory.errorWithCause(
                            CODE_DOMAIN_SERVICE_EXTRACTION_FAILED,
                            "Failed to extract domain service '" + te.getSimpleName() + "': " + e.getMessage(),
                            te,
                            "io.hexaglue.core",
                            e));
                    // Continue processing other elements
                }
            }
        }

        return services;
    }

    /**
     * Validates a domain model and returns all violations.
     *
     * <p>
     * This method applies all validation rules to the domain model and collects
     * any violations found. Violations are returned as human-readable messages.
     * </p>
     *
     * @param model domain model to validate (not {@code null})
     * @return list of validation violation messages (empty if valid)
     * @throws NullPointerException if model is null
     */
    public List<String> validate(DomainModel model) {
        Objects.requireNonNull(model, "model");

        List<String> violations = new ArrayList<>();

        // Validate all domain types
        for (DomainType type : model.types()) {
            violations.addAll(rules.validateDomainType(type));
        }

        // Validate all domain services
        for (DomainService service : model.services()) {
            violations.addAll(rules.validateDomainService(service));
        }

        return violations;
    }

    /**
     * Creates a default domain analyzer with standard dependencies.
     *
     * <p>
     * This factory method is convenient for typical use cases where default behavior
     * is acceptable.
     * </p>
     *
     * @param elements    element utilities from processing environment (not {@code null})
     * @param types       type utilities from processing environment (not {@code null})
     * @param diagnostics diagnostic reporter for error reporting (not {@code null})
     * @return domain analyzer with default configuration (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public static DomainAnalyzer createDefault(Elements elements, Types types, DiagnosticReporter diagnostics) {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(diagnostics, "diagnostics");

        TypeResolver typeResolver = TypeResolver.create(elements, types);
        DomainTypeKindResolver kindResolver = new DomainTypeKindResolver();
        DomainPropertyExtractor propertyExtractor = new DomainPropertyExtractor(typeResolver);
        DomainRules rules = new DomainRules();
        DomainTypeExtractor typeExtractor =
                new DomainTypeExtractor(kindResolver, propertyExtractor, rules, typeResolver, elements, diagnostics);
        DomainServiceExtractor serviceExtractor = new DomainServiceExtractor(elements);

        return new DomainAnalyzer(typeExtractor, serviceExtractor, rules, typeResolver, diagnostics);
    }
}
