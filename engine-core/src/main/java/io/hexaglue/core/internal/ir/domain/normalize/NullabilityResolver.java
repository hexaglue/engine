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
package io.hexaglue.core.internal.ir.domain.normalize;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.spi.types.Nullability;
import java.util.Objects;

/**
 * Resolves nullability from known annotations into a stable enum.
 *
 * <p>This is "normalization": it maps multiple libraries' annotations
 * ({@code @NonNull}, {@code @NotNull}, {@code @Nullable}, etc.) to a single
 * semantic representation ({@link Nullability}).</p>
 *
 * <h2>Supported Annotations</h2>
 * <p>The resolver recognizes annotations from:</p>
 * <ul>
 *   <li>JetBrains ({@code org.jetbrains.annotations.*})</li>
 *   <li>JSR-305 ({@code javax.annotation.*})</li>
 *   <li>Jakarta ({@code jakarta.annotation.*})</li>
 *   <li>Spring ({@code org.springframework.lang.*})</li>
 *   <li>Eclipse ({@code org.eclipse.jdt.annotation.*})</li>
 *   <li>Checker Framework ({@code org.checkerframework.checker.nullness.qual.*})</li>
 * </ul>
 *
 * <h2>Resolution Strategy</h2>
 * <p>The resolver applies the following precedence:</p>
 * <ol>
 *   <li>Nonnull annotations take precedence over nullable</li>
 *   <li>If both are absent, returns {@link Nullability#UNSPECIFIED}</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal nullability resolver; not exposed to plugins")
public final class NullabilityResolver {

    private final NullabilityPolicy policy;

    /**
     * Creates a nullability resolver with the given policy.
     *
     * @param policy policy (not {@code null})
     * @throws NullPointerException if policy is null
     */
    public NullabilityResolver(NullabilityPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    /**
     * Creates a nullability resolver with the default policy.
     *
     * @return resolver (never {@code null})
     */
    public static NullabilityResolver defaults() {
        return new NullabilityResolver(NullabilityPolicy.defaults());
    }

    /**
     * Resolves nullability for a given annotation set.
     *
     * @param annotations index (not {@code null})
     * @return resolved nullability (never {@code null})
     * @throws NullPointerException if annotations is null
     */
    public Nullability resolve(AnnotationIndex annotations) {
        Objects.requireNonNull(annotations, "annotations");

        if (policy.isNonnull(annotations)) {
            return Nullability.NONNULL;
        }
        if (policy.isNullable(annotations)) {
            return Nullability.NULLABLE;
        }
        return Nullability.UNSPECIFIED;
    }

    /**
     * Policy interface to keep resolver stable and configurable.
     */
    public interface NullabilityPolicy {
        /**
         * Checks if the annotations indicate a nonnull type.
         *
         * @param annotations annotation index (not {@code null})
         * @return {@code true} if nonnull
         */
        boolean isNonnull(AnnotationIndex annotations);

        /**
         * Checks if the annotations indicate a nullable type.
         *
         * @param annotations annotation index (not {@code null})
         * @return {@code true} if nullable
         */
        boolean isNullable(AnnotationIndex annotations);

        /**
         * Returns the default policy that recognizes common nullability annotations.
         *
         * @return default policy (never {@code null})
         */
        static NullabilityPolicy defaults() {
            return new DefaultNullabilityPolicy();
        }
    }

    /**
     * Default policy that recognizes common nullability annotations from major libraries.
     */
    private static final class DefaultNullabilityPolicy implements NullabilityPolicy {

        @Override
        public boolean isNonnull(AnnotationIndex annotations) {
            return annotations.hasAny(
                    // JetBrains
                    "org.jetbrains.annotations.NotNull",
                    // JSR-305
                    "javax.annotation.Nonnull",
                    // Jakarta
                    "jakarta.annotation.Nonnull",
                    // Spring
                    "org.springframework.lang.NonNull",
                    // Eclipse
                    "org.eclipse.jdt.annotation.NonNull",
                    // Checker Framework
                    "org.checkerframework.checker.nullness.qual.NonNull");
        }

        @Override
        public boolean isNullable(AnnotationIndex annotations) {
            return annotations.hasAny(
                    // JetBrains
                    "org.jetbrains.annotations.Nullable",
                    // JSR-305
                    "javax.annotation.Nullable",
                    // Jakarta
                    "jakarta.annotation.Nullable",
                    // Spring
                    "org.springframework.lang.Nullable",
                    // Eclipse
                    "org.eclipse.jdt.annotation.Nullable",
                    // Checker Framework
                    "org.checkerframework.checker.nullness.qual.Nullable");
        }
    }
}
