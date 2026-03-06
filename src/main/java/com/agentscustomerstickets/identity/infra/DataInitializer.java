package com.agentscustomerstickets.identity.infra;

import com.agentscustomerstickets.identity.application.IdentityService;
import com.agentscustomerstickets.identity.domain.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DataInitializer {

  @Bean
  CommandLineRunner initDefaultAdmin(IdentityService identityService, UserRepository userRepository) {
    return args -> {
      if (!userRepository.existsByUsername("admin")) {
        identityService.createUser(
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
