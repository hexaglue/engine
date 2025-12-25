/**
 * Bridge between internal IR models and plugin SPI views.
 *
 * <p>
 * <strong>⚠️ WARNING:</strong> This package contains internal plugin bridge components that are
 * <strong>NOT part of the public API</strong>. These classes are implementation details of the
 * HexaGlue compiler and must not be used directly by plugins. Plugins should use
 * {@code io.hexaglue.spi} interfaces instead.
 * </p>
 *
 * <h2>Purpose</h2>
 * <p>
 * This package provides the critical boundary between HexaGlue core internals and the plugin SPI.
 * It contains adapters and wrappers that convert internal IR models into plugin-safe view interfaces,
 * ensuring plugins can never access or modify internal state.
 * </p>
 *
 * <h2>Bridge Components</h2>
 * <p>
 * The plugin bridge system consists of two primary classes:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.plugins.PluginModelBridge} - Adapts internal models to SPI views</li>
 *   <li>{@link io.hexaglue.core.internal.plugins.PluginInputs} - Aggregates internal models for bridge creation</li>
 * </ul>
 *
 * <h2>Architecture Overview</h2>
 * <p>
 * The bridge architecture follows the Adapter pattern to completely isolate plugins from core:
 * </p>
 * <pre>
 * ┌─────────────────────┐
 * │   Plugin Code       │
 * └──────────┬──────────┘
 *            │ Uses
 *            ↓
 * ┌─────────────────────┐
 * │   SPI Interfaces    │ ← Stable contracts
 * │  (IrView, etc.)     │
 * └──────────┬──────────┘
 *            │ Implemented by
 *            ↓
 * ┌─────────────────────┐
 * │ PluginModelBridge   │ ← This package
 * └──────────┬──────────┘
 *            │ Adapts
 *            ↓
 * ┌─────────────────────┐
 * │  Internal Models    │ ← Core internals
 * │ (PortModel, etc.)   │
 * └─────────────────────┘
 * </pre>
 *
 * <h2>Data Flow</h2>
 * <p>
 * The typical data flow through the bridge is:
 * </p>
 * <ol>
 *   <li><strong>Analysis Phase:</strong> Internal analyzers produce {@link io.hexaglue.core.internal.ir.ports.PortModel},
 *       {@link io.hexaglue.core.internal.ir.domain.DomainModel}, {@link io.hexaglue.core.internal.ir.app.ApplicationModel}</li>
 *   <li><strong>Aggregation:</strong> Models are collected in {@link io.hexaglue.core.internal.plugins.PluginInputs}</li>
 *   <li><strong>Bridge Creation:</strong> {@link io.hexaglue.core.internal.plugins.PluginModelBridge} wraps models as views</li>
 *   <li><strong>Context Construction:</strong> {@link io.hexaglue.spi.ir.IrView} becomes part of
 *       {@link io.hexaglue.spi.context.GenerationContextSpec}</li>
 *   <li><strong>Plugin Execution:</strong> Plugins access models through stable SPI interfaces</li>
 * </ol>
 *
 * <h2>View Wrapping Strategy</h2>
 *
 * <h3>Direct Implementation</h3>
 * <p>
 * Some internal model elements already implement their corresponding SPI view interfaces:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.Port} implements {@link io.hexaglue.spi.ir.ports.PortView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortMethod} implements {@link io.hexaglue.spi.ir.ports.PortMethodView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortParameter} implements {@link io.hexaglue.spi.ir.ports.PortParameterView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService} implements {@link io.hexaglue.spi.ir.app.ApplicationServiceView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationService.Operation} implements {@link io.hexaglue.spi.ir.app.ApplicationServiceView.OperationView}</li>
 * </ul>
 * <p>
 * These can be returned directly from view adapters (after casting).
 * </p>
 *
 * <h3>Adapter Wrapping</h3>
 * <p>
 * Other internal types require lightweight adapter wrappers:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainType} → {@link io.hexaglue.spi.ir.domain.DomainTypeView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainProperty} → {@link io.hexaglue.spi.ir.domain.DomainPropertyView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainId} → {@link io.hexaglue.spi.ir.domain.DomainIdView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainService} → {@link io.hexaglue.spi.ir.domain.DomainServiceView}</li>
 * </ul>
 * <p>
 * These adapters simply delegate method calls to the internal instances without copying data.
 * </p>
 *
 * <h3>Container Wrapping</h3>
 * <p>
 * Container models are wrapped with specialized adapters:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.ir.ports.PortModel} → {@link io.hexaglue.spi.ir.ports.PortModelView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.domain.DomainModel} → {@link io.hexaglue.spi.ir.domain.DomainModelView}</li>
 *   <li>{@link io.hexaglue.core.internal.ir.app.ApplicationModel} → {@link io.hexaglue.spi.ir.app.ApplicationModelView}</li>
 * </ul>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Zero-Copy:</strong> Wraps existing models without duplicating data</li>
 *   <li><strong>Read-Only:</strong> All views enforce immutability</li>
 *   <li><strong>Type Safety:</strong> Plugins cannot cast views back to internal types</li>
 *   <li><strong>Encapsulation:</strong> Internal implementation details never leak to plugins</li>
 *   <li><strong>Performance:</strong> Minimal overhead for view delegation</li>
 * </ul>
 *
 * <h2>Security Boundary</h2>
 * <p>
 * This package represents a critical security boundary:
 * </p>
 * <ul>
 *   <li>Plugins MUST NOT have direct access to internal model classes</li>
 *   <li>All plugin access goes through stable SPI interfaces</li>
 *   <li>Views enforce read-only access (no mutation possible)</li>
 *   <li>Internal state remains completely hidden from plugins</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Creating Plugin Inputs</h3>
 * <pre>{@code
 * // After analysis phase
 * PortModel portModel = portAnalyzer.analyze(elements);
 * DomainModel domainModel = domainAnalyzer.analyze(elements);
 * ApplicationModel applicationModel = applicationAnalyzer.analyze(elements);
 *
 * // Aggregate into inputs
 * PluginInputs inputs = PluginInputs.of(portModel, domainModel, applicationModel);
 *
 * // Access individual models
 * PortModel ports = inputs.portModel();
 * DomainModel domain = inputs.domainModel();
 * ApplicationModel app = inputs.applicationModel();
 * }</pre>
 *
 * <h3>Creating IR View Bridge</h3>
 * <pre>{@code
 * // From inputs
 * PluginInputs inputs = ...;
 * IrView irView = inputs.toIrView();
 *
 * // Or directly from models
 * IrView irView = PluginModelBridge.create(portModel, domainModel, applicationModel);
 *
 * // Plugin access through views
 * PortModelView ports = irView.ports();
 * DomainModelView domain = irView.domain();
 * ApplicationModelView app = irView.application();
 * }</pre>
 *
 * <h3>Plugin Context Construction</h3>
 * <pre>{@code
 * // Core builds complete context for plugins
 * PluginInputs inputs = PluginInputs.of(portModel, domainModel, applicationModel);
 * IrView irView = inputs.toIrView();
 *
 * GenerationContextSpec context = GenerationContextSpec.of(
 *     nameStrategy,
 *     irView,  // ← Bridge provides this
 *     typeSystem,
 *     options,
 *     diagnostics,
 *     output,
 *     environment,
 *     request
 * );
 *
 * // Plugins receive context
 * plugin.generate(context);
 * }</pre>
 *
 * <h3>Empty Models</h3>
 * <pre>{@code
 * // Create empty inputs for testing
 * PluginInputs emptyInputs = PluginInputs.empty();
 *
 * // Empty IR view
 * IrView emptyIr = emptyInputs.toIrView();
 *
 * // All queries return empty results
 * List<PortView> ports = emptyIr.ports().ports();  // []
 * List<DomainTypeView> types = emptyIr.domain().types();  // []
 * boolean supported = emptyIr.application().isSupported();  // false
 * }</pre>
 *
 * <h2>Integration Points</h2>
 * <p>
 * This package integrates with:
 * </p>
 * <ul>
 *   <li>{@code io.hexaglue.spi.ir} - SPI view interfaces</li>
 *   <li>{@code io.hexaglue.spi.context} - Generation context for plugins</li>
 *   <li>{@code io.hexaglue.core.internal.ir} - Internal IR models</li>
 *   <li>{@code io.hexaglue.core} - Core orchestration and compilation pipeline</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>View Creation:</strong> One-time cost per compilation</li>
 *   <li><strong>Memory:</strong> Shares data with internal models (no duplication)</li>
 *   <li><strong>Method Delegation:</strong> Lightweight forwarding calls</li>
 *   <li><strong>Stream Operations:</strong> Lazy evaluation where possible</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All bridge classes are thread-safe for concurrent reads:
 * </p>
 * <ul>
 *   <li>{@link io.hexaglue.core.internal.plugins.PluginModelBridge} - Immutable after construction</li>
 *   <li>{@link io.hexaglue.core.internal.plugins.PluginInputs} - Immutable after construction</li>
 *   <li>All wrapped models are immutable</li>
 *   <li>Multiple plugins can safely access views concurrently</li>
 * </ul>
 *
 * <h2>For HexaGlue Contributors</h2>
 * <p>
 * When working with the plugin bridge:
 * </p>
 * <ol>
 *   <li>NEVER expose internal model types through SPI interfaces</li>
 *   <li>Ensure all views are read-only and immutable</li>
 *   <li>Use zero-copy wrapping wherever possible</li>
 *   <li>Validate that bridge maintains complete isolation</li>
 *   <li>Test bridge with various plugin scenarios</li>
 *   <li>Document any new view wrapping strategies</li>
 *   <li>Consider performance impact of wrapping operations</li>
 * </ol>
 */
@io.hexaglue.core.internal.InternalMarker(reason = "Internal plugin bridge; plugins use SPI interfaces")
package io.hexaglue.core.internal.plugins;
