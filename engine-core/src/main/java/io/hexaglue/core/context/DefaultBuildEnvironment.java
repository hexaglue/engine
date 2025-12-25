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

import io.hexaglue.spi.context.BuildEnvironment;
import io.hexaglue.spi.context.ExecutionMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Default {@link BuildEnvironment} implementation for HexaGlue core.
 *
 * <p>
 * This implementation follows the SPI contract strictly: it does not expose annotation processing
 * APIs and provides a stable, informational view for plugins.
 * </p>
 *
 * <p>
 * Values are best-effort. When information is unknown, optionals are empty and the attributes map
 * may be empty.
 * </p>
 */
public final class DefaultBuildEnvironment implements BuildEnvironment {

    private final ExecutionMode mode;
    private final boolean debugEnabled;
    private final Locale locale;
    private final Optional<String> buildTool;
    private final Optional<String> host;
    private final Map<String, String> attributes;

    private DefaultBuildEnvironment(
            ExecutionMode mode,
            boolean debugEnabled,
            Locale locale,
            Optional<String> buildTool,
            Optional<String> host,
            Map<String, String> attributes) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.debugEnabled = debugEnabled;
        this.locale = Objects.requireNonNull(locale, "locale");
        this.buildTool = Objects.requireNonNull(buildTool, "buildTool");
        this.host = Objects.requireNonNull(host, "host");
        this.attributes = Objects.requireNonNull(attributes, "attributes");
    }

    /**
     * Creates a build environment for annotation processing.
     *
     * <p>
     * This factory records a small set of stable attributes; the core may add more keys over time.
     * </p>
     *
     * @param processingEnv processing environment, not {@code null}
     * @param mode execution mode, not {@code null}
     * @param debugEnabled debug flag
     * @param locale effective locale, not {@code null}
     * @param buildTool build tool id (nullable/blank allowed)
     * @param host host id (nullable/blank allowed)
     * @return build environment, never {@code null}
     */
    public static DefaultBuildEnvironment fromProcessing(
            ProcessingEnvironment processingEnv,
            ExecutionMode mode,
            boolean debugEnabled,
            Locale locale,
            String buildTool,
            String host) {
        Objects.requireNonNull(processingEnv, "processingEnv");
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(locale, "locale");

        Map<String, String> attrs = new LinkedHashMap<>();
        attrs.put("java.version", safe(System.getProperty("java.version")));
        attrs.put("java.vendor", safe(System.getProperty("java.vendor")));
        attrs.put("compiler.sourceVersion", String.valueOf(processingEnv.getSourceVersion()));

        // Note: processor options are often large; do not copy them all by default.
        // A future core version may selectively expose stable keys here.

        return new DefaultBuildEnvironment(
                mode, debugEnabled, locale, normalizeOptional(buildTool), normalizeOptional(host), Map.copyOf(attrs));
    }

    /**
     * Creates a build environment from explicit values.
     *
     * @param mode execution mode, not {@code null}
     * @param debugEnabled debug flag
     * @param locale effective locale, not {@code null}
     * @param buildTool build tool id (nullable/blank allowed)
     * @param host host id (nullable/blank allowed)
     * @param attributes additional attributes (nullable)
     * @return build environment, never {@code null}
     */
    public static DefaultBuildEnvironment of(
            ExecutionMode mode,
            boolean debugEnabled,
            Locale locale,
            String buildTool,
            String host,
            Map<String, String> attributes) {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(locale, "locale");
        Map<String, String> attrs = (attributes == null) ? Map.of() : Map.copyOf(attributes);
        return new DefaultBuildEnvironment(
                mode, debugEnabled, locale, normalizeOptional(buildTool), normalizeOptional(host), attrs);
    }

    @Override
    public ExecutionMode mode() {
        return mode;
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public Optional<String> buildTool() {
        return buildTool;
    }

    @Override
    public Optional<String> host() {
        return host;
    }

    @Override
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "BuildEnvironment{mode=" + mode + ", debug=" + debugEnabled + ", locale=" + locale + "}";
    }

    private static Optional<String> normalizeOptional(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String s = value.trim();
        return s.isEmpty() ? Optional.empty() : Optional.of(s);
    }

    private static String safe(String value) {
        return (value == null) ? "" : value;
    }
}
