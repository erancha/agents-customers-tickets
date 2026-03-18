package com.agentscustomerstickets.users.infra;

import static com.agentscustomerstickets.users.infra.RedisCacheConfig.USERS_CACHE;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.Role;

import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements UserDirectory using the users persistence layer.
 *
 * Maps UserEntity records into API-facing User DTOs for read operations.
 */
@Component
@ConditionalOnProperty(name = "users.integration.mode", havingValue = "embedded", matchIfMissing = true)
class UserDirectoryAdapter implements UserDirectory {

   private final UserRepository userRepository;

   UserDirectoryAdapter(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   /**
    * Finds a user by their unique ID, mapping the result to a User DTO if present.
    * Caching: This method is cached using Spring's Cacheable annotation:
    *   - value = USERS_CACHE: Uses the cache defined by RedisCacheConfig.USERS_CACHE.
    *   - key = "#id": Caches results by the user ID parameter.
    *   - Empty Optional results are not cached (Spring unwraps Optional automatically).
    * The method is also marked @Transactional(readOnly = true) for read-only transactional context.
    * @param id the unique user ID
    * @return an Optional containing the user if found, or empty if not found
    */
   @Override
   @Cacheable(value = USERS_CACHE, key = "#id")
   @Transactional(readOnly = true)
   public Optional<User> findById(@NonNull Long id) {
      return userRepository.findById(id).map(this::toUser);
   }

   @Override
   @Transactional(readOnly = true)
   public List<User> findAllByAgentId(@NonNull Long agentId) {
      return userRepository.findAllByAgentId(agentId).stream().map(this::toUser).toList();
   }

   @Override
   @Transactional(readOnly = true)
   public List<User> findAllByRole(@NonNull Role role) {
      return userRepository.findAllByRole(role).stream().map(this::toUser).toList();
   }

   private User toUser(UserEntity userEntity) {
      return new User(userEntity.getId(), userEntity.getUsername(), userEntity.getRole(), userEntity.getAgentId(), userEntity.getFullName(),
            userEntity.getEmail());
   }
}
