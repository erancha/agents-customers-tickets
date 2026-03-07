package com.agentscustomerstickets.users.application;

import com.agentscustomerstickets.users.api.Role;

public interface JwtService {
  String issueAccessToken(Long userId, String username, Role role);
}
