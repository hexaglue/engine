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
package io.hexaglue.core.diagnostics;

import io.hexaglue.spi.diagnostics.ValidationIssue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A plan describing validation rules to be executed by the {@link ValidationEngine}.
 *
 * <p>
 * A validation plan is an ordered list of validation rules. Each rule is a supplier that
 * produces a list of {@link ValidationIssue} instances. Rules are executed sequentially,
 * and all issues are collected.
 * </p>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * The plan is intentionally immutable after construction. This design:
 * <ul>
 *   <li>Ensures validation rules are stable during execution</li>
 *   <li>Allows plans to be shared across threads safely</li>
 *   <li>Encourages explicit composition via the builder</li>
 * </ul>
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ValidationPlan plan = ValidationPlan.builder()
 *     .rule("domain-types", () -> validateDomainTypes())
 *     .rule("ports", () -> validatePorts())
 *     .rule("adapters", () -> validateAdapters())
 *     .build();
 *
 * List<ValidationIssue> issues = ValidationEngine.execute(plan);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Instances are immutable and thread-safe after construction.
 * </p>
 */
public final class ValidationPlan {

    private final List<ValidationRule> rules;

    private ValidationPlan(List<ValidationRule> rules) {
        this.rules = Collections.unmodifiableList(new ArrayList<>(rules));
    }

    /**
     * Creates a new empty builder.
     *
     * @return builder (never {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates an empty validation plan.
     *
     * @return empty plan (never {@code null})
     */
    public static ValidationPlan empty() {
        return new ValidationPlan(Collections.emptyList());
    }

    /**
     * Returns all validation rules in execution order.
     *
     * @return immutable list of rules (never {@code null})
     */
    public List<ValidationRule> rules() {
        return rules;
    }

    /**
     * Returns {@code true} if the plan contains no rules.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return rules.isEmpty();
    }

    /**
     * Returns the number of rules in this plan.
     *
     * @return rule count
     */
    public int size() {
        return rules.size();
    }

    @Override
    public String toString() {
        return "ValidationPlan[rules=" + rules.size() + "]";
    }

    /**
     * A validation rule within a plan.
     *
     * <p>
     * Each rule has:
     * <ul>
     *   <li>A unique name for debugging and logging</li>
     *   <li>A supplier that executes the validation and returns issues</li>
     * </ul>
     * </p>
     *
     * @param name unique rule name (not blank)
     * @param validator validation logic (not {@code null})
     */
    public record ValidationRule(String name, Supplier<List<ValidationIssue>> validator) {

        public ValidationRule {
            Objects.requireNonNull(name, "name");
            if (name.isBlank()) {
                throw new IllegalArgumentException("name must not be blank");
            }
            Objects.requireNonNull(validator, "validator");
        }

        /**
         * Executes the validation logic.
         *
         * @return validation issues (never {@code null}, may be empty)
         */
        public List<ValidationIssue> execute() {
            List<ValidationIssue> issues = validator.get();
            return (issues == null) ? Collections.emptyList() : issues;
        }
    }

    /**
     * Builder for {@link ValidationPlan}.
     */
    public static final class Builder {

        private final List<ValidationRule> rules = new ArrayList<>();

        private Builder() {}

        /**
         * Adds a validation rule to the plan.
         *
         * @param name      unique rule name (not blank)
         * @param validator validation logic (not {@code null})
         * @return this builder
         */
        public Builder rule(String name, Supplier<List<ValidationIssue>> validator) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(validator, "validator");
            rules.add(new ValidationRule(name, validator));
            return this;
        }

        /**
         * Adds a validation rule that produces a single issue or none.
         *
         * <p>
         * This is a convenience method for simple validators that produce at most one issue.
         * </p>
         *
         * @param name      unique rule name (not blank)
         * @param validator validation logic returning a single issue or {@code null}
         * @return this builder
         */
        public Builder singleIssueRule(String name, Supplier<ValidationIssue> validator) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(validator, "validator");

            return rule(name, () -> {
                ValidationIssue issue = validator.get();
                return (issue == null) ? Collections.emptyList() : List.of(issue);
            });
        }

        /**
         * Adds all rules from another plan.
         *
         * @param plan plan to merge (not {@code null})
         * @return this builder
         */
        public Builder addAll(ValidationPlan plan) {
            Objects.requireNonNull(plan, "plan");
            rules.addAll(plan.rules());
            return this;
        }

        /**
         * Builds the validation plan.
         *
         * @return immutable plan (never {@code null})
         */
        public ValidationPlan build() {
            return new ValidationPlan(rules);
        }
    }
}
