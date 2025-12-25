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

import io.hexaglue.spi.stability.Evolvable;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Read-only view of a domain type.
 *
 * <p>A domain type is a semantic type in the business core (entity, value object, identifier, etc.).
 * The domain is analyzed but never modified by HexaGlue.</p>
 *
 * <p>This view is designed to support:
 * <ul>
 *   <li>infra generation (entities, DTOs, mappers)</li>
 *   <li>structural validation</li>
 *   <li>documentation</li>
 * </ul>
 */
@Evolvable(since = "1.0.0")
public interface DomainTypeView {

    /**
     * Qualified name of the domain type.
     *
     * @return qualified name (never blank)
     */
    String qualifiedName();

    /**
     * Simple name of the domain type.
     *
     * @return simple name (never blank)
     */
    String simpleName();

    /**
     * Domain kind classification.
     *
     * @return domain kind (never {@code null})
     */
    DomainTypeKind kind();

    /**
     * Returns the canonical Java type reference for this domain type.
     *
     * @return type reference (never {@code null})
     */
    TypeRef type();

    /**
     * Domain properties/members.
     *
     * @return immutable list of properties (never {@code null})
     */
    List<DomainPropertyView> properties();

    /**
     * Identity definition, if this type is an entity (or otherwise has identity).
     *
     * @return id view if present
     */
    Optional<DomainIdView> id();

    /**
     * Whether this domain type is intended to be treated as immutable.
     *
     * <p>This is a semantic hint; plugins may still generate mutable infra types if required by a framework,
     * but should prefer immutability.</p>
     *
     * @return {@code true} if immutable
     */
    boolean isImmutable();

    /**
     * Indicates whether this domain type is an Aggregate Root.
     *
     * <p>An <strong>Aggregate Root</strong> is the single entry point to a cluster of domain objects
     * (aggregate). It has a stable identity across the system and is referenced from outside the
     * aggregate by ID only.</p>
     *
     * <p>This information is critical for infrastructure generation:
     * <ul>
     *   <li>Only Aggregate Roots should have repository ports</li>
     *   <li>Internal entities are manipulated through their Aggregate Root</li>
     *   <li>References between Aggregates should be by ID only</li>
     * </ul>
     *
     * <p><strong>Detection strategy (implementation responsibility):</strong>
     * The actual detection logic is implemented by the core and may use:
     * <ul>
     *   <li><strong>Explicit annotations:</strong> {@code @AggregateRoot} (jMolecules),
     *       {@code @Entity} (JPA with corresponding repository port)</li>
     *   <li><strong>Configuration:</strong> {@code hexaglue.yaml} declarations or patterns</li>
     *   <li><strong>Heuristics:</strong> Entity type with a corresponding DRIVEN port,
     *       package-based convention (types in "aggregate" packages), naming convention
     *       (types ending with "Aggregate" or "AggregateRoot")</li>
     * </ul>
     *
     * <p>The default implementation returns {@code true} only for types explicitly classified
     * as {@link DomainTypeKind#AGGREGATE_ROOT AGGREGATE_ROOT}. This classification is performed
     * during the ANALYZE phase and stored in the IR.</p>
     *
     * <p><strong>Plugin usage:</strong>
     * <pre>{@code
     * // Example: JPA Repository Plugin
     * for (DomainTypeView domainType : context.domain().types()) {
     *     if (domainType.isAggregateRoot()) {
     *         // Generate repository interface for this aggregate root
     *         generateRepository(domainType);
     *     }
     * }
     * }</pre>
     *
     * @return {@code true} if this type is an Aggregate Root
     * @since 0.3.0
     * @see <a href="https://martinfowler.com/bliki/DDD_Aggregate.html">Martin Fowler - DDD Aggregate</a>
     * @see <a href="https://github.com/xmolecules/jmolecules">jMolecules DDD Annotations</a>
     */
    default boolean isAggregateRoot() {
        // Aggregate root classification is performed during analysis and stored in kind()
        return kind() == DomainTypeKind.AGGREGATE_ROOT;
    }

    /**
     * Optional user-facing description.
     *
     * @return description if available
     */
    default Optional<String> description() {
        return Optional.empty();
    }

    /**
     * Annotations present on this domain type at the source level.
     *
     * <p>This includes annotations on the type declaration (class, interface, record, enum).</p>
     *
     * <p><strong>Common use cases:</strong>
     * <ul>
     *   <li><strong>Architectural:</strong> {@code @AggregateRoot}, {@code @Entity},
     *       {@code @ValueObject} (jMolecules, DDD annotations)</li>
     *   <li><strong>JPA:</strong> {@code @Entity}, {@code @Table}, {@code @Inheritance},
     *       {@code @DiscriminatorColumn}</li>
     *   <li><strong>Documentation:</strong> {@code @Deprecated}</li>
     * </ul>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * // Check if type has @Entity annotation
     * boolean isJpaEntity = domainType.annotations().stream()
     *     .anyMatch(a -> a.is("jakarta.persistence.Entity"));
     *
     * // Extract @Table name
     * Optional<String> tableName = domainType.annotations().stream()
     *     .filter(a -> a.is("jakarta.persistence.Table"))
     *     .flatMap(a -> a.attribute("name", String.class))
     *     .findFirst();
     * }</pre>
     *
     * @return immutable list of annotations (never {@code null}, may be empty)
     * @since 0.3.0
     * @see AnnotationView
     */
    default List<AnnotationView> annotations() {
        return List.of();
    }

    /**
     * Direct supertype (superclass), if any.
     *
     * <p>For classes that don't explicitly extend another class, this returns empty
     * (java.lang.Object is not included).</p>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * // Generate @Inheritance for JPA hierarchy root
     * if (domainType.superType().isEmpty() && hasSubtypes(domainType)) {
     *     // This is the root of a hierarchy
     *     code.addAnnotation("@Inheritance(strategy = InheritanceType.SINGLE_TABLE)");
     *     code.addAnnotation("@DiscriminatorColumn(name = \"type\")");
     * }
     * }</pre>
     *
     * @return supertype or empty
     * @since 0.5.0
     */
    default Optional<TypeRef> superType() {
        return Optional.empty();
    }

    /**
     * Interfaces implemented/extended by this type.
     *
     * <p>This includes all directly implemented or extended interfaces, but does not
     * transitively include interfaces implemented by superclasses or parent interfaces.</p>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * // Check if type implements a specific interface
     * boolean implementsSerializable = domainType.interfaces().stream()
     *     .anyMatch(i -> i.qualifiedName().equals("java.io.Serializable"));
     * }</pre>
     *
     * @return list of interfaces (never {@code null}, may be empty)
     * @since 0.5.0
     */
    default List<TypeRef> interfaces() {
        return List.of();
    }

    /**
     * If this type is sealed, returns the permitted subtypes.
     *
     * <p>Sealed types (Java 17+) restrict which classes can extend or implement them.
     * This method returns the list of permitted subtypes declared in the {@code permits} clause.</p>
     *
     * <p><strong>Usage example:</strong>
     * <pre>{@code
     * // Generate entities for all permitted subtypes of a sealed interface
     * domainType.permittedSubtypes().ifPresent(subtypes -> {
     *     for (TypeRef subtype : subtypes) {
     *         generateEntityForSubtype(subtype);
     *     }
     * });
     * }</pre>
     *
     * @return permitted subtypes or empty if not sealed
     * @since 0.5.0
     */
    default Optional<List<TypeRef>> permittedSubtypes() {
        return Optional.empty();
    }

    /**
     * If this type is an enumeration, returns the enum constants.
     *
     * <p>Enum constants are returned in the order they are declared in the source code.
     * This information is useful for validation, documentation, and generating infrastructure code.</p>
     *
     * <p><strong>Usage examples:</strong>
     * <pre>{@code
     * // Validation: Check if a value is a valid enum constant
     * Optional<DomainTypeView> enumType = model.domain().findType(type.qualifiedName());
     * if (enumType.isPresent()) {
     *     Optional<List<String>> constants = enumType.get().enumConstants();
     *     if (constants.isPresent() && !constants.get().contains(value)) {
     *         diagnostics.error(INVALID_ENUM_VALUE,
     *             "Invalid value '" + value + "'. Expected one of: " + constants.get());
     *     }
     * }
     *
     * // JPA Plugin: Generate @Enumerated annotation with documentation
     * if (domainType.kind() == DomainTypeKind.ENUMERATION) {
     *     code.addAnnotation("@Enumerated(EnumType.STRING)");
     *     domainType.enumConstants().ifPresent(constants -> {
     *         code.addComment("Valid values: " + String.join(", ", constants));
     *     });
     * }
     * }</pre>
     *
     * @return list of constant names or empty if not an enum
     * @since 0.5.0
     */
    default Optional<List<String>> enumConstants() {
        if (kind() != DomainTypeKind.ENUMERATION) {
            return Optional.empty();
        }
        return Optional.of(List.of());
    }

    /**
     * Creates a simple immutable {@link DomainTypeView} instance.
     *
     * <p>This factory is intended for tests and tooling.</p>
     *
     * @param qualifiedName qualified name
     * @param simpleName simple name
     * @param kind kind
     * @param type java type reference
     * @param properties properties list
     * @param id id view (nullable)
     * @param immutable immutable flag
     * @param description description (nullable)
     * @return domain type view
     */
    static DomainTypeView of(
            String qualifiedName,
            String simpleName,
            DomainTypeKind kind,
            TypeRef type,
            List<DomainPropertyView> properties,
            DomainIdView id,
            boolean immutable,
            String description) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(properties, "properties");

        final String qn = qualifiedName.trim();
        final String sn = simpleName.trim();
        if (qn.isEmpty()) throw new IllegalArgumentException("qualifiedName must not be blank");
        if (sn.isEmpty()) throw new IllegalArgumentException("simpleName must not be blank");

        final List<DomainPropertyView> props = List.copyOf(properties);
        for (DomainPropertyView p : props) {
            Objects.requireNonNull(p, "properties contains null");
        }

        final DomainIdView idv = id;
        final String desc = (description == null || description.isBlank()) ? null : description.trim();

        return new DomainTypeView() {
            @Override
            public String qualifiedName() {
                return qn;
            }

            @Override
            public String simpleName() {
                return sn;
            }

            @Override
            public DomainTypeKind kind() {
                return kind;
            }

            @Override
            public TypeRef type() {
                return type;
            }

            @Override
            public List<DomainPropertyView> properties() {
                return props;
            }

            @Override
            public Optional<DomainIdView> id() {
                return Optional.ofNullable(idv);
            }

            @Override
            public boolean isImmutable() {
                return immutable;
            }

            @Override
            public Optional<String> description() {
                return Optional.ofNullable(desc);
            }
        };
    }
}
