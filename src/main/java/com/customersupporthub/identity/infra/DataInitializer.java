package com.customersupporthub.identity.infra;

import com.customersupporthub.identity.application.IdentityService;
import com.customersupporthub.identity.domain.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

  @Bean
  public CommandLineRunner initDefaultAdmin(IdentityService identityService, UserRepository userRepository) {
    return args -> {
      if (!userRepository.existsByUsername("admin")) {
        identityService.createUser(
            "admin",
            "admin123",
            Role.ADMIN,
            null,
            "System Admin",
            "admin@example.com"
        );
      }
    };
  }
}
