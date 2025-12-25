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
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.domain.index.DomainIndex;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves type references to domain types with validation and diagnostics.
 *
 * <p>
 * This resolver acts as a bridge between {@link TypeRef} instances (which represent
 * Java types) and {@link DomainType} instances (which represent analyzed domain types).
 * It validates that type references are supported and locates their corresponding domain
 * types when available.
 * </p>
 *
 * <h2>Resolution Process</h2>
 * <p>
 * The resolver performs the following steps:
 * </p>
 * <ol>
 *   <li><strong>Support Check:</strong> Verify type is supported using {@link DomainTypeSupportPolicy}</li>
 *   <li><strong>Lookup:</strong> Search for type in {@link DomainIndex}</li>
 *   <li><strong>Validation:</strong> Ensure resolved type meets all requirements</li>
 *   <li><strong>Diagnostics:</strong> Generate appropriate messages for failures</li>
 * </ol>
 *
 * <h2>Use Cases</h2>
 * <ul>
 *   <li>Validate that a property type is a supported domain type</li>
 *   <li>Resolve cross-references between domain types</li>
 *   <li>Check if a type is part of the analyzed domain model</li>
 *   <li>Generate diagnostics for unsupported or missing types</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Validation:</strong> Early detection of unsupported types</li>
 *   <li><strong>Performance:</strong> Efficient lookup using domain index</li>
 *   <li><strong>Clarity:</strong> Clear diagnostic messages for resolution failures</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are safe for concurrent use if constructed with thread-safe dependencies.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainIndex index = DomainIndex.from(domainModel);
 * DomainTypeSupportPolicy policy = new DomainTypeSupportPolicy();
 * DomainTypeResolver resolver = new DomainTypeResolver(index, policy);
 *
 * TypeRef customerTypeRef = ...;
 *
 * // Check if supported
 * if (!resolver.isSupported(customerTypeRef)) {
 *     String error = resolver.explainUnsupported(customerTypeRef);
 *     reportError(error);
 * }
 *
 * // Resolve to domain type
 * Optional<DomainType> domainType = resolver.resolve(customerTypeRef);
 * }</pre>
 */
@InternalMarker(reason = "Internal domain resolution; not exposed to plugins")
public final class DomainTypeResolver {

    private final DomainIndex domainIndex;
    private final DomainTypeSupportPolicy supportPolicy;
    private final DomainDiagnostics diagnostics;

    /**
     * Creates a domain type resolver with the given dependencies.
     *
     * @param domainIndex    domain index for lookups (not {@code null})
     * @param supportPolicy  type support policy (not {@code null})
     * @param diagnostics    diagnostics factory (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public DomainTypeResolver(
            DomainIndex domainIndex, DomainTypeSupportPolicy supportPolicy, DomainDiagnostics diagnostics) {
        this.domainIndex = Objects.requireNonNull(domainIndex, "domainIndex");
        this.supportPolicy = Objects.requireNonNull(supportPolicy, "supportPolicy");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    /**
     * Creates a resolver from a domain model with default policies.
     *
     * @param domainModel domain model to resolve against (not {@code null})
     * @return domain type resolver (never {@code null})
     * @throws NullPointerException if domainModel is null
     */
    public static DomainTypeResolver from(DomainModel domainModel) {
        Objects.requireNonNull(domainModel, "domainModel");

        DomainIndex index = DomainIndex.from(domainModel);
        DomainTypeSupportPolicy policy = new DomainTypeSupportPolicy();
        DomainDiagnostics diagnostics = new DomainDiagnostics();

        return new DomainTypeResolver(index, policy, diagnostics);
    }

    /**
     * Determines whether a type reference is supported as a domain type.
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if type is supported
     * @throws NullPointerException if typeRef is null
     */
    public boolean isSupported(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");
        return supportPolicy.isSupported(typeRef);
    }

    /**
     * Returns a diagnostic message explaining why a type is not supported.
     *
     * <p>
     * This should only be called if {@link #isSupported(TypeRef)} returns {@code false}.
     * </p>
     *
     * @param typeRef unsupported type reference (not {@code null})
     * @return diagnostic message (never {@code null})
     * @throws NullPointerException if typeRef is null
     */
    public String explainUnsupported(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        String reason = supportPolicy.getUnsupportedReason(typeRef);
        return diagnostics.unsupportedType(typeRef, reason);
    }

    /**
     * Resolves a type reference to a domain type if available.
     *
     * <p>
     * This looks up the type in the domain index. The type may not be found if:
     * </p>
     * <ul>
     *   <li>It is a standard Java library type (String, List, etc.)</li>
     *   <li>It is from a different module or dependency</li>
     *   <li>It has not been analyzed by the domain analyzer</li>
     * </ul>
     *
     * @param typeRef type reference to resolve (not {@code null})
     * @return domain type if found in the domain model
     * @throws NullPointerException if typeRef is null
     */
    public Optional<DomainType> resolve(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        // Only class and parameterized types can be looked up by name
        // Arrays, primitives, wildcards, and type variables have no domain representation
        if (!isResolvable(typeRef)) {
            return Optional.empty();
        }

        // Lookup by qualified name
        String qualifiedName = typeRef.name().value();
        return domainIndex.findType(qualifiedName);
    }

    /**
     * Determines whether a type reference can be resolved to a domain type.
     *
     * <p>
     * This checks if the type is of a kind that can have a domain representation.
     * Primitives, arrays, wildcards, and bare type variables cannot be resolved.
     * </p>
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if type can potentially be resolved
     * @throws NullPointerException if typeRef is null
     */
    public boolean isResolvable(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        // Only class and parameterized types can be domain types
        return supportPolicy.isSupported(typeRef);
    }

    /**
     * Validates that a type reference is suitable for use in the domain model.
     *
     * <p>
     * This performs comprehensive validation including support checks and
     * additional domain-specific rules.
     * </p>
     *
     * @param typeRef type reference to validate (not {@code null})
     * @return {@code true} if type is valid for domain use
     * @throws NullPointerException if typeRef is null
     */
    public boolean isValid(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");
        return supportPolicy.isValid(typeRef);
    }

    /**
     * Determines whether a type reference is suitable as a domain identifier.
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if suitable as identifier
     * @throws NullPointerException if typeRef is null
     */
    public boolean isSuitableAsIdentifier(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");
        return supportPolicy.isSuitableAsIdentifier(typeRef);
    }

    /**
     * Determines whether a type reference is suitable as a domain property type.
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if suitable as property
     * @throws NullPointerException if typeRef is null
     */
    public boolean isSuitableAsProperty(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");
        return supportPolicy.isSuitableAsProperty(typeRef);
    }

    /**
     * Determines whether a type requires special handling during code generation.
     *
     * @param typeRef type reference to check (not {@code null})
     * @return {@code true} if special handling required
     * @throws NullPointerException if typeRef is null
     */
    public boolean requiresSpecialHandling(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");
        return supportPolicy.requiresSpecialHandling(typeRef);
    }

    /**
     * Returns the domain index used by this resolver.
     *
     * @return domain index (never {@code null})
     */
    public DomainIndex getDomainIndex() {
        return domainIndex;
    }

    /**
     * Returns the support policy used by this resolver.
     *
     * @return support policy (never {@code null})
     */
    public DomainTypeSupportPolicy getSupportPolicy() {
        return supportPolicy;
    }

    /**
     * Returns the diagnostics factory used by this resolver.
     *
     * @return diagnostics factory (never {@code null})
     */
    public DomainDiagnostics getDiagnostics() {
        return diagnostics;
    }
}
