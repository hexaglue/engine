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
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.ports.PortDirection;
import java.util.List;
import java.util.Objects;

/**
 * Detects whether a domain type is an Aggregate Root according to DDD principles.
 *
 * <p>
 * An Aggregate Root is the single entry point to a cluster of domain objects (aggregate).
 * Only Aggregate Roots should have repository ports in Hexagonal Architecture.
 * </p>
 *
 * <h2>Detection Strategy</h2>
 * <p>
 * This detector applies multiple heuristics in priority order:
 * </p>
 * <ol>
 *   <li><strong>Explicit annotations:</strong> {@code @AggregateRoot} (jMolecules),
 *       {@code @Entity} (JPA - if has corresponding repository port)</li>
 *   <li><strong>Configuration:</strong> {@code hexaglue.yaml} declarations (future)</li>
 *   <li><strong>Port analysis:</strong> Entity type has a corresponding DRIVEN repository port</li>
 *   <li><strong>Package convention:</strong> Types in "aggregate" or "aggregates" packages</li>
 *   <li><strong>Naming convention:</strong> Types ending with "Aggregate" or "AggregateRoot"</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * AggregateRootDetector detector = new AggregateRootDetector();
 * DomainType customerType = ...;
 * List<Port> ports = ...;
 *
 * boolean isAggregateRoot = detector.isAggregateRoot(customerType, ports);
 * }</pre>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal aggregate root detection; not exposed to plugins")
public final class AggregateRootDetector {

    /**
     * Creates an aggregate root detector.
     */
    public AggregateRootDetector() {
        // Default constructor
    }

    /**
     * Determines if a domain type is an Aggregate Root.
     *
     * <p>
     * This method applies the detection strategy described in the class documentation.
     * It returns {@code true} if the type is likely an Aggregate Root based on
     * structural analysis, naming conventions, and port relationships.
     * </p>
     *
     * <p>
     * <strong>Conservative fallback:</strong> If the type kind is already {@code AGGREGATE_ROOT},
     * returns {@code true} immediately. For {@code ENTITY} types, applies heuristics.
     * Other kinds return {@code false}.
     * </p>
     *
     * @param domainType domain type to analyze (not {@code null})
     * @param ports      all ports in the IR (not {@code null}, used for repository detection)
     * @return {@code true} if this type is an Aggregate Root
     * @throws NullPointerException if domainType or ports is null
     */
    public boolean isAggregateRoot(DomainType domainType, List<Port> ports) {
        Objects.requireNonNull(domainType, "domainType");
        Objects.requireNonNull(ports, "ports");

        DomainTypeKind kind = domainType.kind();

        // 1. If already classified as AGGREGATE_ROOT, return true
        if (kind == DomainTypeKind.AGGREGATE_ROOT) {
            return true;
        }

        // 2. Only ENTITY types can be aggregate roots
        if (kind != DomainTypeKind.ENTITY) {
            return false;
        }

        // 3. Apply heuristics for ENTITY types

        // Heuristic 1: Check if type has a corresponding DRIVEN repository port
        if (hasRepositoryPort(domainType, ports)) {
            return true;
        }

        // Heuristic 2: Package-based convention
        if (isInAggregatePackage(domainType.qualifiedName())) {
            return true;
        }

        // Heuristic 3: Naming convention
        if (hasAggregateRootName(domainType.simpleName())) {
            return true;
        }

        // NOTE: Annotation detection (@AggregateRoot, @Entity) is performed earlier
        // during domain extraction in DomainTypeExtractor. Types with aggregate root
        // annotations are already classified as DomainTypeKind.AGGREGATE_ROOT.
        // See: AggregateRootAnnotationDetector and DomainTypeExtractor

        // TODO: Add configuration YAML support for explicit aggregate root declarations
        // This would allow users to declare aggregate roots in hexaglue.yaml like:
        //   domain:
        //     aggregateRoots:
        //       - com.example.order.Order
        //       - com.example.customer.Customer
        // Implementation requires:
        //   1. YAML configuration parser
        //   2. Configuration service accessible from this detector
        //   3. Integration with the compilation context

        // Default: For ENTITY types without clear signals, assume internal entity
        return false;
    }

    /**
     * Checks if the domain type has a corresponding DRIVEN repository port.
     *
     * <p>
     * A repository port is identified by:
     * <ul>
     *   <li>Direction: {@link PortDirection#DRIVEN DRIVEN}</li>
     *   <li>Methods that use the domain type as return type or parameter</li>
     *   <li>Name pattern: {@code *Repository}, {@code *Store}, {@code *Dao}</li>
     * </ul>
     * </p>
     *
     * @param domainType domain type (not {@code null})
     * @param ports      all ports (not {@code null})
     * @return {@code true} if has repository port
     */
    private boolean hasRepositoryPort(DomainType domainType, List<Port> ports) {
        String domainQualifiedName = domainType.qualifiedName();

        return ports.stream()
                .filter(port -> port.direction() == PortDirection.DRIVEN)
                .filter(this::looksLikeRepository)
                .flatMap(port -> port.methods().stream())
                .anyMatch(method -> usesType(method.returnType().render(), domainQualifiedName)
                        || method.parameters().stream()
                                .anyMatch(param -> usesType(param.type().render(), domainQualifiedName)));
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
     * Checks if a method signature uses a specific type.
     *
     * <p>
     * This is a simple string-based check that handles:
     * <ul>
     *   <li>Direct type usage: {@code Customer}</li>
     *   <li>Generic type parameters: {@code List<Customer>}, {@code Optional<Customer>}</li>
     * </ul>
     * </p>
     *
     * @param typeSignature type signature from method (not {@code null})
     * @param targetType    qualified name of target type (not {@code null})
     * @return {@code true} if signature uses the type
     */
    private boolean usesType(String typeSignature, String targetType) {
        // Simple string contains check
        // Handles: "Customer", "List<Customer>", "Optional<Customer>", etc.
        return typeSignature.contains(targetType) || typeSignature.contains(extractSimpleName(targetType));
    }

    /**
     * Checks if type is in an "aggregate" or "aggregates" package.
     *
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code com.example.order.aggregate.Order} → {@code true}</li>
     *   <li>{@code com.example.aggregates.Customer} → {@code true}</li>
     *   <li>{@code com.example.domain.Product} → {@code false}</li>
     * </ul>
     * </p>
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if in aggregate package
     */
    private boolean isInAggregatePackage(String qualifiedName) {
        return qualifiedName.matches(".*\\.aggregates?\\..*");
    }

    /**
     * Checks if type name ends with "Aggregate" or "AggregateRoot".
     *
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code OrderAggregate} → {@code true}</li>
     *   <li>{@code CustomerAggregateRoot} → {@code true}</li>
     *   <li>{@code Customer} → {@code false}</li>
     * </ul>
     * </p>
     *
     * @param simpleName simple name (not {@code null})
     * @return {@code true} if has aggregate name
     */
    private boolean hasAggregateRootName(String simpleName) {
        return simpleName.endsWith("Aggregate") || simpleName.endsWith("AggregateRoot");
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
