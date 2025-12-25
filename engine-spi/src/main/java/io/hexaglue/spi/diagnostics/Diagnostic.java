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
package io.hexaglue.spi.diagnostics;

import io.hexaglue.spi.stability.Stable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A diagnostic message emitted by HexaGlue core or plugins.
 *
 * <p>Diagnostics are the primary way to communicate actionable feedback to users:
 * <ul>
 *   <li>errors that prevent generation</li>
 *   <li>warnings about suspicious designs</li>
 *   <li>informational hints</li>
 * </ul>
 *
 * <p>This SPI model is intentionally stable and does not expose compiler internals.
 * It supports structured metadata through {@link #attributes()}.</p>
 */
@Stable(since = "1.0.0")
public final class Diagnostic {

    private final DiagnosticSeverity severity;
    private final DiagnosticCode code;
    private final String message;
    private final DiagnosticLocation location;
    private final String pluginId;
    private final Map<String, String> attributes;
    private final Throwable cause;

    private Diagnostic(Builder b) {
        this.severity = Objects.requireNonNull(b.severity, "severity");
        this.code = Objects.requireNonNull(b.code, "code");
        this.message = requireNonBlank(b.message, "message");
        this.location = (b.location == null) ? DiagnosticLocation.unknown() : b.location;
        this.pluginId = normalizeBlankToNull(b.pluginId);

        if (b.attributes == null || b.attributes.isEmpty()) {
            this.attributes = Collections.emptyMap();
        } else {
            Map<String, String> m = new LinkedHashMap<>();
            for (Map.Entry<String, String> e : b.attributes.entrySet()) {
                if (e.getKey() == null) continue;
                String k = e.getKey().trim();
                if (k.isEmpty()) continue;
                String v = e.getValue();
                if (v != null) {
                    String vt = v.trim();
                    v = vt.isEmpty() ? null : vt;
                }
                m.put(k, v);
            }
            this.attributes = Collections.unmodifiableMap(m);
        }

        this.cause = b.cause;
    }

    /** @return severity (never {@code null}) */
    public DiagnosticSeverity severity() {
        return severity;
    }

    /** @return code (never {@code null}) */
    public DiagnosticCode code() {
        return code;
    }

    /** @return user-facing message (never blank) */
    public String message() {
        return message;
    }

    /** @return location (never {@code null}) */
    public DiagnosticLocation location() {
        return location;
    }

    /**
     * Optional plugin id emitting the diagnostic.
     *
     * <p>Core may omit this for core-originated diagnostics.</p>
     *
     * @return plugin id or {@code null}
     */
    public String pluginId() {
        return pluginId;
    }

    /**
     * Structured attributes for tools and renderers.
     *
     * <p>Typical usage:
     * <ul>
     *   <li>expectedType = "CustomerId"</li>
     *   <li>foundType = "String"</li>
     *   <li>hint = "Annotate the port method with ..."</li>
     * </ul>
     *
     * @return immutable attributes map (never {@code null})
     */
    public Map<String, String> attributes() {
        return attributes;
    }

    /**
     * Optional cause for internal debugging.
     *
     * <p>Implementations should avoid surfacing full stack traces to end-users by default.
     * Tooling may use this to attach details in debug mode.</p>
     *
     * @return cause (nullable)
     */
    public Throwable cause() {
        return cause;
    }

    /** @return builder */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience builder for an error diagnostic.
     *
     * @param code diagnostic code
     * @param message message
     * @return diagnostic
     */
    public static Diagnostic error(DiagnosticCode code, String message) {
        return builder()
                .severity(DiagnosticSeverity.ERROR)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Convenience builder for a warning diagnostic.
     *
     * @param code diagnostic code
     * @param message message
     * @return diagnostic
     */
    public static Diagnostic warning(DiagnosticCode code, String message) {
        return builder()
                .severity(DiagnosticSeverity.WARNING)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Convenience builder for an info diagnostic.
     *
     * @param code diagnostic code
     * @param message message
     * @return diagnostic
     */
    public static Diagnostic info(DiagnosticCode code, String message) {
        return builder()
                .severity(DiagnosticSeverity.INFO)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public String toString() {
        String base = severity + " " + code + ": " + message;
        String loc = (location == null || location.isUnknown()) ? "" : (" @ " + location);
        String pid = (pluginId == null) ? "" : (" [" + pluginId + "]");
        return base + pid + loc;
    }

    /**
     * Builder for {@link Diagnostic}.
     *
     * <p>Builder is intentionally provided instead of a record to keep the model extensible
     * without breaking binary compatibility when adding optional fields.</p>
     */
    public static final class Builder {
        private DiagnosticSeverity severity;
        private DiagnosticCode code;
        private String message;
        private DiagnosticLocation location;
        private String pluginId;
        private Map<String, String> attributes;
        private Throwable cause;

        private Builder() {}

        /** @param severity severity */
        public Builder severity(DiagnosticSeverity severity) {
            this.severity = severity;
            return this;
        }

        /** @param code diagnostic code */
        public Builder code(DiagnosticCode code) {
            this.code = code;
            return this;
        }

        /** @param message user-facing message */
        public Builder message(String message) {
            this.message = message;
            return this;
        }

        /** @param location location information */
        public Builder location(DiagnosticLocation location) {
            this.location = location;
            return this;
        }

        /** @param pluginId emitting plugin id */
        public Builder pluginId(String pluginId) {
            this.pluginId = pluginId;
            return this;
        }

        /**
         * Adds a structured attribute.
         *
         * @param key attribute key (non-blank)
         * @param value attribute value (nullable)
         * @return this builder
         */
        public Builder attribute(String key, String value) {
            if (this.attributes == null) {
                this.attributes = new LinkedHashMap<>();
            }
            this.attributes.put(key, value);
            return this;
        }

        /**
         * Sets attributes map (copied defensively).
         *
         * @param attributes attributes map (nullable)
         * @return this builder
         */
        public Builder attributes(Map<String, String> attributes) {
            this.attributes = (attributes == null) ? null : new LinkedHashMap<>(attributes);
            return this;
        }

        /**
         * Sets an optional cause (for debugging).
         *
         * @param cause cause (nullable)
         * @return this builder
         */
        public Builder cause(Throwable cause) {
            this.cause = cause;
            return this;
        }

        /** @return built diagnostic */
        public Diagnostic build() {
            return new Diagnostic(this);
        }
    }

    private static String requireNonBlank(String v, String label) {
        Objects.requireNonNull(v, label);
        String t = v.trim();
        if (t.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return t;
    }

    private static String normalizeBlankToNull(String v) {
        if (v == null) return null;
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
