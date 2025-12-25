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
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.spi.ir.ports.PortDirection;
import java.util.List;
import java.util.Objects;

/**
 * Matches repository ports for a domain type.
 *
 * <p>This matcher applies naming conventions and type analysis to determine if
 * a domain type has a corresponding repository port, which is a strong signal
 * that the type is an aggregate root.</p>
 *
 * <h2>Repository Port Identification</h2>
 * <p>A port is considered a repository if it:</p>
 * <ul>
 *   <li>Has direction {@link PortDirection#DRIVEN DRIVEN}</li>
 *   <li>Has a name ending with "Repository", "Store", "Dao", or "Storage"</li>
 *   <li>Has methods that reference the domain type in parameters or return type</li>
 * </ul>
 *
 * <h2>Type Matching Strategy</h2>
 * <p>The matcher uses <strong>structural {@code TypeRef} comparison</strong> to detect
 * domain type usage in port method signatures. This approach:</p>
 * <ul>
 *   <li>Compares qualified names for {@code ClassRef} types</li>
 *   <li>Recursively traverses parameterized types (e.g., {@code Optional<Order>}, {@code List<Order>})</li>
 *   <li>Handles arrays, wildcards, and type variables</li>
 *   <li>Avoids false positives from substring matching (e.g., "Customer" vs "CustomerOrder")</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is stateless and thread-safe.</p>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal repository matcher; not exposed to plugins")
public final class RepositoryPortMatcher {

    /**
     * Creates a repository port matcher.
     */
    public RepositoryPortMatcher() {
        // Default constructor
    }

    /**
     * Returns true if any DRIVEN port looks like a repository and references the domain type.
     *
     * @param domainType domain type (not {@code null})
     * @param ports      all ports (not {@code null})
     * @return {@code true} if repository port exists
     * @throws NullPointerException if domainType or ports is null
     */
    public boolean hasRepositoryPort(DomainType domainType, List<Port> ports) {
        Objects.requireNonNull(domainType, "domainType");
        Objects.requireNonNull(ports, "ports");

        return ports.stream()
                .filter(p -> p.direction() == PortDirection.DRIVEN)
                .filter(this::looksLikeRepository)
                .flatMap(p -> p.methods().stream())
                .anyMatch(m -> typeUsesDomainType(m.returnType(), domainType)
                        || m.parameters().stream().anyMatch(arg -> typeUsesDomainType(arg.type(), domainType)));
    }

    /**
     * Checks if a port looks like a repository based on naming conventions.
     *
     * @param port port to check (not {@code null})
     * @return {@code true} if looks like a repository
     */
    private boolean looksLikeRepository(Port port) {
        String simpleName = port.simpleName();
        return simpleName.endsWith("Repository")
                || simpleName.endsWith("Store")
                || simpleName.endsWith("Dao")
                || simpleName.endsWith("Storage");
    }

    /**
     * Checks if a type reference uses the domain type structurally.
     *
     * <p>This method performs structural type comparison by traversing the type hierarchy:</p>
     * <ul>
     *   <li><strong>ClassRef:</strong> Compare qualified names</li>
     *   <li><strong>ParameterizedRef:</strong> Check raw type and recursively check type arguments
     *       (e.g., {@code Optional<Order>}, {@code List<Order>})</li>
     *   <li><strong>ArrayRef:</strong> Recursively check component type (e.g., {@code Order[]})</li>
     *   <li><strong>WildcardRef:</strong> Check bounds (e.g., {@code ? extends Order})</li>
     *   <li><strong>TypeVariableRef:</strong> No match (type variables don't directly reference types)</li>
     *   <li><strong>PrimitiveRef:</strong> No match (primitives can't reference domain types)</li>
     * </ul>
     *
     * <p>This approach avoids false positives from string-based matching
     * (e.g., "Customer" matching "CustomerOrder").</p>
     *
     * @param typeRef    type reference to check (not {@code null})
     * @param domainType target domain type (not {@code null})
     * @return {@code true} if typeRef structurally uses domainType
     * @throws NullPointerException if typeRef or domainType is null
     */
    private boolean typeUsesDomainType(io.hexaglue.spi.types.TypeRef typeRef, DomainType domainType) {
        Objects.requireNonNull(typeRef, "typeRef");
        Objects.requireNonNull(domainType, "domainType");

        String targetQualifiedName = domainType.qualifiedName();

        switch (typeRef.kind()) {
            case CLASS:
                io.hexaglue.spi.types.ClassRef classRef = (io.hexaglue.spi.types.ClassRef) typeRef;
                // Compare by qualified name if available
                return classRef.qualifiedName()
                        .map(qn -> qn.equals(targetQualifiedName))
                        .orElse(false);

            case PARAMETERIZED:
                io.hexaglue.spi.types.ParameterizedRef paramRef = (io.hexaglue.spi.types.ParameterizedRef) typeRef;
                // Check raw type (e.g., Optional, List)
                if (typeUsesDomainType(paramRef.rawType(), domainType)) {
                    return true;
                }
                // Check type arguments recursively (e.g., Optional<Order>, List<Order>)
                return paramRef.typeArguments().stream().anyMatch(arg -> typeUsesDomainType(arg, domainType));

            case ARRAY:
                io.hexaglue.spi.types.ArrayRef arrayRef = (io.hexaglue.spi.types.ArrayRef) typeRef;
                // Check component type (e.g., Order[])
                return typeUsesDomainType(arrayRef.componentType(), domainType);

            case WILDCARD:
                io.hexaglue.spi.types.WildcardRef wildcardRef = (io.hexaglue.spi.types.WildcardRef) typeRef;
                // Check bounds (e.g., ? extends Order, ? super Order)
                if (wildcardRef.upperBound() != null) {
                    return typeUsesDomainType(wildcardRef.upperBound(), domainType);
                }
                if (wildcardRef.lowerBound() != null) {
                    return typeUsesDomainType(wildcardRef.lowerBound(), domainType);
                }
                return false;

            case TYPE_VARIABLE:
                // Type variables (e.g., T) don't directly reference domain types
                // Could check bounds but that's rarely useful for repository detection
                return false;

            case PRIMITIVE:
                // Primitives never reference domain types
                return false;

            default:
                // Unknown type kind - fall back to conservative string-based check
                String qn = targetQualifiedName;
                String sn = extractSimpleName(qn);
                return usesTypeStringFallback(typeRef.render(), qn, sn);
        }
    }

    /**
     * String-based fallback for unknown type kinds.
     *
     * <p><strong>Deprecated:</strong> This method is kept only as a fallback for
     * future type kinds. The primary type detection is now structural via
     * {@link #typeUsesDomainType(io.hexaglue.spi.types.TypeRef, DomainType)}.</p>
     *
     * @param signature type signature from method (not {@code null})
     * @param qualified qualified name of target type (not {@code null})
     * @param simple    simple name of target type (not {@code null})
     * @return {@code true} if signature uses the type
     */
    private boolean usesTypeStringFallback(String signature, String qualified, String simple) {
        if (signature == null) {
            return false;
        }
        return signature.contains(qualified) || signature.contains(simple);
    }

    /**
     * Extracts simple name from qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return simple name (never {@code null})
     */
    private String extractSimpleName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return lastDot >= 0 ? qualifiedName.substring(lastDot + 1) : qualifiedName;
    }
}
