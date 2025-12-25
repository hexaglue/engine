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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Discovers {@link HexaGluePlugin} implementations using {@link ServiceLoader}.
 *
 * <p>
 * HexaGlue supports "drop-in" plugins: any jar on the compilation classpath may contribute one or more
 * {@link HexaGluePlugin} providers through {@code META-INF/services} (or JPMS {@code provides}).
 * </p>
 *
 * <h2>Duplicate handling</h2>
 * <p>
 * If multiple instances of the same implementation class are discovered (which can happen with
 * layered classloaders or shading), the first encountered instance is retained.
 * </p>
 */
public final class ServiceLoaderPluginDiscovery {

    private final PluginSorter sorter;

    /**
     * Creates a discovery service with the default sorter.
     */
    public ServiceLoaderPluginDiscovery() {
        this(new PluginSorter());
    }

    /**
     * Creates a discovery service with a provided sorter.
     *
     * @param sorter sorter to apply to discovered plugins, not {@code null}
     */
    public ServiceLoaderPluginDiscovery(PluginSorter sorter) {
        this.sorter = Objects.requireNonNull(sorter, "sorter");
    }

    /**
     * Discovers all plugins visible from the given classpath and returns them in deterministic order.
     *
     * @param classpath plugin classpath descriptor, not {@code null}
     * @return discovered plugins, never {@code null}
     * @throws PluginDiscoveryException if plugin discovery fails due to service loading errors
     */
    public List<DiscoveredPlugin> discover(PluginClasspath classpath) {
        Objects.requireNonNull(classpath, "classpath");

        LinkedHashMap<String, DiscoveredPlugin> unique = new LinkedHashMap<>();
        ServiceLoader<HexaGluePlugin> loader = ServiceLoader.load(HexaGluePlugin.class, classpath.classLoader());

        try {
            for (HexaGluePlugin plugin : loader) {
                if (plugin == null) {
                    continue;
                }
                PluginMetadata metadata = safeMetadata(plugin);
                DiscoveredPlugin dp = new DiscoveredPlugin(plugin, metadata);
                unique.putIfAbsent(dp.implementationClassName(), dp);
            }
        } catch (ServiceConfigurationError e) {
            throw new PluginDiscoveryException("Failed to load HexaGlue plugins via ServiceLoader", e);
        }

        List<DiscoveredPlugin> discovered = new ArrayList<>(unique.values());
        return sorter.sort(discovered);
    }

    private static PluginMetadata safeMetadata(HexaGluePlugin plugin) {
        // The SPI may evolve; keep discovery resilient if metadata is optional/nullable.
        try {
            return plugin.metadata();
        } catch (RuntimeException ex) {
            // Treat misbehaving plugins as "no metadata" at discovery stage; isolation is handled later.
            return null;
        }
    }

    /**
     * Thrown when plugin discovery cannot complete.
     */
    public static final class PluginDiscoveryException extends RuntimeException {

        /**
         * Creates an exception with a message.
         *
         * @param message message, not {@code null}
         */
        public PluginDiscoveryException(String message) {
            super(Objects.requireNonNull(message, "message"));
        }

        /**
         * Creates an exception with a message and cause.
         *
         * @param message message, not {@code null}
         * @param cause cause, may be {@code null}
         */
        public PluginDiscoveryException(String message, Throwable cause) {
            super(Objects.requireNonNull(message, "message"), cause);
        }
    }
}
