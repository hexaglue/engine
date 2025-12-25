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
package io.hexaglue.core.discovery;

import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.PluginMetadata;
import io.hexaglue.spi.PluginOrder;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable description of a discovered plugin.
 *
 * <p>
 * This is an internal representation used by the core to build an execution plan. It intentionally
 * exposes only coarse metadata and the {@link HexaGluePlugin} instance.
 * </p>
 */
public final class DiscoveredPlugin {

    private final HexaGluePlugin plugin;
    private final String implementationClassName;
    private final PluginMetadata metadata;
    private final int priority;

    /**
     * Creates a discovered plugin description.
     *
     * @param plugin the plugin instance, not {@code null}
     * @param metadata optional plugin metadata, may be {@code null}
     */
    public DiscoveredPlugin(HexaGluePlugin plugin, PluginMetadata metadata) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        Class<?> implClass = plugin.getClass();
        this.implementationClassName = implClass.getName();
        this.metadata = metadata;
        this.priority = resolvePriority(plugin);
    }

    /**
     * Returns the plugin instance.
     *
     * @return the plugin, never {@code null}
     */
    public HexaGluePlugin plugin() {
        return plugin;
    }

    /**
     * Returns the fully-qualified implementation class name.
     *
     * @return class name, never {@code null}
     */
    public String implementationClassName() {
        return implementationClassName;
    }

    /**
     * Returns the plugin metadata if available.
     *
     * @return metadata, never {@code null}
     */
    public Optional<PluginMetadata> metadata() {
        return Optional.ofNullable(metadata);
    }

    /**
     * Returns the numeric priority used for deterministic ordering.
     *
     * <p>Lower numbers run earlier.</p>
     *
     * @return priority
     */
    public int priority() {
        return priority;
    }

    /**
     * Returns a stable identifier for logging and diagnostics.
     *
     * <p>
     * If {@link PluginMetadata} is present and provides a non-empty id (implementation-dependent),
     * core may prefer it in the future. For now, the implementation class name is used.
     * </p>
     *
     * @return identifier, never {@code null}
     */
    public String id() {
        return implementationClassName;
    }

    @Override
    public String toString() {
        return "DiscoveredPlugin{id=" + id() + ", order=" + priority + "}";
    }

    private static int resolvePriority(HexaGluePlugin plugin) {
        try {
            PluginOrder order = plugin.order();
            if (order == null) {
                return PluginOrder.NORMAL.priority();
            }
            return order.priority();
        } catch (RuntimeException ex) {
            // Misbehaving plugins must not break discovery.
            return PluginOrder.NORMAL.priority();
        }
    }
}
