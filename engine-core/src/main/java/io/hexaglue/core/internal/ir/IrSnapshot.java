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
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.ports.PortModel;

/**
 * Immutable snapshot of the Intermediate Representation (IR) at a given compilation phase.
 *
 * <p>
 * The IR snapshot captures the complete state of analyzed domain types, ports, and application
 * services at a specific point in the compilation pipeline. It serves as the foundation for:
 * <ul>
 *   <li>Plugin execution (via read-only SPI views)</li>
 *   <li>Validation and diagnostics</li>
 *   <li>Code generation planning</li>
 *   <li>Incremental compilation (future)</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Immutability:</strong> Once built, the snapshot cannot be modified.</li>
 *   <li><strong>Performance:</strong> Backed by {@link IrIndexes} for fast lookups.</li>
 *   <li><strong>Completeness:</strong> Represents all information needed for generation.</li>
 * </ul>
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Core analyzes source elements and populates internal IR models.</li>
 *   <li>{@link IrSnapshot} is created from the populated models.</li>
 *   <li>{@link IrIndexes} are built for optimized access.</li>
 *   <li>Read-only SPI views ({@link io.hexaglue.spi.ir.IrView}) are created from the snapshot.</li>
 *   <li>Plugins consume the SPI views during generation.</li>
 * </ol>
 *
 * <h2>Internal Representation</h2>
 * <p>
 * The snapshot contains references to internal IR model objects located in:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.core.internal.ir.domain.*} - Domain types, properties, ids, services</li>
 *   <li>{@code io.hexaglue.core.internal.ir.ports.*} - Port interfaces, methods, parameters</li>
 *   <li>{@code io.hexaglue.core.internal.ir.app.*} - Application services (optional)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * IrSnapshot snapshot = IrSnapshot.builder()
 *     .domainModel(domainModel)
 *     .portModel(portModel)
 *     .applicationModel(applicationModel)
 *     .build();
 *
 * IrIndexes indexes = IrIndexes.from(snapshot);
 * }</pre>
 */
@InternalMarker(reason = "Core IR implementation; plugins use io.hexaglue.spi.ir.IrView")
public final class IrSnapshot {

    private final DomainModel domainModel;
    private final PortModel portModel;
    private final ApplicationModel applicationModel;
    private final long timestamp;

    /**
     * Creates an IR snapshot with the given models.
     *
     * @param domainModel      internal domain model (nullable for empty model)
     * @param portModel        internal port model (nullable for empty model)
     * @param applicationModel internal application model (nullable for empty model)
     */
    private IrSnapshot(DomainModel domainModel, PortModel portModel, ApplicationModel applicationModel) {
        this.domainModel = domainModel;
        this.portModel = portModel;
        this.applicationModel = applicationModel;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns the internal domain model.
     *
     * <p>
     * This model contains all analyzed domain types, properties, ids, and domain services.
     * It is an internal representation and must not be exposed directly to plugins.
     * </p>
     *
     * @return domain model (may be {@code null} if no domain was analyzed)
     */
    public DomainModel domainModel() {
        return domainModel;
    }

    /**
     * Returns the internal port model.
     *
     * <p>
     * This model contains all analyzed ports (driving and driven) with their methods and parameters.
     * It is an internal representation and must not be exposed directly to plugins.
     * </p>
     *
     * @return port model (may be {@code null} if no ports were analyzed)
     */
    public PortModel portModel() {
        return portModel;
    }

    /**
     * Returns the internal application model.
     *
     * <p>
     * This model contains analyzed application services / use cases. Not all compilations will
     * model application services explicitly.
     * </p>
     *
     * @return application model (may be {@code null})
     */
    public ApplicationModel applicationModel() {
        return applicationModel;
    }

    /**
     * Legacy accessor kept to reduce migration blast radius while internal IR is being typed.
     *
     * <p>
     * This method returns the same instance as {@link #domainModel()} but typed as {@code Object}.
     * It exists only for incremental migration of internal packages. New code should use
     * {@link #domainModel()} directly.
     * </p>
     *
     * @return domain model as {@code Object} (may be {@code null})
     * @deprecated use {@link #domainModel()}
     */
    @Deprecated(forRemoval = false)
    public Object domainModelObject() {
        return domainModel;
    }

    /**
     * Legacy accessor kept to reduce migration blast radius while internal IR is being typed.
     *
     * @return port model as {@code Object} (may be {@code null})
     * @deprecated use {@link #portModel()}
     */
    @Deprecated(forRemoval = false)
    public Object portModelObject() {
        return portModel;
    }

    /**
     * Legacy accessor kept to reduce migration blast radius while internal IR is being typed.
     *
     * @return application model as {@code Object} (may be {@code null})
     * @deprecated use {@link #applicationModel()}
     */
    @Deprecated(forRemoval = false)
    public Object applicationModelObject() {
        return applicationModel;
    }

    /**
     * Returns the timestamp when this snapshot was created (milliseconds since epoch).
     *
     * <p>
     * This is primarily for debugging and diagnostics. It should not be used for functional logic.
     * </p>
     *
     * @return creation timestamp
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Returns whether this snapshot contains any analyzed elements.
     *
     * @return {@code true} if all models are null/empty
     */
    public boolean isEmpty() {
        return domainModel == null && portModel == null && applicationModel == null;
    }

    @Override
    public String toString() {
        return "IrSnapshot{timestamp=" + timestamp + ", isEmpty=" + isEmpty() + "}";
    }

    /**
     * Creates a builder for constructing {@link IrSnapshot} instances.
     *
     * @return builder (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty IR snapshot.
     *
     * @return empty snapshot (never {@code null})
     */
    public static IrSnapshot empty() {
        return new IrSnapshot(null, null, null);
    }

    /**
     * Builder for {@link IrSnapshot}.
     *
     * <p>
     * This builder is not thread-safe and should be used from a single thread only.
     * </p>
     */
    public static final class Builder {
        private DomainModel domainModel;
        private PortModel portModel;
        private ApplicationModel applicationModel;

        private Builder() {
            // package-private
        }

        /**
         * Sets the domain model.
         *
         * @param domainModel domain model (nullable)
         * @return this builder
         */
        public Builder domainModel(DomainModel domainModel) {
            this.domainModel = domainModel;
            return this;
        }

        /**
         * Sets the port model.
         *
         * @param portModel port model (nullable)
         * @return this builder
         */
        public Builder portModel(PortModel portModel) {
            this.portModel = portModel;
            return this;
        }

        /**
         * Sets the application model.
         *
         * @param applicationModel application model (nullable)
         * @return this builder
         */
        public Builder applicationModel(ApplicationModel applicationModel) {
            this.applicationModel = applicationModel;
            return this;
        }

        /**
         * Builds the IR snapshot.
         *
         * @return immutable snapshot (never {@code null})
         */
        public IrSnapshot build() {
            return new IrSnapshot(domainModel, portModel, applicationModel);
        }
    }
}
