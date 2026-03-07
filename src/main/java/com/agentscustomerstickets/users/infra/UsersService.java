package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.application.PasswordHasher;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ConflictException;
import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsersService {

   private final UserRepository userRepository;
   private final PasswordHasher passwordHasher;

   UsersService(UserRepository userRepository, PasswordHasher passwordHasher) {
      this.userRepository = userRepository;
      this.passwordHasher = passwordHasher;
   }

   @Transactional(readOnly = true)
   public UserEntity requireUser(@NonNull Long id) {
      return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
   }

   @Transactional(readOnly = true)
   UserEntity requireByUsername(String username) {
      return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
   }

   @Transactional
   UserEntity createUser(String username, String rawPassword, Role role, Long agentId, String fullName,
         String email) {
      if (userRepository.existsByUsername(username)) {
         throw new ConflictException("Username already exists");
      }
      UserEntity user = new UserEntity();
      user.setUsername(username);
      user.setPasswordHash(passwordHasher.hash(rawPassword));
      user.setRole(role);
      user.setAgentId(agentId);
      user.setFullName(fullName);
      user.setEmail(email);
      return userRepository.save(user);
   }

   @Transactional(readOnly = true)
   public User authenticate(String username, String rawPassword) {
      UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

      if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
         throw new IllegalArgumentException("Invalid username or password");
      }
      return new User(
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.getAgentId(),
            user.getFullName(),
            user.getEmail());
   }

   @Transactional
   public UserEntity updateProfile(@NonNull Long userId, String fullName, String email) {
      UserEntity user = requireUser(userId);
      user.setFullName(fullName);
      user.setEmail(email);
      return userRepository.save(user);
   }
}
