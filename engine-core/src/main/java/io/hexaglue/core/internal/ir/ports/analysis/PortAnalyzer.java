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
package io.hexaglue.core.internal.ir.ports.analysis;

import io.hexaglue.core.diagnostics.DiagnosticFactory;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Main orchestrator for port model analysis.
 *
 * <p>
 * The port analyzer is the entry point for extracting and validating the complete port
 * model from source code. It coordinates specialized extractors, applies validation rules,
 * and constructs the final {@link PortModel} instance representing all discovered port contracts.
 * </p>
 *
 * <h2>Analysis Pipeline</h2>
 * <p>
 * The analyzer follows a multi-phase process:
 * </p>
 * <ol>
 *   <li><strong>Discovery:</strong> Identify port interface candidates</li>
 *   <li><strong>Filtering:</strong> Apply rules to determine which interfaces are ports</li>
 *   <li><strong>Extraction:</strong> Extract detailed port information using {@link PortExtractor}</li>
 *   <li><strong>Validation:</strong> Apply port rules and constraints using {@link PortRules}</li>
 *   <li><strong>Construction:</strong> Build the immutable port model</li>
 *   <li><strong>Indexing:</strong> Create lookup indexes for efficient querying</li>
 * </ol>
 *
 * <h2>Coordination</h2>
 * <p>
 * The analyzer coordinates the following components:
 * </p>
 * <ul>
 *   <li>{@link PortExtractor} - extracts port contracts from interfaces</li>
 *   <li>{@link PortDirectionResolver} - determines port direction (driving vs driven)</li>
 *   <li>{@link PortRules} - validates port contracts</li>
 * </ul>
 *
 * <h2>Port Discovery Strategy</h2>
 * <p>
 * The analyzer identifies ports using the following heuristics:
 * </p>
 * <ul>
 *   <li>Element must be an interface (not class, enum, etc.)</li>
 *   <li>Must have at least one non-default, non-static method</li>
 *   <li>Must not be from JDK or common libraries</li>
 *   <li>Package name should suggest port role (ports, api, spi, repository, gateway, etc.)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The analyzer does not throw exceptions for port modeling errors. Instead, it:
 * </p>
 * <ul>
 *   <li>Collects validation violations</li>
 *   <li>Reports them through the diagnostic system</li>
 *   <li>Continues analysis where possible</li>
 *   <li>Returns a best-effort port model</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Extract all relevant port information</li>
 *   <li><strong>Resilience:</strong> Handle partial or malformed models gracefully</li>
 *   <li><strong>Performance:</strong> Minimize redundant analysis</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are safe for concurrent use if constructed with thread-safe dependencies.
 * Each analysis produces an independent {@link PortModel}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create analyzer with dependencies
 * Elements elements = processingEnv.getElementUtils();
 * Types types = processingEnv.getTypeUtils();
 * PortAnalyzer analyzer = PortAnalyzer.createDefault(elements, types);
 *
 * // Analyze ports
 * Set<TypeElement> interfaceElements = ...;
 * PortModel model = analyzer.analyze(interfaceElements);
 *
 * // Query results
 * List<Port> drivenPorts = model.drivenPorts();
 * }</pre>
 */
@InternalMarker(reason = "Internal port analysis; not exposed to plugins")
public final class PortAnalyzer {

    private static final DiagnosticCode CODE_PORT_EXTRACTION_FAILED = DiagnosticCode.of("HG-CORE-IR-200");
    private static final DiagnosticCode CODE_PORT_VALIDATION_FAILED = DiagnosticCode.of("HG-CORE-IR-201");

    // jMolecules annotation detection diagnostics (port series)
    static final DiagnosticCode CODE_JMOLECULES_REPOSITORY = DiagnosticCode.of("HG-CORE-IR-001");

    private final PortExtractor portExtractor;
    private final PortRules rules;
    private final DiagnosticReporter diagnostics;
    private final RepositoryAnnotationDetector repositoryAnnotationDetector;

    /**
     * Creates a port analyzer with the given dependencies.
     *
     * @param portExtractor port extractor (not {@code null})
     * @param rules         port validation rules (not {@code null})
     * @param diagnostics   diagnostic reporter for error reporting (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public PortAnalyzer(PortExtractor portExtractor, PortRules rules, DiagnosticReporter diagnostics) {
        this.portExtractor = Objects.requireNonNull(portExtractor, "portExtractor");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
        this.repositoryAnnotationDetector = new RepositoryAnnotationDetector();
    }

    /**
     * Creates a default port analyzer with standard configuration.
     *
     * @param elements    element utilities from annotation processing environment (not {@code null})
     * @param types       type utilities from annotation processing environment (not {@code null})
     * @param diagnostics diagnostic reporter for error reporting (not {@code null})
     * @return port analyzer instance (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public static PortAnalyzer createDefault(Elements elements, Types types, DiagnosticReporter diagnostics) {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(diagnostics, "diagnostics");

        TypeResolver typeResolver = TypeResolver.create(elements, types);
        PortDirectionResolver directionResolver = new PortDirectionResolver();
        PortExtractor extractor = new PortExtractor(directionResolver, typeResolver, elements);
        PortRules portRules = new PortRules();

        return new PortAnalyzer(extractor, portRules, diagnostics);
    }

    /**
     * Analyzes a set of type elements and constructs the port model.
     *
     * <p>
     * This implementation analyzes all provided elements, extracts port contracts,
     * validates them, and builds a complete port model.
     * </p>
     *
     * @param elements source elements to analyze (not {@code null}, must contain {@code TypeElement} instances)
     * @return analyzed port model (never {@code null}, may be empty)
     * @throws NullPointerException if elements is null
     */
    public PortModel analyze(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        // Extract ports
        List<Port> ports = analyzePorts(elements);

        // Build port model
        return PortModel.builder().addPorts(ports).build();
    }

    /**
     * Analyzes ports from a set of elements.
     *
     * <p>
     * This method filters and extracts only port interface elements, delegating to
     * {@link PortExtractor} for detailed extraction.
     * </p>
     *
     * @param elements source elements (not {@code null})
     * @return list of extracted ports (never {@code null})
     */
    public List<Port> analyzePorts(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        List<Port> ports = new ArrayList<>();

        for (Object element : elements) {
            if (element instanceof TypeElement te) {
                // Check if it's a valid port candidate
                if (isPortCandidate(te)) {
                    try {
                        // Extract port
                        Optional<Port> port = portExtractor.extract(te);

                        // Add if successfully extracted and valid
                        if (port.isPresent()) {
                            Port p = port.get();

                            // Validate port
                            List<ValidationIssue> issues = rules.validatePort(p);

                            // Check if there are any errors (warnings don't block port inclusion)
                            boolean hasErrors = issues.stream().anyMatch(ValidationIssue::isError);

                            if (!hasErrors) {
                                // Add port even if there are warnings
                                ports.add(p);
                            }

                            // Report all validation issues with appropriate severity
                            for (ValidationIssue issue : issues) {
                                if (issue.isError()) {
                                    diagnostics.report(DiagnosticFactory.error(
                                            CODE_PORT_VALIDATION_FAILED,
                                            "Port '" + te.getSimpleName() + "' has invalid structure: "
                                                    + issue.message(),
                                            te,
                                            "io.hexaglue.core"));
                                } else {
                                    diagnostics.report(DiagnosticFactory.warning(
                                            CODE_PORT_VALIDATION_FAILED,
                                            "Port '" + te.getSimpleName() + "': " + issue.message(),
                                            te,
                                            "io.hexaglue.core"));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Report extraction failure as diagnostic
                        diagnostics.report(DiagnosticFactory.errorWithCause(
                                CODE_PORT_EXTRACTION_FAILED,
                                "Failed to extract port '" + te.getSimpleName() + "': " + e.getMessage(),
                                te,
                                "io.hexaglue.core",
                                e));
                        // Continue processing other elements
                    }
                }
            }
        }

        return ports;
    }

    /**
     * Determines if a type element is a port candidate.
     *
     * <p>
     * This method applies heuristics to filter out non-port interfaces.
     * </p>
     *
     * @param typeElement type element to check (not {@code null})
     * @return {@code true} if likely a port interface
     */
    private boolean isPortCandidate(TypeElement typeElement) {
        // Must be an interface
        if (typeElement.getKind() != ElementKind.INTERFACE) {
            return false;
        }

        String qualifiedName = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();
        String packageName = extractPackageName(qualifiedName);

        // Count non-default, non-static methods
        long contractMethods = typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> !e.getModifiers().contains(javax.lang.model.element.Modifier.DEFAULT))
                .filter(e -> !e.getModifiers().contains(javax.lang.model.element.Modifier.STATIC))
                .count();

        // Detect jMolecules @Repository annotation
        boolean hasRepositoryAnnotation = repositoryAnnotationDetector.hasRepositoryAnnotation(typeElement);
        if (hasRepositoryAnnotation) {
            diagnostics.report(DiagnosticFactory.info(
                    CODE_JMOLECULES_REPOSITORY,
                    "jMolecules or Spring @Repository detected on port '" + simpleName + "'",
                    typeElement,
                    "io.hexaglue.core"));
        }

        // Use rules to determine if it's a port
        return rules.isLikelyPortInterface(qualifiedName, packageName, (int) contractMethods, hasRepositoryAnnotation);
    }

    /**
     * Extracts package name from qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return package name, or empty string if in default package
     */
    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? "" : qualifiedName.substring(0, lastDot);
    }
}
