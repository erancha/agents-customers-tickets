package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<UserEntity, Long> {
  Optional<UserEntity> findByUsername(String username);

  boolean existsByUsername(String username);

  List<UserEntity> findAllByAgentId(Long agentId);

  List<UserEntity> findAllByRole(Role role);
}
