package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Override
  public String hash(String raw) {
    return encoder.encode(raw);
  }

  @Override
  public boolean matches(String raw, String hashed) {
    return encoder.matches(raw, hashed);
  }
}
