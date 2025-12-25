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
/**
 * Semantic analysis for domain types.
 *
 * <h2>Purpose</h2>
 * <p>This package contains components that make <strong>architectural decisions</strong>
 * about domain types, such as aggregate root classification. These decisions are made
 * during the ANALYZE phase and stored in the IR.</p>
 *
 * <h2>Key Abstractions</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.semantics.AggregateRootClassifier AggregateRootClassifier} -
 *       Orchestrates aggregate root classification</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.semantics.AggregateRootEvidence AggregateRootEvidence} -
 *       Explainable classification result</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.semantics.AggregateRootSignals AggregateRootSignals} -
 *       Centralized signal detection (annotations, conventions)</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.semantics.RepositoryPortMatcher RepositoryPortMatcher} -
 *       Repository port detection</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Single decision point:</strong> All semantic logic is centralized</li>
 *   <li><strong>Explainable:</strong> Decisions include evidence for diagnostics</li>
 *   <li><strong>Conservative:</strong> Prefer false negatives over false positives</li>
 *   <li><strong>Stable:</strong> Results are stored in IR, not recomputed on-the-fly</li>
 * </ul>
 *
 * <h2>Relationship to Other Layers</h2>
 * <ul>
 *   <li><strong>Extract:</strong> Consumes raw data from extraction</li>
 *   <li><strong>Normalize:</strong> Uses normalized annotations and indexes</li>
 *   <li><strong>IR:</strong> Produces stable results stored in IR</li>
 *   <li><strong>SPI Adapter:</strong> Results are exposed via SPI views without re-analysis</li>
 * </ul>
 *
 * @since 0.3.0
 */
package io.hexaglue.core.internal.ir.domain.semantics;
