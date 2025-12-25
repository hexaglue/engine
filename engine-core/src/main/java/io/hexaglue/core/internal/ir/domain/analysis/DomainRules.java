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

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainService;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Validation rules and constraints for domain model analysis.
 *
 * <p>
 * This class encapsulates the business rules and validation logic that govern what constitutes
 * a valid domain model in HexaGlue. It ensures architectural consistency and catches common
 * domain modeling errors early in the compilation process.
 * </p>
 *
 * <h2>Validation Categories</h2>
 * <p>
 * Domain rules are organized into several categories:
 * </p>
 * <ul>
 *   <li><strong>Structural rules:</strong> Type naming, property presence, identity requirements</li>
 *   <li><strong>Semantic rules:</strong> Immutability constraints, aggregate boundaries</li>
 *   <li><strong>Consistency rules:</strong> Duplicate detection, circular references</li>
 *   <li><strong>Best practices:</strong> Naming conventions, architectural patterns</li>
 * </ul>
 *
 * <h2>Rule Severity</h2>
 * <p>
 * Rules are categorized by severity:
 * </p>
 * <ul>
 *   <li><strong>Error:</strong> Violations prevent code generation</li>
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
 * DomainRules rules = new DomainRules();
 * DomainType customerType = ...;
 *
 * List<String> violations = rules.validateDomainType(customerType);
 * if (!violations.isEmpty()) {
 *     // Report diagnostics
 * }
 * }</pre>
 */
@InternalMarker(reason = "Internal domain analysis; not exposed to plugins")
public final class DomainRules {

    /**
     * Creates a domain rules validator.
     */
    public DomainRules() {
        // Default constructor
    }

    /**
     * Validates a domain type against all applicable rules.
     *
     * <p>
     * This method applies comprehensive validation including:
     * </p>
     * <ul>
     *   <li>Naming conventions</li>
     *   <li>Identity requirements for entities</li>
     *   <li>Immutability constraints for value objects</li>
     *   <li>Property validation</li>
     * </ul>
     *
     * @param domainType domain type to validate (not {@code null})
     * @return list of validation violation messages (empty if valid)
     * @throws NullPointerException if domainType is null
     */
    public List<String> validateDomainType(DomainType domainType) {
        Objects.requireNonNull(domainType, "domainType");

        List<String> violations = new ArrayList<>();

        // Validate naming
        violations.addAll(validateNaming(domainType));

        // Validate entity-specific rules
        if (domainType.kind() == DomainTypeKind.ENTITY) {
            violations.addAll(validateEntity(domainType));
        }

        // Validate value object-specific rules
        if (domainType.kind() == DomainTypeKind.VALUE_OBJECT) {
            violations.addAll(validateValueObject(domainType));
        }

        // Validate identifier-specific rules
        if (domainType.kind() == DomainTypeKind.IDENTIFIER) {
            violations.addAll(validateIdentifier(domainType));
        }

        // Validate properties
        violations.addAll(validateProperties(domainType));

        return violations;
    }

    /**
     * Validates a domain service against all applicable rules.
     *
     * @param domainService domain service to validate (not {@code null})
     * @return list of validation violation messages (empty if valid)
     * @throws NullPointerException if domainService is null
     */
    public List<String> validateDomainService(DomainService domainService) {
        Objects.requireNonNull(domainService, "domainService");

        List<String> violations = new ArrayList<>();

        // Validate service naming
        if (!looksLikeDomainService(domainService.simpleName())) {
            violations.add("Domain service name should end with Service, Calculator, or similar: "
                    + domainService.simpleName());
        }

        return violations;
    }

    /**
     * Validates domain type naming conventions.
     *
     * @param domainType domain type (not {@code null})
     * @return list of violations (empty if valid)
     */
    private List<String> validateNaming(DomainType domainType) {
        List<String> violations = new ArrayList<>();

        String simpleName = domainType.simpleName();

        // Type name should be PascalCase
        if (!Character.isUpperCase(simpleName.charAt(0))) {
            violations.add("Domain type name should start with uppercase: " + simpleName);
        }

        // Type name should not contain underscores (prefer camelCase)
        if (simpleName.contains("_")) {
            violations.add("Domain type name should use PascalCase, not snake_case: " + simpleName);
        }

        return violations;
    }

    /**
     * Validates entity-specific rules.
     *
     * @param entity entity type (not {@code null})
     * @return list of violations (empty if valid)
     */
    private List<String> validateEntity(DomainType entity) {
        List<String> violations = new ArrayList<>();

        // Entities should have an identity
        if (entity.id().isEmpty()) {
            violations.add("Entity must have an identity property: " + entity.qualifiedName());
        }

        return violations;
    }

    /**
     * Validates value object-specific rules.
     *
     * @param valueObject value object type (not {@code null})
     * @return list of violations (empty if valid)
     */
    private List<String> validateValueObject(DomainType valueObject) {
        List<String> violations = new ArrayList<>();

        // Value objects should be immutable
        if (!valueObject.isImmutable()) {
            violations.add("Value object should be immutable: " + valueObject.qualifiedName());
        }

        // Value objects should not have identity
        if (valueObject.id().isPresent()) {
            violations.add("Value object should not have identity: " + valueObject.qualifiedName());
        }

        return violations;
    }

    /**
     * Validates identifier-specific rules.
     *
     * @param identifier identifier type (not {@code null})
     * @return list of violations (empty if valid)
     */
    private List<String> validateIdentifier(DomainType identifier) {
        List<String> violations = new ArrayList<>();

        // Identifiers should be immutable
        if (!identifier.isImmutable()) {
            violations.add("Identifier should be immutable: " + identifier.qualifiedName());
        }

        // Identifier names should end with "Id" or "Identifier"
        String name = identifier.simpleName();
        if (!name.endsWith("Id") && !name.endsWith("Identifier")) {
            violations.add("Identifier name should end with 'Id' or 'Identifier': " + name);
        }

        return violations;
    }

    /**
     * Validates domain properties.
     *
     * @param domainType domain type containing properties (not {@code null})
     * @return list of violations (empty if valid)
     */
    private List<String> validateProperties(DomainType domainType) {
        List<String> violations = new ArrayList<>();

        for (DomainProperty property : domainType.properties()) {
            // Property names should be camelCase
            String name = property.name();
            if (!Character.isLowerCase(name.charAt(0))) {
                violations.add("Property name should start with lowercase: " + domainType.simpleName() + "." + name);
            }
        }

        // Check for duplicate property names
        long uniqueNames = domainType.properties().stream()
                .map(DomainProperty::name)
                .distinct()
                .count();
        if (uniqueNames < domainType.properties().size()) {
            violations.add("Domain type has duplicate property names: " + domainType.qualifiedName());
        }

        return violations;
    }

    /**
     * Checks if a name looks like a domain service.
     *
     * @param name type name (not {@code null})
     * @return {@code true} if looks like domain service
     */
    private boolean looksLikeDomainService(String name) {
        return name.endsWith("Service")
                || name.endsWith("Calculator")
                || name.endsWith("Engine")
                || name.endsWith("Policy")
                || name.endsWith("Strategy");
    }

    /**
     * Determines if a domain type kind requires identity.
     *
     * @param kind domain type kind (not {@code null})
     * @return {@code true} if identity is required
     */
    public boolean requiresIdentity(DomainTypeKind kind) {
        Objects.requireNonNull(kind, "kind");
        return kind == DomainTypeKind.ENTITY;
    }

    /**
     * Determines if a domain type kind should be immutable.
     *
     * @param kind domain type kind (not {@code null})
     * @return {@code true} if immutability is recommended
     */
    public boolean shouldBeImmutable(DomainTypeKind kind) {
        Objects.requireNonNull(kind, "kind");
        return kind == DomainTypeKind.VALUE_OBJECT
                || kind == DomainTypeKind.IDENTIFIER
                || kind == DomainTypeKind.RECORD;
    }

    /**
     * Determines if a type is likely a domain type based on heuristics.
     *
     * <p>
     * This method applies package name analysis and type name patterns to identify
     * domain types. Domain types are business entities, value objects, identifiers,
     * aggregates, and domain events that exist in the domain layer.
     * </p>
     *
     * <h2>Inclusion Criteria</h2>
     * <ul>
     *   <li>Must be a class, enum, or record (not interface)</li>
     *   <li>Located in packages with "domain" or "model" in the name</li>
     *   <li>Not excluded by technical type patterns (see exclusions below)</li>
     * </ul>
     *
     * <h2>Exclusion Criteria</h2>
     * <ul>
     *   <li>JDK and standard library types (java.*, javax.*, jakarta.*)</li>
     *   <li>Common framework types (Spring, SLF4J, etc.)</li>
     *   <li>Infrastructure types (Repository, Controller, Adapter, etc.)</li>
     *   <li>Technical configuration types (Config, Configuration)</li>
     * </ul>
     *
     * @param qualifiedName fully qualified type name (not {@code null})
     * @param packageName   package name (not {@code null})
     * @param isInterface   {@code true} if the type is an interface
     * @return {@code true} if likely a domain type
     * @throws NullPointerException if any parameter is null
     */
    public boolean isLikelyDomainType(String qualifiedName, String packageName, boolean isInterface) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(packageName, "packageName");

        // Interfaces are not domain types (they are ports or contracts)
        if (isInterface) {
            return false;
        }

        // Exclude JDK and common libraries
        if (packageName.startsWith("java.")
                || packageName.startsWith("javax.")
                || packageName.startsWith("jakarta.")
                || packageName.startsWith("org.springframework.")
                || packageName.startsWith("org.slf4j.")
                || packageName.startsWith("org.apache.commons.")
                || packageName.startsWith("com.google.common.")) {
            return false;
        }

        // Extract simple name for suffix checks
        String simpleName = extractSimpleName(qualifiedName);

        // Exclude infrastructure/technical types by suffix
        if (simpleName.endsWith("Repository")
                || simpleName.endsWith("Adapter")
                || simpleName.endsWith("Controller")
                || simpleName.endsWith("RestController")
                || simpleName.endsWith("Gateway")
                || simpleName.endsWith("Client")
                || simpleName.endsWith("Config")
                || simpleName.endsWith("Configuration")
                || simpleName.endsWith("Publisher")
                || simpleName.endsWith("Consumer")
                || simpleName.endsWith("Listener")
                || simpleName.endsWith("Handler")) {
            return false;
        }

        // Include if in common domain packages
        String lowerPackage = packageName.toLowerCase();
        if (lowerPackage.contains("domain") || lowerPackage.contains(".model")) {
            return true;
        }

        // Conservative default: not a domain type unless explicitly in domain package
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
