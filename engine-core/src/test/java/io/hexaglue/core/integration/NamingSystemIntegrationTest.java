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
package io.hexaglue.core.integration;

import static com.google.common.truth.Truth.assertThat;

import io.hexaglue.core.naming.DefaultNameStrategy;
import io.hexaglue.core.naming.NameRules;
import io.hexaglue.core.naming.NameSanitizer;
import io.hexaglue.core.naming.QualifiedNames;
import io.hexaglue.spi.naming.NameRole;
import io.hexaglue.spi.naming.NameStrategySpec;
import io.hexaglue.spi.naming.QualifiedName;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration test validating the naming system SPI contracts and Core implementations.
 *
 * <p>
 * This test ensures that:
 * <ul>
 *   <li>QualifiedName (SPI) works correctly</li>
 *   <li>NameRole enum is accessible</li>
 *   <li>NameSanitizer utility functions work correctly</li>
 *   <li>QualifiedNames utility provides name manipulation</li>
 *   <li>NameRules provides naming conventions</li>
 *   <li>DefaultNameStrategy implements NameStrategySpec correctly</li>
 * </ul>
 * </p>
 */
class NamingSystemIntegrationTest {

    // ─────────────────────────────────────────────────────────────────────────
    // QualifiedName (SPI) Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testQualifiedNameCreation() {
        // When: Create qualified names
        QualifiedName simple = QualifiedName.of("Foo");
        QualifiedName qualified = QualifiedName.of("com.example.Foo");

        // Then: Should preserve values
        assertThat(simple.value()).isEqualTo("Foo");
        assertThat(qualified.value()).isEqualTo("com.example.Foo");
    }

    @Test
    void testQualifiedNamePackageExtraction() {
        // Given: Qualified name
        QualifiedName qn = QualifiedName.of("com.example.domain.Customer");

        // When: Extract package
        String pkg = qn.packageName().orElseThrow();

        // Then: Should extract package
        assertThat(pkg).isEqualTo("com.example.domain");
    }

    @Test
    void testQualifiedNameSimpleHasNoPackage() {
        // Given: Simple name
        QualifiedName qn = QualifiedName.of("Customer");

        // When: Check package
        // Then: Should have no package
        assertThat(qn.packageName()).isEmpty();
    }

    @Test
    void testQualifiedNameSimpleName() {
        // Given: Qualified and simple names
        QualifiedName qualified = QualifiedName.of("com.example.Customer");
        QualifiedName simple = QualifiedName.of("Customer");

        // When/Then: Should extract simple name
        assertThat(qualified.simpleName()).isEqualTo("Customer");
        assertThat(simple.simpleName()).isEqualTo("Customer");
    }

    @Test
    void testQualifiedNameEnclosing() {
        // Given: Nested qualified name
        QualifiedName nested = QualifiedName.of("com.example.Outer.Inner");

        // When: Get enclosing
        QualifiedName enclosing = nested.enclosing().orElseThrow();

        // Then: Should return enclosing name
        assertThat(enclosing.value()).isEqualTo("com.example.Outer");
    }

    @Test
    void testQualifiedNameEquality() {
        // Given: QualifiedName instances
        QualifiedName qn1 = QualifiedName.of("com.example.Foo");
        QualifiedName qn2 = QualifiedName.of("com.example.Foo");
        QualifiedName qn3 = QualifiedName.of("com.example.Bar");

        // Then: Equality should work
        assertThat(qn1).isEqualTo(qn2);
        assertThat(qn1).isNotEqualTo(qn3);
    }

    @Test
    void testQualifiedNameComparable() {
        // Given: QualifiedName instances
        List<QualifiedName> names = List.of(
                QualifiedName.of("com.zoo.Z"), QualifiedName.of("com.apple.A"), QualifiedName.of("com.middle.M"));

        // When: Sort
        List<QualifiedName> sorted = names.stream().sorted().toList();

        // Then: Should be alphabetically sorted
        assertThat(sorted.get(0).value()).isEqualTo("com.apple.A");
        assertThat(sorted.get(1).value()).isEqualTo("com.middle.M");
        assertThat(sorted.get(2).value()).isEqualTo("com.zoo.Z");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NameRole (SPI) Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNameRoleEnum() {
        // When: Access NameRole enum values
        NameRole pkg = NameRole.PACKAGE;
        NameRole type = NameRole.TYPE;
        NameRole field = NameRole.FIELD;
        NameRole method = NameRole.METHOD;
        NameRole parameter = NameRole.PARAMETER;
        NameRole constant = NameRole.CONSTANT;
        NameRole resourcePath = NameRole.RESOURCE_PATH;
        NameRole docPath = NameRole.DOC_PATH;

        // Then: All values should be accessible
        assertThat(pkg).isNotNull();
        assertThat(type).isNotNull();
        assertThat(field).isNotNull();
        assertThat(method).isNotNull();
        assertThat(parameter).isNotNull();
        assertThat(constant).isNotNull();
        assertThat(resourcePath).isNotNull();
        assertThat(docPath).isNotNull();
        assertThat(NameRole.values()).hasLength(8);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NameSanitizer Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNameSanitizerIsValidJavaIdentifier() {
        // When/Then: Should validate Java identifiers
        assertThat(NameSanitizer.isValidJavaIdentifier("validName")).isTrue();
        assertThat(NameSanitizer.isValidJavaIdentifier("_underscore")).isTrue();
        assertThat(NameSanitizer.isValidJavaIdentifier("camelCase123")).isTrue();

        assertThat(NameSanitizer.isValidJavaIdentifier("123invalid")).isFalse();
        assertThat(NameSanitizer.isValidJavaIdentifier("invalid-name")).isFalse();
        assertThat(NameSanitizer.isValidJavaIdentifier("class")).isFalse(); // keyword
        assertThat(NameSanitizer.isValidJavaIdentifier("")).isFalse();
    }

    @Test
    void testNameSanitizerSanitize() {
        // When: Sanitize various inputs
        String sanitized1 = NameSanitizer.sanitize("valid-name");
        String sanitized2 = NameSanitizer.sanitize("123number");
        String sanitized3 = NameSanitizer.sanitize("class");
        String sanitized4 = NameSanitizer.sanitize("");

        // Then: Should produce valid identifiers
        assertThat(sanitized1).isEqualTo("valid_name");
        assertThat(sanitized2).isEqualTo("_123number");
        assertThat(sanitized3).isEqualTo("class_");
        assertThat(sanitized4).isEqualTo("unnamed");
    }

    @Test
    void testNameSanitizerSanitizePackage() {
        // When: Sanitize package names
        String pkg1 = NameSanitizer.sanitizePackage("com.example.valid");
        String pkg2 = NameSanitizer.sanitizePackage("com.example.invalid-name");
        String pkg3 = NameSanitizer.sanitizePackage("com.class.package");

        // Then: Should sanitize each segment
        assertThat(pkg1).isEqualTo("com.example.valid");
        assertThat(pkg2).isEqualTo("com.example.invalid_name");
        assertThat(pkg3).isEqualTo("com.class_.package_");
    }

    @Test
    void testNameSanitizerToCamelCase() {
        // When: Convert to camelCase
        String camel1 = NameSanitizer.toCamelCase("hello_world");
        String camel2 = NameSanitizer.toCamelCase("HelloWorld");
        String camel3 = NameSanitizer.toCamelCase("hello-world-test");

        // Then: Should convert to camelCase
        assertThat(camel1).isEqualTo("helloWorld");
        assertThat(camel2).isEqualTo("helloWorld");
        assertThat(camel3).isEqualTo("helloWorldTest");
    }

    @Test
    void testNameSanitizerToPascalCase() {
        // When: Convert to PascalCase
        String pascal1 = NameSanitizer.toPascalCase("hello_world");
        String pascal2 = NameSanitizer.toPascalCase("helloWorld");

        // Then: Should convert to PascalCase
        assertThat(pascal1).isEqualTo("HelloWorld");
        assertThat(pascal2).isEqualTo("HelloWorld");
    }

    @Test
    void testNameSanitizerToConstantCase() {
        // When: Convert to CONSTANT_CASE
        String constant1 = NameSanitizer.toConstantCase("helloWorld");
        String constant2 = NameSanitizer.toConstantCase("hello-world");
        String constant3 = NameSanitizer.toConstantCase("ALREADY_CONSTANT");

        // Then: Should convert to CONSTANT_CASE
        assertThat(constant1).isEqualTo("HELLO_WORLD");
        assertThat(constant2).isEqualTo("HELLO_WORLD");
        assertThat(constant3).isEqualTo("ALREADY_CONSTANT");
    }

    @Test
    void testNameSanitizerIsKeyword() {
        // When/Then: Should detect Java keywords
        assertThat(NameSanitizer.isKeyword("class")).isTrue();
        assertThat(NameSanitizer.isKeyword("public")).isTrue();
        assertThat(NameSanitizer.isKeyword("void")).isTrue();
        assertThat(NameSanitizer.isKeyword("true")).isTrue();
        assertThat(NameSanitizer.isKeyword("null")).isTrue();

        assertThat(NameSanitizer.isKeyword("notAKeyword")).isFalse();
        assertThat(NameSanitizer.isKeyword("Class")).isFalse(); // Case sensitive
    }

    @Test
    void testNameSanitizerEscapeKeyword() {
        // When: Escape keywords
        String escaped1 = NameSanitizer.escapeKeyword("class");
        String escaped2 = NameSanitizer.escapeKeyword("notKeyword");

        // Then: Should escape keywords
        assertThat(escaped1).isEqualTo("class_");
        assertThat(escaped2).isEqualTo("notKeyword");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QualifiedNames Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testQualifiedNamesOf() {
        // When: Create qualified name from package and simple name
        QualifiedName qn = QualifiedNames.of("com.example", "Customer");

        // Then: Should combine correctly
        assertThat(qn.value()).isEqualTo("com.example.Customer");
    }

    @Test
    void testQualifiedNamesOfEmptyPackage() {
        // When: Create with empty package
        QualifiedName qn = QualifiedNames.of("", "Customer");

        // Then: Should use only simple name
        assertThat(qn.value()).isEqualTo("Customer");
    }

    @Test
    void testQualifiedNamesReplacePackage() {
        // Given: Original qualified name
        QualifiedName original = QualifiedName.of("com.example.Customer");

        // When: Replace package
        QualifiedName replaced = QualifiedNames.replacePackage(original, "com.other");

        // Then: Should have new package
        assertThat(replaced.value()).isEqualTo("com.other.Customer");
    }

    @Test
    void testQualifiedNamesAppendSuffix() {
        // Given: Original qualified name
        QualifiedName original = QualifiedName.of("com.example.Customer");

        // When: Append suffix
        QualifiedName suffixed = QualifiedNames.appendSuffix(original, "Entity");

        // Then: Should append to simple name
        assertThat(suffixed.value()).isEqualTo("com.example.CustomerEntity");
    }

    @Test
    void testQualifiedNamesPrependPrefix() {
        // Given: Original qualified name
        QualifiedName original = QualifiedName.of("com.example.Repository");

        // When: Prepend prefix
        QualifiedName prefixed = QualifiedNames.prependPrefix(original, "Default");

        // Then: Should prepend to simple name
        assertThat(prefixed.value()).isEqualTo("com.example.DefaultRepository");
    }

    @Test
    void testQualifiedNamesSibling() {
        // Given: Original qualified name
        QualifiedName original = QualifiedName.of("com.example.Customer");

        // When: Create sibling
        QualifiedName sibling = QualifiedNames.sibling(original, "Order");

        // Then: Should have same package, different simple name
        assertThat(sibling.value()).isEqualTo("com.example.Order");
    }

    @Test
    void testQualifiedNamesNested() {
        // Given: Enclosing qualified name
        QualifiedName enclosing = QualifiedName.of("com.example.Outer");

        // When: Create nested
        QualifiedName nested = QualifiedNames.nested(enclosing, "Inner");

        // Then: Should nest under enclosing
        assertThat(nested.value()).isEqualTo("com.example.Outer.Inner");
    }

    @Test
    void testQualifiedNamesPackagePath() {
        // Given: Qualified name
        QualifiedName qn = QualifiedName.of("com.example.sub.Customer");

        // When: Get package path
        List<String> path = QualifiedNames.packagePath(qn);

        // Then: Should split package segments
        assertThat(path).containsExactly("com", "example", "sub").inOrder();
    }

    @Test
    void testQualifiedNamesTopLevelPackage() {
        // Given: Qualified name
        QualifiedName qn = QualifiedName.of("com.example.sub.Customer");

        // When: Get top-level package
        String topLevel = QualifiedNames.topLevelPackage(qn);

        // Then: Should return first segment
        assertThat(topLevel).isEqualTo("com");
    }

    @Test
    void testQualifiedNamesDepth() {
        // Given: Qualified names with different depths
        QualifiedName simple = QualifiedName.of("Customer");
        QualifiedName depth1 = QualifiedName.of("com.Customer");
        QualifiedName depth3 = QualifiedName.of("com.example.sub.Customer");

        // When/Then: Should calculate depth
        assertThat(QualifiedNames.depth(simple)).isEqualTo(0);
        assertThat(QualifiedNames.depth(depth1)).isEqualTo(1);
        assertThat(QualifiedNames.depth(depth3)).isEqualTo(3);
    }

    @Test
    void testQualifiedNamesIsInPackage() {
        // Given: Qualified name
        QualifiedName qn = QualifiedName.of("com.example.sub.Customer");

        // When/Then: Should check package membership
        assertThat(QualifiedNames.isInPackage(qn, "com.example")).isTrue();
        assertThat(QualifiedNames.isInPackage(qn, "com.example.sub")).isTrue();
        assertThat(QualifiedNames.isInPackage(qn, "com.other")).isFalse();
    }

    @Test
    void testQualifiedNamesToFilePath() {
        // Given: Qualified name
        QualifiedName qn = QualifiedName.of("com.example.Customer");

        // When: Convert to file path
        String path = QualifiedNames.toFilePath(qn, ".java");

        // Then: Should convert dots to slashes
        assertThat(path).isEqualTo("com/example/Customer.java");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NameRules Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNameRulesConstants() {
        // When: Access constants
        String entitySuffix = NameRules.Suffixes.ENTITY;
        String defaultPrefix = NameRules.Prefixes.DEFAULT;
        String domainSegment = NameRules.PackageSegments.DOMAIN;

        // Then: Should have expected values
        assertThat(entitySuffix).isEqualTo("Entity");
        assertThat(defaultPrefix).isEqualTo("Default");
        assertThat(domainSegment).isEqualTo("domain");
    }

    @Test
    void testNameRulesApplyConventionForType() {
        // When: Apply TYPE convention
        String typeName = NameRules.applyConvention("customer", NameRole.TYPE);

        // Then: Should convert to PascalCase
        assertThat(typeName).isEqualTo("Customer");
    }

    @Test
    void testNameRulesApplyConventionForField() {
        // When: Apply FIELD convention
        String fieldName = NameRules.applyConvention("CustomerName", NameRole.FIELD);

        // Then: Should convert to camelCase
        assertThat(fieldName).isEqualTo("customerName");
    }

    @Test
    void testNameRulesApplyConventionForConstant() {
        // When: Apply CONSTANT convention
        String constantName = NameRules.applyConvention("maxValue", NameRole.CONSTANT);

        // Then: Should convert to CONSTANT_CASE
        assertThat(constantName).isEqualTo("MAX_VALUE");
    }

    @Test
    void testNameRulesApplyConventionForPackage() {
        // When: Apply PACKAGE convention
        String pkgName = NameRules.applyConvention("MyPackage", NameRole.PACKAGE);

        // Then: Should convert to lowercase
        assertThat(pkgName).isEqualTo("mypackage");
    }

    @Test
    void testNameRulesIsConventionalClassName() {
        // When/Then: Should validate class names (starts with uppercase, valid identifier)
        assertThat(NameRules.isConventionalClassName("Customer")).isTrue();
        assertThat(NameRules.isConventionalClassName("CustomerEntity")).isTrue();
        assertThat(NameRules.isConventionalClassName("CUSTOMER")).isTrue(); // Valid, starts with uppercase

        assertThat(NameRules.isConventionalClassName("customer")).isFalse(); // Starts with lowercase
        assertThat(NameRules.isConventionalClassName("")).isFalse();
    }

    @Test
    void testNameRulesIsConventionalMemberName() {
        // When/Then: Should validate member names
        assertThat(NameRules.isConventionalMemberName("customerName")).isTrue();
        assertThat(NameRules.isConventionalMemberName("id")).isTrue();

        assertThat(NameRules.isConventionalMemberName("CustomerName")).isFalse();
        assertThat(NameRules.isConventionalMemberName("CUSTOMER_NAME")).isFalse();
    }

    @Test
    void testNameRulesIsConventionalConstantName() {
        // When/Then: Should validate constant names
        assertThat(NameRules.isConventionalConstantName("MAX_VALUE")).isTrue();
        assertThat(NameRules.isConventionalConstantName("DEFAULT_SIZE")).isTrue();

        assertThat(NameRules.isConventionalConstantName("maxValue")).isFalse();
        assertThat(NameRules.isConventionalConstantName("Max_Value")).isFalse();
    }

    @Test
    void testNameRulesIsConventionalPackageName() {
        // When/Then: Should validate package names
        assertThat(NameRules.isConventionalPackageName("com.example.domain")).isTrue();
        assertThat(NameRules.isConventionalPackageName("io.hexaglue.spi")).isTrue();

        assertThat(NameRules.isConventionalPackageName("com.Example.Domain")).isFalse();
        assertThat(NameRules.isConventionalPackageName("com.example.my_package"))
                .isFalse();
    }

    @Test
    void testNameRulesSuggestGetter() {
        // When: Suggest getter names
        String getter1 = NameRules.suggestGetter("name", "String");
        String getter2 = NameRules.suggestGetter("active", "boolean");
        String getter3 = NameRules.suggestGetter("enabled", "Boolean");

        // Then: Should suggest appropriate getter names
        assertThat(getter1).isEqualTo("getName");
        assertThat(getter2).isEqualTo("isActive");
        assertThat(getter3).isEqualTo("isEnabled");
    }

    @Test
    void testNameRulesSuggestSetter() {
        // When: Suggest setter name
        String setter = NameRules.suggestSetter("name");

        // Then: Should suggest setter name
        assertThat(setter).isEqualTo("setName");
    }

    @Test
    void testNameRulesSuggestBuilder() {
        // When: Suggest builder method name
        String builder = NameRules.suggestBuilder("name");

        // Then: Should use fluent style (same as field name)
        assertThat(builder).isEqualTo("name");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DefaultNameStrategy Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testDefaultNameStrategyBuilder() {
        // When: Build naming strategy
        DefaultNameStrategy strategy = DefaultNameStrategy.builder()
                .basePackage("com.example.infrastructure")
                .mirrorDomainPackages(false)
                .build();

        // Then: Should have correct base package
        assertThat(strategy.basePackage()).isEqualTo("com.example.infrastructure");
    }

    @Test
    void testDefaultNameStrategyOf() {
        // When: Create with factory method
        DefaultNameStrategy strategy = DefaultNameStrategy.of("com.example.infra");

        // Then: Should have base package
        assertThat(strategy.basePackage()).isEqualTo("com.example.infra");
    }

    @Test
    void testDefaultNameStrategyPackageNameWithoutMirroring() {
        // Given: Strategy without mirroring
        DefaultNameStrategy strategy = DefaultNameStrategy.builder()
                .basePackage("com.example.infrastructure")
                .mirrorDomainPackages(false)
                .build();

        // When: Generate package name
        String pkg = strategy.packageName("com.example.domain", NameRole.TYPE, "persistence");

        // Then: Should use base package with hint
        assertThat(pkg).isEqualTo("com.example.infrastructure.persistence");
    }

    @Test
    void testDefaultNameStrategyPackageNameWithMirroring() {
        // Given: Strategy with mirroring
        DefaultNameStrategy strategy = DefaultNameStrategy.builder()
                .basePackage("com.example.infrastructure")
                .mirrorDomainPackages(true)
                .build();

        // When: Generate package name
        String pkg = strategy.packageName("com.example.domain.customer", NameRole.TYPE, "persistence");

        // Then: Should mirror domain package
        assertThat(pkg).isEqualTo("com.example.domain.customer.persistence");
    }

    @Test
    void testDefaultNameStrategySimpleName() {
        // Given: Strategy
        DefaultNameStrategy strategy = DefaultNameStrategy.of("com.example.infra");

        // When: Generate simple name
        String name = strategy.simpleName("Customer", NameRole.TYPE, "Entity");

        // Then: Should append suffix
        assertThat(name).isEqualTo("CustomerEntity");
    }

    @Test
    void testDefaultNameStrategyMemberName() {
        // Given: Strategy
        DefaultNameStrategy strategy = DefaultNameStrategy.of("com.example.infra");

        // When: Generate member name
        String name = strategy.memberName("customer_name", NameRole.FIELD, null);

        // Then: Should apply camelCase convention
        assertThat(name).isEqualTo("customerName");
    }

    @Test
    void testDefaultNameStrategyQualifiedType() {
        // Given: Strategy
        DefaultNameStrategy strategy = DefaultNameStrategy.of("com.example.infrastructure");

        // When: Create qualified type name
        QualifiedName qn = strategy.qualifiedType("com.example.domain", "Customer", "persistence");

        // Then: Should combine package and simple name
        assertThat(qn.value()).isEqualTo("com.example.infrastructure.persistence.Customer");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NameStrategySpec Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void testNameStrategySpecSimpleFactory() {
        // When: Create simple naming strategy
        NameStrategySpec strategy = NameStrategySpec.simple("com.example.generated");

        // Then: Should have base package
        assertThat(strategy.basePackage()).isEqualTo("com.example.generated");
    }

    @Test
    void testNameStrategySpecSimplePackageName() {
        // Given: Simple strategy
        NameStrategySpec strategy = NameStrategySpec.simple("com.example.gen");

        // When: Generate package name
        String pkg = strategy.packageName(null, NameRole.PACKAGE, "custom");

        // Then: Should append segment
        assertThat(pkg).isEqualTo("com.example.gen.custom");
    }

    @Test
    void testNameStrategySpecSimpleSimpleName() {
        // Given: Simple strategy
        NameStrategySpec strategy = NameStrategySpec.simple("com.example.gen");

        // When: Generate simple name
        String name = strategy.simpleName("Customer", NameRole.TYPE, "Dto");

        // Then: Should append suffix
        assertThat(name).isEqualTo("CustomerDto");
    }

    @Test
    void testNameStrategySpecQualifiedTypeName() {
        // Given: Simple strategy
        NameStrategySpec strategy = NameStrategySpec.simple("com.example.gen");

        // When: Create qualified type name
        QualifiedName qn = strategy.qualifiedTypeName("com.example.gen.model", "Customer");

        // Then: Should combine package and name
        assertThat(qn.value()).isEqualTo("com.example.gen.model.Customer");
    }
}
