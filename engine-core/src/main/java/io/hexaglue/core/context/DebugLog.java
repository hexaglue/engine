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

import java.util.Objects;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

/**
 * Small debug logger for HexaGlue core.
 *
 * <p>
 * Debug logs are emitted as {@link Diagnostic.Kind#NOTE} through the annotation processing
 * {@link Messager}. This keeps HexaGlue strictly JDK-only and integrates naturally with build tools.
 * </p>
 *
 * <p>
 * The logger is intentionally lightweight; core must never rely on debug logs for correctness.
 * </p>
 */
public final class DebugLog {

    private final Messager messager;
    private final boolean enabled;
    private final String prefix;

    /**
     * Creates a debug logger.
     *
     * @param messager messager to use, not {@code null}
     * @param enabled whether debug is enabled
     * @param prefix message prefix (e.g. {@code "[HexaGlue]"}), not {@code null}
     */
    public DebugLog(Messager messager, boolean enabled, String prefix) {
        this.messager = Objects.requireNonNull(messager, "messager");
        this.enabled = enabled;
        this.prefix = Objects.requireNonNull(prefix, "prefix");
    }

    /**
     * Returns whether logging is enabled.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Prints a debug message if enabled.
     *
     * @param message the message, not {@code null}
     */
    public void note(String message) {
        Objects.requireNonNull(message, "message");
        if (!enabled) {
            return;
        }
        messager.printMessage(Diagnostic.Kind.NOTE, prefix + " " + message);
    }

    /**
     * Prints a debug message with a throwable if enabled.
     *
     * <p>
     * The throwable is rendered using {@link Throwable#toString()} only (no stacktrace),
     * to avoid noisy outputs by default.
     * </p>
     *
     * @param message the message, not {@code null}
     * @param error the error, not {@code null}
     */
    public void note(String message, Throwable error) {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(error, "error");
        if (!enabled) {
            return;
        }
        messager.printMessage(Diagnostic.Kind.NOTE, prefix + " " + message + " (" + error + ")");
    }
}
