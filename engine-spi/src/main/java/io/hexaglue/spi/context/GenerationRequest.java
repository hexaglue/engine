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
package io.hexaglue.spi.context;

import io.hexaglue.spi.HexaGlueVersion;
import io.hexaglue.spi.stability.Evolvable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Describes the current generation request.
 *
 * <p>This is a stable, informational object that provides high-level context:
 * <ul>
 *   <li>project identity (optional)</li>
 *   <li>target platform or language level (optional)</li>
 *   <li>active plugin ids (best-effort)</li>
 *   <li>HexaGlue core version</li>
 * </ul>
 *
 * <p>It must not expose annotation processing internals or compiler implementation details.</p>
 */
@Evolvable(since = "1.0.0")
public interface GenerationRequest {

    /**
     * Returns the HexaGlue core version for this execution, if known.
     *
     * <p>Core should provide this for traceability and compatibility checks.</p>
     *
     * @return core version, if known
     */
    Optional<HexaGlueVersion> coreVersion();

    /**
     * Returns a stable project identifier (best-effort).
     *
     * <p>Examples: a Maven coordinates string, a Gradle project path, or a workspace module id.</p>
     *
     * @return project id, if known
     */
    Optional<String> projectId();

    /**
     * Returns the target Java release (best-effort).
     *
     * <p>Example: {@code 17} for Java 17.</p>
     *
     * @return Java release number, if known
     */
    Optional<Integer> targetJavaRelease();

    /**
     * Returns the ids of plugins the compiler considers active for this run (best-effort).
     *
     * <p>Core may populate this after discovery and filtering. Plugins should not rely on
     * this being complete or authoritative.</p>
     *
     * @return immutable set of active plugin ids (never {@code null})
     */
    Set<String> activePluginIds();

    /**
     * Creates an immutable {@link GenerationRequest} instance.
     *
     * <p>This factory is provided for tests and lightweight tooling.</p>
     *
     * @param coreVersion core version (nullable)
     * @param projectId project id (nullable)
     * @param targetJavaRelease target Java release (nullable)
     * @param activePluginIds active plugin ids (nullable)
     * @return immutable request
     */
    static GenerationRequest of(
            HexaGlueVersion coreVersion, String projectId, Integer targetJavaRelease, Set<String> activePluginIds) {
        final Optional<HexaGlueVersion> cv = Optional.ofNullable(coreVersion);
        final Optional<String> pid =
                Optional.ofNullable(projectId).map(String::trim).filter(s -> !s.isEmpty());
        final Optional<Integer> tjr = Optional.ofNullable(targetJavaRelease).filter(v -> v > 0);

        final Set<String> ids;
        if (activePluginIds == null || activePluginIds.isEmpty()) {
            ids = Collections.emptySet();
        } else {
            Set<String> normalized = new LinkedHashSet<>();
            for (String id : activePluginIds) {
                if (id == null) continue;
                String t = id.trim();
                if (!t.isEmpty()) normalized.add(t);
            }
            ids = Collections.unmodifiableSet(normalized);
        }

        return new GenerationRequest() {
            @Override
            public Optional<HexaGlueVersion> coreVersion() {
                return cv;
            }

            @Override
            public Optional<String> projectId() {
                return pid;
            }

            @Override
            public Optional<Integer> targetJavaRelease() {
                return tjr;
            }

            @Override
            public Set<String> activePluginIds() {
                return ids;
            }
        };
    }
}
