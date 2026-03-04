package com.customersupporthub.identity.infra;

import com.customersupporthub.identity.domain.Role;

public record JwtPrincipal(Long userId, String username, Role role) {
}
