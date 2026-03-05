package com.customersupporthub.identity.infra;

import com.customersupporthub.identity.domain.Role;

record JwtPrincipal(Long userId, String username, Role role) {
}
