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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Internal utilities for IR construction, validation, and transformation.
 *
 * <p>
 * This utility class provides internal-only helpers for:
 * <ul>
 *   <li>IR validation and integrity checks</li>
 *   <li>IR model transformations</li>
 *   <li>SPI view construction from internal models</li>
 *   <li>Debugging and diagnostics support</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Encapsulation:</strong> Keep IR manipulation logic internal to core.</li>
 *   <li><strong>Reusability:</strong> Provide shared utilities for IR processing.</li>
 *   <li><strong>Safety:</strong> Validate IR invariants early.</li>
 * </ul>
 *
 * <h2>Typical Use Cases</h2>
 * <ul>
 *   <li>Validating IR consistency after analysis phase</li>
 *   <li>Creating SPI-compliant read-only views from internal models</li>
 *   <li>Extracting metadata for diagnostics and logging</li>
 *   <li>Applying IR transformations (normalization, enrichment)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are stateless and thread-safe.
 * </p>
 */
@InternalMarker(reason = "Internal IR utilities; not exposed to plugins")
public final class IrInternals {

    private IrInternals() {
        // utility class
    }

    /**
     * Validates that an IR snapshot is well-formed and internally consistent.
     *
     * <p>
     * This method performs basic sanity checks on the snapshot structure. It does not perform
     * deep semantic validation (which is done by the validation engine).
     * </p>
     *
     * <p>
     * Checks include:
     * </p>
     * <ul>
     *   <li>Non-null models (if present) have valid structure</li>
     *   <li>No duplicate qualified names within a model</li>
     *   <li>Cross-references are structurally sane (not semantic)</li>
     * </ul>
     *
     * @param snapshot IR snapshot to validate (not {@code null})
     * @throws IllegalStateException if the snapshot is malformed
     */
    public static void validateSnapshot(IrSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        if (snapshot.isEmpty()) {
            return;
        }

        if (snapshot.domainModel() != null) {
            validateDomainModel(snapshot.domainModel());
        }

        if (snapshot.portModel() != null) {
            validatePortModel(snapshot.portModel());
        }

        if (snapshot.applicationModel() != null) {
            validateApplicationModel(snapshot.applicationModel());
        }
    }

    private static void validateDomainModel(DomainModel domainModel) {
        Objects.requireNonNull(domainModel, "domainModel");

        requireNoNulls(domainModel.types(), "domainModel.types");
        requireNoNulls(domainModel.services(), "domainModel.services");

        ensureUniqueQualifiedNames(
                domainModel.types().stream().map(DomainType::qualifiedName).toList(), "domain types");

        ensureUniqueQualifiedNames(
                domainModel.services().stream()
                        .map(DomainService::qualifiedName)
                        .toList(),
                "domain services");
    }

    private static void validatePortModel(PortModel portModel) {
        Objects.requireNonNull(portModel, "portModel");

        requireNoNulls(portModel.ports(), "portModel.ports");

        ensureUniqueQualifiedNames(
                portModel.ports().stream().map(Port::qualifiedName).toList(), "ports");
    }

    private static void validateApplicationModel(ApplicationModel applicationModel) {
        Objects.requireNonNull(applicationModel, "applicationModel");

        requireNoNulls(applicationModel.services(), "applicationModel.services");

        ensureUniqueQualifiedNames(
                applicationModel.services().stream()
                        .map(ApplicationService::qualifiedName)
                        .toList(),
                "application services");
    }

    private static <T> void requireNoNulls(List<T> list, String label) {
        Objects.requireNonNull(list, label);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == null) {
                throw new IllegalStateException(label + " contains a null element at index " + i);
            }
        }
    }

    private static void ensureUniqueQualifiedNames(List<String> qualifiedNames, String label) {
        Set<String> seen = new HashSet<>();
        for (String qn : qualifiedNames) {
            if (qn == null || qn.isBlank()) {
                throw new IllegalStateException("Invalid qualified name in " + label + ": '" + qn + "'");
            }
            if (!seen.add(qn)) {
                throw new IllegalStateException("Duplicate qualified name in " + label + ": " + qn);
            }
        }
    }

    /**
     * Extracts summary statistics from an IR snapshot for diagnostics.
     *
     * <p>
     * Returns a human-readable summary string containing counts of domain types, ports,
     * services, etc. Useful for logging and debugging.
     * </p>
     *
     * @param snapshot IR snapshot (not {@code null})
     * @return summary string (never {@code null})
     */
    public static String summarize(IrSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        if (snapshot.isEmpty()) {
            return "IR: empty";
        }

        int domainTypes = 0;
        int domainServices = 0;
        int ports = 0;
        int appServices = 0;

        DomainModel dm = snapshot.domainModel();
        if (dm != null) {
            domainTypes = dm.types().size();
            domainServices = dm.services().size();
        }

        PortModel pm = snapshot.portModel();
        if (pm != null) {
            ports = pm.ports().size();
        }

        ApplicationModel am = snapshot.applicationModel();
        if (am != null) {
            appServices = am.services().size();
        }

        return "IR: domains=" + domainTypes
                + ", domainServices=" + domainServices
                + ", ports=" + ports
                + ", applicationServices=" + appServices;
    }

    public static String summarize(IrIndexes indexes) {
        Objects.requireNonNull(indexes, "indexes");

        if (indexes.isEmpty()) {
            return "IR Indexes: empty";
        }

        return "IR Indexes: domains="
                + indexes.domainTypeCount()
                + ", ports="
                + indexes.portCount()
                + ", services="
                + indexes.serviceCount();
    }

    public static IrSnapshot copy(IrSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return snapshot;
    }

    /**
     * Checks if two IR snapshots are structurally equivalent.
     *
     * <p>
     * Two snapshots are considered equivalent if they contain the same domain types, ports,
     * and services with the same qualified names. This is a structural comparison, not a
     * deep semantic comparison of all properties.
     * </p>
     *
     * <p>
     * This method is useful for incremental compilation to detect if the IR has changed
     * between compilation rounds.
     * </p>
     *
     * @param snapshot1 first snapshot (not {@code null})
     * @param snapshot2 second snapshot (not {@code null})
     * @return {@code true} if snapshots are structurally equivalent
     * @throws NullPointerException if any parameter is null
     */
    public static boolean areEquivalent(IrSnapshot snapshot1, IrSnapshot snapshot2) {
        Objects.requireNonNull(snapshot1, "snapshot1");
        Objects.requireNonNull(snapshot2, "snapshot2");

        // Fast path: same reference
        if (snapshot1 == snapshot2) {
            return true;
        }

        // Check if both are empty
        if (snapshot1.isEmpty() && snapshot2.isEmpty()) {
            return true;
        }

        // If one is empty and the other is not, they're not equivalent
        if (snapshot1.isEmpty() != snapshot2.isEmpty()) {
            return false;
        }

        // Compare domain models
        if (!areModelsEquivalent(snapshot1.domainModel(), snapshot2.domainModel())) {
            return false;
        }

        // Compare port models
        if (!areModelsEquivalent(snapshot1.portModel(), snapshot2.portModel())) {
            return false;
        }

        // Compare application models
        if (!areModelsEquivalent(snapshot1.applicationModel(), snapshot2.applicationModel())) {
            return false;
        }

        return true;
    }

    /**
     * Checks if two models are equivalent (null-safe).
     *
     * @param model1 first model (may be {@code null})
     * @param model2 second model (may be {@code null})
     * @return {@code true} if both are null or both contain equivalent data
     */
    private static boolean areModelsEquivalent(Object model1, Object model2) {
        // Both null is equivalent
        if (model1 == null && model2 == null) {
            return true;
        }

        // One null, one not null is not equivalent
        if (model1 == null || model2 == null) {
            return false;
        }

        // Same reference is equivalent
        if (model1 == model2) {
            return true;
        }

        // Compare based on type
        if (model1 instanceof DomainModel dm1 && model2 instanceof DomainModel dm2) {
            return areDomainModelsEquivalent(dm1, dm2);
        } else if (model1 instanceof PortModel pm1 && model2 instanceof PortModel pm2) {
            return arePortModelsEquivalent(pm1, pm2);
        } else if (model1 instanceof ApplicationModel am1 && model2 instanceof ApplicationModel am2) {
            return areApplicationModelsEquivalent(am1, am2);
        }

        // Unknown type - conservative approach
        return false;
    }

    /**
     * Checks if two domain models are equivalent.
     *
     * @param dm1 first domain model (not {@code null})
     * @param dm2 second domain model (not {@code null})
     * @return {@code true} if equivalent
     */
    private static boolean areDomainModelsEquivalent(DomainModel dm1, DomainModel dm2) {
        // Compare by qualified names of types and services
        Set<String> typeNames1 = new HashSet<>(
                dm1.types().stream().map(DomainType::qualifiedName).toList());
        Set<String> typeNames2 = new HashSet<>(
                dm2.types().stream().map(DomainType::qualifiedName).toList());

        if (!typeNames1.equals(typeNames2)) {
            return false;
        }

        Set<String> serviceNames1 = new HashSet<>(
                dm1.services().stream().map(DomainService::qualifiedName).toList());
        Set<String> serviceNames2 = new HashSet<>(
                dm2.services().stream().map(DomainService::qualifiedName).toList());

        return serviceNames1.equals(serviceNames2);
    }

    /**
     * Checks if two port models are equivalent.
     *
     * @param pm1 first port model (not {@code null})
     * @param pm2 second port model (not {@code null})
     * @return {@code true} if equivalent
     */
    private static boolean arePortModelsEquivalent(PortModel pm1, PortModel pm2) {
        // Compare by qualified names of ports
        Set<String> portNames1 =
                new HashSet<>(pm1.ports().stream().map(Port::qualifiedName).toList());
        Set<String> portNames2 =
                new HashSet<>(pm2.ports().stream().map(Port::qualifiedName).toList());

        return portNames1.equals(portNames2);
    }

    /**
     * Checks if two application models are equivalent.
     *
     * @param am1 first application model (not {@code null})
     * @param am2 second application model (not {@code null})
     * @return {@code true} if equivalent
     */
    private static boolean areApplicationModelsEquivalent(ApplicationModel am1, ApplicationModel am2) {
        // Compare by qualified names of services
        Set<String> serviceNames1 = new HashSet<>(
                am1.services().stream().map(ApplicationService::qualifiedName).toList());
        Set<String> serviceNames2 = new HashSet<>(
                am2.services().stream().map(ApplicationService::qualifiedName).toList());

        return serviceNames1.equals(serviceNames2);
    }
}
