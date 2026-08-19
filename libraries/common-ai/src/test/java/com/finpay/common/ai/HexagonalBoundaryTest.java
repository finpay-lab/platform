package com.finpay.common.ai;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * AGENTS.md Rule 4: the ports (com.finpay.common.ai) and the resilience
 * decorator (com.finpay.common.ai.resilience) must stay free of infrastructure
 * frameworks. Only the implementation package (com.finpay.common.ai.infrastructure)
 * may depend on Spring / SLF4J / servlet.
 */
@AnalyzeClasses(packages = "com.finpay.common.ai")
class HexagonalBoundaryTest {

    @ArchTest
    static final ArchRule ports_and_resilience_are_free_of_infrastructure_frameworks = noClasses()
            .that()
            .resideInAPackage("com.finpay.common.ai")
            .or()
            .resideInAPackage("com.finpay.common.ai.resilience..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "org.slf4j..", "jakarta.servlet..");
}