package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import com.agentscustomerstickets.users.api.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Infrastructure adapter that implements {@link UserDirectory} using the users persistence layer.
 * <p>Maps {@link UserEntity} records into API-facing {@link User} DTOs for read operations.</p>
 */
@Component
@ConditionalOnProperty(name = "users.integration.mode", havingValue = "embedded", matchIfMissing = true)
class UserDirectoryAdapter implements UserDirectory {

   private final UserRepository userRepository;

   UserDirectoryAdapter(UserRepository userRepository) {
      this.userRepository = userRepository;
   }

   @Override
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
      return new User(
            userEntity.getId(),
            userEntity.getUsername(),
            userEntity.getRole(),
            userEntity.getAgentId(),
            userEntity.getFullName(),
            userEntity.getEmail());
   }
}
