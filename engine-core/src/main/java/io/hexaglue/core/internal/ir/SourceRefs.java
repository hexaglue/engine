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
package io.hexaglue.core.internal.ir;

import io.hexaglue.core.internal.InternalMarker;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

/**
 * Internal helpers for producing stable {@link SourceRef} instances.
 *
 * <p>
 * This utility exists to centralize all "best-effort" conversions from analysis-time
 * compiler objects (JSR-269 {@link Element}) into stable references suitable for storage
 * in the IR. Storing raw {@code Element} instances in the IR is intentionally avoided.
 * </p>
 *
 * <p>
 * The conversion strategy is deliberately conservative:
 * <ul>
 *   <li>If an existing {@link SourceRef} is passed, it is returned as-is.</li>
 *   <li>If a JSR-269 {@link Element} is passed, a {@link SourceRef} is built using a caller-provided stable id.</li>
 *   <li>Otherwise, {@code null} is returned.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Line/column/file mapping is intentionally not performed here. If you want richer locations,
 * resolve them at diagnostic rendering time using compiler services.
 * </p>
 */
@InternalMarker(reason = "Internal IR helper; centralizes SourceRef construction and conversion.")
public final class SourceRefs {

    private SourceRefs() {
        // utility class
    }

    /**
     * Coerces an arbitrary analysis object into a {@link SourceRef}.
     *
     * @param maybeElementOrSourceRef a {@link SourceRef} or a JSR-269 {@link Element} (nullable)
     * @param stableId                stable id to store in the ref (not {@code null}, not blank)
     * @param kind                    source kind (not {@code null})
     * @return a {@link SourceRef} or {@code null} if input is {@code null} or unsupported
     */
    public static SourceRef coerce(Object maybeElementOrSourceRef, String stableId, SourceRef.Kind kind) {
        if (maybeElementOrSourceRef == null) {
            return null;
        }
        Objects.requireNonNull(stableId, "stableId");
        Objects.requireNonNull(kind, "kind");

        String id = stableId.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("stableId must not be blank");
        }

        if (maybeElementOrSourceRef instanceof SourceRef ref) {
            return ref;
        }
        if (maybeElementOrSourceRef instanceof Element el) {
            return ofJsr269Element(el, id, kind);
        }
        return null;
    }

    /**
     * Creates a {@link SourceRef} from a JSR-269 element using a caller-provided stable id.
     *
     * @param element  element (not {@code null})
     * @param stableId stable id (not {@code null}, not blank)
     * @param kind     ref kind (not {@code null})
     * @return source ref (never {@code null})
     */
    public static SourceRef ofJsr269Element(Element element, String stableId, SourceRef.Kind kind) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(stableId, "stableId");
        Objects.requireNonNull(kind, "kind");

        SourceRef.Builder b = SourceRef.builder(kind, stableId).origin("jsr269");

        // Very lightweight hints that are stable enough to keep around:
        ElementKind ek = element.getKind();
        if (ek != null) {
            b = b.hint(ek.name());
        }

        return b.build();
    }
}
