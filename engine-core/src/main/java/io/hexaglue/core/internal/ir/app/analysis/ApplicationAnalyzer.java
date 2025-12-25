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
package io.hexaglue.core.internal.ir.app.analysis;

import io.hexaglue.core.diagnostics.DiagnosticFactory;
import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.app.ApplicationModel;
import io.hexaglue.core.internal.ir.app.ApplicationService;
import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.diagnostics.DiagnosticCode;
import io.hexaglue.spi.diagnostics.DiagnosticReporter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Main orchestrator for application service model analysis.
 *
 * <p>
 * The application analyzer is the entry point for extracting and validating the complete
 * application model from source code. It coordinates specialized extractors, applies validation
 * rules, and constructs the final {@link ApplicationModel} instance representing all discovered
 * application services (use cases).
 * </p>
 *
 * <h2>Analysis Pipeline</h2>
 * <p>
 * The analyzer follows a multi-phase process:
 * </p>
 * <ol>
 *   <li><strong>Discovery:</strong> Identify application service class candidates</li>
 *   <li><strong>Filtering:</strong> Apply rules to determine which classes are services</li>
 *   <li><strong>Extraction:</strong> Extract detailed service information using {@link ApplicationServiceExtractor}</li>
 *   <li><strong>Validation:</strong> Apply service rules and constraints using {@link ApplicationRules}</li>
 *   <li><strong>Construction:</strong> Build the immutable application model</li>
 * </ol>
 *
 * <h2>Coordination</h2>
 * <p>
 * The analyzer coordinates the following components:
 * </p>
 * <ul>
 *   <li>{@link ApplicationServiceExtractor} - extracts service contracts from classes</li>
 *   <li>{@link ApplicationRules} - validates service contracts</li>
 * </ul>
 *
 * <h2>Application Service Discovery Strategy</h2>
 * <p>
 * The analyzer identifies application services using the following heuristics:
 * </p>
 * <ul>
 *   <li>Element must be a concrete class (not interface, abstract class, enum, etc.)</li>
 *   <li>Must have at least one public operation (method)</li>
 *   <li>Must not be from JDK or common libraries</li>
 *   <li>Package name should suggest application layer (application, usecase, service, etc.)</li>
 *   <li>Class name should follow common patterns (UseCase, Service, Command, Query suffixes)</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>
 * The analyzer does not throw exceptions for modeling errors. Instead, it:
 * </p>
 * <ul>
 *   <li>Collects validation violations</li>
 *   <li>Reports them through the diagnostic system</li>
 *   <li>Continues analysis where possible</li>
 *   <li>Returns a best-effort application model</li>
 * </ul>
 *
 * <h2>Optional Nature</h2>
 * <p>
 * The application model is optional in HexaGlue:
 * </p>
 * <ul>
 *   <li>Not all projects expose application services explicitly</li>
 *   <li>The model may be empty if no services are discovered</li>
 *   <li>This is normal and does not prevent adapter generation</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Extract all relevant service information</li>
 *   <li><strong>Resilience:</strong> Handle partial or malformed models gracefully</li>
 *   <li><strong>Performance:</strong> Minimize redundant analysis</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are safe for concurrent use if constructed with thread-safe dependencies.
 * Each analysis produces an independent {@link ApplicationModel}.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create analyzer with dependencies
 * Elements elements = processingEnv.getElementUtils();
 * Types types = processingEnv.getTypeUtils();
 * ApplicationAnalyzer analyzer = ApplicationAnalyzer.createDefault(elements, types);
 *
 * // Analyze application services
 * Set<TypeElement> classElements = ...;
 * ApplicationModel model = analyzer.analyze(classElements);
 *
 * // Query results
 * List<ApplicationService> services = model.services();
 * if (!model.isEmpty()) {
 *     // Generate documentation
 * }
 * }</pre>
 */
@InternalMarker(reason = "Internal application service analysis; not exposed to plugins")
public final class ApplicationAnalyzer {

    private static final DiagnosticCode CODE_APPLICATION_SERVICE_EXTRACTION_FAILED =
            DiagnosticCode.of("HG-CORE-IR-205");
    private static final DiagnosticCode CODE_APPLICATION_SERVICE_VALIDATION_FAILED =
            DiagnosticCode.of("HG-CORE-IR-206");

    private final ApplicationServiceExtractor extractor;
    private final ApplicationRules rules;
    private final DiagnosticReporter diagnostics;

    /**
     * Creates an application analyzer with the given dependencies.
     *
     * @param extractor   application service extractor (not {@code null})
     * @param rules       application validation rules (not {@code null})
     * @param diagnostics diagnostic reporter for error reporting (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public ApplicationAnalyzer(
            ApplicationServiceExtractor extractor, ApplicationRules rules, DiagnosticReporter diagnostics) {
        this.extractor = Objects.requireNonNull(extractor, "extractor");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Creates a default application analyzer with standard configuration.
     *
     * @param elements    element utilities from annotation processing environment (not {@code null})
     * @param types       type utilities from annotation processing environment (not {@code null})
     * @param diagnostics diagnostic reporter for error reporting (not {@code null})
     * @return application analyzer instance (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public static ApplicationAnalyzer createDefault(Elements elements, Types types, DiagnosticReporter diagnostics) {
        Objects.requireNonNull(elements, "elements");
        Objects.requireNonNull(types, "types");
        Objects.requireNonNull(diagnostics, "diagnostics");

        TypeResolver typeResolver = TypeResolver.create(elements, types);
        ApplicationServiceExtractor serviceExtractor = new ApplicationServiceExtractor(typeResolver, elements);
        ApplicationRules applicationRules = new ApplicationRules();

        return new ApplicationAnalyzer(serviceExtractor, applicationRules, diagnostics);
    }

    /**
     * Analyzes a set of type elements and constructs the application model.
     *
     * <p>
     * This implementation analyzes all provided elements, extracts application services,
     * validates them, and builds a complete application model. The model may be empty if
     * no application services are discovered, which is normal for many projects.
     * </p>
     *
     * @param elements source elements to analyze (not {@code null}, must contain {@code TypeElement} instances)
     * @return analyzed application model (never {@code null}, may be empty)
     * @throws NullPointerException if elements is null
     */
    public ApplicationModel analyze(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        // Extract application services
        List<ApplicationService> services = analyzeServices(elements);

        // Build application model
        return ApplicationModel.builder().addServices(services).build();
    }

    /**
     * Analyzes application services from a set of elements.
     *
     * <p>
     * This method filters and extracts only application service class elements, delegating to
     * {@link ApplicationServiceExtractor} for detailed extraction.
     * </p>
     *
     * @param elements source elements (not {@code null})
     * @return list of extracted services (never {@code null})
     */
    public List<ApplicationService> analyzeServices(Set<?> elements) {
        Objects.requireNonNull(elements, "elements");

        List<ApplicationService> services = new ArrayList<>();

        for (Object element : elements) {
            if (element instanceof TypeElement te) {
                // Check if it's a valid service candidate
                if (isServiceCandidate(te)) {
                    try {
                        // Extract service
                        Optional<ApplicationService> service = extractor.extract(te);

                        // Add if successfully extracted and valid
                        if (service.isPresent()) {
                            ApplicationService s = service.get();

                            // Validate service
                            List<String> violations = rules.validateService(s);
                            if (violations.isEmpty()) {
                                services.add(s);
                            } else {
                                // Report validation failures as diagnostics
                                for (String violation : violations) {
                                    diagnostics.report(DiagnosticFactory.error(
                                            CODE_APPLICATION_SERVICE_VALIDATION_FAILED,
                                            "Application service '"
                                                    + te.getSimpleName()
                                                    + "' has invalid structure: "
                                                    + violation,
                                            te,
                                            "io.hexaglue.core"));
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Report extraction failure as diagnostic
                        diagnostics.report(DiagnosticFactory.errorWithCause(
                                CODE_APPLICATION_SERVICE_EXTRACTION_FAILED,
                                "Failed to extract application service '" + te.getSimpleName() + "': " + e.getMessage(),
                                te,
                                "io.hexaglue.core",
                                e));
                        // Continue processing other elements
                    }
                }
            }
        }

        return services;
    }

    /**
     * Determines if a type element is an application service candidate.
     *
     * <p>
     * This method applies heuristics to filter out non-service classes.
     * </p>
     *
     * @param typeElement type element to check (not {@code null})
     * @return {@code true} if likely an application service
     */
    private boolean isServiceCandidate(TypeElement typeElement) {
        // Must be a class
        if (typeElement.getKind() != ElementKind.CLASS) {
            return false;
        }

        // Must not be abstract
        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            return false;
        }

        String qualifiedName = typeElement.getQualifiedName().toString();
        String packageName = extractPackageName(qualifiedName);

        // Count public operations
        long publicOperations = typeElement.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.METHOD)
                .filter(e -> e.getModifiers().contains(Modifier.PUBLIC))
                .filter(e -> !e.getModifiers().contains(Modifier.STATIC))
                .count();

        // Use rules to determine if it's an application service
        return rules.isLikelyApplicationService(qualifiedName, packageName, (int) publicOperations);
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
