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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/**
 * In-memory {@link javax.tools.JavaFileObject} for compilation tests.
 *
 * <p>Used to provide Java source code to {@code javac} without touching disk.</p>
 */
final class MemoryJavaFileObject extends SimpleJavaFileObject {

    private final String source;

    private MemoryJavaFileObject(URI uri, Kind kind, String source) {
        super(uri, kind);
        this.source = source;
    }

    static MemoryJavaFileObject source(String qualifiedName, String source) {
        String path = qualifiedName.replace('.', '/') + Kind.SOURCE.extension;
        URI uri = URI.create("mem:///" + path);
        return new MemoryJavaFileObject(uri, Kind.SOURCE, source);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return source;
    }

    @Override
    public Reader openReader(boolean ignoreEncodingErrors) throws IOException {
        return new StringReader(source);
    }
}
