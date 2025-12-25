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

import io.hexaglue.core.testing.HexaGlueTestHooks;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * HexaGlue JSR-269 annotation processor.
 *
 * <p>
 * HexaGlue is an "architectural compiler" that analyzes domain, ports and application services,
 * and generates infrastructure artifacts through discovered plugins.
 * </p>
 *
 * <p>
 * This processor intentionally declares {@code "*"} to be able to inspect the full compilation
 * model (domain/ports/services), without requiring users to reference a "marker annotation".
 * </p>
 *
 * <h2>Processor behavior</h2>
 * <ul>
 *   <li>HexaGlue does not modify user sources; it only generates additional artifacts.</li>
 *   <li>HexaGlue avoids interfering with other processors: {@link #process(Set, RoundEnvironment)}
 *       returns {@code false} (does not claim annotations).</li>
 * </ul>
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public final class HexaGlueProcessor extends AbstractProcessor {

    private volatile ProcessorBootstrap bootstrap;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        // If test overrides are installed (via hexaglue-testing-harness), use them.
        var overrides = HexaGlueTestHooks.current().orElse(null);
        this.bootstrap = ProcessorBootstrap.create(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        ProcessorBootstrap b = this.bootstrap;
        if (b == null) {
            // Defensive: should not happen, but avoids NPEs in unusual toolchains.
            return false;
        }

        RoundContext round = new RoundContext(processingEnv, roundEnv, b.options());
        b.processRound(round);

        // Do not "claim" any annotation: allow other processors to work normally.
        return false;
    }
}
