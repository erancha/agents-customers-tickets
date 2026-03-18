package com.agentscustomerstickets.users.infra;

import static com.agentscustomerstickets.users.infra.RedisCacheConfig.USERS_CACHE;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserManagement;
import com.agentscustomerstickets.users.api.Role;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements UserManagement via UsersService.
 * Converts managed UserEntity instances to API-facing User DTOs.
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
   public User createUser(@NonNull String username, @NonNull String rawPassword, @NonNull Role role, Long agentId, @NonNull String fullName,
         @NonNull String email) {
      UserEntity created = usersService.createUser(username, rawPassword, role, agentId, fullName, email);
      return toUser(created);
   }

   /**
    * Updates the profile information for a user and returns the updated User DTO.
    * Caching: This method uses Spring's CacheEvict annotation:
    *   - value = USERS_CACHE: Evicts the cache entry from the cache defined by RedisCacheConfig.USERS_CACHE.
    *   - key = "#userId": Evicts the cache entry corresponding to the given user ID.
    * The method is also marked @Transactional to ensure the update is performed within a transaction.
    * @param userId the unique user ID
    * @param fullName the new full name for the user
    * @param email the new email address for the user
    * @return the updated User DTO
    */
   @Override
   @CacheEvict(value = USERS_CACHE, key = "#userId")
   @Transactional
   public User updateProfile(@NonNull Long userId, @NonNull String fullName, @NonNull String email) {
      UserEntity updated = usersService.updateProfile(userId, fullName, email);
      return toUser(updated);
   }

   private User toUser(UserEntity userEntity) {
      return new User(userEntity.getId(), userEntity.getUsername(), userEntity.getRole(), userEntity.getAgentId(), userEntity.getFullName(),
            userEntity.getEmail());
   }
}
