package com.agentscustomerstickets.identity.infra;

import org.springframework.lang.NonNull;

import com.agentscustomerstickets.identity.domain.Role;

record JwtPrincipal(@NonNull Long userId, String username, Role role) {
}
