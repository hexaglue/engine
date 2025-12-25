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
package io.hexaglue.core.lifecycle;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents one HexaGlue compilation session.
 *
 * <p>
 * A session groups all annotation-processing rounds that belong to the same compilation invocation.
 * The session is immutable; per-round state belongs in {@link CompilationInputs} and per-compiler state
 * belongs in the pipeline implementation.
 * </p>
 */
public final class CompilationSession {

    private final String id;
    private final Instant startedAt;
    private final boolean debugEnabled;
    private final String mode;

    private CompilationSession(String id, Instant startedAt, boolean debugEnabled, String mode) {
        this.id = Objects.requireNonNull(id, "id");
        this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
        this.debugEnabled = debugEnabled;
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /**
     * Creates a new session with a random unique identifier.
     *
     * @param debugEnabled whether debug logs are enabled
     * @param mode execution mode hint (non-empty), e.g. {@code "DEFAULT"}
     * @return a new session, never {@code null}
     */
    public static CompilationSession create(boolean debugEnabled, String mode) {
        String m = Objects.requireNonNull(mode, "mode").trim();
        if (m.isEmpty()) {
            throw new IllegalArgumentException("mode must not be blank");
        }
        return new CompilationSession(UUID.randomUUID().toString(), Instant.now(), debugEnabled, m);
    }

    /**
     * Returns a stable identifier for this session.
     *
     * @return session id, never {@code null}
     */
    public String id() {
        return id;
    }

    /**
     * Returns the session start instant.
     *
     * @return start time, never {@code null}
     */
    public Instant startedAt() {
        return startedAt;
    }

    /**
     * Returns whether debug logs are enabled for this session.
     *
     * @return {@code true} if debug is enabled
     */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Returns the execution mode hint.
     *
     * <p>
     * This is an internal string hint derived from processor options. The mapping to SPI execution
     * modes (if any) is handled elsewhere in core.
     * </p>
     *
     * @return mode string, never {@code null}
     */
    public String mode() {
        return mode;
    }

    @Override
    public String toString() {
        return "CompilationSession{id=" + id + ", debug=" + debugEnabled + ", mode=" + mode + "}";
    }
}
