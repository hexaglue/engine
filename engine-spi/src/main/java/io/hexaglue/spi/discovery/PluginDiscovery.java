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
package io.hexaglue.spi.discovery;

import io.hexaglue.spi.HexaGluePlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Utility entry-point for discovering {@link HexaGluePlugin} implementations.
 *
 * <p>HexaGlue uses Java's {@link ServiceLoader} discovery model. Plugins must register
 * themselves under:
 *
 * <pre>
 * META-INF/services/io.hexaglue.spi.HexaGluePlugin
 * </pre>
 *
 * <p>This SPI utility is provided so that:
 * <ul>
 *   <li>Core can reuse a stable, shared discovery routine.</li>
 *   <li>Plugin authors can test discovery without depending on core internals.</li>
 * </ul>
 *
 * <p>Note: This API does not define any ordering or filtering rules. Ordering, selection
 * and orchestration are concerns of the compiler/runtime using the SPI.</p>
 */
public final class PluginDiscovery {

    private PluginDiscovery() {
        // utility class
    }

    /**
     * Discovers all {@link HexaGluePlugin} implementations visible from the given classloader.
     *
     * <p>This method is strict: failures during service loading result in an exception to
     * surface configuration errors early.</p>
     *
     * @param classLoader classloader used for service discovery (never {@code null})
     * @return an immutable list of discovered plugin instances (never {@code null})
     * @throws NullPointerException if {@code classLoader} is {@code null}
     * @throws ServiceConfigurationError if a provider cannot be instantiated or configured
     */
    public static List<HexaGluePlugin> discoverStrict(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        ServiceLoader<HexaGluePlugin> loader = ServiceLoader.load(HexaGluePlugin.class, classLoader);

        List<HexaGluePlugin> result = new ArrayList<>();
        for (HexaGluePlugin plugin : loader) {
            // ServiceLoader may return null providers in some broken configurations; guard defensively.
            if (plugin != null) {
                result.add(plugin);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Discovers all {@link HexaGluePlugin} implementations visible from the given classloader.
     *
     * <p>This method is lenient: misconfigured providers are skipped and captured in the returned result.</p>
     *
     * <p>Typical usage:
     * <ul>
     *   <li>In compilers that want to continue and report diagnostics instead of failing hard.</li>
     *   <li>In test tooling that wants visibility into both successes and failures.</li>
     * </ul>
     *
     * @param classLoader classloader used for service discovery (never {@code null})
     * @return a discovery result containing both plugins and loading errors (never {@code null})
     * @throws NullPointerException if {@code classLoader} is {@code null}
     */
    public static DiscoveryResult discoverLenient(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "classLoader");
        ServiceLoader<HexaGluePlugin> loader = ServiceLoader.load(HexaGluePlugin.class, classLoader);

        List<HexaGluePlugin> plugins = new ArrayList<>();
        List<ServiceConfigurationError> errors = new ArrayList<>();

        try {
            for (HexaGluePlugin plugin : loader) {
                if (plugin != null) {
                    plugins.add(plugin);
                }
            }
        } catch (ServiceConfigurationError e) {
            // Some ServiceLoader failures happen while iterating; keep going if possible.
            errors.add(e);

            // Attempt a best-effort reload; depending on JVM implementation, iteration may be compromised.
            // This is intentionally conservative and does not try to introspect provider configurations.
            try {
                loader.reload();
                for (HexaGluePlugin plugin : loader) {
                    if (plugin != null) {
                        plugins.add(plugin);
                    }
                }
            } catch (ServiceConfigurationError e2) {
                errors.add(e2);
            }
        }

        return new DiscoveryResult(Collections.unmodifiableList(plugins), Collections.unmodifiableList(errors));
    }

    /**
     * Result of plugin discovery.
     *
     * <p>This type is immutable and JDK-only.</p>
     *
     * @param plugins successfully loaded plugins
     * @param errors service configuration errors encountered during discovery
     */
    public record DiscoveryResult(List<HexaGluePlugin> plugins, List<ServiceConfigurationError> errors) {

        public DiscoveryResult {
            plugins = (plugins == null) ? List.of() : List.copyOf(plugins);
            errors = (errors == null) ? List.of() : List.copyOf(errors);
        }

        /** @return {@code true} if at least one error occurred during discovery */
        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
