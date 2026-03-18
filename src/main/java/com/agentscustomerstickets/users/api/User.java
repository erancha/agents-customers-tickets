package com.agentscustomerstickets.users.api;

import java.io.Serializable;
import org.springframework.lang.NonNull;

/**
 * DTO used across users public APIs to expose user data without leaking internal entities.
 */
public record User(@NonNull Long id, @NonNull String username, @NonNull Role role, Long agentId, @NonNull String fullName,
        @NonNull String email) implements Serializable {
}
