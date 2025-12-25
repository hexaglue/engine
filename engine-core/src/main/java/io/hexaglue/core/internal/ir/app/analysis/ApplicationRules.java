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

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.app.ApplicationService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Validation rules and constraints for application service analysis.
 *
 * <p>
 * This class encapsulates the business rules and validation logic that govern what constitutes
 * a valid application service in HexaGlue's Hexagonal Architecture. It ensures architectural
 * consistency and catches common service modeling errors early in the compilation process.
 * </p>
 *
 * <h2>Validation Categories</h2>
 * <p>
 * Application service rules are organized into several categories:
 * </p>
 * <ul>
 *   <li><strong>Structural rules:</strong> Class naming, operation presence, signature requirements</li>
 *   <li><strong>Semantic rules:</strong> Use case clarity, operation consistency</li>
 *   <li><strong>Consistency rules:</strong> Duplicate detection, signature uniqueness</li>
 *   <li><strong>Best practices:</strong> Naming conventions, architectural patterns</li>
 * </ul>
 *
 * <h2>Application Service-Specific Rules</h2>
 * <ul>
 *   <li>Services must have at least one public operation</li>
 *   <li>Service names should follow class naming conventions</li>
 *   <li>Operation signatures should be unique within a service</li>
 *   <li>Operation names should follow method naming conventions</li>
 *   <li>Services should be in appropriate packages (application, usecase, etc.)</li>
 * </ul>
 *
 * <h2>Rule Severity</h2>
 * <p>
 * Rules are categorized by severity:
 * </p>
 * <ul>
 *   <li><strong>Error:</strong> Violations prevent documentation generation</li>
 *   <li><strong>Warning:</strong> Suspicious patterns that may indicate mistakes</li>
 *   <li><strong>Info:</strong> Suggestions for improvement</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Early Detection:</strong> Catch issues during compilation, not at runtime</li>
 *   <li><strong>Actionable:</strong> Provide clear guidance on how to fix violations</li>
 *   <li><strong>Extensible:</strong> New rules can be added without breaking existing code</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ApplicationRules rules = new ApplicationRules();
 * ApplicationService registerCustomer = ...;
 *
 * List<String> violations = rules.validateService(registerCustomer);
 * if (!violations.isEmpty()) {
 *     // Report diagnostics
 * }
 * }</pre>
 */
@InternalMarker(reason = "Internal application service analysis; not exposed to plugins")
public final class ApplicationRules {

    /**
     * Creates an application rules validator.
     */
    public ApplicationRules() {
        // Default constructor
    }

    /**
     * Validates an application service against all applicable rules.
     *
     * <p>
     * This method applies comprehensive validation including:
     * </p>
     * <ul>
     *   <li>Naming conventions</li>
     *   <li>Operation presence and quality</li>
     *   <li>Signature uniqueness</li>
     * </ul>
     *
     * @param service service to validate (not {@code null})
     * @return list of validation messages (never {@code null}, empty if valid)
     * @throws NullPointerException if service is null
     */
    public List<String> validateService(ApplicationService service) {
        Objects.requireNonNull(service, "service");

        List<String> violations = new ArrayList<>();

        // Check naming conventions
        violations.addAll(validateServiceNaming(service));

        // Check operations
        violations.addAll(validateServiceOperations(service));

        // Check for duplicate operation signatures
        violations.addAll(validateOperationUniqueness(service));

        return violations;
    }

    /**
     * Validates application service naming conventions.
     *
     * @param service service to validate (not {@code null})
     * @return list of violations (never {@code null})
     */
    private List<String> validateServiceNaming(ApplicationService service) {
        List<String> violations = new ArrayList<>();

        String simpleName = service.simpleName();

        // Service should start with uppercase
        if (!Character.isUpperCase(simpleName.charAt(0))) {
            violations.add("Service name '" + simpleName + "' should start with uppercase letter");
        }

        // Service should not have underscore (Java convention)
        if (simpleName.contains("_")) {
            violations.add("Service name '" + simpleName + "' should not contain underscores");
        }

        // Warn if service doesn't have common suffix
        if (!hasCommonServiceSuffix(simpleName)) {
            violations.add("Service name '"
                    + simpleName
                    + "' should have a common suffix like UseCase, Service, Command, Query, Handler, etc.");
        }

        return violations;
    }

    /**
     * Validates application service operations.
     *
     * @param service service to validate (not {@code null})
     * @return list of violations (never {@code null})
     */
    private List<String> validateServiceOperations(ApplicationService service) {
        List<String> violations = new ArrayList<>();

        List<ApplicationService.Operation> operations = service.internalOperations();

        if (operations.isEmpty()) {
            violations.add("Service '" + service.simpleName() + "' has no public operations");
        }

        // Validate each operation
        for (ApplicationService.Operation operation : operations) {
            violations.addAll(validateOperation(service, operation));
        }

        return violations;
    }

    /**
     * Validates a single application service operation.
     *
     * @param service   service containing the operation (not {@code null})
     * @param operation operation to validate (not {@code null})
     * @return list of violations (never {@code null})
     */
    private List<String> validateOperation(ApplicationService service, ApplicationService.Operation operation) {
        List<String> violations = new ArrayList<>();

        String operationName = operation.name();

        // Operation name should start with lowercase
        if (!Character.isLowerCase(operationName.charAt(0))) {
            violations.add("Operation name '"
                    + operationName
                    + "' in service '"
                    + service.simpleName()
                    + "' should start with lowercase letter");
        }

        // Operation name should not have underscore (Java convention)
        if (operationName.contains("_")) {
            violations.add("Operation name '"
                    + operationName
                    + "' in service '"
                    + service.simpleName()
                    + "' should not contain underscores");
        }

        return violations;
    }

    /**
     * Validates operation signature uniqueness within a service.
     *
     * @param service service to validate (not {@code null})
     * @return list of violations (never {@code null})
     */
    private List<String> validateOperationUniqueness(ApplicationService service) {
        List<String> violations = new ArrayList<>();

        Set<String> signatures = new HashSet<>();
        for (ApplicationService.Operation operation : service.internalOperations()) {
            Optional<String> signatureIdOpt = operation.signatureId();
            if (signatureIdOpt.isPresent()) {
                String signature = signatureIdOpt.get();
                if (!signatures.add(signature)) {
                    violations.add(
                            "Service '" + service.simpleName() + "' has duplicate operation signature: " + signature);
                }
            } else {
                // Build signature manually if not available
                String signature = buildSignature(operation);
                if (!signatures.add(signature)) {
                    violations.add(
                            "Service '" + service.simpleName() + "' has duplicate operation signature: " + signature);
                }
            }
        }

        return violations;
    }

    /**
     * Determines if a service name has a common suffix.
     *
     * @param serviceName service name (not {@code null})
     * @return {@code true} if has common suffix
     */
    private boolean hasCommonServiceSuffix(String serviceName) {
        return serviceName.endsWith("UseCase")
                || serviceName.endsWith("Service")
                || serviceName.endsWith("Command")
                || serviceName.endsWith("Query")
                || serviceName.endsWith("Handler")
                || serviceName.endsWith("Orchestrator")
                || serviceName.endsWith("Coordinator")
                || serviceName.endsWith("Manager")
                || serviceName.endsWith("Processor")
                || serviceName.endsWith("Executor")
                || serviceName.endsWith("Controller")
                || serviceName.endsWith("Facade");
    }

    /**
     * Builds an operation signature string for uniqueness checking.
     *
     * @param operation operation (not {@code null})
     * @return signature string
     */
    private String buildSignature(ApplicationService.Operation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(operation.name()).append('(');

        List<io.hexaglue.spi.types.TypeRef> paramTypes = operation.parameterTypes();
        for (int i = 0; i < paramTypes.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(paramTypes.get(i).render());
        }

        sb.append(')');
        return sb.toString();
    }

    /**
     * Determines if a type element should be considered an application service.
     *
     * <p>
     * This method applies heuristics to filter out non-service classes like:
     * </p>
     * <ul>
     *   <li>JDK classes (java.*, javax.*)</li>
     *   <li>Common library classes (org.springframework.*, jakarta.*)</li>
     *   <li>Classes with no public operations</li>
     *   <li>Infrastructure classes (repositories, controllers, etc.)</li>
     * </ul>
     *
     * @param qualifiedName   qualified name of the class (not {@code null})
     * @param packageName     package name (not {@code null})
     * @param operationCount  number of public operations
     * @return {@code true} if likely an application service
     * @throws NullPointerException if any parameter is null
     */
    public boolean isLikelyApplicationService(String qualifiedName, String packageName, int operationCount) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(packageName, "packageName");

        // Must have at least one operation
        if (operationCount == 0) {
            return false;
        }

        // Exclude JDK and common libraries
        if (packageName.startsWith("java.")
                || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || packageName.startsWith("org.springframework.")
                || packageName.startsWith("org.slf4j.")) {
            return false;
        }

        // Exclude infrastructure components (these are adapters, not application services)
        String simpleName = extractSimpleName(qualifiedName);
        if (simpleName.endsWith("Repository")
                || simpleName.endsWith("Adapter")
                || simpleName.endsWith("Controller")
                || simpleName.endsWith("RestController")
                || simpleName.endsWith("Gateway")
                || simpleName.endsWith("Client")
                || simpleName.endsWith("Config")
                || simpleName.endsWith("Configuration")) {
            return false;
        }

        // Include if in common application service packages
        String lowerPackage = packageName.toLowerCase();
        if (lowerPackage.contains("application")
                || lowerPackage.contains("usecase")
                || lowerPackage.contains("service")
                || lowerPackage.contains("command")
                || lowerPackage.contains("query")) {
            return true;
        }

        // Include if has common service suffix
        if (hasCommonServiceSuffix(simpleName)) {
            return true;
        }

        // Default: not likely a service unless explicitly marked
        return false;
    }

    /**
     * Extracts simple name from qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return simple name
     */
    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? qualifiedName : qualifiedName.substring(lastDot + 1);
    }
}
