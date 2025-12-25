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
package io.hexaglue.core.options;

import java.io.BufferedReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public final class HexaGlueYamlOptionsParser {

    private HexaGlueYamlOptionsParser() {}

    public static RawOptionsStore parse(BufferedReader reader, String sourceId) {
        Objects.requireNonNull(reader, "reader");
        Objects.requireNonNull(sourceId, "sourceId");

        Map<String, Object> root = loadRoot(reader);

        Object hexaglue = root.get("hexaglue");
        if (!(hexaglue instanceof Map<?, ?> h)) {
            return new RawOptionsStore(Map.of(), Map.of());
        }

        // String src = "config-file:" + sourceId;

        Map<String, RawOptionsStore.RawEntry> globals = new LinkedHashMap<>();
        Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> plugins = new LinkedHashMap<>();

        Object routing = h.get("routing");
        if (routing instanceof Map<?, ?> r) {
            flattenRouting(r, globals, sourceId);
        }

        Object pluginSection = h.get("plugins");
        if (pluginSection instanceof Map<?, ?> p) {
            flattenPlugins(p, plugins, sourceId);
        }

        Object ports = h.get("ports");
        if (ports instanceof Map<?, ?> pm) {
            flattenPortOverrides(pm, plugins, sourceId);
        }

        return new RawOptionsStore(globals, plugins);
    }

    private static Map<String, Object> loadRoot(BufferedReader reader) {
        LoaderOptions lo = new LoaderOptions();
        lo.setAllowDuplicateKeys(false);
        lo.setMaxAliasesForCollections(50);
        lo.setNestingDepthLimit(50);

        Yaml yaml = new Yaml(new SafeConstructor(lo));
        Object loaded = yaml.load(reader);
        if (loaded == null) return Map.of();
        if (!(loaded instanceof Map<?, ?> m)) {
            throw new IllegalArgumentException("hexaglue.yaml must be a YAML mapping at document root");
        }

        Map<String, Object> r = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (e.getKey() instanceof String k) {
                r.put(k, (Object) e.getValue());
            }
        }
        return r;
    }

    private static void flattenRouting(Map<?, ?> routing, Map<String, RawOptionsStore.RawEntry> globals, String src) {
        // default:
        Object def = routing.get("default");
        if (def instanceof Map<?, ?> d) {
            putGlobal(globals, "hexaglue.routing.default.driven", d.get("driven"), src);
            putGlobal(globals, "hexaglue.routing.default.driving", d.get("driving"), src);
        }

        // port:
        Object port = routing.get("port");
        if (port instanceof Map<?, ?> p) {
            for (Map.Entry<?, ?> e : p.entrySet()) {
                if (e.getKey() instanceof String portFqn) {
                    putGlobal(globals, "hexaglue.routing.port." + portFqn, e.getValue(), src);
                }
            }
        }

        // package:
        Object pkg = routing.get("package");
        if (pkg instanceof Map<?, ?> p) {
            for (Map.Entry<?, ?> e : p.entrySet()) {
                if (e.getKey() instanceof String pkgPrefix) {
                    putGlobal(globals, "hexaglue.routing.package." + pkgPrefix, e.getValue(), src);
                }
            }
        }
    }

    private static void flattenPlugins(
            Map<?, ?> plugins, Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> out, String src) {
        for (Map.Entry<?, ?> e : plugins.entrySet()) {
            if (!(e.getKey() instanceof String pluginId)) continue;
            if (!(e.getValue() instanceof Map<?, ?> cfg)) continue;

            String pid = pluginId.trim();
            if (pid.isEmpty()) continue;

            // Extract "types" section if present (for per-type, per-property metadata)
            Object typesSection = cfg.get("types");
            if (typesSection instanceof Map<?, ?> typesMap) {
                flattenTypesSection(pid, typesMap, out, src);
            }

            // Flatten remaining plugin-level config (excluding "types" key already handled)
            Map<?, ?> pluginLevelConfig = new LinkedHashMap<>(cfg);
            pluginLevelConfig.remove("types"); // Already processed
            flattenPluginMap(pid, "", pluginLevelConfig, out, src);
        }
    }

    /**
     * Flattens type-level configuration section for a plugin.
     *
     * <p>Processes the {@code types} section structure:
     * <pre>
     * types:
     *   com.example.Customer:
     *     tableName: customers
     *     properties:
     *       email:
     *         column:
     *           length: 255
     * </pre>
     *
     * @param pluginId plugin identifier
     * @param typesMap map of type FQN to type config
     * @param out output store
     * @param src source identifier
     * @since 0.4.0
     */
    private static void flattenTypesSection(
            String pluginId,
            Map<?, ?> typesMap,
            Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> out,
            String src) {
        for (Map.Entry<?, ?> e : typesMap.entrySet()) {
            if (!(e.getKey() instanceof String typeFqn)) continue;
            if (!(e.getValue() instanceof Map<?, ?> typeConfig)) continue;

            String fqn = typeFqn.trim();
            if (fqn.isEmpty()) continue;

            // Extract "properties" section if present
            Object propertiesSection = typeConfig.get("properties");
            if (propertiesSection instanceof Map<?, ?> propsMap) {
                flattenPropertiesSection(pluginId, fqn, propsMap, out, src);
            }

            // Flatten type-level config (excluding "properties" key already handled)
            String typePrefix = "types." + fqn + ".";
            Map<?, ?> typeLevelConfig = new LinkedHashMap<>(typeConfig);
            typeLevelConfig.remove("properties"); // Already processed
            flattenPluginMap(pluginId, typePrefix, typeLevelConfig, out, src);
        }
    }

    /**
     * Flattens property-level configuration section for a type.
     *
     * <p>Processes the {@code properties} section structure:
     * <pre>
     * properties:
     *   email:
     *     column:
     *       length: 255
     *       unique: true
     *     validation:
     *       pattern: "^..."
     * </pre>
     *
     * @param pluginId plugin identifier
     * @param typeFqn type fully qualified name
     * @param propertiesMap map of property name to property config
     * @param out output store
     * @param src source identifier
     * @since 0.4.0
     */
    private static void flattenPropertiesSection(
            String pluginId,
            String typeFqn,
            Map<?, ?> propertiesMap,
            Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> out,
            String src) {
        for (Map.Entry<?, ?> e : propertiesMap.entrySet()) {
            if (!(e.getKey() instanceof String propertyName)) continue;
            if (!(e.getValue() instanceof Map<?, ?> propertyConfig)) continue;

            String propName = propertyName.trim();
            if (propName.isEmpty()) continue;

            String propertyPrefix = "types." + typeFqn + ".properties." + propName + ".";
            flattenPluginMap(pluginId, propertyPrefix, propertyConfig, out, src);
        }
    }

    private static void flattenPortOverrides(
            Map<?, ?> ports, Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> out, String src) {
        for (Map.Entry<?, ?> e : ports.entrySet()) {
            if (!(e.getKey() instanceof String portFqn)) continue;
            if (!(e.getValue() instanceof Map<?, ?> perPlugin)) continue;

            String fqn = portFqn.trim();
            if (fqn.isEmpty()) continue;

            for (Map.Entry<?, ?> pe : perPlugin.entrySet()) {
                if (!(pe.getKey() instanceof String pluginId)) continue;
                if (!(pe.getValue() instanceof Map<?, ?> cfg)) continue;

                String pid = pluginId.trim();
                if (pid.isEmpty()) continue;

                String prefix = "ports." + fqn + ".";
                flattenPluginMap(pid, prefix, cfg, out, src);
            }
        }
    }

    private static void flattenPluginMap(
            String pluginId,
            String prefix,
            Map<?, ?> map,
            Map<RawOptionsStore.PluginNameKey, RawOptionsStore.RawEntry> out,
            String src) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!(e.getKey() instanceof String k)) continue;
            String key = k.trim();
            if (key.isEmpty()) continue;

            Object v = e.getValue();
            String full = prefix + key;

            if (v instanceof Map<?, ?> nested) {
                flattenPluginMap(pluginId, full + ".", nested, out, src);
            } else {
                out.put(new RawOptionsStore.PluginNameKey(pluginId, full), new RawOptionsStore.RawEntry(v, src));
            }
        }
    }

    private static void putGlobal(Map<String, RawOptionsStore.RawEntry> globals, String name, Object v, String src) {
        if (name == null) return;
        String n = name.trim();
        if (n.isEmpty()) return;
        globals.put(n, new RawOptionsStore.RawEntry(v, src));
    }
}
