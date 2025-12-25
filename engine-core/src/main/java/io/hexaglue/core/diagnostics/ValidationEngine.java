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

/**
 * Engine for executing validation plans and collecting validation issues.
 *
 * <p>
 * The validation engine is responsible for:
 * <ul>
 *   <li>Executing validation rules in order</li>
 *   <li>Collecting all validation issues</li>
 *   <li>Handling errors during validation execution</li>
 *   <li>Providing execution statistics</li>
 * </ul>
 * </p>
 *
 * <h2>Execution Model</h2>
 * <p>
 * Rules are executed sequentially in the order they appear in the plan. If a rule throws an
 * exception, the engine records an internal error but continues executing remaining rules.
 * This fail-fast-per-rule, continue-with-plan approach ensures that validation is comprehensive
 * even when individual rules fail.
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * The engine itself is stateless and thread-safe. However, the {@link ValidationPlan} and
 * individual validation rules must be designed with thread safety in mind if they access
 * shared mutable state.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ValidationPlan plan = ValidationPlan.builder()
 *     .rule("domain-types", () -> validateDomainTypes())
 *     .rule("ports", () -> validatePorts())
 *     .build();
 *
 * ValidationResult result = ValidationEngine.execute(plan);
 *
 * if (result.hasIssues()) {
 *   List<ValidationIssue> issues = result.issues();
 *   // Convert to diagnostics and report
 * }
 * }</pre>
 */
public final class ValidationEngine {

    private ValidationEngine() {
        // utility class
    }

    /**
     * Executes a validation plan and returns the result.
     *
     * @param plan validation plan to execute (not {@code null})
     * @return validation result (never {@code null})
     */
    public static ValidationResult execute(ValidationPlan plan) {
        Objects.requireNonNull(plan, "plan");

        List<ValidationIssue> allIssues = new ArrayList<>();
        List<ValidationError> errors = new ArrayList<>();

        for (ValidationPlan.ValidationRule rule : plan.rules()) {
            try {
                List<ValidationIssue> issues = rule.execute();
                if (issues != null && !issues.isEmpty()) {
                    allIssues.addAll(issues);
                }
            } catch (Exception e) {
                // Record execution error but continue
                errors.add(new ValidationError(rule.name(), e));
            }
        }

        return new ValidationResult(allIssues, errors, plan.size());
    }

    /**
     * Result of executing a validation plan.
     *
     * <p>
     * The result contains:
     * <ul>
     *   <li>All validation issues collected from successful rule executions</li>
     *   <li>Execution errors that occurred during rule execution</li>
     *   <li>Execution statistics</li>
     * </ul>
     * </p>
     *
     * @param issues         all validation issues (never {@code null})
     * @param executionErrors errors during rule execution (never {@code null})
     * @param totalRules     total number of rules executed
     */
    public record ValidationResult(
            List<ValidationIssue> issues, List<ValidationError> executionErrors, int totalRules) {

        public ValidationResult {
            issues = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(issues, "issues")));
            executionErrors = Collections.unmodifiableList(
                    new ArrayList<>(Objects.requireNonNull(executionErrors, "executionErrors")));
        }

        /**
         * Returns {@code true} if any validation issues were found.
         *
         * @return {@code true} if issues are present
         */
        public boolean hasIssues() {
            return !issues.isEmpty();
        }

        /**
         * Returns {@code true} if any rules failed during execution.
         *
         * @return {@code true} if execution errors occurred
         */
        public boolean hasExecutionErrors() {
            return !executionErrors.isEmpty();
        }

        /**
         * Returns the number of successfully executed rules.
         *
         * @return count of successful rules
         */
        public int successfulRules() {
            return totalRules - executionErrors.size();
        }

        /**
         * Returns a summary string.
         *
         * @return summary (never {@code null})
         */
        public String summary() {
            return String.format(
                    "ValidationResult[issues=%d, executionErrors=%d, successfulRules=%d/%d]",
                    issues.size(), executionErrors.size(), successfulRules(), totalRules);
        }

        @Override
        public String toString() {
            return summary();
        }
    }

    /**
     * Records an error that occurred during validation rule execution.
     *
     * <p>
     * This type distinguishes between validation issues (which are expected findings) and
     * execution errors (which are unexpected failures in the validation logic itself).
     * </p>
     *
     * @param ruleName name of the failed rule (not blank)
     * @param cause    exception that caused the failure (not {@code null})
     */
    public record ValidationError(String ruleName, Throwable cause) {

        public ValidationError {
            Objects.requireNonNull(ruleName, "ruleName");
            if (ruleName.isBlank()) {
                throw new IllegalArgumentException("ruleName must not be blank");
            }
            Objects.requireNonNull(cause, "cause");
        }

        @Override
        public String toString() {
            return "ValidationError[rule=" + ruleName + ", cause="
                    + cause.getClass().getSimpleName() + ": " + cause.getMessage() + "]";
        }
    }
}
