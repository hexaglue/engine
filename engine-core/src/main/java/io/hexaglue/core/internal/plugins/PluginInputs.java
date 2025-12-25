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
package io.hexaglue.core.internal.plugins;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.app.ApplicationModel;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.ir.IrView;
import java.util.Objects;

/**
 * Container for inputs required to create plugin-accessible views.
 *
 * <p>
 * This class aggregates the internal IR models ({@link PortModel}, {@link DomainModel},
 * {@link ApplicationModel}) produced by analysis phases. It serves as a structured
 * input for creating {@link IrView} instances that plugins can safely consume.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * The primary purpose of this class is to:
 * </p>
 * <ul>
 *   <li>Collect all internal models in one place</li>
 *   <li>Provide a structured way to pass models to bridge creation</li>
 *   <li>Validate that all required models are present</li>
 *   <li>Simplify the construction of plugin-facing views</li>
 * </ul>
 *
 * <h2>Usage in Compilation Pipeline</h2>
 * <pre>
 * Analysis Phase → Internal Models → PluginInputs → PluginModelBridge → IrView → Plugins
 * </pre>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Simple:</strong> Minimal wrapper with clear responsibilities</li>
 *   <li><strong>Immutable:</strong> All fields are final and cannot be modified</li>
 *   <li><strong>Validated:</strong> Enforces non-null constraints on construction</li>
 *   <li><strong>Focused:</strong> Only contains what's needed for bridge creation</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable after construction and safe for concurrent access.
 * All contained models are also immutable.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In HexaGlue core orchestration
 * PortModel portModel = portAnalyzer.analyze(elements);
 * DomainModel domainModel = domainAnalyzer.analyze(elements);
 * ApplicationModel applicationModel = applicationAnalyzer.analyze(elements);
 *
 * // Create inputs
 * PluginInputs inputs = PluginInputs.of(portModel, domainModel, applicationModel);
 *
 * // Create bridge from inputs
 * IrView irView = inputs.toIrView();
 *
 * // Or use the inputs to construct a complete generation context
 * GenerationContextSpec context = buildContext(inputs);
 * plugin.generate(context);
 * }</pre>
 */
@InternalMarker(reason = "Internal plugin input container; not exposed to plugins")
public final class PluginInputs {

    private final PortModel portModel;
    private final DomainModel domainModel;
    private final ApplicationModel applicationModel;

    /**
     * Creates plugin inputs with the given models.
     *
     * @param portModel        port model (not {@code null})
     * @param domainModel      domain model (not {@code null})
     * @param applicationModel application model (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    private PluginInputs(PortModel portModel, DomainModel domainModel, ApplicationModel applicationModel) {
        this.portModel = Objects.requireNonNull(portModel, "portModel");
        this.domainModel = Objects.requireNonNull(domainModel, "domainModel");
        this.applicationModel = Objects.requireNonNull(applicationModel, "applicationModel");
    }

    /**
     * Returns the port model.
     *
     * @return port model (never {@code null})
     */
    public PortModel portModel() {
        return portModel;
    }

    /**
     * Returns the domain model.
     *
     * @return domain model (never {@code null})
     */
    public DomainModel domainModel() {
        return domainModel;
    }

    /**
     * Returns the application model.
     *
     * @return application model (never {@code null})
     */
    public ApplicationModel applicationModel() {
        return applicationModel;
    }

    /**
     * Converts these inputs to an IR view suitable for plugin consumption.
     *
     * <p>
     * This is a convenience method that delegates to {@link PluginModelBridge#create(PortModel, DomainModel, ApplicationModel)}.
     * </p>
     *
     * @return IR view (never {@code null})
     */
    public IrView toIrView() {
        return PluginModelBridge.create(portModel, domainModel, applicationModel);
    }

    /**
     * Creates plugin inputs from models.
     *
     * @param portModel        port model (not {@code null})
     * @param domainModel      domain model (not {@code null})
     * @param applicationModel application model (not {@code null})
     * @return plugin inputs (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public static PluginInputs of(PortModel portModel, DomainModel domainModel, ApplicationModel applicationModel) {
        return new PluginInputs(portModel, domainModel, applicationModel);
    }

    /**
     * Creates plugin inputs with empty models.
     *
     * <p>
     * This factory method is useful for testing or when no analysis has been performed yet.
     * </p>
     *
     * @return plugin inputs with empty models (never {@code null})
     */
    public static PluginInputs empty() {
        return new PluginInputs(PortModel.empty(), DomainModel.empty(), ApplicationModel.empty());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PluginInputs other)) return false;
        return portModel.equals(other.portModel)
                && domainModel.equals(other.domainModel)
                && applicationModel.equals(other.applicationModel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(portModel, domainModel, applicationModel);
    }

    @Override
    public String toString() {
        return "PluginInputs{"
                + "ports="
                + portModel.ports().size()
                + ", domainTypes="
                + domainModel.types().size()
                + ", appServices="
                + applicationModel.services().size()
                + '}';
    }
}
