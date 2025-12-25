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
package io.hexaglue.core.internal.spi;

import io.hexaglue.core.frontend.AnnotationModel;
import io.hexaglue.spi.ir.domain.AnnotationView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor14;

/**
 * Converts annotation representations between internal and SPI layers.
 *
 * <p>This utility converts {@link AnnotationModel} instances (which wrap JSR-269 {@link AnnotationMirror})
 * into {@link AnnotationView} instances suitable for plugin consumption.</p>
 *
 * <p><strong>Attribute Value Conversion:</strong>
 * <ul>
 *   <li>Primitives and wrappers: {@code Integer}, {@code Boolean}, etc.</li>
 *   <li>Strings: {@code String}</li>
 *   <li>Enums: {@code String} (qualified name)</li>
 *   <li>Classes: {@code String} (qualified name)</li>
 *   <li>Arrays: {@code List<?>}</li>
 *   <li>Nested annotations: {@code AnnotationView}</li>
 * </ul>
 */
final class AnnotationViewConverter {

    private AnnotationViewConverter() {
        // utility class
    }

    /**
     * Converts an internal annotation model to an SPI annotation view.
     *
     * @param model annotation model (not {@code null})
     * @return annotation view (never {@code null})
     */
    static AnnotationView toView(AnnotationModel model) {
        Objects.requireNonNull(model, "model");

        Map<String, Object> attributes = new HashMap<>();
        for (Map.Entry<String, AnnotationValue> entry : model.attributes().entrySet()) {
            Object decodedValue = decodeAnnotationValue(entry.getValue());
            if (decodedValue != null) {
                attributes.put(entry.getKey(), decodedValue);
            }
        }

        return AnnotationView.of(model.qualifiedName(), attributes);
    }

    /**
     * Converts a list of annotation models to annotation views.
     *
     * @param models annotation models (not {@code null})
     * @return annotation views (never {@code null}, immutable)
     */
    static List<AnnotationView> toViews(List<AnnotationModel> models) {
        Objects.requireNonNull(models, "models");
        return models.stream().map(AnnotationViewConverter::toView).toList();
    }

    /**
     * Decodes an annotation value to a simple Java type suitable for SPI exposure.
     *
     * @param value annotation value (not {@code null})
     * @return decoded value or {@code null} if unsupported
     */
    private static Object decodeAnnotationValue(AnnotationValue value) {
        if (value == null) {
            return null;
        }

        return value.accept(
                new SimpleAnnotationValueVisitor14<Object, Void>() {
                    @Override
                    public Object visitBoolean(boolean b, Void unused) {
                        return b;
                    }

                    @Override
                    public Object visitByte(byte b, Void unused) {
                        return b;
                    }

                    @Override
                    public Object visitChar(char c, Void unused) {
                        return c;
                    }

                    @Override
                    public Object visitDouble(double d, Void unused) {
                        return d;
                    }

                    @Override
                    public Object visitFloat(float f, Void unused) {
                        return f;
                    }

                    @Override
                    public Object visitInt(int i, Void unused) {
                        return i;
                    }

                    @Override
                    public Object visitLong(long i, Void unused) {
                        return i;
                    }

                    @Override
                    public Object visitShort(short s, Void unused) {
                        return s;
                    }

                    @Override
                    public Object visitString(String s, Void unused) {
                        return s;
                    }

                    @Override
                    public Object visitType(TypeMirror t, Void unused) {
                        // Return qualified class name as string
                        return t.toString();
                    }

                    @Override
                    public Object visitEnumConstant(VariableElement c, Void unused) {
                        // Return qualified enum constant name as string
                        return c.getEnclosingElement().toString() + "." + c.getSimpleName();
                    }

                    @Override
                    public Object visitAnnotation(AnnotationMirror a, Void unused) {
                        // Recursively convert nested annotations
                        return toView(AnnotationModel.of(a));
                    }

                    @Override
                    public Object visitArray(List<? extends AnnotationValue> vals, Void unused) {
                        // Convert array to list of decoded values
                        List<Object> result = new ArrayList<>(vals.size());
                        for (AnnotationValue val : vals) {
                            Object decoded = decodeAnnotationValue(val);
                            if (decoded != null) {
                                result.add(decoded);
                            }
                        }
                        return List.copyOf(result);
                    }

                    @Override
                    protected Object defaultAction(Object o, Void unused) {
                        // Unknown value type - convert to string as fallback
                        return o != null ? o.toString() : null;
                    }
                },
                null);
    }
}
