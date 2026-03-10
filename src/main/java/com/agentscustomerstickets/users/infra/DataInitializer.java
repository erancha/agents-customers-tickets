package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.Role;
import com.agentscustomerstickets.shared.error.ConflictException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DataInitializer {

  @Bean
  // CommandLineRunner is fine for lightweight bootstrap, especially in dev/test,
  // but for production-critical initialization, migrations (e.g. Flyway) +
  // externalized secrets should be used.
  CommandLineRunner initDefaultAdmin(UsersService usersService, UserRepository userRepository) {
    return args -> {
      if (!userRepository.existsByUsername("admin")) {
        try {
          usersService.createUser(
              "admin",
              "admin123",
              Role.ADMIN,
              null,
              "System Admin",
              "admin@example.com");
        } catch (ConflictException ignored) {
        }
      }
    };
  }
}
