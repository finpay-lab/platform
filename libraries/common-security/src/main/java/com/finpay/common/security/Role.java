package com.finpay.common.security;

import java.util.Collection;
import java.util.List;

/** Roles used across FinPay services (RBAC, SECURITY.md). */
public enum Role {
    CUSTOMER,
    OPERATOR,
    AUDITOR,
    ADMIN,
    SERVICE;

    public static List<String> authorities(Collection<Role> roles) {
        return roles.stream().map(r -> "ROLE_" + r.name()).toList();
    }
}
