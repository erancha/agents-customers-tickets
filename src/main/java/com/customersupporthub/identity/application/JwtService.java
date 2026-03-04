package com.customersupporthub.identity.application;

import com.customersupporthub.identity.domain.Role;

public interface JwtService {
  String issueAccessToken(Long userId, String username, Role role);
}
