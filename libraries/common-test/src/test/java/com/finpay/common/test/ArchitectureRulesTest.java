package com.finpay.common.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    @Test
    void domain_rule_is_assembled() {
        var rule = ArchitectureRules.domainIsIndependentOfInfrastructure();
        assertThat(rule.getDescription())
                .contains("no classes")
                .contains("domain");
    }
}