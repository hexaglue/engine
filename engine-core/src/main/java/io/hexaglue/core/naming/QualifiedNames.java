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
package io.hexaglue.core.naming;

import io.hexaglue.spi.naming.QualifiedName;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

/**
 * Utilities for working with {@link QualifiedName} instances.
 *
 * <p>
 * This class provides:
 * <ul>
 *   <li>Conversion from JSR-269 elements</li>
 *   <li>Package manipulation</li>
 *   <li>Name transformation</li>
 *   <li>Common name patterns</li>
 * </ul>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All methods are thread-safe and stateless.
 * </p>
 */
public final class QualifiedNames {

    private QualifiedNames() {
        // utility class
    }

    /**
     * Creates a qualified name from a JSR-269 type element.
     *
     * @param typeElement type element (not {@code null})
     * @return qualified name (never {@code null})
     */
    public static QualifiedName fromElement(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement");
        return QualifiedName.of(typeElement.getQualifiedName().toString());
    }

    /**
     * Creates a qualified name from a package element.
     *
     * @param packageElement package element (not {@code null})
     * @return qualified name (never {@code null})
     */
    public static QualifiedName fromPackage(PackageElement packageElement) {
        Objects.requireNonNull(packageElement, "packageElement");
        String name = packageElement.getQualifiedName().toString();
        return name.isEmpty() ? QualifiedName.of("unnamed") : QualifiedName.of(name);
    }

    /**
     * Creates a qualified name from a package and simple name.
     *
     * @param packageName package name (not {@code null})
     * @param simpleName  simple name (not {@code null})
     * @return qualified name (never {@code null})
     */
    public static QualifiedName of(String packageName, String simpleName) {
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(simpleName, "simpleName");

        String pkg = packageName.trim();
        String simple = simpleName.trim();

        if (pkg.isEmpty()) {
            return QualifiedName.of(simple);
        }
        if (simple.isEmpty()) {
            throw new IllegalArgumentException("simpleName must not be blank");
        }

        return QualifiedName.of(pkg + "." + simple);
    }

    /**
     * Replaces the package of a qualified name.
     *
     * <p>
     * Example: {@code replacePackage("com.example.Foo", "com.other")} returns {@code "com.other.Foo"}.
     * </p>
     *
     * @param original    original qualified name (not {@code null})
     * @param newPackage  new package name (not {@code null})
     * @return qualified name with replaced package (never {@code null})
     */
    public static QualifiedName replacePackage(QualifiedName original, String newPackage) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(newPackage, "newPackage");

        String simple = original.simpleName();
        return of(newPackage, simple);
    }

    /**
     * Appends a suffix to the simple name.
     *
     * <p>
     * Example: {@code appendSuffix("com.example.Customer", "Entity")} returns {@code "com.example.CustomerEntity"}.
     * </p>
     *
     * @param original original qualified name (not {@code null})
     * @param suffix   suffix to append (not blank)
     * @return qualified name with suffix (never {@code null})
     */
    public static QualifiedName appendSuffix(QualifiedName original, String suffix) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(suffix, "suffix");

        String trimmedSuffix = suffix.trim();
        if (trimmedSuffix.isEmpty()) {
            throw new IllegalArgumentException("suffix must not be blank");
        }

        String simple = original.simpleName() + trimmedSuffix;
        return original.packageName().map(pkg -> of(pkg, simple)).orElse(QualifiedName.of(simple));
    }

    /**
     * Prepends a prefix to the simple name.
     *
     * <p>
     * Example: {@code prependPrefix("com.example.Repository", "Default")} returns {@code "com.example.DefaultRepository"}.
     * </p>
     *
     * @param original original qualified name (not {@code null})
     * @param prefix   prefix to prepend (not blank)
     * @return qualified name with prefix (never {@code null})
     */
    public static QualifiedName prependPrefix(QualifiedName original, String prefix) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(prefix, "prefix");

        String trimmedPrefix = prefix.trim();
        if (trimmedPrefix.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }

        String simple = trimmedPrefix + original.simpleName();
        return original.packageName().map(pkg -> of(pkg, simple)).orElse(QualifiedName.of(simple));
    }

    /**
     * Creates a sibling qualified name (same package, different simple name).
     *
     * <p>
     * Example: {@code sibling("com.example.Foo", "Bar")} returns {@code "com.example.Bar"}.
     * </p>
     *
     * @param original       original qualified name (not {@code null})
     * @param newSimpleName  new simple name (not blank)
     * @return sibling qualified name (never {@code null})
     */
    public static QualifiedName sibling(QualifiedName original, String newSimpleName) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(newSimpleName, "newSimpleName");

        String simple = newSimpleName.trim();
        if (simple.isEmpty()) {
            throw new IllegalArgumentException("newSimpleName must not be blank");
        }

        return original.packageName().map(pkg -> of(pkg, simple)).orElse(QualifiedName.of(simple));
    }

    /**
     * Extracts the package path from a qualified name.
     *
     * <p>
     * Example: {@code packagePath("com.example.Foo")} returns {@code ["com", "example"]}.
     * </p>
     *
     * @param qn qualified name (not {@code null})
     * @return list of package segments (never {@code null}, may be empty)
     */
    public static List<String> packagePath(QualifiedName qn) {
        Objects.requireNonNull(qn, "qn");
        return qn.packageName().map(pkg -> List.of(pkg.split("\\."))).orElse(List.of());
    }

    /**
     * Returns the top-level package segment.
     *
     * <p>
     * Example: {@code topLevelPackage("com.example.sub.Foo")} returns {@code "com"}.
     * </p>
     *
     * @param qn qualified name (not {@code null})
     * @return top-level package segment, or empty if no package
     */
    public static String topLevelPackage(QualifiedName qn) {
        Objects.requireNonNull(qn, "qn");
        return qn.packageName()
                .map(pkg -> {
                    int idx = pkg.indexOf('.');
                    return (idx < 0) ? pkg : pkg.substring(0, idx);
                })
                .orElse("");
    }

    /**
     * Creates a nested qualified name.
     *
     * <p>
     * Example: {@code nested("com.example.Outer", "Inner")} returns {@code "com.example.Outer.Inner"}.
     * </p>
     *
     * @param enclosing    enclosing qualified name (not {@code null})
     * @param nestedName   nested simple name (not blank)
     * @return nested qualified name (never {@code null})
     */
    public static QualifiedName nested(QualifiedName enclosing, String nestedName) {
        Objects.requireNonNull(enclosing, "enclosing");
        Objects.requireNonNull(nestedName, "nestedName");

        String nested = nestedName.trim();
        if (nested.isEmpty()) {
            throw new IllegalArgumentException("nestedName must not be blank");
        }

        return QualifiedName.of(enclosing.value() + "." + nested);
    }

    /**
     * Returns the depth of the package hierarchy.
     *
     * <p>
     * Example: {@code depth("com.example.sub.Foo")} returns {@code 3}.
     * </p>
     *
     * @param qn qualified name (not {@code null})
     * @return package depth (0 if no package)
     */
    public static int depth(QualifiedName qn) {
        Objects.requireNonNull(qn, "qn");
        return qn.packageName()
                .map(pkg -> {
                    int count = 1;
                    for (int i = 0; i < pkg.length(); i++) {
                        if (pkg.charAt(i) == '.') {
                            count++;
                        }
                    }
                    return count;
                })
                .orElse(0);
    }

    /**
     * Returns {@code true} if the qualified name is in the given package (or a subpackage).
     *
     * @param qn          qualified name (not {@code null})
     * @param packageName package name (not {@code null})
     * @return {@code true} if in package
     */
    public static boolean isInPackage(QualifiedName qn, String packageName) {
        Objects.requireNonNull(qn, "qn");
        Objects.requireNonNull(packageName, "packageName");

        return qn.packageName()
                .map(pkg -> pkg.equals(packageName) || pkg.startsWith(packageName + "."))
                .orElse(false);
    }

    /**
     * Converts a qualified name to a file path.
     *
     * <p>
     * Example: {@code toFilePath("com.example.Foo", ".java")} returns {@code "com/example/Foo.java"}.
     * </p>
     *
     * @param qn        qualified name (not {@code null})
     * @param extension file extension (not {@code null}, should include the dot)
     * @return file path (never {@code null})
     */
    public static String toFilePath(QualifiedName qn, String extension) {
        Objects.requireNonNull(qn, "qn");
        Objects.requireNonNull(extension, "extension");

        return qn.value().replace('.', '/') + extension;
    }

    /**
     * Extracts the element name from an element, falling back to toString if needed.
     *
     * @param element element (not {@code null})
     * @return qualified name (never {@code null})
     */
    public static QualifiedName fromElementSafe(Element element) {
        Objects.requireNonNull(element, "element");

        if (element instanceof TypeElement) {
            return fromElement((TypeElement) element);
        }
        if (element instanceof PackageElement) {
            return fromPackage((PackageElement) element);
        }

        // Fallback: use simple name
        return QualifiedName.of(element.getSimpleName().toString());
    }
}
