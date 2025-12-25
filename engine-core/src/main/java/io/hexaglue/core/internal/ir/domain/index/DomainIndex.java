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
package io.hexaglue.core.internal.ir.domain.index;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainService;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Unified index providing efficient access to all domain model elements.
 *
 * <p>
 * This is the main entry point for querying the domain model. It aggregates specialized
 * indexes ({@link DomainTypeIndex} and {@link DomainServiceIndex}) and provides a unified
 * API for accessing domain types and services.
 * </p>
 *
 * <h2>Index Structure</h2>
 * <p>
 * The domain index is composed of:
 * </p>
 * <ul>
 *   <li><strong>Type Index:</strong> Fast lookups for domain types (entities, value objects, etc.)</li>
 *   <li><strong>Service Index:</strong> Fast lookups for domain services</li>
 * </ul>
 *
 * <h2>Common Queries</h2>
 * <p>
 * The index optimizes the following query patterns:
 * </p>
 * <ul>
 *   <li>Find a specific type or service by qualified name</li>
 *   <li>List all types of a specific kind (entities, value objects)</li>
 *   <li>List all types or services in a package</li>
 *   <li>Search by simple name across the domain</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Unified API:</strong> Single access point for all domain queries</li>
 *   <li><strong>Performance:</strong> Delegate to specialized indexes for optimal lookup</li>
 *   <li><strong>Immutability:</strong> Index is immutable after construction</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * DomainIndex index = DomainIndex.from(domainModel);
 *
 * // Access specialized indexes
 * DomainTypeIndex types = index.types();
 * DomainServiceIndex services = index.services();
 *
 * // Direct lookups
 * Optional<DomainType> customer = index.findType("com.example.Customer");
 * List<DomainType> entities = index.findTypesByKind(DomainTypeKind.ENTITY);
 * }</pre>
 */
@InternalMarker(reason = "Internal domain index; not exposed to plugins")
public final class DomainIndex {

    private final DomainTypeIndex typeIndex;
    private final DomainServiceIndex serviceIndex;

    /**
     * Creates a domain index with the given specialized indexes.
     *
     * @param typeIndex    domain type index (not {@code null})
     * @param serviceIndex domain service index (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    private DomainIndex(DomainTypeIndex typeIndex, DomainServiceIndex serviceIndex) {
        this.typeIndex = Objects.requireNonNull(typeIndex, "typeIndex");
        this.serviceIndex = Objects.requireNonNull(serviceIndex, "serviceIndex");
    }

    /**
     * Creates an index from a domain model.
     *
     * @param model domain model to index (not {@code null})
     * @return domain index (never {@code null})
     * @throws NullPointerException if model is null
     */
    public static DomainIndex from(DomainModel model) {
        Objects.requireNonNull(model, "model");

        DomainTypeIndex typeIndex = DomainTypeIndex.from(model.types());
        DomainServiceIndex serviceIndex = DomainServiceIndex.from(model.services());

        return new DomainIndex(typeIndex, serviceIndex);
    }

    /**
     * Creates an empty index.
     *
     * @return empty index (never {@code null})
     */
    public static DomainIndex empty() {
        return new DomainIndex(DomainTypeIndex.empty(), DomainServiceIndex.empty());
    }

    /**
     * Returns the domain type index.
     *
     * <p>
     * Use this to access specialized type lookup operations.
     * </p>
     *
     * @return type index (never {@code null})
     */
    public DomainTypeIndex types() {
        return typeIndex;
    }

    /**
     * Returns the domain service index.
     *
     * <p>
     * Use this to access specialized service lookup operations.
     * </p>
     *
     * @return service index (never {@code null})
     */
    public DomainServiceIndex services() {
        return serviceIndex;
    }

    /**
     * Finds a domain type by its qualified name.
     *
     * <p>
     * This is a convenience method that delegates to {@link DomainTypeIndex#findByQualifiedName(String)}.
     * </p>
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return domain type if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<DomainType> findType(String qualifiedName) {
        return typeIndex.findByQualifiedName(qualifiedName);
    }

    /**
     * Finds a domain service by its qualified name.
     *
     * <p>
     * This is a convenience method that delegates to {@link DomainServiceIndex#findByQualifiedName(String)}.
     * </p>
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return domain service if found
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<DomainService> findService(String qualifiedName) {
        return serviceIndex.findByQualifiedName(qualifiedName);
    }

    /**
     * Finds all domain types of the given kind.
     *
     * <p>
     * This is a convenience method that delegates to {@link DomainTypeIndex#findByKind(DomainTypeKind)}.
     * </p>
     *
     * @param kind domain type kind (not {@code null})
     * @return list of matching types (never {@code null}, may be empty)
     * @throws NullPointerException if kind is null
     */
    public List<DomainType> findTypesByKind(DomainTypeKind kind) {
        return typeIndex.findByKind(kind);
    }

    /**
     * Finds all domain types in the given package.
     *
     * <p>
     * This is a convenience method that delegates to {@link DomainTypeIndex#findByPackage(String)}.
     * </p>
     *
     * @param packageName package name (not {@code null})
     * @return list of types in package (never {@code null}, may be empty)
     * @throws NullPointerException if packageName is null
     */
    public List<DomainType> findTypesByPackage(String packageName) {
        return typeIndex.findByPackage(packageName);
    }

    /**
     * Finds all domain services in the given package.
     *
     * <p>
     * This is a convenience method that delegates to {@link DomainServiceIndex#findByPackage(String)}.
     * </p>
     *
     * @param packageName package name (not {@code null})
     * @return list of services in package (never {@code null}, may be empty)
     * @throws NullPointerException if packageName is null
     */
    public List<DomainService> findServicesByPackage(String packageName) {
        return serviceIndex.findByPackage(packageName);
    }

    /**
     * Returns all domain types.
     *
     * @return immutable list of all types (never {@code null})
     */
    public List<DomainType> allTypes() {
        return typeIndex.all();
    }

    /**
     * Returns all domain services.
     *
     * @return immutable list of all services (never {@code null})
     */
    public List<DomainService> allServices() {
        return serviceIndex.all();
    }

    /**
     * Returns the total number of types in the index.
     *
     * @return type count
     */
    public int typeCount() {
        return typeIndex.size();
    }

    /**
     * Returns the total number of services in the index.
     *
     * @return service count
     */
    public int serviceCount() {
        return serviceIndex.size();
    }

    /**
     * Returns whether the index is empty (no types and no services).
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return typeIndex.isEmpty() && serviceIndex.isEmpty();
    }

    /**
     * Returns whether a type with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if type exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean containsType(String qualifiedName) {
        return typeIndex.contains(qualifiedName);
    }

    /**
     * Returns whether a service with the given qualified name exists.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return {@code true} if service exists
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean containsService(String qualifiedName) {
        return serviceIndex.contains(qualifiedName);
    }

    @Override
    public String toString() {
        return "DomainIndex{types=" + typeCount() + ", services=" + serviceCount() + "}";
    }
}
