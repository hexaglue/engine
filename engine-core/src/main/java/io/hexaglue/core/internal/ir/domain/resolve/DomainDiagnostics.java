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
package io.hexaglue.core.internal.ir.domain.resolve;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;

/**
 * Factory for domain-specific diagnostic messages.
 *
 * <p>
 * This class provides standardized diagnostic messages for domain model analysis and validation.
 * It ensures consistent error reporting across the domain analysis pipeline and provides
 * actionable guidance to users.
 * </p>
 *
 * <h2>Diagnostic Categories</h2>
 * <p>
 * Diagnostics are organized by category:
 * </p>
 * <ul>
 *   <li><strong>Type Support:</strong> Type is not supported for domain use</li>
 *   <li><strong>Structural:</strong> Type structure violates domain modeling rules</li>
 *   <li><strong>Naming:</strong> Type or property names violate conventions</li>
 *   <li><strong>Semantics:</strong> Type semantics are inconsistent (e.g., mutable value object)</li>
 * </ul>
 *
 * <h2>Message Guidelines</h2>
 * <p>
 * All diagnostic messages follow these principles:
 * </p>
 * <ul>
 *   <li><strong>Clear:</strong> Explain what is wrong in simple terms</li>
 *   <li><strong>Actionable:</strong> Suggest how to fix the problem</li>
 *   <li><strong>Contextual:</strong> Include type names and relevant details</li>
 *   <li><strong>Consistent:</strong> Use standard terminology and formatting</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Centralization:</strong> All domain diagnostics originate here</li>
 *   <li><strong>Consistency:</strong> Uniform message format and tone</li>
 *   <li><strong>Maintainability:</strong> Easy to update messages without code changes</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainDiagnostics diagnostics = new DomainDiagnostics();
 *
 * // Unsupported type
 * String error = diagnostics.unsupportedType(primitiveType, "Primitive types must use wrappers");
 *
 * // Missing identity
 * String warning = diagnostics.entityMissingIdentity("com.example.Customer");
 *
 * // Invalid naming
 * String info = diagnostics.invalidNaming("customerId", "Should be 'id' for consistency");
 * }</pre>
 */
@InternalMarker(reason = "Internal domain diagnostics; not exposed to plugins")
public final class DomainDiagnostics {

    private static final String PREFIX_ERROR = "[DOMAIN ERROR] ";
    private static final String PREFIX_WARNING = "[DOMAIN WARNING] ";
    private static final String PREFIX_INFO = "[DOMAIN INFO] ";

    /**
     * Creates a domain diagnostics factory.
     */
    public DomainDiagnostics() {
        // Default constructor
    }

    /**
     * Creates an error message for an unsupported type.
     *
     * @param typeRef type that is not supported (not {@code null})
     * @param reason  reason why type is unsupported (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String unsupportedType(TypeRef typeRef, String reason) {
        Objects.requireNonNull(typeRef, "typeRef");
        Objects.requireNonNull(reason, "reason");

        return PREFIX_ERROR + "Type '" + typeRef.render() + "' is not supported as a domain type. " + reason;
    }

    /**
     * Creates a warning message for an entity missing an identity.
     *
     * @param qualifiedName qualified name of entity (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if qualifiedName is null
     */
    public String entityMissingIdentity(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return PREFIX_ERROR + "Entity '" + qualifiedName + "' must have an identity property. "
                + "Add a property named 'id' or mark a property with an identity annotation.";
    }

    /**
     * Creates a warning message for a value object that is not immutable.
     *
     * @param qualifiedName qualified name of value object (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if qualifiedName is null
     */
    public String valueObjectNotImmutable(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return PREFIX_WARNING + "Value object '" + qualifiedName + "' should be immutable. "
                + "Consider making all fields final and removing setters.";
    }

    /**
     * Creates a warning message for an identifier that is not immutable.
     *
     * @param qualifiedName qualified name of identifier (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if qualifiedName is null
     */
    public String identifierNotImmutable(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return PREFIX_ERROR + "Identifier '" + qualifiedName + "' must be immutable. "
                + "Make all fields final and remove setters.";
    }

    /**
     * Creates an info message for invalid naming convention.
     *
     * @param name        element name (not {@code null})
     * @param suggestion  naming suggestion (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String invalidNaming(String name, String suggestion) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(suggestion, "suggestion");

        return PREFIX_INFO + "Name '" + name + "' does not follow recommended conventions. " + suggestion;
    }

    /**
     * Creates a warning for a domain type with no properties.
     *
     * @param qualifiedName qualified name of type (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if qualifiedName is null
     */
    public String typeHasNoProperties(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return PREFIX_WARNING + "Domain type '" + qualifiedName + "' has no properties. "
                + "This may indicate an incomplete type definition.";
    }

    /**
     * Creates an error for duplicate property names.
     *
     * @param qualifiedName qualified name of type (not {@code null})
     * @param propertyName  duplicate property name (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String duplicatePropertyName(String qualifiedName, String propertyName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(propertyName, "propertyName");

        return PREFIX_ERROR + "Domain type '" + qualifiedName + "' has duplicate property '" + propertyName + "'. "
                + "Property names must be unique within a type.";
    }

    /**
     * Creates a warning for a property name that doesn't follow conventions.
     *
     * @param typeName     type name (not {@code null})
     * @param propertyName property name (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String propertyNamingConvention(String typeName, String propertyName) {
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(propertyName, "propertyName");

        return PREFIX_INFO + "Property '" + propertyName + "' in type '" + typeName + "' should use camelCase naming. "
                + "Start with lowercase letter.";
    }

    /**
     * Creates a warning for a type name that doesn't follow conventions.
     *
     * @param qualifiedName qualified name of type (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if qualifiedName is null
     */
    public String typeNamingConvention(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");

        return PREFIX_INFO + "Type '" + qualifiedName + "' should use PascalCase naming. "
                + "Start with uppercase letter and avoid underscores.";
    }

    /**
     * Creates an error for a circular dependency in domain types.
     *
     * @param type1 first type in cycle (not {@code null})
     * @param type2 second type in cycle (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String circularDependency(String type1, String type2) {
        Objects.requireNonNull(type1, "type1");
        Objects.requireNonNull(type2, "type2");

        return PREFIX_ERROR + "Circular dependency detected between '" + type1 + "' and '" + type2 + "'. "
                + "Consider using aggregation or refactoring to break the cycle.";
    }

    /**
     * Creates a comprehensive validation summary.
     *
     * @param domainType   domain type being validated (not {@code null})
     * @param errorCount   number of errors found
     * @param warningCount number of warnings found
     * @return diagnostic summary (never {@code null})
     * @throws NullPointerException if domainType is null
     */
    public String validationSummary(DomainType domainType, int errorCount, int warningCount) {
        Objects.requireNonNull(domainType, "domainType");

        if (errorCount == 0 && warningCount == 0) {
            return PREFIX_INFO + "Domain type '" + domainType.qualifiedName() + "' is valid.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Domain type '").append(domainType.qualifiedName()).append("' validation: ");

        if (errorCount > 0) {
            sb.append(errorCount).append(" error(s)");
        }

        if (warningCount > 0) {
            if (errorCount > 0) {
                sb.append(", ");
            }
            sb.append(warningCount).append(" warning(s)");
        }

        return sb.toString();
    }

    /**
     * Creates an error for an unsupported property type.
     *
     * @param typeName     type name (not {@code null})
     * @param propertyName property name (not {@code null})
     * @param propertyType property type (not {@code null})
     * @param reason       reason why unsupported (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String unsupportedPropertyType(String typeName, String propertyName, TypeRef propertyType, String reason) {
        Objects.requireNonNull(typeName, "typeName");
        Objects.requireNonNull(propertyName, "propertyName");
        Objects.requireNonNull(propertyType, "propertyType");
        Objects.requireNonNull(reason, "reason");

        return PREFIX_ERROR + "Property '" + propertyName + "' in type '" + typeName + "' has unsupported type '"
                + propertyType.render() + "'. " + reason;
    }

    /**
     * Creates a generic diagnostic message.
     *
     * @param level   diagnostic level ("ERROR", "WARNING", "INFO") (not {@code null})
     * @param message diagnostic message (not {@code null})
     * @return formatted diagnostic (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public String format(String level, String message) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(message, "message");

        return "[DOMAIN " + level.toUpperCase() + "] " + message;
    }
}
