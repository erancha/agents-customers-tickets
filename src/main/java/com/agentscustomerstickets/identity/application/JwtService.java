package com.agentscustomerstickets.identity.application;

import com.agentscustomerstickets.identity.domain.Role;

public interface JwtService {
  String issueAccessToken(Long userId, String username, Role role);
}
