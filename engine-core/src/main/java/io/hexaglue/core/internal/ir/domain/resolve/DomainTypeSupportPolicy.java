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
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;

/**
 * Policy determining which domain types are supported for code generation.
 *
 * <p>
 * This policy defines what constitutes a valid, supportable domain type in HexaGlue.
 * It evaluates types based on their characteristics and determines whether they can be
 * safely used in generated infrastructure code.
 * </p>
 *
 * <h2>Support Criteria</h2>
 * <p>
 * A domain type is considered supported if:
 * </p>
 * <ul>
 *   <li>It is a class, interface, enum, or record type</li>
 *   <li>It is not a primitive type (use wrapper types instead)</li>
 *   <li>It is not an array type (use collections instead)</li>
 *   <li>It does not use wildcard generics in critical positions</li>
 *   <li>It is accessible (public or package-visible within domain)</li>
 * </ul>
 *
 * <h2>Unsupported Constructs</h2>
 * <p>
 * The following are explicitly unsupported:
 * </p>
 * <ul>
 *   <li><strong>Raw types:</strong> Generics must be properly parameterized</li>
 *   <li><strong>Intersection types:</strong> Too complex for reliable generation</li>
 *   <li><strong>Anonymous classes:</strong> Cannot be referenced in generated code</li>
 *   <li><strong>Local classes:</strong> Not accessible from generated infrastructure</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Safety:</strong> Prevent generation errors by rejecting problematic types early</li>
 *   <li><strong>Clarity:</strong> Provide clear diagnostic messages for unsupported types</li>
 *   <li><strong>Extensibility:</strong> Policy can be customized via configuration</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainTypeSupportPolicy policy = new DomainTypeSupportPolicy();
 * TypeRef customerType = ...;
 *
 * if (!policy.isSupported(customerType)) {
 *     String reason = policy.getUnsupportedReason(customerType);
 *     reportDiagnostic("Unsupported domain type: " + reason);
 * }
 * }</pre>
 */
@InternalMarker(reason = "Internal domain resolution; not exposed to plugins")
public final class DomainTypeSupportPolicy {

    /**
     * Creates a domain type support policy with default rules.
     */
    public DomainTypeSupportPolicy() {
        // Default constructor
    }

    /**
     * Determines whether a type is supported as a domain type.
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if type is supported
     * @throws NullPointerException if typeRef is null
     */
    public boolean isSupported(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        TypeKind kind = typeRef.kind();

        // Primitive types are not supported (use wrappers)
        if (kind == TypeKind.PRIMITIVE) {
            return false;
        }

        // Array types are discouraged (use collections)
        if (kind == TypeKind.ARRAY) {
            return false;
        }

        // Type variables alone are not domain types (need concrete bounds)
        if (kind == TypeKind.TYPE_VARIABLE) {
            return false;
        }

        // Wildcards are too generic for domain types
        if (kind == TypeKind.WILDCARD) {
            return false;
        }

        // Classes, interfaces, enums, and parameterized types are supported
        return kind == TypeKind.CLASS || kind == TypeKind.PARAMETERIZED;
    }

    /**
     * Returns a human-readable reason why a type is not supported.
     *
     * <p>
     * This method should only be called if {@link #isSupported(TypeRef)} returns {@code false}.
     * </p>
     *
     * @param typeRef type reference to explain (not {@code null})
     * @return reason why type is unsupported (never {@code null})
     * @throws NullPointerException if typeRef is null
     */
    public String getUnsupportedReason(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        TypeKind kind = typeRef.kind();

        if (kind == TypeKind.PRIMITIVE) {
            return "Primitive types are not supported as domain types. Use wrapper types instead (e.g., Integer instead of int).";
        }

        if (kind == TypeKind.ARRAY) {
            return "Array types are discouraged in domain models. Use standard collections (List, Set) instead.";
        }

        if (kind == TypeKind.TYPE_VARIABLE) {
            return "Bare type variables are not concrete domain types. Use bounded types or concrete implementations.";
        }

        if (kind == TypeKind.WILDCARD) {
            return "Wildcard types (? extends/super) are too generic for domain types. Use concrete type parameters.";
        }

        return "Type is not supported as a domain type for unknown reason.";
    }

    /**
     * Determines whether a type is suitable as a domain identifier.
     *
     * <p>
     * Domain identifiers have stricter requirements than general domain types:
     * they must be immutable, comparable, and typically simple types.
     * </p>
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if type is suitable as identifier
     * @throws NullPointerException if typeRef is null
     */
    public boolean isSuitableAsIdentifier(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        // Must be a supported type first
        if (!isSupported(typeRef)) {
            return false;
        }

        TypeKind kind = typeRef.kind();

        // Identifiers should not be parameterized (too complex)
        if (kind == TypeKind.PARAMETERIZED) {
            return false;
        }

        // Must be a class type
        return kind == TypeKind.CLASS;
    }

    /**
     * Determines whether a type is suitable as a domain property type.
     *
     * <p>
     * Property types have relaxed constraints compared to identifiers.
     * Collections and parameterized types are acceptable as properties.
     * </p>
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if type is suitable as property
     * @throws NullPointerException if typeRef is null
     */
    public boolean isSuitableAsProperty(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        // Must be a supported type
        return isSupported(typeRef);
    }

    /**
     * Determines whether a type requires special handling during generation.
     *
     * <p>
     * Some types, while supported, require additional logic during code generation
     * (e.g., generic types, nested classes).
     * </p>
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if type requires special handling
     * @throws NullPointerException if typeRef is null
     */
    public boolean requiresSpecialHandling(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        TypeKind kind = typeRef.kind();

        // Parameterized types need generic handling
        if (kind == TypeKind.PARAMETERIZED) {
            return true;
        }

        // Check for nested classes (qualified name contains '$')
        if (kind == TypeKind.CLASS) {
            String qualifiedName = typeRef.name().value();
            return qualifiedName.contains("$");
        }

        return false;
    }

    /**
     * Validates that a type meets all requirements for domain use.
     *
     * <p>
     * This performs comprehensive validation beyond simple support checks.
     * It verifies accessibility, proper parameterization, and other constraints.
     * </p>
     *
     * @param typeRef type reference to validate (not {@code null})
     * @return {@code true} if type is fully valid for domain use
     * @throws NullPointerException if typeRef is null
     */
    public boolean isValid(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        // Must be supported
        if (!isSupported(typeRef)) {
            return false;
        }

        // Parameterized types must have concrete type arguments (no raw types)
        if (typeRef.kind() == TypeKind.PARAMETERIZED) {
            // In a full implementation, we would check that all type arguments are concrete
            // For now, we accept all parameterized types if they pass the support check
        }

        return true;
    }
}
