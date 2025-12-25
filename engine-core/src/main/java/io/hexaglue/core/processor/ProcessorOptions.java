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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;

/**
 * Parsed options for the HexaGlue annotation processor.
 *
 * <p>
 * Options are read from {@link ProcessingEnvironment#getOptions()} and are expected to be provided by
 * the build tool (Maven/Gradle) through compiler arguments.
 * </p>
 *
 * <h2>Names</h2>
 * <p>
 * HexaGlue core uses the {@code "hexaglue."} prefix for processor options.
 * </p>
 *
 * <h2>Stability</h2>
 * <p>
 * This is an internal core class. Option keys are part of the user-facing contract and should be
 * treated as stable once documented.
 * </p>
 */
public final class ProcessorOptions {

    /**
     * Standard option prefix for HexaGlue processor arguments.
     */
    public static final String PREFIX = "hexaglue.";

    /**
     * Enables debug logs (printed as NOTE diagnostics).
     */
    public static final String KEY_DEBUG = PREFIX + "debug";

    /**
     * Execution mode hint for the compiler (e.g. {@code "DEFAULT"}, {@code "VERIFY"}, {@code "DOCS_ONLY"}).
     *
     * <p>
     * The core may ignore unknown values; validation and mapping to SPI's execution mode is internal.
     * </p>
     */
    public static final String KEY_MODE = PREFIX + "mode";

    private final boolean debugEnabled;
    private final String mode;
    private final Map<String, String> raw;

    private ProcessorOptions(boolean debugEnabled, String mode, Map<String, String> raw) {
        this.debugEnabled = debugEnabled;
        this.mode = mode;
        this.raw = raw;
    }

    /**
     * Parses processor options from the processing environment.
     *
     * @param processingEnv the processing environment, not {@code null}
     * @return parsed options, never {@code null}
     */
    public static ProcessorOptions parse(ProcessingEnvironment processingEnv) {
        Objects.requireNonNull(processingEnv, "processingEnv");

        Map<String, String> source = processingEnv.getOptions();
        if (source == null || source.isEmpty()) {
            return new ProcessorOptions(false, "DEFAULT", Collections.emptyMap());
        }

        Map<String, String> raw = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : source.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            if (k != null) {
                raw.put(k, v);
            }
        }

        boolean debug = parseBoolean(raw.get(KEY_DEBUG), false);
        String mode = normalize(raw.get(KEY_MODE), "DEFAULT");

        return new ProcessorOptions(debug, mode, Collections.unmodifiableMap(raw));
    }

    /**
     * Returns whether debug logs are enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Returns the execution mode hint.
     *
     * @return a non-empty mode string, never {@code null}
     */
    public String mode() {
        return mode;
    }

    /**
     * Returns the raw option map (unmodifiable).
     *
     * @return raw options, never {@code null}
     */
    public Map<String, String> raw() {
        return raw;
    }

    /**
     * Returns a debug-friendly string representation.
     *
     * @return debug string, never {@code null}
     */
    public String toDebugString() {
        return "debug=" + debugEnabled + ", mode=" + mode;
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String s = value.trim();
        if (s.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s) || "on".equalsIgnoreCase(s);
    }

    private static String normalize(String value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String s = value.trim();
        return s.isEmpty() ? defaultValue : s;
    }
}
