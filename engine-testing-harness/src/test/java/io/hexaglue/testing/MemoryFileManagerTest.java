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
package io.hexaglue.testing;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Validates that {@link MemoryFileManager} captures generated sources and resources written via {@code Filer}.
 */
class MemoryFileManagerTest {

    @Test
    void shouldCaptureGeneratedSourceAndDoc() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();

        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();

        try (StandardJavaFileManager std = compiler.getStandardFileManager(diags, null, null)) {
            MemoryFileManager fm = new MemoryFileManager(std);

            // Minimal input source (not used by processor, but required by javac task)
            JavaFileObject input = MemoryJavaFileObject.source("com.example.Input", """
                            package com.example;
                            public final class Input {}
                            """);

            JavaCompiler.CompilationTask task = compiler.getTask(
                    /* out */ null,
                    /* fileManager */ fm,
                    /* diagnosticListener */ diags,
                    /* options */ List.of("-proc:only"),
                    /* classes */ null,
                    /* compilationUnits */ List.of(input));

            task.setProcessors(List.of(new MiniGeneratingProcessor()));

            boolean ok = Boolean.TRUE.equals(task.call());
            assertThat(ok).isTrue();

            MemoryFileManager.GeneratedFilesSnapshot snap = fm.snapshot();

            // Generated Java source
            assertThat(snap.findSource("com.example.Gen")).isPresent();
            assertThat(snap.findSource("com.example.Gen").get()).contains("class Gen");

            // Generated doc resource (we don't assume the exact key format besides the suffix)
            String docKey = snap.resourcesByPath().keySet().stream()
                    .filter(k -> k.endsWith("docs/hello.md"))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("docs/hello.md was not captured"));

            assertThat(snap.findResourceText(docKey)).isPresent();
            assertThat(snap.findResourceText(docKey).get()).contains("# hello");
        }
    }

    @SupportedAnnotationTypes("*")
    @SupportedSourceVersion(SourceVersion.RELEASE_17)
    private static final class MiniGeneratingProcessor extends AbstractProcessor {

        private boolean generated;

        @Override
        public synchronized void init(ProcessingEnvironment processingEnv) {
            super.init(processingEnv);
            this.generated = false;
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (generated) {
                return false;
            }
            generated = true;

            Filer filer = processingEnv.getFiler();

            // 1) Generate a Java source: com.example.Gen
            try {
                JavaFileObject src = filer.createSourceFile("com.example.Gen");
                try (Writer w = src.openWriter()) {
                    w.write("""
                            package com.example;

                            public final class Gen {
                                public static String hello() { return "hello"; }
                            }
                            """);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // 2) Generate a doc resource: docs/hello.md
            try {
                // Using packageName="" so relativeName can contain slashes (docs/hello.md)
                var fo = filer.createResource(javax.tools.StandardLocation.SOURCE_OUTPUT, "", "docs/hello.md");
                try (Writer w = fo.openWriter()) {
                    w.write("""
                            # hello

                            generated by MiniGeneratingProcessor
                            """);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Don't claim annotations.
            return false;
        }
    }
}
