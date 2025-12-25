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

import java.util.Objects;

/**
 * Represents the classpath context used to discover plugins.
 *
 * <p>
 * HexaGlue relies on {@link java.util.ServiceLoader}. The primary input for discovery is therefore
 * a {@link ClassLoader}.
 * </p>
 *
 * <p>
 * This abstraction exists mainly for testability and future extensibility (e.g., isolating plugin
 * classloaders), while staying strictly JDK-only.
 * </p>
 */
public final class PluginClasspath {

    private final ClassLoader classLoader;

    private PluginClasspath(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    /**
     * Creates a plugin classpath backed by the given classloader.
     *
     * @param classLoader the classloader to use for {@link java.util.ServiceLoader}, not {@code null}
     * @return a classpath descriptor, never {@code null}
     */
    public static PluginClasspath of(ClassLoader classLoader) {
        return new PluginClasspath(classLoader);
    }

    /**
     * Creates a plugin classpath backed by the current thread context classloader when available,
     * falling back to the given fallback loader.
     *
     * @param fallback fallback classloader to use when the thread context classloader is {@code null}
     * @return a classpath descriptor, never {@code null}
     */
    public static PluginClasspath contextOr(ClassLoader fallback) {
        Objects.requireNonNull(fallback, "fallback");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        return new PluginClasspath(tccl != null ? tccl : fallback);
    }

    /**
     * Returns the classloader used for plugin discovery.
     *
     * @return classloader, never {@code null}
     */
    public ClassLoader classLoader() {
        return classLoader;
    }

    @Override
    public String toString() {
        return "PluginClasspath{classLoader=" + classLoader + "}";
    }
}
