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
package io.hexaglue.core.options;

import io.hexaglue.spi.options.OptionsView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Objects;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * Loads HexaGlue configuration from YAML (optional).
 *
 * <p><strong>Why Filer?</strong> In JSR-269, the processor classloader usually cannot see
 * the project's resources (src/main/resources). Using {@link ProcessingEnvironment#getFiler()}
 * is the robust way to access {@code hexaglue.yaml} during compilation.</p>
 */
public final class CoreOptionsViewLoader {

    /** Default resource name searched on compilation paths. */
    public static final String DEFAULT_YAML_RESOURCE = "hexaglue.yaml";

    private final ProcessingEnvironment processingEnv;

    public CoreOptionsViewLoader(ProcessingEnvironment processingEnv) {
        this.processingEnv = Objects.requireNonNull(processingEnv, "processingEnv");
    }

    /**
     * Loads options from {@code hexaglue.yaml} if present, otherwise returns an empty view.
     *
     * <p>No compiler-arg options are supported.</p>
     */
    public OptionsView load() {
        RawOptionsStore store = loadYamlIfPresent(DEFAULT_YAML_RESOURCE);
        return new StoreBackedOptionsView(store);
    }

    private RawOptionsStore loadYamlIfPresent(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName");

        // Typical case: build tool copies resources to CLASS_PATH before compilation.
        RawOptionsStore fromClassPath = tryLoad(StandardLocation.CLASS_PATH, resourceName);
        if (fromClassPath != null) return fromClassPath;

        // Fallback: some toolchains may expose resources on SOURCE_PATH.
        RawOptionsStore fromSourcePath = tryLoad(StandardLocation.SOURCE_PATH, resourceName);
        if (fromSourcePath != null) return fromSourcePath;

        // Not found => empty store (plugins must use defaults)
        return new RawOptionsStore(Map.of(), Map.of());
    }

    private RawOptionsStore tryLoad(StandardLocation location, String resourceName) {
        try {
            FileObject fo = processingEnv.getFiler().getResource(location, "", resourceName);
            try (Reader in = fo.openReader(true);
                    BufferedReader br = new BufferedReader(in)) {
                return HexaGlueYamlOptionsParser.parse(br, "config-file:" + location.getName() + ":/" + resourceName);
            }
        } catch (IOException ex) {
            // Not found / not readable in that location => treat as absent.
            return null;
        } catch (RuntimeException ex) {
            // Present but invalid => fail fast (better than silently ignoring config)
            throw new IllegalStateException(
                    "Failed to parse " + location.getName() + ":/" + resourceName + ": " + ex.getMessage(), ex);
        }
    }
}
