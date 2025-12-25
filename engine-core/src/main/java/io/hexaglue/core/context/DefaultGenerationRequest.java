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
package io.hexaglue.core.context;

import io.hexaglue.core.HexaGlueCoreVersion;
import io.hexaglue.spi.HexaGlueVersion;
import io.hexaglue.spi.context.GenerationRequest;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Default {@link GenerationRequest} implementation used by HexaGlue core.
 *
 * <p>
 * This is a stable, informational object passed to plugins through the SPI. It must not expose
 * annotation processing internals directly.
 * </p>
 *
 * <p>
 * Core may populate only best-effort values. Unknown values are returned as empty {@link Optional}s,
 * and {@link #activePluginIds()} is always non-null.
 * </p>
 */
public final class DefaultGenerationRequest implements GenerationRequest {

    private final Optional<HexaGlueVersion> coreVersion;
    private final Optional<String> projectId;
    private final Optional<Integer> targetJavaRelease;
    private final Set<String> activePluginIds;

    /**
     * Creates a generation request from explicit values.
     *
     * @param coreVersion core version (nullable)
     * @param projectId project id (nullable/blank allowed)
     * @param targetJavaRelease target Java release (nullable)
     * @param activePluginIds active plugin ids (nullable)
     */
    public DefaultGenerationRequest(
            HexaGlueVersion coreVersion, String projectId, Integer targetJavaRelease, Set<String> activePluginIds) {
        this.coreVersion = Optional.ofNullable(coreVersion);
        this.projectId = normalizeOptional(projectId);
        this.targetJavaRelease = Optional.ofNullable(targetJavaRelease).filter(v -> v > 0);
        this.activePluginIds = normalizeIds(activePluginIds);
    }

    /**
     * Creates a best-effort request from annotation processing state.
     *
     * <p>
     * This helper intentionally extracts only stable, non-internal signals:
     * </p>
     * <ul>
     *   <li>core version: from {@link HexaGlueCoreVersion#coreVersion()} when possible</li>
     *   <li>target Java release: best-effort from {@link ProcessingEnvironment#getSourceVersion()}</li>
     * </ul>
     *
     * <p>
     * Project id and active plugin ids are not inferred here; those should be supplied by the core
     * pipeline when available.
     * </p>
     *
     * @param processingEnv processing environment, not {@code null}
     * @return request instance, never {@code null}
     */
    public static DefaultGenerationRequest fromProcessingEnvironment(ProcessingEnvironment processingEnv) {
        Objects.requireNonNull(processingEnv, "processingEnv");

        HexaGlueVersion hv = versionOrNull(HexaGlueCoreVersion.coreVersion());
        Integer release = javaReleaseBestEffort(processingEnv);

        return new DefaultGenerationRequest(hv, null, release, Collections.emptySet());
    }

    @Override
    public Optional<HexaGlueVersion> coreVersion() {
        return coreVersion;
    }

    @Override
    public Optional<String> projectId() {
        return projectId;
    }

    @Override
    public Optional<Integer> targetJavaRelease() {
        return targetJavaRelease;
    }

    @Override
    public Set<String> activePluginIds() {
        return activePluginIds;
    }

    /**
     * Returns a copy of this request with the provided active plugin ids.
     *
     * <p>
     * This is a core convenience to set active plugin ids after discovery while keeping the type
     * immutable. Unknown/blank ids are ignored.
     * </p>
     *
     * @param ids active plugin ids (nullable)
     * @return a new request instance, never {@code null}
     */
    public DefaultGenerationRequest withActivePluginIds(Set<String> ids) {
        return new DefaultGenerationRequest(
                coreVersion.orElse(null), projectId.orElse(null), targetJavaRelease.orElse(null), ids);
    }

    /**
     * Returns a copy of this request with the provided project id.
     *
     * @param projectId project id (nullable/blank allowed)
     * @return a new request instance, never {@code null}
     */
    public DefaultGenerationRequest withProjectId(String projectId) {
        return new DefaultGenerationRequest(
                coreVersion.orElse(null), projectId, targetJavaRelease.orElse(null), activePluginIds);
    }

    private static Optional<String> normalizeOptional(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String s = value.trim();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    private static Set<String> normalizeIds(Set<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null) {
                continue;
            }
            String t = id.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return Collections.unmodifiableSet(out);
    }

    private static Integer javaReleaseBestEffort(ProcessingEnvironment env) {
        // Best-effort mapping from SourceVersion. We avoid internal compiler APIs.
        // Example: "RELEASE_17" -> 17.
        String name = String.valueOf(env.getSourceVersion());
        int idx = name.lastIndexOf('_');
        if (idx < 0 || idx == name.length() - 1) {
            return null;
        }
        String tail = name.substring(idx + 1);
        try {
            int v = Integer.parseInt(tail);
            return (v > 0) ? v : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static HexaGlueVersion versionOrNull(String coreVersionString) {
        if (coreVersionString == null) {
            return null;
        }
        String s = coreVersionString.trim();
        if (s.isEmpty() || "UNKNOWN".equalsIgnoreCase(s)) {
            return null;
        }
        // Minimal, stable mapping: treat the full string as a version label.
        // If HexaGlueVersion becomes structured later, this can be refined without breaking SPI.
        return HexaGlueVersion.of(Integer.valueOf(s), 0, 0);
    }
}
