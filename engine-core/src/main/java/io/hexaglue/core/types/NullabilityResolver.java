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
package io.hexaglue.core.types;

import io.hexaglue.spi.types.Nullability;
import io.hexaglue.spi.types.TypeRef;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

/**
 * Resolves nullability information from various sources.
 *
 * <p>
 * This class provides utilities to determine {@link Nullability} based on:
 * <ul>
 *   <li>Nullability annotations (JSR-305, JetBrains, etc.)</li>
 *   <li>Type characteristics (primitives are always non-null)</li>
 *   <li>Default conventions</li>
 * </ul>
 * </p>
 *
 * <h2>Supported Annotations</h2>
 * <p>
 * The resolver recognizes common nullability annotation frameworks:
 * <ul>
 *   <li>JSR-305: {@code @Nonnull}, {@code @Nullable}</li>
 *   <li>JetBrains: {@code @NotNull}, {@code @Nullable}</li>
 *   <li>Eclipse: {@code @NonNull}, {@code @Nullable}</li>
 *   <li>Checker Framework: {@code @NonNull}, {@code @Nullable}</li>
 *   <li>Android: {@code @NonNull}, {@code @Nullable}</li>
 *   <li>FindBugs: {@code @Nonnull}, {@code @Nullable}</li>
 * </ul>
 * </p>
 *
 * <h2>Resolution Strategy</h2>
 * <ol>
 *   <li>If the type is primitive → {@link Nullability#NONNULL}</li>
 *   <li>If an explicit nullability annotation is present → use it</li>
 *   <li>Otherwise → {@link Nullability#UNSPECIFIED}</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class NullabilityResolver {

    /**
     * Simple names of non-null annotations recognized by the resolver.
     */
    private static final Set<String> NONNULL_ANNOTATION_SIMPLE_NAMES =
            Set.of("Nonnull", "NonNull", "NotNull", "NonNullable");

    /**
     * Simple names of nullable annotations recognized by the resolver.
     */
    private static final Set<String> NULLABLE_ANNOTATION_SIMPLE_NAMES = Set.of("Nullable", "CheckForNull");

    private NullabilityResolver() {
        // utility class
    }

    /**
     * Resolves nullability from a JSR-269 element.
     *
     * <p>
     * This method inspects annotations on the element and determines nullability.
     * </p>
     *
     * @param element element to inspect (not {@code null})
     * @return resolved nullability (never {@code null})
     */
    public static Nullability fromElement(Element element) {
        Objects.requireNonNull(element, "element");

        List<? extends AnnotationMirror> annotations = element.getAnnotationMirrors();
        return fromAnnotations(annotations);
    }

    /**
     * Resolves nullability from a type mirror.
     *
     * <p>
     * This method considers:
     * <ul>
     *   <li>Primitive types are always non-null</li>
     *   <li>Annotations on the type (if available)</li>
     * </ul>
     * </p>
     *
     * @param typeMirror type mirror to inspect (not {@code null})
     * @return resolved nullability (never {@code null})
     */
    public static Nullability fromTypeMirror(TypeMirror typeMirror) {
        Objects.requireNonNull(typeMirror, "typeMirror");

        // Primitives are always non-null
        if (typeMirror.getKind().isPrimitive()) {
            return Nullability.NONNULL;
        }

        // Check annotations on the type itself
        List<? extends AnnotationMirror> annotations = typeMirror.getAnnotationMirrors();
        return fromAnnotations(annotations);
    }

    /**
     * Resolves nullability from a type reference.
     *
     * <p>
     * This is a convenience method that considers the type's existing nullability
     * marker and type characteristics.
     * </p>
     *
     * @param typeRef type reference (not {@code null})
     * @return resolved nullability (never {@code null})
     */
    public static Nullability fromTypeRef(TypeRef typeRef) {
        Objects.requireNonNull(typeRef, "typeRef");

        // If nullability is already specified, return it
        if (typeRef.nullability() != Nullability.UNSPECIFIED) {
            return typeRef.nullability();
        }

        // Primitives are always non-null
        if (TypeComparators.isPrimitive(typeRef)) {
            return Nullability.NONNULL;
        }

        return Nullability.UNSPECIFIED;
    }

    /**
     * Resolves nullability from annotation mirrors.
     *
     * @param annotations annotations to inspect (not {@code null})
     * @return resolved nullability (never {@code null})
     */
    public static Nullability fromAnnotations(List<? extends AnnotationMirror> annotations) {
        Objects.requireNonNull(annotations, "annotations");

        for (AnnotationMirror annotation : annotations) {
            String simpleName = extractSimpleName(annotation);
            if (simpleName == null) {
                continue;
            }

            if (NONNULL_ANNOTATION_SIMPLE_NAMES.contains(simpleName)) {
                return Nullability.NONNULL;
            }
            if (NULLABLE_ANNOTATION_SIMPLE_NAMES.contains(simpleName)) {
                return Nullability.NULLABLE;
            }
        }

        return Nullability.UNSPECIFIED;
    }

    /**
     * Returns {@code true} if the nullability is explicitly marked as non-null.
     *
     * @param nullability nullability to check (not {@code null})
     * @return {@code true} if non-null
     */
    public static boolean isNonNull(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return nullability == Nullability.NONNULL;
    }

    /**
     * Returns {@code true} if the nullability is explicitly marked as nullable.
     *
     * @param nullability nullability to check (not {@code null})
     * @return {@code true} if nullable
     */
    public static boolean isNullable(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return nullability == Nullability.NULLABLE;
    }

    /**
     * Returns {@code true} if the nullability is unspecified.
     *
     * @param nullability nullability to check (not {@code null})
     * @return {@code true} if unspecified
     */
    public static boolean isUnspecified(Nullability nullability) {
        Objects.requireNonNull(nullability, "nullability");
        return nullability == Nullability.UNSPECIFIED;
    }

    /**
     * Returns a default nullability for reference types.
     *
     * <p>
     * This method returns {@link Nullability#UNSPECIFIED} by default. Implementations
     * may choose different defaults based on project conventions.
     * </p>
     *
     * @return default nullability (never {@code null})
     */
    public static Nullability defaultForReferenceTypes() {
        return Nullability.UNSPECIFIED;
    }

    /**
     * Returns the nullability for primitives (always non-null).
     *
     * @return {@link Nullability#NONNULL}
     */
    public static Nullability forPrimitive() {
        return Nullability.NONNULL;
    }

    /**
     * Merges two nullability markers, preferring the more specific one.
     *
     * <p>
     * Resolution order:
     * <ul>
     *   <li>If either is NONNULL → NONNULL</li>
     *   <li>If either is NULLABLE → NULLABLE</li>
     *   <li>Otherwise → UNSPECIFIED</li>
     * </ul>
     * </p>
     *
     * @param a first nullability (not {@code null})
     * @param b second nullability (not {@code null})
     * @return merged nullability (never {@code null})
     */
    public static Nullability merge(Nullability a, Nullability b) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(b, "b");

        if (a == Nullability.NONNULL || b == Nullability.NONNULL) {
            return Nullability.NONNULL;
        }
        if (a == Nullability.NULLABLE || b == Nullability.NULLABLE) {
            return Nullability.NULLABLE;
        }
        return Nullability.UNSPECIFIED;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static String extractSimpleName(AnnotationMirror annotation) {
        try {
            String qualifiedName = annotation.getAnnotationType().toString();
            int lastDot = qualifiedName.lastIndexOf('.');
            return (lastDot < 0) ? qualifiedName : qualifiedName.substring(lastDot + 1);
        } catch (Exception e) {
            // Best-effort: if extraction fails, return null
            return null;
        }
    }
}
