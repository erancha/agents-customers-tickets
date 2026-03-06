package com.customersupporthub.identity.infra;

import org.springframework.lang.NonNull;

import com.customersupporthub.identity.domain.Role;

record JwtPrincipal(@NonNull Long userId, String username, Role role) {
}
