package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserManagement;
import com.agentscustomerstickets.users.api.Role;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements {@link UserManagement} via {@link UsersService}.
 * <p>Converts managed {@link UserEntity} instances to API-facing {@link User} DTOs.</p>
 */
@Component
@ConditionalOnProperty(name = "users.integration.mode", havingValue = "embedded", matchIfMissing = true)
class UserManagementAdapter implements UserManagement {

   private final UsersService usersService;

   UserManagementAdapter(UsersService usersService) {
      this.usersService = usersService;
   }

   @Override
   @Transactional
   public User createUser(
         @NonNull String username,
         @NonNull String rawPassword,
         @NonNull Role role,
         Long agentId,
         @NonNull String fullName,
         @NonNull String email) {
      UserEntity created = usersService.createUser(username, rawPassword, role, agentId, fullName, email);
      return toUser(created);
   }

   @Override
   @Transactional
   public User updateProfile(
         @NonNull Long userId,
         @NonNull String fullName,
         @NonNull String email) {
      UserEntity updated = usersService.updateProfile(userId, fullName, email);
      return toUser(updated);
   }

   private User toUser(UserEntity userEntity) {
      return new User(
            userEntity.getId(),
            userEntity.getUsername(),
            userEntity.getRole(),
            userEntity.getAgentId(),
            userEntity.getFullName(),
            userEntity.getEmail());
   }
}
