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
package io.hexaglue.spi.ir.app;

import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of an application service (use case) in the hexagon.
 *
 * <p>Application services orchestrate domain operations and outbound ports.
 * HexaGlue may analyze them as <em>signals</em> for documentation and diagnostics,
 * but must never treat them as authoritative sources for inferring domain structure.</p>
 *
 * <p>This view is intentionally signature-level and tool-agnostic.</p>
 */
public interface ApplicationServiceView {

    /**
     * Qualified name of the application service type.
     *
     * @return qualified name (never blank)
     */
    String qualifiedName();

    /**
     * Simple name of the application service type.
     *
     * @return simple name (never blank)
     */
    String simpleName();

    /**
     * Operations (methods) exposed by the application service.
     *
     * @return immutable list of operations (never {@code null})
     */
    List<OperationView> operations();

    /**
     * Optional user-facing description for documentation.
     *
     * @return description if available
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Minimal view of an application service operation.
     *
     * <p>Kept nested to reduce SPI surface area and keep evolution easy.</p>
     */
    interface OperationView {

        /** @return operation name (never blank) */
        String name();

        /** @return return type (never {@code null}) */
        TypeRef returnType();

        /** @return parameter types (never {@code null}) */
        List<TypeRef> parameterTypes();

        /**
         * Optional stable signature id (best-effort).
         *
         * @return signature id if available
         */
        default Optional<String> signatureId() {
            return Optional.empty();
        }
    }

    /**
     * Creates a simple immutable {@link ApplicationServiceView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param qualifiedName qualified name
     * @param simpleName simple name
     * @param operations operations list
     * @param description description (nullable)
     * @return application service view
     */
    static ApplicationServiceView of(
            String qualifiedName, String simpleName, List<OperationView> operations, String description) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(operations, "operations");

        final String qn = qualifiedName.trim();
        final String sn = simpleName.trim();
        if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
        if (sn.isEmpty()) throw new IllegalArgumentException("simpleName must not be blank");

        final List<OperationView> ops = List.copyOf(operations);
        for (OperationView op : ops) Objects.requireNonNull(op, "operations contains null");

        final String desc = (description == null || description.isBlank()) ? null : description.trim();

        return new ApplicationServiceView() {
            @Override
            public String qualifiedName() {
                return qn;
            }

            @Override
            public String simpleName() {
                return sn;
            }

            @Override
            public List<OperationView> operations() {
                return ops;
            }

            @Override
            public Optional<String> description() {
                return Optional.ofNullable(desc);
            }
        };
    }

    /**
     * Creates a simple immutable {@link OperationView} instance.
     *
     * <p>This helper is intended for tests and tooling.</p>
     *
     * @param name operation name
     * @param returnType return type
     * @param parameterTypes parameter types
     * @param signatureId signature id (nullable)
     * @return operation view
     */
    static OperationView operation(String name, TypeRef returnType, List<TypeRef> parameterTypes, String signatureId) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(returnType, "returnType");
        Objects.requireNonNull(parameterTypes, "parameterTypes");

        final String n = name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("name must not be blank");

        final List<TypeRef> pts = List.copyOf(parameterTypes);
        for (TypeRef t : pts) Objects.requireNonNull(t, "parameterTypes contains null");

        final String sid = (signatureId == null || signatureId.isBlank()) ? null : signatureId.trim();

        return new OperationView() {
            @Override
            public String name() {
                return n;
            }

            @Override
            public TypeRef returnType() {
                return returnType;
            }

            @Override
            public List<TypeRef> parameterTypes() {
                return pts;
            }

            @Override
            public Optional<String> signatureId() {
                return Optional.ofNullable(sid);
            }
        };
    }
}
