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
package io.hexaglue.core.internal.ir.app.analysis;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.core.internal.ir.app.ApplicationService;
import io.hexaglue.core.types.TypeResolver;
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
 * Extracts application service contracts from analyzed class elements.
 *
 * <p>
 * This extractor is responsible for building complete {@link ApplicationService} instances from
 * source elements (typically {@code javax.lang.model.element.TypeElement} instances
 * representing classes). Unlike port extraction which targets interfaces, application service
 * extraction analyzes concrete service classes that orchestrate domain operations.
 * </p>
 *
 * <h2>Extraction Process</h2>
 * <p>
 * For each application service class, the extractor:
 * </p>
 * <ol>
 *   <li>Validates that the element is a class (not interface, enum, etc.)</li>
 *   <li>Determines the service's qualified and simple names</li>
 *   <li>Extracts all public method signatures as operations</li>
 *   <li>Captures method return types and parameter types</li>
 *   <li>Extracts documentation from Javadoc comments</li>
 *   <li>Builds the final {@link ApplicationService} instance</li>
 * </ol>
 *
 * <h2>Operation Extraction</h2>
 * <p>
 * The extractor processes public methods declared in the service class, capturing:
 * </p>
 * <ul>
 *   <li>Operation name (method name)</li>
 *   <li>Return type with full generic information</li>
 *   <li>Parameter types (without names for simplicity)</li>
 *   <li>Signature ID for identification</li>
 * </ul>
 *
 * <h2>Application Service Characteristics</h2>
 * <p>
 * Application services are discovered based on:
 * </p>
 * <ul>
 *   <li>Being concrete classes (not interfaces or abstract classes)</li>
 *   <li>Having public methods that represent use case operations</li>
 *   <li>Following naming conventions (UseCase, Service, Command, Query suffixes)</li>
 *   <li>Being in appropriate packages (application, usecase, etc.)</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <p>
 * This extractor collaborates with:
 * </p>
 * <ul>
 *   <li>{@link TypeResolver} - for type reference resolution</li>
 *   <li>{@link ApplicationRules} - for validation during extraction</li>
 *   <li>{@link Elements} - for accessing Javadoc and element information</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Simplicity:</strong> Capture only essential operation metadata</li>
 *   <li><strong>Non-Invasive:</strong> Analyze without modifying service code</li>
 *   <li><strong>Accuracy:</strong> Preserve method signatures precisely</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is safe for concurrent use if constructed with thread-safe dependencies.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * TypeResolver typeResolver = TypeResolver.create(elements, types);
 * ApplicationServiceExtractor extractor = new ApplicationServiceExtractor(typeResolver, elements);
 *
 * TypeElement serviceElement = ...;
 * Optional<ApplicationService> service = extractor.extract(serviceElement);
 * }</pre>
 */
@InternalMarker(reason = "Internal application service analysis; not exposed to plugins")
public final class ApplicationServiceExtractor {

    private final TypeResolver typeResolver;
    private final Elements elementUtils;

    /**
     * Creates an application service extractor with the given dependencies.
     *
     * @param typeResolver type resolver (not {@code null})
     * @param elementUtils element utilities (not {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public ApplicationServiceExtractor(TypeResolver typeResolver, Elements elementUtils) {
        this.typeResolver = Objects.requireNonNull(typeResolver, "typeResolver");
        this.elementUtils = Objects.requireNonNull(elementUtils, "elementUtils");
    }

    /**
     * Extracts an application service from a source element.
     *
     * <p>
     * This implementation uses JSR-269 APIs to analyze the type element and extract complete
     * structural and semantic information. Only class elements are considered application services.
     * </p>
     *
     * @param typeElement source type element (not {@code null}, must be {@code TypeElement})
     * @return extracted service if valid class, or empty if not a service
     * @throws NullPointerException     if typeElement is null
     * @throws IllegalArgumentException if typeElement is not a TypeElement
     */
    public Optional<ApplicationService> extract(Object typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");

        if (!(typeElement instanceof TypeElement te)) {
            throw new IllegalArgumentException("typeElement must be a TypeElement");
        }

        // Only process classes (not interfaces, enums, annotations)
        if (te.getKind() != ElementKind.CLASS) {
            return Optional.empty();
        }

        // Skip abstract classes
        if (te.getModifiers().contains(Modifier.ABSTRACT)) {
            return Optional.empty();
        }

        // Extract basic information
        String qualifiedName = te.getQualifiedName().toString();
        String simpleName = te.getSimpleName().toString();

        // Extract operations
        List<ApplicationService.Operation> operations = extractOperations(te);

        // Extract documentation
        String description = extractDocumentation(te);

        // Build application service
        ApplicationService service = ApplicationService.builder()
                .qualifiedName(qualifiedName)
                .simpleName(simpleName)
                .addOperations(operations)
                .description(description)
                .build();

        return Optional.of(service);
    }

    /**
     * Extracts all public operations from an application service class.
     *
     * @param typeElement type element (not {@code null})
     * @return list of operations (never {@code null})
     */
    private List<ApplicationService.Operation> extractOperations(TypeElement typeElement) {
        List<ApplicationService.Operation> operations = new ArrayList<>();

        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                ExecutableElement methodElement = (ExecutableElement) enclosedElement;

                // Only extract public methods
                if (methodElement.getModifiers().contains(Modifier.PUBLIC)) {
                    // Skip static methods (they're not instance operations)
                    if (!methodElement.getModifiers().contains(Modifier.STATIC)) {
                        ApplicationService.Operation operation = extractOperation(methodElement);
                        operations.add(operation);
                    }
                }
            }
        }

        return operations;
    }

    /**
     * Extracts a single operation from an executable element.
     *
     * @param methodElement method element (not {@code null})
     * @return application service operation (never {@code null})
     */
    private ApplicationService.Operation extractOperation(ExecutableElement methodElement) {
        String operationName = methodElement.getSimpleName().toString();

        // Resolve return type
        TypeRef returnType = typeResolver.resolve(methodElement.getReturnType());

        // Extract parameter types (without names)
        List<TypeRef> parameterTypes = extractParameterTypes(methodElement);

        // Build signature ID
        String signatureId = buildSignatureId(operationName, parameterTypes, returnType);

        return ApplicationService.Operation.builder()
                .name(operationName)
                .returnType(returnType)
                .addParameterTypes(parameterTypes)
                .signatureId(signatureId)
                .build();
    }

    /**
     * Extracts parameter types from a method element.
     *
     * @param methodElement method element (not {@code null})
     * @return list of parameter types (never {@code null})
     */
    private List<TypeRef> extractParameterTypes(ExecutableElement methodElement) {
        List<TypeRef> parameterTypes = new ArrayList<>();

        List<? extends VariableElement> paramElements = methodElement.getParameters();
        for (VariableElement paramElement : paramElements) {
            TypeRef paramType = typeResolver.resolve(paramElement.asType());
            parameterTypes.add(paramType);
        }

        return parameterTypes;
    }

    /**
     * Builds a signature ID for an operation.
     *
     * @param operationName  operation name (not {@code null})
     * @param parameterTypes parameter types (not {@code null})
     * @param returnType     return type (not {@code null})
     * @return signature ID
     */
    private String buildSignatureId(String operationName, List<TypeRef> parameterTypes, TypeRef returnType) {
        StringBuilder sb = new StringBuilder();
        sb.append(operationName).append('(');

        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(parameterTypes.get(i).render());
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
}
