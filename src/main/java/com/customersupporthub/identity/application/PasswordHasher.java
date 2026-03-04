package com.customersupporthub.identity.application;

public interface PasswordHasher {
  String hash(String raw);

  boolean matches(String raw, String hashed);
}
