package com.agentscustomerstickets.users.api;

import org.springframework.lang.NonNull;

/**
 * DTO describing the authenticated principal extracted from the security context.
 */
public record CurrentUser(@NonNull Long id, String username, Role role) {
}
