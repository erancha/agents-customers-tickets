package com.agentscustomerstickets.identity.api;

import org.springframework.lang.NonNull;

import com.agentscustomerstickets.identity.domain.Role;

public record CurrentUser(@NonNull Long id, String username, Role role) {
}
