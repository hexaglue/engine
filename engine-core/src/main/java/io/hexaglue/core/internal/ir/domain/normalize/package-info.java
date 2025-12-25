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
 * Annotation normalization layer.
 *
 * <h2>Purpose</h2>
 * <p>This package provides utilities for normalizing raw annotations into indexed,
 * queryable structures. It sits between extraction (frontend) and semantic interpretation
 * (semantics).</p>
 *
 * <h2>Key Abstractions</h2>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.normalize.AnnotationIndex AnnotationIndex} -
 *       Fast lookup index over annotation sets</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.normalize.NullabilityResolver NullabilityResolver} -
 *       Resolves nullability from diverse annotation libraries</li>
 * </ul>
 *
 * <h2>Design Principle</h2>
 * <p>Normalization is about <strong>indexing and harmonizing</strong> raw data without
 * making semantic decisions. It prepares data for semantic analyzers to consume efficiently.</p>
 *
 * @since 0.3.0
 */
package io.hexaglue.core.internal.ir.domain.normalize;
