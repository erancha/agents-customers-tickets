package com.customersupporthub.identity.application;

import com.customersupporthub.identity.domain.Role;
import com.customersupporthub.identity.infra.UserEntity;
import com.customersupporthub.identity.infra.UserRepository;
import com.customersupporthub.shared.error.ConflictException;
import com.customersupporthub.shared.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {

  private final UserRepository userRepository;
  private final PasswordHasher passwordHasher;

  public IdentityService(UserRepository userRepository, PasswordHasher passwordHasher) {
    this.userRepository = userRepository;
    this.passwordHasher = passwordHasher;
  }

  @Transactional(readOnly = true)
  public UserEntity requireUser(Long id) {
    return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  @Transactional(readOnly = true)
  public UserEntity requireByUsername(String username) {
    return userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));
  }

  @Transactional
  public UserEntity createUser(String username, String rawPassword, Role role, Long agentId, String fullName, String email) {
    if (userRepository.existsByUsername(username)) {
      throw new ConflictException("Username already exists");
    }
    UserEntity u = new UserEntity();
    u.setUsername(username);
    u.setPasswordHash(passwordHasher.hash(rawPassword));
    u.setRole(role);
    u.setAgentId(agentId);
    u.setFullName(fullName);
    u.setEmail(email);
    return userRepository.save(u);
  }

  @Transactional(readOnly = true)
  public UserEntity authenticate(String username, String rawPassword) {
    UserEntity u = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password"));

    if (!passwordHasher.matches(rawPassword, u.getPasswordHash())) {
      throw new IllegalArgumentException("Invalid username or password");
    }
    return u;
  }

  @Transactional
  public UserEntity updateProfile(Long userId, String fullName, String email) {
    UserEntity u = requireUser(userId);
    u.setFullName(fullName);
    u.setEmail(email);
    return userRepository.save(u);
  }
}
