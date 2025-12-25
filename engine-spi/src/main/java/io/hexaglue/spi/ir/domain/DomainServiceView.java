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
package io.hexaglue.spi.ir.domain;

import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of a domain service.
 *
 * <p>A domain service expresses pure business rules that do not belong to a single entity.
 * HexaGlue never generates domain services; it may analyze them for diagnostics and documentation.</p>
 */
public interface DomainServiceView {

    /**
     * Qualified name of the domain service type.
     *
     * @return qualified name (never blank)
     */
    String qualifiedName();

    /**
     * Stable simple name of the service type.
     *
     * @return simple name (never blank)
     */
    String simpleName();

    /**
     * Returns a user-facing description if available.
     *
     * @return description if available
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Returns a read-only list of operations exposed by this domain service, if modeled.
     *
     * <p>This is intentionally minimal: only signature-level information relevant to docs/diagnostics.</p>
     *
     * @return list of operations (never {@code null})
     */
    default List<OperationView> operations() {
        return List.of();
    }

    /**
     * Minimal view of a service operation.
     *
     * <p>Kept nested to avoid growing the SPI surface area prematurely.</p>
     */
    interface OperationView {

        /** @return operation name (never blank) */
        String name();

        /** @return return type (never {@code null}) */
        TypeRef returnType();

        /** @return parameter types (never {@code null}) */
        List<TypeRef> parameterTypes();
    }

    /**
     * Creates a simple immutable {@link DomainServiceView} instance without operations.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param qualifiedName qualified name
     * @param simpleName simple name
     * @param description description (nullable)
     * @return domain service view
     */
    static DomainServiceView of(String qualifiedName, String simpleName, String description) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        final String qn = qualifiedName.trim();
        final String sn = simpleName.trim();
        if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
        if (sn.isEmpty()) throw new IllegalArgumentException("simpleName must not be blank");
        final String desc = (description == null || description.isBlank()) ? null : description.trim();

        return new DomainServiceView() {
            @Override
            public String qualifiedName() {
                return qn;
            }

            @Override
            public String simpleName() {
                return sn;
            }

            @Override
            public Optional<String> description() {
                return Optional.ofNullable(desc);
            }
        };
    }
}
