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
import io.hexaglue.core.internal.ir.app.ApplicationService;
import io.hexaglue.core.internal.ir.domain.DomainId;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainService;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.ir.app.ApplicationModelView;
import io.hexaglue.spi.ir.app.ApplicationServiceView;
import io.hexaglue.spi.ir.domain.DomainIdView;
import io.hexaglue.spi.ir.domain.DomainModelView;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainServiceView;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.ir.ports.PortDirection;
import io.hexaglue.spi.ir.ports.PortModelView;
import io.hexaglue.spi.ir.ports.PortView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Bridge between internal IR models and SPI view interfaces for plugin consumption.
 *
 * <p>
 * This class is the critical boundary between HexaGlue core internals and the plugin SPI.
 * It wraps internal model classes ({@link PortModel}, {@link DomainModel}, {@link ApplicationModel})
 * and exposes them through stable SPI view interfaces ({@link IrView}, {@link PortModelView},
 * {@link DomainModelView}, {@link ApplicationModelView}).
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * The bridge serves several critical functions:
 * </p>
 * <ul>
 *   <li><strong>Decoupling:</strong> Isolates plugins from core implementation details</li>
 *   <li><strong>Stability:</strong> Provides stable SPI interfaces while internals can evolve</li>
 *   <li><strong>Type Safety:</strong> Ensures plugins only access view interfaces, never internal types</li>
 *   <li><strong>Read-Only Access:</strong> Enforces immutability through view interfaces</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>
 * The bridge follows the Adapter pattern:
 * </p>
 * <pre>
 * Plugin Code
 *     ↓
 * IrView (SPI interface)
 *     ↓
 * PluginModelBridge (Adapter)
 *     ↓
 * Internal Models (PortModel, DomainModel, ApplicationModel)
 * </pre>
 *
 * <h2>View Wrapping Strategy</h2>
 * <p>
 * Individual model elements ({@link Port}, {@link DomainType}, {@link ApplicationService})
 * already implement their corresponding view interfaces ({@link PortView}, {@link DomainTypeView},
 * {@link ApplicationServiceView}), so they can be returned directly. Container models
 * ({@link PortModel}, {@link DomainModel}, {@link ApplicationModel}) are wrapped in
 * lightweight adapter implementations.
 * </p>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Zero-Copy:</strong> Wraps existing models without copying data</li>
 *   <li><strong>Lightweight:</strong> Minimal overhead for view delegation</li>
 *   <li><strong>Immutable:</strong> All views are read-only</li>
 *   <li><strong>Thread-Safe:</strong> Safe for concurrent access by multiple plugins</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is immutable after construction and safe for concurrent access.
 * All wrapped models are also immutable.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // In HexaGlue core orchestration
 * PortModel portModel = portAnalyzer.analyze(elements);
 * DomainModel domainModel = domainAnalyzer.analyze(elements);
 * ApplicationModel applicationModel = applicationAnalyzer.analyze(elements);
 *
 * // Create bridge
 * IrView irView = PluginModelBridge.create(portModel, domainModel, applicationModel);
 *
 * // Pass to plugins via GenerationContextSpec
 * GenerationContextSpec context = ...; // irView is part of context
 * plugin.generate(context);
 * }</pre>
 */
@InternalMarker(reason = "Internal bridge to SPI; plugins use IrView interface")
public final class PluginModelBridge implements IrView {

    private final PortModelView portModelView;
    private final DomainModelView domainModelView;
    private final ApplicationModelView applicationModelView;

    /**
     * Creates a plugin model bridge with the given models.
     *
     * @param portModelView        port model view (not {@code null})
     * @param domainModelView      domain model view (not {@code null})
     * @param applicationModelView application model view (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    private PluginModelBridge(
            PortModelView portModelView, DomainModelView domainModelView, ApplicationModelView applicationModelView) {
        this.portModelView = Objects.requireNonNull(portModelView, "portModelView");
        this.domainModelView = Objects.requireNonNull(domainModelView, "domainModelView");
        this.applicationModelView = Objects.requireNonNull(applicationModelView, "applicationModelView");
    }

    @Override
    public DomainModelView domain() {
        return domainModelView;
    }

    @Override
    public PortModelView ports() {
        return portModelView;
    }

    @Override
    public ApplicationModelView application() {
        return applicationModelView;
    }

    /**
     * Creates an IR view from internal models.
     *
     * <p>
     * This is the primary factory method used by HexaGlue core to create plugin-safe
     * views of the internal IR.
     * </p>
     *
     * @param portModel        port model (not {@code null})
     * @param domainModel      domain model (not {@code null})
     * @param applicationModel application model (not {@code null})
     * @return IR view (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public static IrView create(PortModel portModel, DomainModel domainModel, ApplicationModel applicationModel) {
        Objects.requireNonNull(portModel, "portModel");
        Objects.requireNonNull(domainModel, "domainModel");
        Objects.requireNonNull(applicationModel, "applicationModel");

        PortModelView portView = wrapPortModel(portModel);
        DomainModelView domainView = wrapDomainModel(domainModel);
        ApplicationModelView appView = wrapApplicationModel(applicationModel);

        return new PluginModelBridge(portView, domainView, appView);
    }

    /**
     * Wraps a port model as a view.
     *
     * @param portModel port model (not {@code null})
     * @return port model view (never {@code null})
     */
    private static PortModelView wrapPortModel(PortModel portModel) {
        return new PortModelView() {
            @Override
            public List<PortView> allPorts() {
                // Port implements PortView, so we can cast directly
                return portModel.ports().stream().map(p -> (PortView) p).toList();
            }

            @Override
            public List<PortView> allPorts(PortDirection direction) {
                return portModel.portsByDirection(direction).stream()
                        .map(p -> (PortView) p)
                        .toList();
            }

            @Override
            public Optional<PortView> findPort(String qualifiedName) {
                return portModel.findPort(qualifiedName).map(p -> (PortView) p);
            }
        };
    }

    /**
     * Wraps a domain model as a view.
     *
     * @param domainModel domain model (not {@code null})
     * @return domain model view (never {@code null})
     */
    private static DomainModelView wrapDomainModel(DomainModel domainModel) {
        return new DomainModelView() {
            @Override
            public List<DomainTypeView> allTypes() {
                return domainModel.types().stream()
                        .map(PluginModelBridge::wrapDomainType)
                        .toList();
            }

            @Override
            public Optional<DomainTypeView> findType(String qualifiedName) {
                return domainModel.findType(qualifiedName).map(PluginModelBridge::wrapDomainType);
            }

            @Override
            public List<DomainServiceView> allServices() {
                return domainModel.services().stream()
                        .map(PluginModelBridge::wrapDomainService)
                        .toList();
            }

            @Override
            public Optional<DomainServiceView> findService(String qualifiedName) {
                return domainModel.findService(qualifiedName).map(PluginModelBridge::wrapDomainService);
            }
        };
    }

    /**
     * Wraps a domain type as a view.
     *
     * @param domainType domain type (not {@code null})
     * @return domain type view (never {@code null})
     */
    private static DomainTypeView wrapDomainType(DomainType domainType) {
        return new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return domainType.qualifiedName();
            }

            @Override
            public String simpleName() {
                return domainType.simpleName();
            }

            @Override
            public io.hexaglue.spi.ir.domain.DomainTypeKind kind() {
                return domainType.kind();
            }

            @Override
            public io.hexaglue.spi.types.TypeRef type() {
                return domainType.type();
            }

            @Override
            public List<DomainPropertyView> properties() {
                return domainType.properties().stream()
                        .map(PluginModelBridge::wrapDomainProperty)
                        .toList();
            }

            @Override
            public Optional<DomainIdView> id() {
                return domainType.id().map(PluginModelBridge::wrapDomainId);
            }

            @Override
            public boolean isImmutable() {
                return domainType.isImmutable();
            }

            @Override
            public Optional<String> description() {
                return domainType.description();
            }
        };
    }

    /**
     * Wraps a domain property as a view.
     *
     * @param property domain property (not {@code null})
     * @return domain property view (never {@code null})
     */
    private static DomainPropertyView wrapDomainProperty(DomainProperty property) {
        return new DomainPropertyView() {
            @Override
            public String name() {
                return property.name();
            }

            @Override
            public io.hexaglue.spi.types.TypeRef type() {
                return property.type();
            }

            @Override
            public boolean isIdentity() {
                return property.isIdentity();
            }

            @Override
            public boolean isImmutable() {
                return property.isImmutable();
            }

            @Override
            public Optional<String> declaringType() {
                return property.declaringType();
            }

            @Override
            public Optional<String> description() {
                return property.description();
            }
        };
    }

    /**
     * Wraps a domain id as a view.
     *
     * @param id domain id (not {@code null})
     * @return domain id view (never {@code null})
     */
    private static DomainIdView wrapDomainId(DomainId id) {
        return new DomainIdView() {
            @Override
            public Optional<String> declaringEntity() {
                return id.declaringEntity();
            }

            @Override
            public String name() {
                return id.name();
            }

            @Override
            public io.hexaglue.spi.types.TypeRef type() {
                return id.type();
            }

            @Override
            public boolean isComposite() {
                return id.isComposite();
            }
        };
    }

    /**
     * Wraps a domain service as a view.
     *
     * @param service domain service (not {@code null})
     * @return domain service view (never {@code null})
     */
    private static DomainServiceView wrapDomainService(DomainService service) {
        return new DomainServiceView() {
            @Override
            public String qualifiedName() {
                return service.qualifiedName();
            }

            @Override
            public String simpleName() {
                return service.simpleName();
            }

            @Override
            public Optional<String> description() {
                return service.description();
            }
        };
    }

    /**
     * Wraps an application model as a view.
     *
     * @param applicationModel application model (not {@code null})
     * @return application model view (never {@code null})
     */
    private static ApplicationModelView wrapApplicationModel(ApplicationModel applicationModel) {
        // If the model is empty, return an unsupported view
        if (applicationModel.isEmpty()) {
            return ApplicationModelView.unsupported();
        }

        return new ApplicationModelView() {
            @Override
            public List<ApplicationServiceView> allServices() {
                // ApplicationService implements ApplicationServiceView
                return applicationModel.services().stream()
                        .map(s -> (ApplicationServiceView) s)
                        .toList();
            }

            @Override
            public Optional<ApplicationServiceView> findService(String qualifiedName) {
                return applicationModel.findService(qualifiedName).map(s -> (ApplicationServiceView) s);
            }

            @Override
            public boolean isSupported() {
                return !applicationModel.isEmpty();
            }
        };
    }

    @Override
    public String toString() {
        return "PluginModelBridge{"
                + "ports="
                + portModelView.allPorts().size()
                + ", domainTypes="
                + domainModelView.allTypes().size()
                + ", appServices="
                + applicationModelView.allServices().size()
                + '}';
    }
}
