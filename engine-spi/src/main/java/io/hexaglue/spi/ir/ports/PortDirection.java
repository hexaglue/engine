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
package io.hexaglue.spi.ir.ports;

/**
 * Direction of a port in Hexagonal Architecture terminology.
 *
 * <p>Driving ports (inbound) express what the application offers to the outside.
 * Driven ports (outbound) express what the application requires from the outside.</p>
 */
public enum PortDirection {

    /**
     * Inbound / driving port (e.g., use-case API).
     */
    DRIVING,

    /**
     * Outbound / driven port (e.g., repository, gateway).
     */
    DRIVEN
}
