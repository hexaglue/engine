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
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of a domain identity definition.
 *
 * <p>This abstraction allows plugins to work with entity identity without assuming a specific
 * Java representation (e.g., {@code @Id}, {@code @EmbeddedId}, record wrapper, etc.).</p>
 *
 * <p>Identity may be represented as:
 * <ul>
 *   <li>a dedicated identifier type (recommended)</li>
 *   <li>a scalar (less desirable but supported)</li>
 *   <li>a composite structure (future-compatible)</li>
 * </ul>
 */
public interface DomainIdView {

    /**
     * Declaring entity qualified name, if known.
     *
     * @return declaring entity qualified name
     */
    Optional<String> declaringEntity();

    /**
     * Logical name of the id (often {@code "id"}).
     *
     * @return id name (never blank)
     */
    String name();

    /**
     * Type of the id.
     *
     * @return id type (never {@code null})
     */
    TypeRef type();

    /**
     * Whether this id is composite (made of multiple parts).
     *
     * <p>Composite ids are not mandatory to support initially, but the SPI allows representing them.</p>
     *
     * @return {@code true} if composite
     */
    boolean isComposite();

    /**
     * Creates a simple immutable {@link DomainIdView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param declaringEntity declaring entity qualified name (nullable)
     * @param name id name
     * @param type id type
     * @param composite whether composite
     * @return id view
     */
    static DomainIdView of(String declaringEntity, String name, TypeRef type, boolean composite) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        final String n = name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("name must not be blank");
        final String de = (declaringEntity == null || declaringEntity.isBlank()) ? null : declaringEntity.trim();

        return new DomainIdView() {
            @Override
            public Optional<String> declaringEntity() {
                return Optional.ofNullable(de);
            }

            @Override
            public String name() {
                return n;
            }

            @Override
            public TypeRef type() {
                return type;
            }

            @Override
            public boolean isComposite() {
                return composite;
            }
        };
    }
}
