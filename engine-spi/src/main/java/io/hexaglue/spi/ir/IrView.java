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
package io.hexaglue.spi.ir;

import io.hexaglue.spi.ir.app.ApplicationModelView;
import io.hexaglue.spi.ir.domain.DomainModelView;
import io.hexaglue.spi.ir.ports.PortModelView;
import io.hexaglue.spi.stability.Stable;

/**
 * Root read-only view of HexaGlue's Intermediate Representation (IR).
 *
 * <p>The IR is an internal compiler model that represents analyzed domain types,
 * ports and (optionally) application services. Plugins must never depend on
 * core internals; they interact with the IR only through these stable views.</p>
 *
 * <p>This interface is intentionally small:
 * <ul>
 *   <li>It exposes only what plugins need.</li>
 *   <li>It remains read-only.</li>
 *   <li>It can evolve by adding new sub-views.</li>
 * </ul>
 *
 * <p>All sub-views returned by this interface must be stable and deterministic for a given compilation.</p>
 */
@Stable(since = "1.0.0")
public interface IrView {

    /**
     * Read-only view of the domain model (domain types, properties, ids, domain services).
     *
     * @return domain model view (never {@code null})
     */
    DomainModelView domain();

    /**
     * Read-only view of ports (driving/inbound and driven/outbound).
     *
     * @return port model view (never {@code null})
     */
    PortModelView ports();

    /**
     * Read-only view of application services / use cases, if the compiler models them.
     *
     * <p>Not all HexaGlue integrations must provide this view. Implementations should
     * still return a non-null object that represents "empty/unsupported" semantics,
     * rather than returning {@code null}.</p>
     *
     * @return application model view (never {@code null})
     */
    ApplicationModelView application();
}
