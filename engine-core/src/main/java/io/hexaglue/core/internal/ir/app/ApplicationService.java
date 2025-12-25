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
package io.hexaglue.core.internal.ir.app;

import io.hexaglue.core.internal.InternalMarker;
import io.hexaglue.spi.ir.app.ApplicationServiceView;
import io.hexaglue.spi.types.TypeRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Internal representation of an application service (use case).
 *
 * <p>
 * An application service orchestrates domain operations and coordinates calls to outbound ports.
 * It defines the procedural flow of a use case, sitting at the boundary of the hexagon and
 * coordinating domain entities, domain services, and port interactions.
 * </p>
 *
 * <h2>Application Service Characteristics</h2>
 * <ul>
 *   <li><strong>Orchestration:</strong> Coordinates domain and infrastructure operations</li>
 *   <li><strong>Transaction Boundary:</strong> Often defines transactional boundaries</li>
 *   <li><strong>Port Coordination:</strong> Calls outbound ports for persistence, messaging, etc.</li>
 *   <li><strong>Thin Logic:</strong> Should delegate business logic to domain layer</li>
 * </ul>
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li><strong>RegisterCustomer:</strong> Validates, creates entity, saves via repository</li>
 *   <li><strong>PlaceOrder:</strong> Checks inventory, creates order, publishes event</li>
 *   <li><strong>ProcessPayment:</strong> Validates payment, calls payment gateway, updates order</li>
 * </ul>
 *
 * <h2>HexaGlue's Role</h2>
 * <p>
 * HexaGlue <strong>analyzes but never generates</strong> application services. They are discovered for:
 * </p>
 * <ul>
 *   <li>Documentation and architecture visualization</li>
 *   <li>Use case analysis and diagnostics</li>
 *   <li>Dependency mapping (which ports are used by which use cases)</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Minimal Representation:</strong> Capture only what's needed for analysis</li>
 *   <li><strong>Immutability:</strong> Once built, the service is immutable</li>
 *   <li><strong>Non-Invasive:</strong> No modification to application service code</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable after construction and safe for concurrent read access.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * ApplicationService registerCustomer = ApplicationService.builder()
 *     .qualifiedName("com.example.application.RegisterCustomer")
 *     .simpleName("RegisterCustomer")
 *     .addOperation(registerOperation)
 *     .description("Registers a new customer in the system")
 *     .build();
 * }</pre>
 */
@InternalMarker(reason = "Internal application service; plugins use io.hexaglue.spi.ir.app.ApplicationServiceView")
public final class ApplicationService implements ApplicationServiceView {

    private final String qualifiedName;
    private final String simpleName;
    private final List<Operation> operations;
    private final String description;

    /**
     * Creates an application service with the given attributes.
     *
     * @param qualifiedName qualified name (not {@code null}, not blank)
     * @param simpleName    simple name (not {@code null}, not blank)
     * @param operations    operations list (not {@code null})
     * @param description   optional description (nullable)
     * @throws NullPointerException     if required fields are null
     * @throws IllegalArgumentException if validation fails
     */
    private ApplicationService(
            String qualifiedName, String simpleName, List<Operation> operations, String description) {
        Objects.requireNonNull(qualifiedName, "qualifiedName");
        Objects.requireNonNull(simpleName, "simpleName");
        Objects.requireNonNull(operations, "operations");

        String trimmedQualifiedName = qualifiedName.trim();
        String trimmedSimpleName = simpleName.trim();

        if (trimmedQualifiedName.isEmpty()) {
            throw new IllegalArgumentException("Qualified name must not be blank");
        }
        if (trimmedSimpleName.isEmpty()) {
            throw new IllegalArgumentException("Simple name must not be blank");
        }

        // Validate operations list contains no nulls
        for (Operation operation : operations) {
            Objects.requireNonNull(operation, "operations list contains null");
        }

        this.qualifiedName = trimmedQualifiedName;
        this.simpleName = trimmedSimpleName;
        this.operations = Collections.unmodifiableList(new ArrayList<>(operations));
        this.description = (description == null || description.isBlank()) ? null : description.trim();
    }

    @Override
    public String qualifiedName() {
        return qualifiedName;
    }

    @Override
    public String simpleName() {
        return simpleName;
    }

    @Override
    public List<OperationView> operations() {
        return Collections.unmodifiableList(operations);
    }

    /**
     * Returns the operations as {@link Operation} instances.
     *
     * <p>
     * This is a convenience method for internal code that needs the concrete type.
     * </p>
     *
     * @return immutable list of operations (never {@code null})
     */
    public List<Operation> internalOperations() {
        return operations;
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    /**
     * Returns the package name of this application service.
     *
     * @return package name, or empty string if in default package
     */
    public String packageName() {
        int lastDot = qualifiedName.lastIndexOf('.');
        return (lastDot < 0) ? "" : qualifiedName.substring(0, lastDot);
    }

    /**
     * Returns a builder for creating application services.
     *
     * @return new builder instance (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ApplicationService other)) return false;
        return qualifiedName.equals(other.qualifiedName)
                && simpleName.equals(other.simpleName)
                && operations.equals(other.operations)
                && Objects.equals(description, other.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName, simpleName, operations, description);
    }

    @Override
    public String toString() {
        return "ApplicationService{" + "qualifiedName='"
                + qualifiedName + '\'' + ", operations="
                + operations.size() + '}';
    }

    /**
     * Builder for creating {@link ApplicationService} instances.
     */
    public static final class Builder {
        private String qualifiedName;
        private String simpleName;
        private final List<Operation> operations = new ArrayList<>();
        private String description;

        private Builder() {}

        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
            return this;
        }

        public Builder simpleName(String simpleName) {
            this.simpleName = simpleName;
            return this;
        }

        public Builder addOperation(Operation operation) {
            Objects.requireNonNull(operation, "operation");
            this.operations.add(operation);
            return this;
        }

        public Builder addOperations(List<Operation> operations) {
            Objects.requireNonNull(operations, "operations");
            for (Operation operation : operations) {
                addOperation(operation);
            }
            return this;
        }

        public Builder operations(List<Operation> operations) {
            Objects.requireNonNull(operations, "operations");
            this.operations.clear();
            return addOperations(operations);
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public ApplicationService build() {
            return new ApplicationService(qualifiedName, simpleName, operations, description);
        }
    }

    /**
     * Represents an operation (method) in an application service.
     */
    public static final class Operation implements OperationView {
        private final String name;
        private final TypeRef returnType;
        private final List<TypeRef> parameterTypes;
        private final String signatureId;

        private Operation(String name, TypeRef returnType, List<TypeRef> parameterTypes, String signatureId) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(returnType, "returnType");
            Objects.requireNonNull(parameterTypes, "parameterTypes");

            String trimmedName = name.trim();
            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException("Operation name must not be blank");
            }

            // Validate parameter types list contains no nulls
            for (TypeRef paramType : parameterTypes) {
                Objects.requireNonNull(paramType, "parameterTypes list contains null");
            }

            this.name = trimmedName;
            this.returnType = returnType;
            this.parameterTypes = Collections.unmodifiableList(new ArrayList<>(parameterTypes));
            this.signatureId = (signatureId == null || signatureId.isBlank()) ? null : signatureId.trim();
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public TypeRef returnType() {
            return returnType;
        }

        @Override
        public List<TypeRef> parameterTypes() {
            return parameterTypes;
        }

        @Override
        public Optional<String> signatureId() {
            return Optional.ofNullable(signatureId);
        }

        public static Builder builder() {
            return new Builder();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Operation other)) return false;
            return name.equals(other.name)
                    && returnType.equals(other.returnType)
                    && parameterTypes.equals(other.parameterTypes)
                    && Objects.equals(signatureId, other.signatureId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, returnType, parameterTypes, signatureId);
        }

        @Override
        public String toString() {
            return "Operation{" + "name='"
                    + name + '\'' + ", returnType="
                    + returnType.render() + ", parameters="
                    + parameterTypes.size() + '}';
        }

        /**
         * Builder for creating {@link Operation} instances.
         */
        public static final class Builder {
            private String name;
            private TypeRef returnType;
            private final List<TypeRef> parameterTypes = new ArrayList<>();
            private String signatureId;

            private Builder() {}

            public Builder name(String name) {
                this.name = name;
                return this;
            }

            public Builder returnType(TypeRef returnType) {
                this.returnType = returnType;
                return this;
            }

            public Builder addParameterType(TypeRef parameterType) {
                Objects.requireNonNull(parameterType, "parameterType");
                this.parameterTypes.add(parameterType);
                return this;
            }

            public Builder addParameterTypes(List<TypeRef> parameterTypes) {
                Objects.requireNonNull(parameterTypes, "parameterTypes");
                for (TypeRef paramType : parameterTypes) {
                    addParameterType(paramType);
                }
                return this;
            }

            public Builder parameterTypes(List<TypeRef> parameterTypes) {
                Objects.requireNonNull(parameterTypes, "parameterTypes");
                this.parameterTypes.clear();
                return addParameterTypes(parameterTypes);
            }

            public Builder signatureId(String signatureId) {
                this.signatureId = signatureId;
                return this;
            }

            public Operation build() {
                return new Operation(name, returnType, parameterTypes, signatureId);
            }
        }
    }
}
