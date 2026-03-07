package com.agentscustomerstickets.users.api;

import org.springframework.lang.NonNull;

/**
 * Write-side public contract for user management operations in the users module.
 * <p>Other modules should create or update users through this API, not through users services or repositories.</p>
 */
public interface UserManagement {

      /**
       * Creates a new user account.
       */
      User createUser(
                  @NonNull String username,
                  @NonNull String rawPassword,
                  @NonNull Role role,
                  Long agentId,
                  @NonNull String fullName,
                  @NonNull String email);

      /**
       * Updates profile fields for an existing user.
       */
      User updateProfile(
                  @NonNull Long userId,
                  @NonNull String fullName,
                  @NonNull String email);
}
