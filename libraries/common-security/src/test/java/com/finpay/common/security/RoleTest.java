package com.finpay.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.List;

class RoleTest {

    @Test
    void maps_roles_to_spring_authority_names() {
        assertThat(Role.authorities(List.of(Role.CUSTOMER, Role.ADMIN)))
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }
}
