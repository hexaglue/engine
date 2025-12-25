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
package io.hexaglue.core.internal.spi;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.IrSnapshot;
import io.hexaglue.core.internal.ir.app.ApplicationModel;
import io.hexaglue.core.internal.ir.domain.DomainId;
import io.hexaglue.core.internal.ir.domain.DomainModel;
import io.hexaglue.core.internal.ir.domain.DomainProperty;
import io.hexaglue.core.internal.ir.domain.DomainType;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortModel;
import io.hexaglue.spi.ir.IrView;
import io.hexaglue.spi.ir.app.ApplicationModelView;
import io.hexaglue.spi.ir.domain.AnnotationView;
import io.hexaglue.spi.ir.domain.DomainIdView;
import io.hexaglue.spi.ir.domain.DomainModelView;
import io.hexaglue.spi.ir.domain.DomainPropertyView;
import io.hexaglue.spi.ir.domain.DomainServiceView;
import io.hexaglue.spi.ir.domain.DomainTypeKind;
import io.hexaglue.spi.ir.domain.DomainTypeView;
import io.hexaglue.spi.ir.ports.PortModelView;
import io.hexaglue.spi.ir.ports.PortView;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Adapter that converts internal IR models to SPI-compatible views for plugin consumption.
 *
 * <p>
 * This adapter bridges the gap between HexaGlue's internal IR representation
 * ({@link IrSnapshot}) and the stable SPI interface ({@link IrView}) that plugins consume.
 * </p>
 *
 * <h2>Design Strategy</h2>
 * <p>
 * The adapter leverages the fact that internal model classes already implement their
 * corresponding SPI interfaces:
 * </p>
 * <ul>
 *   <li>{@link Port} implements {@link PortView}</li>
 *   <li>This allows direct casting without object creation overhead</li>
 * </ul>
 *
 * <h2>Conversion Strategy</h2>
 * <ul>
 *   <li><strong>PortModelView:</strong> Uses {@link PortModelView#of(List)} factory with {@link Port} list</li>
 *   <li><strong>DomainModelView:</strong> Uses {@link DomainModelView#of(List, List)} factory</li>
 *   <li><strong>ApplicationModelView:</strong> Uses {@link ApplicationModelView#unsupported()} for now</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This adapter is stateless and thread-safe. The underlying {@link IrSnapshot} is immutable.
 * </p>
 */
@InternalMarker(reason = "Internal SPI adapter; not exposed to plugins")
public final class IrViewAdapter {

    private IrViewAdapter() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates an {@link IrView} from an {@link IrSnapshot}.
     *
     * <p>
     * This method adapts the internal IR representation to the plugin-facing SPI interface.
     * The resulting view is read-only and provides access to domain, port, and application models.
     * </p>
     *
     * @param snapshot IR snapshot from analysis phase (not {@code null})
     * @return immutable IR view for plugin consumption (never {@code null})
     * @throws NullPointerException if snapshot is null
     */
    public static IrView from(IrSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        // Convert each model to its corresponding view
        DomainModelView domainView = adaptDomainModel(snapshot.domainModel(), snapshot);
        PortModelView portView = adaptPortModel(snapshot.portModel());
        ApplicationModelView appView = adaptApplicationModel(snapshot.applicationModel());

        // Create immutable IrView implementation
        return new IrViewImpl(domainView, portView, appView);
    }

    /**
     * Adapts a {@link DomainModel} to {@link DomainModelView}.
     *
     * <p>
     * This method converts internal domain types and services to their SPI view equivalents
     * using the factory methods provided by the SPI interfaces.
     * </p>
     *
     * <p>
     * <strong>Note:</strong> Aggregate root classification is performed during the ANALYZE phase
     * and stored in the IR. This adapter simply reflects the IR state without recomputing semantics.
     * </p>
     *
     * @param domainModel internal domain model (nullable)
     * @param snapshot    full IR snapshot (not {@code null}) - parameter kept for future use
     * @return domain model view (never {@code null})
     */
    private static DomainModelView adaptDomainModel(DomainModel domainModel, IrSnapshot snapshot) {
        if (domainModel == null) {
            return DomainModelView.of(List.of(), List.of());
        }

        // Convert domain types - no semantic recomputation, just mapping
        List<DomainTypeView> typeViews =
                domainModel.types().stream().map(IrViewAdapter::adaptDomainType).collect(Collectors.toList());

        // Convert domain services
        // Note: DomainService to DomainServiceView conversion not yet implemented, so we pass empty list for now
        List<DomainServiceView> serviceViews = List.of();

        return DomainModelView.of(typeViews, serviceViews);
    }

    /**
     * Adapts a {@link DomainType} to {@link DomainTypeView}.
     *
     * <p>
     * This is a thin mapping layer that exposes the IR state without semantic recomputation.
     * The domain type kind (including {@code AGGREGATE_ROOT} classification) has already been
     * determined during the ANALYZE phase.
     * </p>
     *
     * @param domainType internal domain type (not {@code null})
     * @return domain type view (never {@code null})
     * @throws NullPointerException if domainType is null
     */
    private static DomainTypeView adaptDomainType(DomainType domainType) {
        Objects.requireNonNull(domainType, "domainType");

        // Convert properties
        List<DomainPropertyView> propertyViews = domainType.properties().stream()
                .map(IrViewAdapter::adaptDomainProperty)
                .collect(Collectors.toList());

        // Convert id
        DomainIdView idView = domainType.id().map(IrViewAdapter::adaptDomainId).orElse(null);

        // Extract description
        String description = domainType.description().orElse(null);

        // Create view using factory - wrap to add annotation support
        DomainTypeView baseView = DomainTypeView.of(
                domainType.qualifiedName(),
                domainType.simpleName(),
                domainType.kind(),
                domainType.type(),
                propertyViews,
                idView,
                domainType.isImmutable(),
                description);

        // Wrap to add annotations() support
        return new DomainTypeViewWithAnnotations(baseView, domainType);
    }

    /**
     * Adapts a {@link DomainProperty} to {@link DomainPropertyView}.
     *
     * @param property internal domain property (not {@code null})
     * @return domain property view (never {@code null})
     */
    private static DomainPropertyView adaptDomainProperty(DomainProperty property) {
        Objects.requireNonNull(property, "property");

        String declaringType = property.declaringType().orElse(null);

        DomainPropertyView baseView = DomainPropertyView.of(
                property.name(), property.type(), property.isIdentity(), property.isImmutable(), declaringType);

        // Wrap with annotations support
        return new DomainPropertyViewWithAnnotations(baseView, property);
    }

    /**
     * Adapts a {@link DomainId} to {@link DomainIdView}.
     *
     * @param id internal domain id (not {@code null})
     * @return domain id view (never {@code null})
     */
    private static DomainIdView adaptDomainId(DomainId id) {
        Objects.requireNonNull(id, "id");

        String declaringEntity = id.declaringEntity().orElse(null);

        return DomainIdView.of(declaringEntity, id.name(), id.type(), id.isComposite());
    }

    /**
     * Adapts a {@link PortModel} to {@link PortModelView}.
     *
     * <p>
     * Since {@link Port} already implements {@link PortView}, we can directly
     * use the port list from the model. The {@link PortModelView#of(List)} factory
     * creates an immutable view wrapper.
     * </p>
     *
     * @param portModel internal port model (nullable)
     * @return port model view (never {@code null})
     */
    private static PortModelView adaptPortModel(PortModel portModel) {
        if (portModel == null) {
            return PortModelView.of(List.of());
        }

        // Port implements PortView, so we can cast the list
        List<Port> ports = portModel.ports();
        List<PortView> portViews = List.copyOf(ports); // Port implements PortView

        return PortModelView.of(portViews);
    }

    /**
     * Adapts an {@link ApplicationModel} to {@link ApplicationModelView}.
     *
     * <p>
     * For now, this returns an unsupported view since application service analysis
     * is not yet implemented.
     * </p>
     *
     * @param applicationModel internal application model (nullable)
     * @return application model view (never {@code null})
     */
    private static ApplicationModelView adaptApplicationModel(ApplicationModel applicationModel) {
        // Application model analysis not yet implemented
        return ApplicationModelView.unsupported();
    }

    /**
     * Immutable implementation of {@link IrView}.
     *
     * <p>
     * This is a simple value object that holds references to the three model views.
     * </p>
     */
    private static final class IrViewImpl implements IrView {
        private final DomainModelView domainView;
        private final PortModelView portView;
        private final ApplicationModelView appView;

        IrViewImpl(DomainModelView domainView, PortModelView portView, ApplicationModelView appView) {
            this.domainView = Objects.requireNonNull(domainView, "domainView");
            this.portView = Objects.requireNonNull(portView, "portView");
            this.appView = Objects.requireNonNull(appView, "appView");
        }

        @Override
        public DomainModelView domain() {
            return domainView;
        }

        @Override
        public PortModelView ports() {
            return portView;
        }

        @Override
        public ApplicationModelView application() {
            return appView;
        }

        @Override
        public String toString() {
            return "IrView{ports=" + portView.allPorts().size() + ", domain="
                    + domainView.allTypes().size() + ", app="
                    + (appView.isSupported() ? appView.allServices().size() : "unsupported") + "}";
        }
    }

    /**
     * Wrapper for {@link DomainTypeView} that adds annotation support.
     *
     * <p>
     * This class delegates all methods to the wrapped base view except {@code annotations()},
     * which is overridden to expose annotations from the internal {@link DomainType} model.
     * </p>
     *
     * <p>
     * <strong>Design note:</strong> Unlike the previous implementation, this wrapper does NOT
     * recompute aggregate root classification. The {@code kind()} and {@code isAggregateRoot()}
     * methods simply reflect the IR state as determined during the ANALYZE phase.
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> Annotation conversion is memoized - the conversion from
     * {@link io.hexaglue.core.frontend.AnnotationModel} to {@link AnnotationView} happens
     * only once per domain type, not on every call to {@code annotations()}.
     * </p>
     */
    private static final class DomainTypeViewWithAnnotations implements DomainTypeView {
        private final DomainTypeView delegate;
        private final DomainType domainType;
        private volatile List<AnnotationView> cachedAnnotations;

        DomainTypeViewWithAnnotations(DomainTypeView delegate, DomainType domainType) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.domainType = Objects.requireNonNull(domainType, "domainType");
        }

        @Override
        public List<AnnotationView> annotations() {
            // Memoize conversion to avoid reconverting on each call
            List<AnnotationView> result = cachedAnnotations;
            if (result == null) {
                synchronized (this) {
                    result = cachedAnnotations;
                    if (result == null) {
                        cachedAnnotations = result = AnnotationViewConverter.toViews(domainType.annotations());
                    }
                }
            }
            return result;
        }

        // Delegate all other methods to base view

        @Override
        public String qualifiedName() {
            return delegate.qualifiedName();
        }

        @Override
        public String simpleName() {
            return delegate.simpleName();
        }

        @Override
        public DomainTypeKind kind() {
            return delegate.kind();
        }

        @Override
        public TypeRef type() {
            return delegate.type();
        }

        @Override
        public List<DomainPropertyView> properties() {
            return delegate.properties();
        }

        @Override
        public Optional<DomainIdView> id() {
            return delegate.id();
        }

        @Override
        public boolean isImmutable() {
            return delegate.isImmutable();
        }

        @Override
        public Optional<String> description() {
            return delegate.description();
        }

        @Override
        public Optional<TypeRef> superType() {
            return domainType.superType();
        }

        @Override
        public List<TypeRef> interfaces() {
            return domainType.interfaces();
        }

        @Override
        public Optional<List<TypeRef>> permittedSubtypes() {
            return domainType.permittedSubtypes();
        }

        @Override
        public Optional<List<String>> enumConstants() {
            return domainType.enumConstants();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }

    /**
     * Wrapper for {@link DomainPropertyView} that adds annotation support.
     *
     * <p>
     * This class delegates all methods to the wrapped base view except {@code annotations()},
     * which is overridden to expose annotations from the internal {@link DomainProperty} model.
     * </p>
     *
     * <p>
     * <strong>Performance:</strong> Annotation conversion is memoized - the conversion from
     * {@link io.hexaglue.core.frontend.AnnotationModel} to {@link AnnotationView} happens
     * only once per property, not on every call to {@code annotations()}.
     * </p>
     */
    private static final class DomainPropertyViewWithAnnotations implements DomainPropertyView {
        private final DomainPropertyView delegate;
        private final DomainProperty property;
        private volatile List<AnnotationView> cachedAnnotations;

        DomainPropertyViewWithAnnotations(DomainPropertyView delegate, DomainProperty property) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.property = Objects.requireNonNull(property, "property");
        }

        @Override
        public List<AnnotationView> annotations() {
            // Memoize conversion to avoid reconverting on each call
            List<AnnotationView> result = cachedAnnotations;
            if (result == null) {
                synchronized (this) {
                    result = cachedAnnotations;
                    if (result == null) {
                        cachedAnnotations = result = AnnotationViewConverter.toViews(property.annotations());
                    }
                }
            }
            return result;
        }

        @Override
        public Optional<io.hexaglue.spi.ir.domain.RelationshipMetadata> relationship() {
            // Relationship metadata is already in SPI format, no conversion needed
            return property.relationship();
        }

        // Delegate all other methods to base view

        @Override
        public String name() {
            return delegate.name();
        }

        @Override
        public TypeRef type() {
            return delegate.type();
        }

        @Override
        public boolean isIdentity() {
            return delegate.isIdentity();
        }

        @Override
        public boolean isImmutable() {
            return delegate.isImmutable();
        }

        @Override
        public Optional<String> declaringType() {
            return delegate.declaringType();
        }

        @Override
        public Optional<String> description() {
            return delegate.description();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
