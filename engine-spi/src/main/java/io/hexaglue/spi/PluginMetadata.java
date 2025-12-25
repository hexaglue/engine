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
package io.hexaglue.spi;

import io.hexaglue.spi.stability.Stable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Describes a plugin for diagnostics, traceability and generated documentation.
 *
 * <p>All fields are optional except the plugin id.</p>
 *
 * <p>This type is intentionally dependency-free and stable.</p>
 *
 * @param id plugin id (non-blank)
 * @param displayName human-readable name
 * @param description short description of what the plugin provides
 * @param vendor organization or author name
 * @param websiteUrl optional website URL (string form)
 * @param pluginVersion plugin version string (free form, typically semver)
 * @param requiresVersionAtLeast minimal HexaGlue version required (optional)
 * @param capabilities a stable set of capability identifiers (e.g., "spring-jpa", "rest", "graphql")
 */
@Stable(since = "1.0.0")
public record PluginMetadata(
        String id,
        String displayName,
        String description,
        String vendor,
        String websiteUrl,
        String pluginVersion,
        HexaGlueVersion requiresVersionAtLeast,
        Set<String> capabilities) {

    /**
     * Creates a minimal metadata instance with only an id.
     *
     * @param id plugin id
     * @return minimal metadata
     */
    public static PluginMetadata minimal(String id) {
        Objects.requireNonNull(id, "id");
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("plugin id must not be blank");
        }
        return new PluginMetadata(trimmed, null, null, null, null, null, null, Collections.emptySet());
    }

    public PluginMetadata {
        Objects.requireNonNull(id, "id");
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("plugin id must not be blank");
        }
        id = trimmed;

        // Normalize capability set to be stable/deterministic and non-null.
        if (capabilities == null || capabilities.isEmpty()) {
            capabilities = Collections.emptySet();
        } else {
            Set<String> normalized = new LinkedHashSet<>();
            for (String c : capabilities) {
                if (c == null) continue;
                String ct = c.trim();
                if (!ct.isEmpty()) normalized.add(ct);
            }
            capabilities = Collections.unmodifiableSet(normalized);
        }
    }

    /**
     * Returns a safe display name for UI/logging purposes.
     *
     * @return displayName if present, otherwise the plugin id
     */
    public String safeDisplayName() {
        return (displayName == null || displayName.isBlank()) ? id : displayName;
    }
}
