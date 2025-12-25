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
import io.hexaglue.spi.ir.ports.PortDirection;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves the direction of a port in Hexagonal Architecture.
 *
 * <p>
 * This resolver determines whether a port interface is a driving port (inbound) or a driven port
 * (outbound) based on naming conventions, package structure, and semantic analysis. The direction
 * classification is fundamental to generating the correct adapters.
 * </p>
 *
 * <h2>Classification Strategy</h2>
 * <p>
 * The resolver applies the following heuristics in order of precedence:
 * </p>
 * <ol>
 *   <li><strong>Package Name:</strong> Check if package contains "inbound", "outbound", "driving", "driven"</li>
 *   <li><strong>Interface Name:</strong> Analyze suffix and semantic patterns (Repository, Gateway → DRIVEN; UseCase, Command, Query → DRIVING)</li>
 *   <li><strong>Default:</strong> If uncertain, defaults to {@link PortDirection#DRIVEN} (most common for infrastructure generation)</li>
 * </ol>
 *
 * <h2>Driven Port Patterns (Outbound)</h2>
 * <p>
 * The following patterns indicate a driven port:
 * </p>
 * <ul>
 *   <li><strong>Repositories:</strong> Interfaces ending with "Repository"</li>
 *   <li><strong>Gateways:</strong> Interfaces ending with "Gateway"</li>
 *   <li><strong>Clients:</strong> Interfaces ending with "Client"</li>
 *   <li><strong>Publishers:</strong> Interfaces ending with "Publisher", "EventPublisher"</li>
 *   <li><strong>Providers:</strong> Interfaces ending with "Provider"</li>
 *   <li><strong>Package markers:</strong> Packages containing "outbound", "driven", "secondary", "spi"</li>
 * </ul>
 *
 * <h2>Driving Port Patterns (Inbound)</h2>
 * <p>
 * The following patterns indicate a driving port:
 * </p>
 * <ul>
 *   <li><strong>Use Cases:</strong> Interfaces ending with "UseCase"</li>
 *   <li><strong>Commands:</strong> Interfaces ending with "Command", "CommandHandler"</li>
 *   <li><strong>Queries:</strong> Interfaces ending with "Query", "QueryHandler"</li>
 *   <li><strong>API:</strong> Interfaces ending with "Api", "Facade"</li>
 *   <li><strong>Package markers:</strong> Packages containing "inbound", "driving", "primary", "api", "usecase"</li>
 * </ul>
 *
 * <h2>Design Goals</h2>
 * <ul>
 *   <li><strong>Deterministic:</strong> Same input always produces same output</li>
 *   <li><strong>Conservative:</strong> Defaults to DRIVEN when uncertain (infrastructure focus)</li>
 *   <li><strong>Extensible:</strong> Can be enhanced with annotation-based hints</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This class is stateless and safe for concurrent use.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PortDirectionResolver resolver = new PortDirectionResolver();
 *
 * // Repository → DRIVEN
 * PortDirection direction1 = resolver.resolve(
 *     "CustomerRepository",
 *     "com.example.domain.ports"
 * );
 *
 * // UseCase → DRIVING
 * PortDirection direction2 = resolver.resolve(
 *     "CreateOrderUseCase",
 *     "com.example.application.usecases"
 * );
 * }</pre>
 */
@InternalMarker(reason = "Internal port analysis; not exposed to plugins")
public final class PortDirectionResolver {

    // Driven port indicators (outbound)
    private static final Set<String> DRIVEN_SUFFIXES =
            Set.of("Repository", "Gateway", "Client", "Publisher", "EventPublisher", "Provider", "Adapter");

    private static final Set<String> DRIVEN_PACKAGE_MARKERS = Set.of("outbound", "driven", "secondary", "spi");

    // Driving port indicators (inbound)
    private static final Set<String> DRIVING_SUFFIXES =
            Set.of("UseCase", "Command", "CommandHandler", "Query", "QueryHandler", "Api", "Facade", "Service");

    private static final Set<String> DRIVING_PACKAGE_MARKERS =
            Set.of("inbound", "driving", "primary", "api", "usecase", "application");

    /**
     * Creates a port direction resolver.
     */
    public PortDirectionResolver() {
        // Default constructor
    }

    /**
     * Resolves the direction of a port interface.
     *
     * <p>
     * This method analyzes the interface name and package to determine if it represents
     * a driving (inbound) or driven (outbound) port. The analysis is based on common
     * naming conventions and package structures in Hexagonal Architecture.
     * </p>
     *
     * @param interfaceName simple name of the interface (not {@code null})
     * @param packageName   package name (not {@code null})
     * @return port direction (never {@code null})
     * @throws NullPointerException if any parameter is null
     */
    public PortDirection resolve(String interfaceName, String packageName) {
        Objects.requireNonNull(interfaceName, "interfaceName");
        Objects.requireNonNull(packageName, "packageName");

        // Check package markers first (most explicit)
        String lowerPackage = packageName.toLowerCase();
        for (String marker : DRIVING_PACKAGE_MARKERS) {
            if (lowerPackage.contains(marker)) {
                return PortDirection.DRIVING;
            }
        }

        for (String marker : DRIVEN_PACKAGE_MARKERS) {
            if (lowerPackage.contains(marker)) {
                return PortDirection.DRIVEN;
            }
        }

        // Check interface name patterns
        for (String suffix : DRIVING_SUFFIXES) {
            if (interfaceName.endsWith(suffix)) {
                return PortDirection.DRIVING;
            }
        }

        for (String suffix : DRIVEN_SUFFIXES) {
            if (interfaceName.endsWith(suffix)) {
                return PortDirection.DRIVEN;
            }
        }

        // Default to DRIVEN (most common for infrastructure generation)
        return PortDirection.DRIVEN;
    }

    /**
     * Determines if an interface name suggests a driven port.
     *
     * @param interfaceName interface name (not {@code null})
     * @return {@code true} if name suggests driven port
     * @throws NullPointerException if interfaceName is null
     */
    public boolean isDrivenPortName(String interfaceName) {
        Objects.requireNonNull(interfaceName, "interfaceName");
        return DRIVEN_SUFFIXES.stream().anyMatch(interfaceName::endsWith);
    }

    /**
     * Determines if an interface name suggests a driving port.
     *
     * @param interfaceName interface name (not {@code null})
     * @return {@code true} if name suggests driving port
     * @throws NullPointerException if interfaceName is null
     */
    public boolean isDrivingPortName(String interfaceName) {
        Objects.requireNonNull(interfaceName, "interfaceName");
        return DRIVING_SUFFIXES.stream().anyMatch(interfaceName::endsWith);
    }

    /**
     * Determines if a package name suggests driven ports.
     *
     * @param packageName package name (not {@code null})
     * @return {@code true} if package suggests driven ports
     * @throws NullPointerException if packageName is null
     */
    public boolean isDrivenPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        String lower = packageName.toLowerCase();
        return DRIVEN_PACKAGE_MARKERS.stream().anyMatch(lower::contains);
    }

    /**
     * Determines if a package name suggests driving ports.
     *
     * @param packageName package name (not {@code null})
     * @return {@code true} if package suggests driving ports
     * @throws NullPointerException if packageName is null
     */
    public boolean isDrivingPackage(String packageName) {
        Objects.requireNonNull(packageName, "packageName");
        String lower = packageName.toLowerCase();
        return DRIVING_PACKAGE_MARKERS.stream().anyMatch(lower::contains);
    }
}
