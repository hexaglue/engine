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

import io.hexaglue.core.frontend.AnnotationModel;
import io.hexaglue.core.internal.InternalMarker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable index over a set of annotations for fast lookup by qualified name.
 *
 * <p>The index provides convenience queries and is intended to be created once
 * per analyzed element (type/property) and reused across semantic analyzers.</p>
 *
 * <p>This normalization layer sits between raw annotation extraction and semantic
 * interpretation, providing efficient access patterns for detection logic.</p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is immutable and thread-safe.</p>
 *
 * @since 0.3.0
 */
@InternalMarker(reason = "Internal annotation index; not exposed to plugins")
public final class AnnotationIndex {

    private final List<AnnotationModel> all;

    private AnnotationIndex(List<AnnotationModel> all) {
        this.all = Collections.unmodifiableList(new ArrayList<>(all));
    }

    /**
     * Creates an index for the given annotations.
     *
     * @param annotations annotations (not {@code null})
     * @return immutable index (never {@code null})
     * @throws NullPointerException if annotations is null
     */
    public static AnnotationIndex of(List<AnnotationModel> annotations) {
        Objects.requireNonNull(annotations, "annotations");
        return new AnnotationIndex(annotations);
    }

    /**
     * Returns all annotations.
     *
     * @return all annotations (never {@code null}, immutable)
     */
    public List<AnnotationModel> all() {
        return all;
    }

    /**
     * Returns the first annotation matching the given qualified name.
     *
     * @param qualifiedName fully-qualified annotation name (not {@code null})
     * @return matching annotation if present
     * @throws NullPointerException if qualifiedName is null
     */
    public Optional<AnnotationModel> first(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        String qn = qualifiedName.trim();
        if (qn.isEmpty()) {
            return Optional.empty();
        }

        return all.stream().filter(a -> qn.equals(a.qualifiedName())).findFirst();
    }

    /**
     * Returns whether an annotation with the given qualified name is present.
     *
     * @param qualifiedName fully-qualified annotation name (not {@code null})
     * @return {@code true} if present
     * @throws NullPointerException if qualifiedName is null
     */
    public boolean has(String qualifiedName) {
        return first(qualifiedName).isPresent();
    }

    /**
     * Returns whether any of the given qualified names are present.
     *
     * @param qualifiedNames fully-qualified annotation names (not {@code null})
     * @return {@code true} if at least one is present
     * @throws NullPointerException if qualifiedNames is null
     */
    public boolean hasAny(String... qualifiedNames) {
        Objects.requireNonNull(qualifiedNames, "qualifiedNames");
        for (String name : qualifiedNames) {
            if (has(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all annotations with the given qualified name.
     *
     * <p>This is useful for repeatable annotations.</p>
     *
     * @param qualifiedName fully-qualified annotation name (not {@code null})
     * @return immutable list (never {@code null}, may be empty)
     * @throws NullPointerException if qualifiedName is null
     */
    public List<AnnotationModel> allOf(String qualifiedName) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        String qn = qualifiedName.trim();
        if (qn.isEmpty()) {
            return List.of();
        }

        return all.stream().filter(a -> qn.equals(a.qualifiedName())).toList();
    }

    @Override
    public String toString() {
        return "AnnotationIndex{count=" + all.size() + "}";
    }
}
