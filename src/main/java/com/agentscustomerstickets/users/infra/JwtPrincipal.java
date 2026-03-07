package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.Role;
import org.springframework.lang.NonNull;

record JwtPrincipal(@NonNull Long userId, String username, Role role) {
}
