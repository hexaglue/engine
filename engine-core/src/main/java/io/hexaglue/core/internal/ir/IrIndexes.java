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
package io.hexaglue.core.internal.ir;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.app.ApplicationModel;
import io.hexaglue.core.internal.ir.app.ApplicationService;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainService;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortModel;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Performance-optimized indexes for fast IR lookups.
 *
 * <p>
 * The IR indexes provide O(1) or near-O(1) access to IR elements by various keys (qualified name,
 * annotation type, port direction, etc.). This is critical for plugin performance when querying
 * the IR during code generation.
 * </p>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Speed:</strong> Fast lookups for common queries (by name, by kind, by annotation).</li>
 *   <li><strong>Memory Efficiency:</strong> Indexes are built lazily and share underlying data.</li>
 *   <li><strong>Immutability:</strong> Indexes are immutable after construction.</li>
 * </ul>
 *
 * <h2>Index Types</h2>
 * <p>
 * This class provides several specialized indexes:
 * </p>
 * <ul>
 *   <li><strong>Domain Type Index:</strong> Lookup domain types by qualified name</li>
 *   <li><strong>Port Index:</strong> Lookup ports by qualified name and direction (driving/driven)</li>
 *   <li><strong>Application Service Index:</strong> Lookup services by qualified name</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>An {@link IrSnapshot} is created from analyzed elements.</li>
 *   <li>{@link IrIndexes} are built from the snapshot using {@link #from(IrSnapshot)}.</li>
 *   <li>Core and SPI views use the indexes for efficient queries.</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 */
@InternalMarker(reason = "Internal IR indexing; plugins query via SPI views")
public final class IrIndexes {

    private final Map<String, DomainType> domainTypesByQualifiedName;
    private final Map<String, Port> portsByQualifiedName;
    private final Map<String, Object> servicesByQualifiedName;

    private IrIndexes(
            Map<String, DomainType> domainTypesByQualifiedName,
            Map<String, Port> portsByQualifiedName,
            Map<String, Object> servicesByQualifiedName) {
        this.domainTypesByQualifiedName = Map.copyOf(domainTypesByQualifiedName);
        this.portsByQualifiedName = Map.copyOf(portsByQualifiedName);
        this.servicesByQualifiedName = Map.copyOf(servicesByQualifiedName);
    }

    public Optional<DomainType> findDomainType(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return Optional.ofNullable(domainTypesByQualifiedName.get(qualifiedName));
    }

    public Optional<Port> findPort(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return Optional.ofNullable(portsByQualifiedName.get(qualifiedName));
    }

    /**
     * Finds a service by its qualified name.
     *
     * <p>
     * This index merges both domain services and application services:
     * <ul>
     *   <li>{@link DomainService} instances from {@link DomainModel#services()}</li>
     *   <li>{@link ApplicationService} instances from {@link ApplicationModel#services()}</li>
     * </ul>
     * The value type is {@code Object} to avoid forcing a common supertype here.
     * </p>
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return service if present
     */
    public Optional<Object> findService(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        return Optional.ofNullable(servicesByQualifiedName.get(qualifiedName));
    }

    public int domainTypeCount() {
        return domainTypesByQualifiedName.size();
    }

    public int portCount() {
        return portsByQualifiedName.size();
    }

    public int serviceCount() {
        return servicesByQualifiedName.size();
    }

    public boolean isEmpty() {
        return domainTypesByQualifiedName.isEmpty()
                && portsByQualifiedName.isEmpty()
                && servicesByQualifiedName.isEmpty();
    }

    @Override
    public String toString() {
        return "IrIndexes{domains="
                + domainTypeCount()
                + ", ports="
                + portCount()
                + ", services="
                + serviceCount()
                + "}";
    }

    public static IrIndexes from(IrSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        Map<String, DomainType> domainTypes = new HashMap<>();
        Map<String, Port> ports = new HashMap<>();
        Map<String, Object> services = new HashMap<>();

        DomainModel dm = snapshot.domainModel();
        if (dm != null) {
            for (DomainType t : dm.types()) {
                String qn = t.qualifiedName();
                if (domainTypes.putIfAbsent(qn, t) != null) {
                    throw new IllegalStateException("Duplicate domain type qualified name: " + qn);
                }
            }
            for (DomainService s : dm.services()) {
                String qn = s.qualifiedName();
                if (services.putIfAbsent(qn, s) != null) {
                    throw new IllegalStateException("Duplicate service qualified name: " + qn);
                }
            }
        }

        PortModel pm = snapshot.portModel();
        if (pm != null) {
            for (Port p : pm.ports()) {
                String qn = p.qualifiedName();
                if (ports.putIfAbsent(qn, p) != null) {
                    throw new IllegalStateException("Duplicate port qualified name: " + qn);
                }
            }
        }

        ApplicationModel am = snapshot.applicationModel();
        if (am != null) {
            for (ApplicationService s : am.services()) {
                String qn = s.qualifiedName();
                if (services.putIfAbsent(qn, s) != null) {
                    throw new IllegalStateException("Duplicate service qualified name: " + qn);
                }
            }
        }

        return new IrIndexes(domainTypes, ports, services);
    }

    public static IrIndexes empty() {
        return new IrIndexes(Map.of(), Map.of(), Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, DomainType> domainTypes = new HashMap<>();
        private final Map<String, Port> ports = new HashMap<>();
        private final Map<String, Object> services = new HashMap<>();

        private Builder() {}

        public Builder putDomainType(String qualifiedName, DomainType domainType) {
            Objects.requireNonNull(qualifiedName, "qualifiedName");
            Objects.requireNonNull(domainType, "domainType");
            domainTypes.put(qualifiedName, domainType);
            return this;
        }

        public Builder putPort(String qualifiedName, Port port) {
            Objects.requireNonNull(qualifiedName, "qualifiedName");
            Objects.requireNonNull(port, "port");
            ports.put(qualifiedName, port);
            return this;
        }

        public Builder putService(String qualifiedName, Object service) {
            Objects.requireNonNull(qualifiedName, "qualifiedName");
            Objects.requireNonNull(service, "service");
            services.put(qualifiedName, service);
            return this;
        }

        public IrIndexes build() {
            return new IrIndexes(domainTypes, ports, services);
        }
    }
}
