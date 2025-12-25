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
package io.hexaglue.spi.types;

import io.hexaglue.spi.stability.Stable;
import java.util.Objects;
import java.util.Optional;

/**
 * A stable, tool-agnostic representation of a Java type.
 *
 * <p>This SPI intentionally does not expose compiler internals (e.g., JSR-269 types).
 * HexaGlue core is responsible for mapping from compiler-specific models to this
 * stable representation.</p>
 *
 * <p>Type references are primarily used for:
 * <ul>
 *   <li>consistency checks and diagnostics</li>
 *   <li>code generation decisions</li>
 *   <li>rendering into generated source code</li>
 * </ul>
 */
@Stable(since = "1.0.0")
public interface TypeRef {

    /**
     * Structural kind of this type.
     *
     * @return type kind (never {@code null})
     */
    TypeKind kind();

    /**
     * Nullability marker for this type.
     *
     * @return nullability (never {@code null})
     */
    Nullability nullability();

    /**
     * The primary name associated with this type.
     *
     * <p>For primitives, this is the primitive keyword (e.g., {@code "int"}).
     * For class types, this is typically the qualified name if known.</p>
     *
     * @return type name (never {@code null})
     */
    TypeName name();

    /**
     * Returns a copy of this type with a different nullability.
     *
     * <p>Implementations should preserve structure and change only the nullability marker.</p>
     *
     * @param nullability new nullability (never {@code null})
     * @return updated type reference
     */
    TypeRef withNullability(Nullability nullability);

    /**
     * Renders this type as a Java source type expression.
     *
     * <p>This is a best-effort renderer. Implementations should avoid leaking internal names.
     * For class types, prefer qualified names unless a codegen system later shortens imports.</p>
     *
     * @return Java source representation (never blank)
     */
    String render();

    /**
     * For collection types, returns collection metadata.
     *
     * <p>This method provides detailed information about collection types (List, Set, Map, etc.)
     * including their kind, element type, and key type (for Maps).</p>
     *
     * <p>This is particularly useful for persistence layer code generation where different
     * collection types require different mapping strategies (e.g., JPA's {@code @OrderColumn}
     * for List vs simple {@code @OneToMany} for Set).</p>
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code List<String>} → present with kind=LIST, elementType=String</li>
     *   <li>{@code Set<Customer>} → present with kind=SET, elementType=Customer</li>
     *   <li>{@code Map<Long, Order>} → present with kind=MAP, keyType=Long, elementType=Order</li>
     *   <li>{@code String} → empty (not a collection)</li>
     * </ul>
     *
     * @return collection metadata or empty if this is not a collection type
     * @since 0.4.0
     */
    default Optional<CollectionMetadata> collectionMetadata() {
        return Optional.empty();
    }

    /**
     * Utility: checks that a reference is non-null and returns it.
     *
     * @param ref reference
     * @param label label for error messages
     * @param <T> type
     * @return ref
     */
    static <T> T require(T ref, String label) {
        return Objects.requireNonNull(ref, label);
    }
}
