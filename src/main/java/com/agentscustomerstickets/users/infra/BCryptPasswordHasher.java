package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.application.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  /**
   * Hashes the provided raw password using BCrypt.
   *
   * @param raw the raw password to hash
   * @return the hashed password
   */
  @Override
  public String hash(String raw) {
    return encoder.encode(raw);
  }

  /**
   * Verifies that the raw password matches the hashed password.
   *
   * The raw password is not hashed by the caller. Instead, the encoder internally hashes the raw password
   * and compares it to the provided hashed value. If the hash of the raw password matches the hashed value,
   * this method returns true.
   *
   * @param raw the raw (plain text) password to verify
   * @param hashed the previously hashed password
   * @return true if the raw password matches the hashed password, false otherwise
   */
  @Override
  public boolean matches(String raw, String hashed) {
    return encoder.matches(raw, hashed);
  }
}
