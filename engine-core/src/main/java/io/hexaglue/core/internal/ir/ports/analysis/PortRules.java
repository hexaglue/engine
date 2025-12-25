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

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortMethod;
import io.hexaglue.core.internal.ir.ports.PortParameter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validation rules and constraints for port contract analysis.
 *
 * <p>
 * This class encapsulates the business rules and validation logic that govern what constitutes
 * a valid port contract in HexaGlue's Hexagonal Architecture. It ensures architectural consistency
 * and catches common port modeling errors early in the compilation process.
 * </p>
 *
 * <h2>Validation Categories</h2>
 * <p>
 * Port rules are organized into several categories:
 * </p>
 * <ul>
 *   <li><strong>Structural rules:</strong> Interface naming, method presence, parameter requirements</li>
 *   <li><strong>Semantic rules:</strong> Direction consistency, contract clarity</li>
 *   <li><strong>Consistency rules:</strong> Duplicate detection, signature uniqueness</li>
 *   <li><strong>Best practices:</strong> Naming conventions, architectural patterns</li>
 * </ul>
 *
 * <h2>Port-Specific Rules</h2>
 * <ul>
 *   <li>Ports must have at least one non-default, non-static method</li>
 *   <li>Port names should follow interface naming conventions</li>
 *   <li>Method signatures should be unique within a port</li>
 *   <li>Parameter names should be meaningful (not arg0, arg1, etc.)</li>
 *   <li>Ports should be in appropriate packages (ports, api, spi, etc.)</li>
 * </ul>
 *
 * <h2>Rule Severity</h2>
 * <p>
 * Rules are categorized by severity:
 * </p>
 * <ul>
 *   <li><strong>Error:</strong> Violations prevent adapter generation</li>
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
 * PortRules rules = new PortRules();
 * Port customerRepository = ...;
 *
 * List<String> violations = rules.validatePort(customerRepository);
 * if (!violations.isEmpty()) {
 *     // Report diagnostics
 * }
 * }</pre>
 */
@InternalMarker(reason = "Internal port analysis; not exposed to plugins")
public final class PortRules {

    /**
     * Creates a port rules validator.
     */
    public PortRules() {
        // Default constructor
    }

    /**
     * Validates a port against all applicable rules.
     *
     * <p>
     * This method applies comprehensive validation including:
     * </p>
     * <ul>
     *   <li>Naming conventions</li>
     *   <li>Method presence and quality</li>
     *   <li>Parameter naming</li>
     *   <li>Signature uniqueness</li>
     * </ul>
     *
     * <p>
     * Returns a list of {@link ValidationIssue} instances, each with a severity level
     * (ERROR or WARNING) and a descriptive message.
     * </p>
     *
     * @param port port to validate (not {@code null})
     * @return list of validation issues (never {@code null}, empty if valid)
     * @throws NullPointerException if port is null
     */
    public List<ValidationIssue> validatePort(Port port) {
        Objects.requireNonNull(port, "port");

        List<ValidationIssue> issues = new ArrayList<>();

        // Check naming conventions
        issues.addAll(validatePortNaming(port));

        // Check methods
        issues.addAll(validatePortMethods(port));

        // Check for duplicate method signatures
        issues.addAll(validateMethodUniqueness(port));

        return issues;
    }

    /**
     * Validates port naming conventions.
     *
     * @param port port to validate (not {@code null})
     * @return list of validation issues (never {@code null})
     */
    private List<ValidationIssue> validatePortNaming(Port port) {
        List<ValidationIssue> issues = new ArrayList<>();

        String simpleName = port.simpleName();

        // ERROR: Port must start with uppercase (Java naming convention)
        if (!Character.isUpperCase(simpleName.charAt(0))) {
            issues.add(ValidationIssue.error("Port name '" + simpleName + "' should start with uppercase letter"));
        }

        // ERROR: Port should not have underscore (Java convention)
        if (simpleName.contains("_")) {
            issues.add(ValidationIssue.error("Port name '" + simpleName + "' should not contain underscores"));
        }

        // WARNING: Port doesn't have common suffix (style recommendation, not a structural error)
        if (!hasCommonPortSuffix(simpleName)) {
            issues.add(ValidationIssue.warning("Port name '"
                    + simpleName
                    + "' could have a common suffix like Repository, Gateway, UseCase, Api, etc. for clarity"));
        }

        return issues;
    }

    /**
     * Validates port methods.
     *
     * @param port port to validate (not {@code null})
     * @return list of validation issues (never {@code null})
     */
    private List<ValidationIssue> validatePortMethods(Port port) {
        List<ValidationIssue> issues = new ArrayList<>();

        List<PortMethod> methods = port.internalMethods();

        // Count non-default, non-static methods
        long contractMethods =
                methods.stream().filter(m -> !m.isDefault() && !m.isStatic()).count();

        // ERROR: Port must have at least one contract method
        if (contractMethods == 0) {
            issues.add(ValidationIssue.error(
                    "Port '" + port.simpleName() + "' has no contract methods (all methods are default or static)"));
        }

        // Validate each method
        for (PortMethod method : methods) {
            issues.addAll(validateMethod(port, method));
        }

        return issues;
    }

    /**
     * Validates a single port method.
     *
     * @param port   port containing the method (not {@code null})
     * @param method method to validate (not {@code null})
     * @return list of validation issues (never {@code null})
     */
    private List<ValidationIssue> validateMethod(Port port, PortMethod method) {
        List<ValidationIssue> issues = new ArrayList<>();

        String methodName = method.name();

        // ERROR: Method name must start with lowercase (Java naming convention)
        if (!Character.isLowerCase(methodName.charAt(0))) {
            issues.add(ValidationIssue.error("Method name '" + methodName + "' in port '" + port.simpleName()
                    + "' should start with lowercase letter"));
        }

        // Validate parameters
        List<PortParameter> params = method.internalParameters();
        for (int i = 0; i < params.size(); i++) {
            PortParameter param = params.get(i);
            String paramName = param.name();

            // WARNING: Synthetic parameter names (style recommendation)
            if (isSyntheticParameterName(paramName)) {
                issues.add(ValidationIssue.warning("Parameter '"
                        + paramName + "' in method '"
                        + methodName + "' of port '"
                        + port.simpleName() + "' has synthetic name. Consider using -parameters compiler flag."));
            }
        }

        return issues;
    }

    /**
     * Validates method signature uniqueness within a port.
     *
     * @param port port to validate (not {@code null})
     * @return list of validation issues (never {@code null})
     */
    private List<ValidationIssue> validateMethodUniqueness(Port port) {
        List<ValidationIssue> issues = new ArrayList<>();

        Set<String> signatures = new HashSet<>();
        for (PortMethod method : port.internalMethods()) {
            String signature = buildSignature(method);
            if (!signatures.add(signature)) {
                // ERROR: Duplicate signatures are structural errors
                issues.add(ValidationIssue.error(
                        "Port '" + port.simpleName() + "' has duplicate method signature: " + signature));
            }
        }

        return issues;
    }

    /**
     * Determines if a port name has a common suffix.
     *
     * @param portName port name (not {@code null})
     * @return {@code true} if has common suffix
     */
    private boolean hasCommonPortSuffix(String portName) {
        return portName.endsWith("Repository")
                || portName.endsWith("Gateway")
                || portName.endsWith("Client")
                || portName.endsWith("Publisher")
                || portName.endsWith("Provider")
                || portName.endsWith("UseCase")
                || portName.endsWith("Command")
                || portName.endsWith("Query")
                || portName.endsWith("Api")
                || portName.endsWith("Facade")
                || portName.endsWith("Service")
                || portName.endsWith("Port")
                || portName.endsWith("Adapter");
    }

    /**
     * Determines if a parameter name is synthetic (generated by compiler).
     *
     * @param paramName parameter name (not {@code null})
     * @return {@code true} if synthetic
     */
    private boolean isSyntheticParameterName(String paramName) {
        // Check for arg0, arg1, etc.
        if (paramName.matches("arg\\d+")) {
            return true;
        }
        // Check for param0, param1, etc.
        if (paramName.matches("param\\d+")) {
            return true;
        }
        return false;
    }

    /**
     * Builds a method signature string for uniqueness checking.
     *
     * @param method method (not {@code null})
     * @return signature string
     */
    private String buildSignature(PortMethod method) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.name()).append('(');

        List<PortParameter> params = method.internalParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(params.get(i).type().render());
        }

        sb.append(')');
        return sb.toString();
    }

    /**
     * Determines if a type element should be considered a port interface.
     *
     * <p>
     * This method applies heuristics to filter out non-port interfaces like:
     * </p>
     * <ul>
     *   <li>JDK interfaces (java.*, javax.*)</li>
     *   <li>Common library interfaces (org.springframework.*, jakarta.*)</li>
     *   <li>Marker interfaces with no methods</li>
     * </ul>
     *
     * @param qualifiedName qualified name of the interface (not {@code null})
     * @param packageName   package name (not {@code null})
     * @param methodCount   number of non-default, non-static methods
     * @param hasRepositoryAnnotation whether the interface has {@code @Repository} annotation
     * @return {@code true} if likely a port interface
     * @throws NullPointerException if any parameter is null
     */
    public boolean isLikelyPortInterface(
            String qualifiedName, String packageName, int methodCount, boolean hasRepositoryAnnotation) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(packageName, "packageName");

        // Must have at least one method
        if (methodCount == 0) {
            return false;
        }

        // Explicit @Repository annotation is strong signal
        if (hasRepositoryAnnotation) {
            return true;
        }

        // Exclude JDK and common libraries
        if (packageName.startsWith("java.")
                || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || packageName.startsWith("org.springframework.")
                || packageName.startsWith("org.slf4j.")) {
            return false;
        }

        // Include if in common port packages
        String lowerPackage = packageName.toLowerCase();
        if (lowerPackage.contains("port")
                || lowerPackage.contains("api")
                || lowerPackage.contains("spi")
                || lowerPackage.contains("repository")
                || lowerPackage.contains("gateway")
                || lowerPackage.contains("usecase")) {
            return true;
        }

        // Default: likely a port if not excluded
        return true;
    }
}
