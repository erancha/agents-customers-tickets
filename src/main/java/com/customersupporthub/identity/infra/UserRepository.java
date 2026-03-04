package com.customersupporthub.identity.infra;

import com.customersupporthub.identity.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByUsername(String username);

  boolean existsByUsername(String username);

  List<UserEntity> findAllByAgentId(Long agentId);

  List<UserEntity> findAllByRole(Role role);
}
