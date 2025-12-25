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
package io.hexaglue.core.processor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Lightweight scanner over the root elements of a compilation round.
 *
 * <p>
 * HexaGlue typically needs to inspect types and packages across the compilation unit.
 * This helper provides a stable, JDK-only way to derive those from {@link RoundContext#rootElements()}.
 * </p>
 *
 * <p>
 * This scanner does not attempt deep traversal of type members. Deeper analysis belongs to
 * dedicated analyzers in internal packages.
 * </p>
 */
public final class ElementScanner {

    /**
     * Scans the current round and returns a summary snapshot.
     *
     * <p>
     * The returned snapshot is immutable and safe to cache across the pipeline.
     * </p>
     *
     * @param round the round context, not {@code null}
     * @return a scan result snapshot, never {@code null}
     */
    public ScanResult scan(RoundContext round) {
        Objects.requireNonNull(round, "round");

        Set<? extends Element> roots = round.rootElements();
        if (roots == null || roots.isEmpty()) {
            return ScanResult.empty();
        }

        List<TypeElement> types = new ArrayList<>();
        List<PackageElement> packages = new ArrayList<>();

        for (Element e : roots) {
            if (e == null) {
                continue;
            }
            ElementKind kind = e.getKind();
            if (kind == ElementKind.PACKAGE && e instanceof PackageElement pe) {
                packages.add(pe);
                continue;
            }
            if (e instanceof TypeElement te) {
                types.add(te);
            }
        }

        return new ScanResult(Collections.unmodifiableList(types), Collections.unmodifiableList(packages));
    }

    /**
     * Immutable scan result.
     */
    public static final class ScanResult {

        private static final ScanResult EMPTY = new ScanResult(List.of(), List.of());

        private final List<TypeElement> types;
        private final List<PackageElement> packages;

        private ScanResult(List<TypeElement> types, List<PackageElement> packages) {
            this.types = Objects.requireNonNull(types, "types");
            this.packages = Objects.requireNonNull(packages, "packages");
        }

        /**
         * Returns an empty scan result.
         *
         * @return empty result, never {@code null}
         */
        public static ScanResult empty() {
            return EMPTY;
        }

        /**
         * Returns all root types discovered in this round.
         *
         * @return root types, never {@code null}
         */
        public List<TypeElement> types() {
            return types;
        }

        /**
         * Returns all root packages discovered in this round.
         *
         * @return root packages, never {@code null}
         */
        public List<PackageElement> packages() {
            return packages;
        }

        /**
         * Returns whether the scan result contains no elements.
         *
         * @return {@code true} if empty
         */
        public boolean isEmpty() {
            return types.isEmpty() && packages.isEmpty();
        }
    }
}
