package com.finpay.common.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretRedactorTest {

    @Test
    void masks_all_but_the_last_four_characters() {
        assertThat(SecretRedactor.mask("sk-super-secret-1234567890")).isEqualTo("****7890");
    }

    @Test
    void never_contains_the_raw_secret() {
        String raw = "sk-super-secret-1234567890";
        assertThat(SecretRedactor.mask(raw)).doesNotContain("sk-super-secret");
    }

    @Test
    void short_secrets_are_fully_masked() {
        assertThat(SecretRedactor.mask("abc")).isEqualTo("****");
        assertThat(SecretRedactor.mask("abcd")).isEqualTo("****");
    }

    @Test
    void blank_or_null_secrets_are_reported_as_unset() {
        assertThat(SecretRedactor.mask(null)).isEqualTo("(unset)");
        assertThat(SecretRedactor.mask("  ")).isEqualTo("(unset)");
    }
}