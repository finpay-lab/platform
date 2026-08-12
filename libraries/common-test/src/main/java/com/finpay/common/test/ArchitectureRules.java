package com.finpay.common.test;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Shared FinPay architecture rules (AGENTS.md). Exposed as reusable rule
 * objects so every service imports the same checks instead of re-implementing
 * them; later phases add Testcontainers base classes here too.
 */
public final class ArchitectureRules {

    private ArchitectureRules() {
    }

    /** Rule 4: domain logic must not depend on infrastructure frameworks. */
    public static ArchRule domainIsIndependentOfInfrastructure() {
        return ArchRuleDefinition.noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.hibernate..",
                        "org.apache.kafka..");
    }
}