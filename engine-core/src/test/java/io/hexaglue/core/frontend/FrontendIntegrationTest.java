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
package io.hexaglue.core.frontend;

import static com.google.common.truth.Truth.assertThat;

import io.hexaglue.core.frontend.jsr269.Jsr269Elements;
import io.hexaglue.core.frontend.jsr269.Jsr269Locations;
import io.hexaglue.core.frontend.jsr269.Jsr269Mirrors;
import io.hexaglue.core.frontend.jsr269.Jsr269Types;
import io.hexaglue.spi.diagnostics.DiagnosticLocation;
import java.io.Writer;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the frontend package including JSR-269 utilities.
 *
 * <p>This test uses the Java Compiler API to create real JSR-269 objects
 * and tests multiple frontend layers in a single pass:
 * <ul>
 *   <li>AnnotationModel</li>
 *   <li>ElementModel</li>
 *   <li>Jsr269Elements, Jsr269Types, Jsr269Mirrors, Jsr269Locations</li>
 *   <li>SourceModelFactory (if applicable)</li>
 * </ul>
 * </p>
 */
class FrontendIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Jsr269Elements Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testJsr269ElementsBasicOperations() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    private String field;
                    public void method() {}
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test isPublic
            assertThat(Jsr269Elements.isPublic(typeElement)).isTrue();

            // Test isAbstract
            assertThat(Jsr269Elements.isAbstract(typeElement)).isFalse();

            // Test isFinal
            assertThat(Jsr269Elements.isFinal(typeElement)).isFalse();

            // Test isStatic
            assertThat(Jsr269Elements.isStatic(typeElement)).isFalse();

            // Test getSimpleName
            assertThat(Jsr269Elements.getSimpleName(typeElement)).isEqualTo("TestClass");

            // Test getQualifiedName
            Optional<String> qualifiedName = Jsr269Elements.getQualifiedName(typeElement);
            assertThat(qualifiedName).isPresent();
            assertThat(qualifiedName.get()).isEqualTo("test.TestClass");

            // Test field modifiers
            List<? extends Element> enclosed = typeElement.getEnclosedElements();
            VariableElement field = (VariableElement) enclosed.stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();

            assertThat(Jsr269Elements.isPublic(field)).isFalse();
            assertThat(Jsr269Elements.isPrivate(field)).isTrue();

            // Test method
            ExecutableElement method = (ExecutableElement) enclosed.stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD)
                    .findFirst()
                    .orElseThrow();

            assertThat(Jsr269Elements.isPublic(method)).isTrue();
            assertThat(Jsr269Elements.getSimpleName(method)).isEqualTo("method");
        });
    }

    @Test
    void testJsr269ElementsAnnotations() throws Exception {
        compileAndProcess("""
                package test;
                @Deprecated
                @SuppressWarnings("unchecked")
                class AnnotatedClass {
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test findAnnotation
            Optional<AnnotationMirror> deprecated = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.Deprecated");
            assertThat(deprecated).isPresent();

            Optional<AnnotationMirror> suppressWarnings =
                    Jsr269Mirrors.findAnnotation(typeElement, "java.lang.SuppressWarnings");
            assertThat(suppressWarnings).isPresent();

            Optional<AnnotationMirror> notPresent = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.Override");
            assertThat(notPresent).isEmpty();

            // Test getAnnotationMirrors
            List<? extends AnnotationMirror> annotations = typeElement.getAnnotationMirrors();
            assertThat(annotations).hasSize(2);
        });
    }

    @Test
    void testJsr269ElementsHierarchy() throws Exception {
        compileAndProcess("""
                package test;
                class ParentClass {
                    public class InnerClass {
                    }
                }
                """, (typeElement, roundEnv, processor) -> {
            // Find inner class
            TypeElement innerClass = typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.CLASS)
                    .map(e -> (TypeElement) e)
                    .findFirst()
                    .orElseThrow();

            // Test getEnclosingTypeElement
            Optional<TypeElement> enclosing = Jsr269Elements.getEnclosingTypeElement(innerClass);
            assertThat(enclosing).isPresent();
            assertThat(Jsr269Elements.getSimpleName(enclosing.get())).isEqualTo("ParentClass");
        });
    }

    @Test
    void testJsr269ElementsClassChecks() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test isPublicClass
            assertThat(Jsr269Elements.isPublicClass(typeElement)).isTrue();

            // Test isPublicInterface
            assertThat(Jsr269Elements.isPublicInterface(typeElement)).isFalse();

            // Test isEnum
            assertThat(Jsr269Elements.isEnum(typeElement)).isFalse();

            // Test isAnnotationType
            assertThat(Jsr269Elements.isAnnotationType(typeElement)).isFalse();
        });
    }

    @Test
    void testJsr269ElementsEnumAndInterface() throws Exception {
        compileAndProcess("""
                package test;
                public enum TestEnum {
                    VALUE1, VALUE2
                }
                """, (typeElement, roundEnv, processor) -> {
            assertThat(Jsr269Elements.isEnum(typeElement)).isTrue();
            assertThat(Jsr269Elements.isPublicClass(typeElement)).isFalse();
        });

        compileAndProcess("""
                package test;
                public interface TestInterface {
                }
                """, (typeElement, roundEnv, processor) -> {
            assertThat(Jsr269Elements.isPublicInterface(typeElement)).isTrue();
            assertThat(Jsr269Elements.isPublicClass(typeElement)).isFalse();
        });

        compileAndProcess("""
                package test;
                public @interface TestAnnotation {
                }
                """, (typeElement, roundEnv, processor) -> {
            assertThat(Jsr269Elements.isAnnotationType(typeElement)).isTrue();
        });
    }

    @Test
    void testJsr269ElementsPublicMembersAccess() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    public String publicField;
                    private String privateField;
                    public void publicMethod() {}
                    private void privateMethod() {}
                    public int getValue() { return 42; }
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test getPublicMethods
            List<ExecutableElement> publicMethods = Jsr269Elements.getPublicMethods(typeElement);
            assertThat(publicMethods).hasSize(2);
            assertThat(publicMethods.stream()
                            .map(m -> m.getSimpleName().toString())
                            .toList())
                    .containsExactly("publicMethod", "getValue");

            // Test getPublicFields
            List<VariableElement> publicFields = Jsr269Elements.getPublicFields(typeElement);
            assertThat(publicFields).hasSize(1);
            assertThat(publicFields.get(0).getSimpleName().toString()).isEqualTo("publicField");
        });
    }

    @Test
    void testJsr269ElementsEnclosedElementsFiltering() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    public void method1() {}
                    public void method2() {}
                    private String field1;
                    public int field2;
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test getEnclosedElementsOfKind for methods
            List<? extends Element> methods = Jsr269Elements.getEnclosedElementsOfKind(typeElement, ElementKind.METHOD);
            assertThat(methods).hasSize(2);

            // Test getEnclosedElementsOfKind for fields
            List<? extends Element> fields = Jsr269Elements.getEnclosedElementsOfKind(typeElement, ElementKind.FIELD);
            assertThat(fields).hasSize(2);
        });
    }

    @Test
    void testJsr269ElementsModifierChecks() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    public static final String CONSTANT = "value";
                    private volatile int counter;
                }
                """, (typeElement, roundEnv, processor) -> {
            VariableElement constant = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getSimpleName().toString().equals("CONSTANT"))
                    .findFirst()
                    .orElseThrow();

            // Test hasAnyModifier
            assertThat(Jsr269Elements.hasAnyModifier(constant, Modifier.PUBLIC, Modifier.PRIVATE))
                    .isTrue();
            assertThat(Jsr269Elements.hasAnyModifier(constant, Modifier.PRIVATE, Modifier.PROTECTED))
                    .isFalse();

            // Test hasAllModifiers
            assertThat(Jsr269Elements.hasAllModifiers(constant, Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL))
                    .isTrue();
            assertThat(Jsr269Elements.hasAllModifiers(constant, Modifier.PUBLIC, Modifier.PRIVATE))
                    .isFalse();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jsr269Types Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testJsr269TypesBasicChecks() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    private int primitiveField;
                    private String classField;
                    private int[] arrayField;
                }
                """, (typeElement, roundEnv, processor) -> {
            List<? extends Element> fields = typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .toList();

            // Test primitive field
            VariableElement primitiveField = (VariableElement) fields.stream()
                    .filter(e -> e.getSimpleName().toString().equals("primitiveField"))
                    .findFirst()
                    .orElseThrow();
            TypeMirror primitiveType = primitiveField.asType();

            assertThat(Jsr269Types.isPrimitive(primitiveType)).isTrue();
            assertThat(Jsr269Types.isDeclared(primitiveType)).isFalse();
            assertThat(Jsr269Types.isArray(primitiveType)).isFalse();
            assertThat(Jsr269Types.asPrimitiveType(primitiveType)).isPresent();

            // Test class field
            VariableElement classField = (VariableElement) fields.stream()
                    .filter(e -> e.getSimpleName().toString().equals("classField"))
                    .findFirst()
                    .orElseThrow();
            TypeMirror classType = classField.asType();

            assertThat(Jsr269Types.isPrimitive(classType)).isFalse();
            assertThat(Jsr269Types.isDeclared(classType)).isTrue();
            assertThat(Jsr269Types.isArray(classType)).isFalse();
            assertThat(Jsr269Types.asDeclaredType(classType)).isPresent();

            // Test array field
            VariableElement arrayField = (VariableElement) fields.stream()
                    .filter(e -> e.getSimpleName().toString().equals("arrayField"))
                    .findFirst()
                    .orElseThrow();
            TypeMirror arrayType = arrayField.asType();

            assertThat(Jsr269Types.isArray(arrayType)).isTrue();
            assertThat(Jsr269Types.asArrayType(arrayType)).isPresent();

            Optional<TypeMirror> componentType = Jsr269Types.getArrayComponentType(arrayType);
            assertThat(componentType).isPresent();
            assertThat(Jsr269Types.isPrimitive(componentType.get())).isTrue();
        });
    }

    @Test
    void testJsr269TypesParameterized() throws Exception {
        compileAndProcess("""
                package test;
                import java.util.List;
                public class TestClass {
                    private List<String> listField;
                }
                """, (typeElement, roundEnv, processor) -> {
            VariableElement field = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();

            TypeMirror fieldType = field.asType();

            // Test hasTypeArguments
            assertThat(Jsr269Types.hasTypeArguments(fieldType)).isTrue();

            // Test getTypeArguments
            List<? extends TypeMirror> typeArgs = Jsr269Types.getTypeArguments(fieldType);
            assertThat(typeArgs).hasSize(1);

            TypeMirror stringType = typeArgs.get(0);
            assertThat(Jsr269Types.isDeclared(stringType)).isTrue();
        });
    }

    @Test
    void testJsr269TypesVoidAndTypeVariable() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass<T> {
                    private T genericField;
                    public void voidMethod() {}
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test isVoid
            ExecutableElement voidMethod = (ExecutableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD)
                    .findFirst()
                    .orElseThrow();
            TypeMirror returnType = voidMethod.getReturnType();
            assertThat(Jsr269Types.isVoid(returnType)).isTrue();

            // Test isTypeVariable
            VariableElement genericField = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();
            TypeMirror fieldType = genericField.asType();
            assertThat(Jsr269Types.isTypeVariable(fieldType)).isTrue();
            assertThat(Jsr269Types.asTypeVariable(fieldType)).isPresent();
        });
    }

    @Test
    void testJsr269TypesBoxingAndUnboxing() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    private int primitiveInt;
                    private Integer wrapperInt;
                }
                """, (typeElement, roundEnv, processor) -> {
            VariableElement primitiveField = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getSimpleName().toString().equals("primitiveInt"))
                    .findFirst()
                    .orElseThrow();

            VariableElement wrapperField = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getSimpleName().toString().equals("wrapperInt"))
                    .findFirst()
                    .orElseThrow();

            TypeMirror primitiveType = primitiveField.asType();
            TypeMirror wrapperType = wrapperField.asType();

            // Test isWrapperType
            assertThat(Jsr269Types.isWrapperType(
                            wrapperType, processor.getProcessingEnvironment().getTypeUtils()))
                    .isTrue();
            assertThat(Jsr269Types.isWrapperType(
                            primitiveType, processor.getProcessingEnvironment().getTypeUtils()))
                    .isFalse();

            // Test unboxedType
            assertThat(Jsr269Types.unboxedType(
                            wrapperType, processor.getProcessingEnvironment().getTypeUtils()))
                    .isPresent();
            assertThat(Jsr269Types.unboxedType(
                            primitiveType, processor.getProcessingEnvironment().getTypeUtils()))
                    .isEmpty();
        });
    }

    @Test
    void testJsr269TypesComparisons() throws Exception {
        compileAndProcess("""
                package test;
                import java.util.ArrayList;
                import java.util.List;
                public class TestClass {
                    private ArrayList<String> arrayList;
                    private List<String> list;
                }
                """, (typeElement, roundEnv, processor) -> {
            VariableElement arrayListField = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getSimpleName().toString().equals("arrayList"))
                    .findFirst()
                    .orElseThrow();

            VariableElement listField = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getSimpleName().toString().equals("list"))
                    .findFirst()
                    .orElseThrow();

            TypeMirror arrayListType = arrayListField.asType();
            TypeMirror listType = listField.asType();

            // Test isSubtype
            assertThat(Jsr269Types.isSubtype(
                            arrayListType,
                            listType,
                            processor.getProcessingEnvironment().getTypeUtils()))
                    .isTrue();

            // Test isAssignable
            assertThat(Jsr269Types.isAssignable(
                            arrayListType,
                            listType,
                            processor.getProcessingEnvironment().getTypeUtils()))
                    .isTrue();

            // Test isSameType
            assertThat(Jsr269Types.isSameType(
                            arrayListType,
                            arrayListType,
                            processor.getProcessingEnvironment().getTypeUtils()))
                    .isTrue();
            assertThat(Jsr269Types.isSameType(
                            arrayListType,
                            listType,
                            processor.getProcessingEnvironment().getTypeUtils()))
                    .isFalse();
        });
    }

    @Test
    void testJsr269TypesErasure() throws Exception {
        compileAndProcess("""
                package test;
                import java.util.List;
                public class TestClass {
                    private List<String> genericList;
                }
                """, (typeElement, roundEnv, processor) -> {
            VariableElement field = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();

            TypeMirror parameterizedType = field.asType();
            assertThat(Jsr269Types.hasTypeArguments(parameterizedType)).isTrue();

            // Test erasure
            TypeMirror erasedType = Jsr269Types.erasure(
                    parameterizedType, processor.getProcessingEnvironment().getTypeUtils());
            assertThat(Jsr269Types.hasTypeArguments(erasedType)).isFalse();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jsr269Mirrors Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testJsr269MirrorsTypeNames() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    private int intField;
                    private String stringField;
                }
                """, (typeElement, roundEnv, processor) -> {
            List<? extends Element> fields = typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .toList();

            // Test type name for primitive
            VariableElement intField = (VariableElement) fields.stream()
                    .filter(e -> e.getSimpleName().toString().equals("intField"))
                    .findFirst()
                    .orElseThrow();
            String intTypeName = intField.asType().toString();
            assertThat(intTypeName).isEqualTo("int");

            // Test type name for class
            VariableElement stringField = (VariableElement) fields.stream()
                    .filter(e -> e.getSimpleName().toString().equals("stringField"))
                    .findFirst()
                    .orElseThrow();
            Optional<String> stringTypeName = Jsr269Types.getQualifiedName(stringField.asType());
            assertThat(stringTypeName).isPresent();
            assertThat(stringTypeName.get()).isEqualTo("java.lang.String");
        });
    }

    @Test
    void testJsr269MirrorsTypeElement() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    private String field;
                }
                """, (typeElement, roundEnv, processor) -> {
            VariableElement field = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();

            TypeMirror fieldType = field.asType();

            // Test getTypeElement
            Optional<TypeElement> typeElem = Jsr269Types.getTypeElement(fieldType);
            assertThat(typeElem).isPresent();
            assertThat(Jsr269Elements.getQualifiedName(typeElem.get()).get()).isEqualTo("java.lang.String");
        });
    }

    @Test
    void testJsr269MirrorsAnnotationAttributes() throws Exception {
        compileAndProcess("""
                package test;
                @SuppressWarnings(value = {"unchecked", "rawtypes"})
                public class TestClass {
                }
                """, (typeElement, roundEnv, processor) -> {
            AnnotationMirror suppressMirror = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.SuppressWarnings")
                    .orElseThrow();

            // Test getAnnotationSimpleName
            String simpleName = Jsr269Mirrors.getAnnotationSimpleName(suppressMirror);
            assertThat(simpleName).isEqualTo("SuppressWarnings");

            // Test getAnnotationType
            String qualifiedName = Jsr269Mirrors.getAnnotationType(suppressMirror);
            assertThat(qualifiedName).isEqualTo("java.lang.SuppressWarnings");

            // Test hasAttribute
            assertThat(Jsr269Mirrors.hasAttribute(suppressMirror, "value")).isTrue();
            assertThat(Jsr269Mirrors.hasAttribute(suppressMirror, "nonExistent"))
                    .isFalse();

            // Test getAttributeAsStringList
            List<String> values = Jsr269Mirrors.getAttributeAsStringList(suppressMirror, "value");
            assertThat(values).hasSize(2);
            assertThat(values).containsExactly("unchecked", "rawtypes");

            // Test getAnnotationTypeElement
            TypeElement annotationType = Jsr269Mirrors.getAnnotationTypeElement(suppressMirror);
            assertThat(annotationType).isNotNull();
            assertThat(annotationType.getQualifiedName().toString()).isEqualTo("java.lang.SuppressWarnings");
        });
    }

    @Test
    void testJsr269MirrorsComplexAttributes() throws Exception {
        compileAndProcess("""
                package test;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                @Retention(RetentionPolicy.RUNTIME)
                public @interface CustomAnnotation {
                    String name() default "default";
                    int value() default 42;
                    boolean enabled() default true;
                    Class<?> targetClass() default Object.class;
                }
                """, (typeElement, roundEnv, processor) -> {
            AnnotationMirror retentionMirror = Jsr269Mirrors.findAnnotation(
                            typeElement, "java.lang.annotation.Retention")
                    .orElseThrow();

            // Test getAttribute for enum value
            Optional<javax.lang.model.element.VariableElement> enumValue =
                    Jsr269Mirrors.getAttributeAsEnum(retentionMirror, "value");
            assertThat(enumValue).isPresent();
            assertThat(enumValue.get().getSimpleName().toString()).isEqualTo("RUNTIME");
        });
    }

    @Test
    void testJsr269MirrorsSameAnnotationType() throws Exception {
        compileAndProcess("""
                package test;
                @Deprecated
                public class TestClass1 {
                }
                """, (typeElement, roundEnv, processor) -> {
            // Create a second test to compare annotation types
            compileAndProcess("""
                    package test;
                    @Deprecated
                    @SuppressWarnings("all")
                    public class TestClass2 {
                    }
                    """, (typeElement2, roundEnv2, processor2) -> {
                AnnotationMirror deprecated1 = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.Deprecated")
                        .orElseThrow();
                AnnotationMirror deprecated2 = Jsr269Mirrors.findAnnotation(typeElement2, "java.lang.Deprecated")
                        .orElseThrow();
                AnnotationMirror suppress = Jsr269Mirrors.findAnnotation(typeElement2, "java.lang.SuppressWarnings")
                        .orElseThrow();

                // Test isSameAnnotationType
                assertThat(Jsr269Mirrors.isSameAnnotationType(deprecated1, deprecated2))
                        .isTrue();
                assertThat(Jsr269Mirrors.isSameAnnotationType(deprecated1, suppress))
                        .isFalse();
            });
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jsr269Locations Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testJsr269LocationsFromElement() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    private String field;
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test location from type element
            DiagnosticLocation typeLocation = Jsr269Locations.of(typeElement);
            assertThat(typeLocation).isNotNull();

            // Test location from field element
            VariableElement field = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();

            DiagnosticLocation fieldLocation = Jsr269Locations.of(field);
            assertThat(fieldLocation).isNotNull();
        });
    }

    @Test
    void testJsr269LocationsWithAnnotationContext() throws Exception {
        compileAndProcess("""
                package test;
                @Deprecated
                public class TestClass {
                    @SuppressWarnings("unchecked")
                    private String field;
                }
                """, (typeElement, roundEnv, processor) -> {
            // Test location with annotation mirror
            AnnotationMirror deprecatedMirror = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.Deprecated")
                    .orElseThrow();
            DiagnosticLocation locationWithAnnotation = Jsr269Locations.of(typeElement, deprecatedMirror);
            assertThat(locationWithAnnotation).isNotNull();

            // Test location with annotation and attribute
            VariableElement field = (VariableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.FIELD)
                    .findFirst()
                    .orElseThrow();

            AnnotationMirror suppressMirror = Jsr269Mirrors.findAnnotation(field, "java.lang.SuppressWarnings")
                    .orElseThrow();
            DiagnosticLocation locationWithAttribute = Jsr269Locations.of(field, suppressMirror, "value");
            assertThat(locationWithAttribute).isNotNull();
        });
    }

    @Test
    void testJsr269LocationsCustomDescription() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    public void method() {}
                }
                """, (typeElement, roundEnv, processor) -> {
            ExecutableElement method = (ExecutableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD)
                    .findFirst()
                    .orElseThrow();

            // Test withDescription
            DiagnosticLocation locationWithDesc = Jsr269Locations.withDescription(method, "Custom error location");
            assertThat(locationWithDesc).isNotNull();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AnnotationModel Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testAnnotationModelCreation() throws Exception {
        compileAndProcess("""
                package test;
                @Deprecated
                public class TestClass {
                }
                """, (typeElement, roundEnv, processor) -> {
            AnnotationMirror deprecatedMirror = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.Deprecated")
                    .orElseThrow();

            // Create AnnotationModel
            AnnotationModel annotationModel = AnnotationModel.of(deprecatedMirror);

            // Test qualifiedName
            assertThat(annotationModel.qualifiedName()).isEqualTo("java.lang.Deprecated");

            // Test simpleName
            assertThat(annotationModel.simpleName()).isEqualTo("Deprecated");

            // Test isType
            assertThat(annotationModel.isType("java.lang.Deprecated")).isTrue();
            assertThat(annotationModel.isType("java.lang.Override")).isFalse();

            // Test hasSimpleName
            assertThat(annotationModel.hasSimpleName("Deprecated")).isTrue();
            assertThat(annotationModel.hasSimpleName("Override")).isFalse();

            // Test mirror
            assertThat(annotationModel.mirror()).isEqualTo(deprecatedMirror);

            // Test toString
            assertThat(annotationModel.toString()).isEqualTo("@java.lang.Deprecated");
        });
    }

    @Test
    void testAnnotationModelWithAttributes() throws Exception {
        compileAndProcess("""
                package test;
                @SuppressWarnings(value = "unchecked")
                public class TestClass {
                }
                """, (typeElement, roundEnv, processor) -> {
            AnnotationMirror suppressMirror = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.SuppressWarnings")
                    .orElseThrow();

            AnnotationModel annotationModel = AnnotationModel.of(suppressMirror);

            // Test hasAttribute
            assertThat(annotationModel.hasAttribute("value")).isTrue();
            assertThat(annotationModel.hasAttribute("nonExistent")).isFalse();

            // Test attribute
            assertThat(annotationModel.attribute("value")).isPresent();
            assertThat(annotationModel.attribute("nonExistent")).isEmpty();

            // Test attributeAsString
            assertThat(annotationModel.attributeAsString("value")).isPresent();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ElementModel Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testElementModelCreation() throws Exception {
        compileAndProcess("""
                package test;
                @Deprecated
                public final class TestClass {
                    private String field;
                    public void method() {}
                }
                """, (typeElement, roundEnv, processor) -> {
            // Create ElementModel for class
            AnnotationMirror deprecatedMirror = Jsr269Mirrors.findAnnotation(typeElement, "java.lang.Deprecated")
                    .orElseThrow();
            AnnotationModel annotationModel = AnnotationModel.of(deprecatedMirror);

            ElementModel elementModel = new ElementModel(
                    "TestClass",
                    "test.TestClass",
                    ElementKind.CLASS,
                    Set.of(Modifier.PUBLIC, Modifier.FINAL),
                    List.of(annotationModel),
                    null,
                    typeElement);

            // Test basic properties
            assertThat(elementModel.simpleName()).isEqualTo("TestClass");
            assertThat(elementModel.qualifiedName()).isPresent();
            assertThat(elementModel.qualifiedName().get()).isEqualTo("test.TestClass");
            assertThat(elementModel.kind()).isEqualTo(ElementKind.CLASS);

            // Test modifiers
            assertThat(elementModel.isPublic()).isTrue();
            assertThat(elementModel.isPrivate()).isFalse();
            assertThat(elementModel.isFinal()).isTrue();
            assertThat(elementModel.isAbstract()).isFalse();
            assertThat(elementModel.isStatic()).isFalse();

            // Test annotations
            assertThat(elementModel.annotations()).hasSize(1);
            assertThat(elementModel.hasAnnotation("java.lang.Deprecated")).isTrue();
            assertThat(elementModel.hasAnnotation("java.lang.Override")).isFalse();

            // Test findAnnotation
            assertThat(elementModel.findAnnotation("java.lang.Deprecated")).isPresent();
            assertThat(elementModel.findAnnotation("java.lang.Override")).isEmpty();

            // Test kind checks
            assertThat(elementModel.isType()).isTrue();
            assertThat(elementModel.isMethod()).isFalse();
            assertThat(elementModel.isField()).isFalse();
            assertThat(elementModel.isParameter()).isFalse();

            // Test element
            assertThat(elementModel.element()).isEqualTo(typeElement);

            // Test toString
            assertThat(elementModel.toString()).contains("CLASS");
            assertThat(elementModel.toString()).contains("test.TestClass");
        });
    }

    @Test
    void testElementModelForMethod() throws Exception {
        compileAndProcess("""
                package test;
                public class TestClass {
                    public static void staticMethod() {}
                    private void privateMethod() {}
                }
                """, (typeElement, roundEnv, processor) -> {
            ExecutableElement staticMethod = (ExecutableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD
                            && e.getSimpleName().toString().equals("staticMethod"))
                    .findFirst()
                    .orElseThrow();

            ElementModel staticMethodModel = new ElementModel(
                    "staticMethod",
                    null,
                    ElementKind.METHOD,
                    Set.of(Modifier.PUBLIC, Modifier.STATIC),
                    List.of(),
                    null,
                    staticMethod);

            assertThat(staticMethodModel.isMethod()).isTrue();
            assertThat(staticMethodModel.isStatic()).isTrue();
            assertThat(staticMethodModel.isPublic()).isTrue();

            ExecutableElement privateMethod = (ExecutableElement) typeElement.getEnclosedElements().stream()
                    .filter(e -> e.getKind() == ElementKind.METHOD
                            && e.getSimpleName().toString().equals("privateMethod"))
                    .findFirst()
                    .orElseThrow();

            ElementModel privateMethodModel = new ElementModel(
                    "privateMethod",
                    null,
                    ElementKind.METHOD,
                    Set.of(Modifier.PRIVATE),
                    List.of(),
                    null,
                    privateMethod);

            assertThat(privateMethodModel.isPrivate()).isTrue();
            assertThat(privateMethodModel.isPublic()).isFalse();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Infrastructure
    // ─────────────────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface ProcessorCallback {
        void process(TypeElement typeElement, RoundEnvironment roundEnv, TestProcessor processor) throws Exception;
    }

    private void compileAndProcess(String sourceCode, ProcessorCallback callback) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertThat(compiler).isNotNull();

        try (var fileManager = compiler.getStandardFileManager(null, null, null)) {
            // Extract class name from source code
            String className = extractClassName(sourceCode);

            // Create in-memory source file
            JavaFileObject sourceFile = new InMemoryJavaFileObject(className, sourceCode);

            // Create test processor
            TestProcessor processor = new TestProcessor(callback);

            // Compile with processor
            JavaCompiler.CompilationTask task =
                    compiler.getTask(null, fileManager, null, List.of("-proc:only"), null, List.of(sourceFile));

            task.setProcessors(List.of(processor));

            Boolean result = task.call();
            assertThat(result).isTrue();

            if (processor.exception != null) {
                throw processor.exception;
            }
        }
    }

    private String extractClassName(String sourceCode) {
        // Extract first class/enum/interface/annotation name from source code
        String[] lines = sourceCode.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();

            // Check for class, enum, interface, or @interface
            String className = null;
            if (trimmed.contains("class ")) {
                int startIdx = trimmed.indexOf("class ") + 6;
                int endIdx = minOf(
                        trimmed.indexOf(' ', startIdx) == -1 ? trimmed.length() : trimmed.indexOf(' ', startIdx),
                        trimmed.indexOf('{', startIdx) == -1 ? trimmed.length() : trimmed.indexOf('{', startIdx),
                        trimmed.indexOf('<', startIdx) == -1 ? trimmed.length() : trimmed.indexOf('<', startIdx));
                className = trimmed.substring(startIdx, endIdx).trim();
            } else if (trimmed.contains("enum ")) {
                int startIdx = trimmed.indexOf("enum ") + 5;
                int endIdx = minOf(
                        trimmed.indexOf(' ', startIdx) == -1 ? trimmed.length() : trimmed.indexOf(' ', startIdx),
                        trimmed.indexOf('{', startIdx) == -1 ? trimmed.length() : trimmed.indexOf('{', startIdx));
                className = trimmed.substring(startIdx, endIdx).trim();
            } else if (trimmed.contains("interface ")) {
                int startIdx = trimmed.indexOf("interface ") + 10;
                int endIdx = minOf(
                        trimmed.indexOf(' ', startIdx) == -1 ? trimmed.length() : trimmed.indexOf(' ', startIdx),
                        trimmed.indexOf('{', startIdx) == -1 ? trimmed.length() : trimmed.indexOf('{', startIdx));
                className = trimmed.substring(startIdx, endIdx).trim();
            }

            if (className != null && !className.isEmpty()) {
                // Extract package name
                for (String pkgLine : lines) {
                    String pkgTrimmed = pkgLine.trim();
                    if (pkgTrimmed.startsWith("package ")) {
                        String pkgName =
                                pkgTrimmed.substring(8, pkgTrimmed.indexOf(';')).trim();
                        return pkgName + "." + className;
                    }
                }
                return className;
            }
        }
        return "TestClass";
    }

    private int minOf(int a, int b) {
        return Math.min(a, b);
    }

    private int minOf(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    @SupportedAnnotationTypes("*")
    @SupportedSourceVersion(SourceVersion.RELEASE_17)
    private static class TestProcessor extends AbstractProcessor {
        private final ProcessorCallback callback;
        private Exception exception;

        TestProcessor(ProcessorCallback callback) {
            this.callback = callback;
        }

        public javax.annotation.processing.ProcessingEnvironment getProcessingEnvironment() {
            return processingEnv;
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver()) {
                return false;
            }

            try {
                for (Element element : roundEnv.getRootElements()) {
                    if (element instanceof TypeElement typeElement) {
                        callback.process(typeElement, roundEnv, this);
                    }
                }
            } catch (Exception e) {
                this.exception = e;
            }

            return false;
        }
    }

    private static class InMemoryJavaFileObject implements JavaFileObject {
        private final String className;
        private final String sourceCode;

        InMemoryJavaFileObject(String className, String sourceCode) {
            this.className = className;
            this.sourceCode = sourceCode;
        }

        @Override
        public Kind getKind() {
            return Kind.SOURCE;
        }

        @Override
        public boolean isNameCompatible(String simpleName, Kind kind) {
            return kind == Kind.SOURCE && className.endsWith(simpleName);
        }

        @Override
        public javax.lang.model.element.NestingKind getNestingKind() {
            return null;
        }

        @Override
        public javax.lang.model.element.Modifier getAccessLevel() {
            return null;
        }

        @Override
        public java.net.URI toUri() {
            return java.net.URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension);
        }

        @Override
        public String getName() {
            return className;
        }

        @Override
        public java.io.InputStream openInputStream() {
            return new java.io.ByteArrayInputStream(sourceCode.getBytes());
        }

        @Override
        public java.io.OutputStream openOutputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.Reader openReader(boolean ignoreEncodingErrors) {
            return new java.io.StringReader(sourceCode);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceCode;
        }

        @Override
        public Writer openWriter() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLastModified() {
            return 0;
        }

        @Override
        public boolean delete() {
            return false;
        }
    }
}
