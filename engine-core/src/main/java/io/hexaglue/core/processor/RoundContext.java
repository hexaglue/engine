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

import java.util.Objects;
import java.util.Set;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

/**
 * Immutable view of the current annotation processing round.
 *
 * <p>
 * This is a convenience wrapper that centralizes access to JSR-269 services
 * and round data. It is intentionally minimal and JDK-only.
 * </p>
 */
public final class RoundContext {

    private final ProcessingEnvironment processingEnv;
    private final RoundEnvironment roundEnv;
    private final ProcessorOptions options;

    /**
     * Creates a new round context.
     *
     * @param processingEnv the processing environment, not {@code null}
     * @param roundEnv the round environment, not {@code null}
     * @param options parsed processor options, not {@code null}
     */
    public RoundContext(ProcessingEnvironment processingEnv, RoundEnvironment roundEnv, ProcessorOptions options) {
        this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv");
        this.roundEnv = Objects.requireNonNull(roundEnv, "roundEnv");
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Returns the processing environment.
     *
     * @return the processing environment, never {@code null}
     */
    public ProcessingEnvironment processingEnv() {
        return processingEnv;
    }

    /**
     * Returns the round environment.
     *
     * @return the round environment, never {@code null}
     */
    public RoundEnvironment roundEnv() {
        return roundEnv;
    }

    /**
     * Returns the processor options.
     *
     * @return options, never {@code null}
     */
    public ProcessorOptions options() {
        return options;
    }

    /**
     * Returns the JSR-269 messager for reporting diagnostics.
     *
     * @return messager, never {@code null}
     */
    public Messager messager() {
        return processingEnv.getMessager();
    }

    /**
     * Returns the element utilities.
     *
     * @return elements utility, never {@code null}
     */
    public Elements elements() {
        return processingEnv.getElementUtils();
    }

    /**
     * Returns the type utilities.
     *
     * @return types utility, never {@code null}
     */
    public Types types() {
        return processingEnv.getTypeUtils();
    }

    /**
     * Returns the root elements for this round.
     *
     * @return root elements, never {@code null}
     */
    public Set<? extends Element> rootElements() {
        return roundEnv.getRootElements();
    }

    /**
     * Returns whether this is the final processing round.
     *
     * @return {@code true} if processing is over
     */
    public boolean isProcessingOver() {
        return roundEnv.processingOver();
    }
}
