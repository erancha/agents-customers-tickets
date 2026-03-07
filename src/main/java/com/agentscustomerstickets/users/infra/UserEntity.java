package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "ix_users_username", columnList = "username", unique = true),
    @Index(name = "ix_users_agent_id", columnList = "agent_id")
})
class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false, length = 200)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Role role;

  @Column(name = "agent_id")
  private Long agentId;

  @Column(name = "full_name", nullable = false, length = 200)
  private String fullName;

  @Column(nullable = false, length = 200)
  private String email;

  Long getId() {
    return id;
  }

  void setId(Long id) {
    this.id = id;
  }

  String getUsername() {
    return username;
  }

  void setUsername(String username) {
    this.username = username;
  }

  String getPasswordHash() {
    return passwordHash;
  }

  void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  Role getRole() {
    return role;
  }

  void setRole(Role role) {
    this.role = role;
  }

  Long getAgentId() {
    return agentId;
  }

  void setAgentId(Long agentId) {
    this.agentId = agentId;
  }

  String getFullName() {
    return fullName;
  }

  void setFullName(String fullName) {
    this.fullName = fullName;
  }

  String getEmail() {
    return email;
  }

  void setEmail(String email) {
    this.email = email;
  }
}
