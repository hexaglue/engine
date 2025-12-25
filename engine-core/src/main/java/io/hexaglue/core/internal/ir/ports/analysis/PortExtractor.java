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
package io.hexaglue.core.internal.ir.ports.analysis;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.ports.Port;
import io.hexaglue.core.internal.ir.ports.PortMethod;
import io.hexaglue.core.internal.ir.ports.PortParameter;
import io.hexaglue.core.types.TypeResolver;
import io.hexaglue.spi.ir.ports.PortDirection;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * Extracts port contracts from analyzed interface elements.
 *
 * <p>
 * This extractor is responsible for building complete {@link Port} instances from
 * source elements (typically {@code javax.lang.model.element.TypeElement} instances
 * representing interfaces). It coordinates with {@link PortDirectionResolver} to
 * determine port direction and builds comprehensive port representations.
 * </p>
 *
 * <h2>Extraction Process</h2>
 * <p>
 * For each port interface, the extractor:
 * </p>
 * <ol>
 *   <li>Validates that the element is an interface</li>
 *   <li>Determines the port's qualified and simple names</li>
 *   <li>Resolves the port direction (DRIVING or DRIVEN)</li>
 *   <li>Extracts all method signatures</li>
 *   <li>Extracts parameters for each method</li>
 *   <li>Captures documentation and descriptions</li>
 *   <li>Builds the final {@link Port} instance</li>
 * </ol>
 *
 * <h2>Method Extraction</h2>
 * <p>
 * The extractor processes all methods declared in the interface, capturing:
 * </p>
 * <ul>
 *   <li>Method name and signature</li>
 *   <li>Return type with full generic information</li>
 *   <li>Parameters with names and types</li>
 *   <li>Modifiers (default, static)</li>
 *   <li>Varargs indicators</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>
 * This extractor collaborates with:
 * </p>
 * <ul>
 *   <li>{@link PortDirectionResolver} - for port direction classification</li>
 *   <li>{@link TypeResolver} - for type reference resolution</li>
 *   <li>{@link PortRules} - for validation during extraction</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Completeness:</strong> Extract all relevant port information</li>
 *   <li><strong>Consistency:</strong> Apply uniform extraction rules</li>
 *   <li><strong>Accuracy:</strong> Preserve semantic intent from source</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is safe for concurrent use if constructed with thread-safe dependencies.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortDirectionResolver directionResolver = new PortDirectionResolver();
 * TypeResolver typeResolver = TypeResolver.create(elements, types);
 * PortExtractor extractor = new PortExtractor(directionResolver, typeResolver, elements);
 *
 * TypeElement repositoryElement = ...;
 * Optional<Port> repository = extractor.extract(repositoryElement);
 * }</pre>
 */
@InternalMarker(reason = "Internal port analysis; not exposed to plugins")
public final class PortExtractor {

    private final PortDirectionResolver directionResolver;
    private final TypeResolver typeResolver;
    private final Elements elementUtils;

    /**
     * Creates a port extractor with the given dependencies.
     *
     * @param directionResolver direction resolver (not {@code null})
     * @param typeResolver      type resolver (not {@code null})
     * @param elementUtils      element utilities (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public PortExtractor(PortDirectionResolver directionResolver, TypeResolver typeResolver, Elements elementUtils) {
        this.directionResolver = Objects.requireNonNull(directionResolver, "directionResolver");
        this.typeResolver = Objects.requireNonNull(typeResolver, "typeResolver");
        this.elementUtils = Objects.requireNonNull(elementUtils, "elementUtils");
    }

    /**
     * Extracts a port from a source element.
     *
     * <p>
     * This implementation uses JSR-269 APIs to analyze the type element and extract complete
     * structural and semantic information. Only interface elements are considered ports.
     * </p>
     *
     * @param typeElement source type element (not {@code null}, must be {@code TypeElement})
     * @return extracted port if valid interface, or empty if not a port
     * @throws NullPointerException     if typeElement is null
     * @throws IllegalArgumentException if typeElement is not a TypeElement
     */
    public Optional<Port> extract(Object typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        if (!(typeElement instanceof TypeElement te)) {
            throw new IllegalArgumentException("typeElement must be a TypeElement");
        }

        // Only process interfaces
        if (te.getKind() != ElementKind.INTERFACE) {
            return Optional.empty();
        }

        // Extract basic information
        String qualifiedName = te.getQualifiedName().toString();
        String simpleName = te.getSimpleName().toString();
        String packageName = extractPackageName(qualifiedName);

        // Resolve direction
        PortDirection direction = directionResolver.resolve(simpleName, packageName);

        // Resolve type reference
        TypeRef typeRef = typeResolver.resolveFromElement(te);

        // Extract methods
        List<PortMethod> methods = extractMethods(te);

        // Extract documentation
        String description = extractDocumentation(te);

        // Build port
        Port port = Port.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .direction(direction)
                .type(typeRef)
                .methods(methods)
                .description(description)
                .build();

        return Optional.of(port);
    }

    /**
     * Extracts all methods from a port interface.
     *
     * @param typeElement type element (not {@code null})
     * @return list of port methods (never {@code null})
     */
    private List<PortMethod> extractMethods(TypeElement typeElement) {
        List<PortMethod> methods = new ArrayList<>();

        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                ExecutableElement methodElement = (ExecutableElement) enclosedElement;
                PortMethod method = extractMethod(methodElement);
                methods.add(method);
            }
        }

        return methods;
    }

    /**
     * Extracts a single method from an executable element.
     *
     * @param methodElement method element (not {@code null})
     * @return port method (never {@code null})
     */
    private PortMethod extractMethod(ExecutableElement methodElement) {
        String methodName = methodElement.getSimpleName().toString();

        // Resolve return type
        TypeRef returnType = typeResolver.resolve(methodElement.getReturnType());

        // Extract parameters
        List<PortParameter> parameters = extractParameters(methodElement);

        // Check modifiers
        boolean isDefault = methodElement.getModifiers().contains(Modifier.DEFAULT);
        boolean isStatic = methodElement.getModifiers().contains(Modifier.STATIC);

        // Build signature ID
        String signatureId = buildSignatureId(methodName, parameters, returnType);

        // Extract documentation
        String description = extractMethodDocumentation(methodElement);

        return PortMethod.builder()
                .name(methodName)
                .returnType(returnType)
                .parameters(parameters)
                .isDefault(isDefault)
                .isStatic(isStatic)
                .signatureId(signatureId)
                .description(description)
                .build();
    }

    /**
     * Extracts parameters from a method element.
     *
     * @param methodElement method element (not {@code null})
     * @return list of port parameters (never {@code null})
     */
    private List<PortParameter> extractParameters(ExecutableElement methodElement) {
        List<PortParameter> parameters = new ArrayList<>();

        List<? extends VariableElement> paramElements = methodElement.getParameters();
        boolean isVarArgs = methodElement.isVarArgs();

        for (int i = 0; i < paramElements.size(); i++) {
            VariableElement paramElement = paramElements.get(i);
            String paramName = paramElement.getSimpleName().toString();
            TypeRef paramType = typeResolver.resolve(paramElement.asType());

            // Last parameter is varargs if method is varargs
            boolean isVarArgsParam = isVarArgs && (i == paramElements.size() - 1);

            PortParameter parameter = PortParameter.builder()
                    .name(paramName)
                    .type(paramType)
                    .varArgs(isVarArgsParam)
                    .build();

            parameters.add(parameter);
        }

        return parameters;
    }

    /**
     * Extracts package name from qualified name.
     *
     * @param qualifiedName qualified name (not {@code null})
     * @return package name, or empty string if in default package
     */
    private String extractPackageName(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? "" : qualifiedName.substring(0, lastDot);
    }

    /**
     * Builds a signature ID for a method.
     *
     * @param methodName method name (not {@code null})
     * @param parameters parameters (not {@code null})
     * @param returnType return type (not {@code null})
     * @return signature ID
     */
    private String buildSignatureId(String methodName, List<PortParameter> parameters, TypeRef returnType) {
        StringBuilder sb = new StringBuilder();
        sb.append(methodName).append('(');

        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameters.get(i).type().render());
        }

        sb.append("):").append(returnType.render());
        return sb.toString();
    }

    /**
     * Extracts documentation from a type element.
     *
     * @param typeElement type element (not {@code null})
     * @return documentation, or {@code null} if not available
     */
    private String extractDocumentation(TypeElement typeElement) {
        String docComment = elementUtils.getDocComment(typeElement);
        if (docComment == null || docComment.isBlank()) {
            return null;
        }

        // Extract first sentence as description
        int firstPeriod = docComment.indexOf('.');
        if (firstPeriod > 0) {
            return docComment.substring(0, firstPeriod + 1).trim();
        }

        return docComment.trim();
    }

    /**
     * Extracts documentation from a method element.
     *
     * @param methodElement method element (not {@code null})
     * @return documentation, or {@code null} if not available
     */
    private String extractMethodDocumentation(ExecutableElement methodElement) {
        String docComment = elementUtils.getDocComment(methodElement);
        if (docComment == null || docComment.isBlank()) {
            return null;
        }

        // Extract first sentence as description
        int firstPeriod = docComment.indexOf('.');
        if (firstPeriod > 0) {
            return docComment.substring(0, firstPeriod + 1).trim();
        }

        return docComment.trim();
    }
}
