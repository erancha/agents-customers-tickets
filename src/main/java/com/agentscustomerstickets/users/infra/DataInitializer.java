package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DataInitializer {

  @Bean
  CommandLineRunner initDefaultAdmin(UsersService usersService, UserRepository userRepository) {
    return args -> {
      if (!userRepository.existsByUsername("admin")) {
        usersService.createUser(
            "admin",
            "admin123",
            Role.ADMIN,
            null,
            "System Admin",
            "admin@example.com");
      }
    };
  }
}
