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
package io.hexaglue.core.types.model;

import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeKind;
import io.hexaglue.spi.types.TypeName;
import io.hexaglue.spi.types.TypeRef;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.type.TypeMirror;

/**
 * Abstract base class for internal type reference implementations.
 *
 * <p>
 * This class provides common functionality for all type references used internally
 * within HexaGlue core. It bridges compiler-specific types ({@link TypeMirror})
 * with the stable SPI abstraction ({@link TypeRef}).
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * While the SPI provides stable record-based type references, the core needs to maintain
 * additional metadata such as:
 * <ul>
 *   <li>Original {@link TypeMirror} for advanced type operations</li>
 *   <li>Resolution context and caching</li>
 *   <li>Internal flags for optimization</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Implementations should be immutable. The {@link TypeMirror} reference is optional
 * and must be used only within the annotation processing round that created it.
 * </p>
 *
 * <h2>Equality</h2>
 * <p>
 * Equality is based on structural type equality, not the underlying {@link TypeMirror}.
 * Two type references are equal if they represent the same type structure.
 * </p>
 */
public abstract class BaseTypeRef implements TypeRef {

    private final TypeName name;
    private final Nullability nullability;
    private final TypeMirror typeMirror;

    /**
     * Constructs a base type reference.
     *
     * @param name        type name (not {@code null})
     * @param nullability nullability marker (not {@code null})
     * @param typeMirror  optional underlying type mirror (may be {@code null})
     */
    protected BaseTypeRef(TypeName name, Nullability nullability, TypeMirror typeMirror) {
        this.name = Objects.requireNonNull(name, "name");
        this.nullability = Objects.requireNonNull(nullability, "nullability");
        this.typeMirror = typeMirror;
    }

    @Override
    public final TypeName name() {
        return name;
    }

    @Override
    public final Nullability nullability() {
        return nullability;
    }

    /**
     * Returns the underlying {@link TypeMirror} if available.
     *
     * <p>
     * The type mirror provides access to compiler-specific type information
     * for advanced operations such as:
     * <ul>
     *   <li>Assignability checks via {@link javax.lang.model.util.Types}</li>
     *   <li>Type element resolution via {@link javax.lang.model.util.Elements}</li>
     *   <li>Generic type introspection</li>
     * </ul>
     * </p>
     *
     * <p>
     * <strong>Important:</strong> The {@link TypeMirror} is only valid within the
     * annotation processing round that created it. Do not cache references across rounds.
     * </p>
     *
     * @return type mirror if available
     */
    public final Optional<TypeMirror> typeMirror() {
        return Optional.ofNullable(typeMirror);
    }

    /**
     * Returns a copy of this type reference with a different nullability.
     *
     * <p>
     * Subclasses must implement this to preserve their specific structure while
     * updating only the nullability marker.
     * </p>
     *
     * @param nullability new nullability (not {@code null})
     * @return updated type reference (never {@code null})
     */
    @Override
    public abstract BaseTypeRef withNullability(Nullability nullability);

    /**
     * Returns the structural type kind.
     *
     * @return type kind (never {@code null})
     */
    @Override
    public abstract TypeKind kind();

    /**
     * Renders this type as Java source code.
     *
     * <p>
     * The default implementation delegates to {@link #name()}.
     * Subclasses should override this for complex types (arrays, parameterized types, etc.).
     * </p>
     *
     * @return Java source representation (never blank)
     */
    @Override
    public String render() {
        return name.value();
    }

    /**
     * Converts this internal type reference to its stable SPI representation.
     *
     * <p>
     * This method strips internal metadata (like {@link TypeMirror}) and returns
     * a pure SPI-compatible type reference suitable for plugin consumption.
     * </p>
     *
     * @return SPI type reference (never {@code null})
     */
    public abstract TypeRef toSpiType();

    @Override
    public String toString() {
        return render();
    }
}
