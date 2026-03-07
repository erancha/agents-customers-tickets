package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.CurrentUser;
import com.agentscustomerstickets.users.api.CurrentUserProvider;
import com.agentscustomerstickets.users.api.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

  /**
   * Resolves the authenticated user from Spring Security's context.
   *
   * Resolution order:
   * - prefer {@link JwtPrincipal} when the custom JWT converter populated it
   * - fallback to raw {@link Jwt} claims ({@code uid}, {@code role})
   * Throws {@link IllegalStateException} when authentication is missing, required claims are absent, or the principal type is unsupported.
   */
  @Override
  public CurrentUser get() {
    CurrentUser currentUser;

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null) {
      throw new IllegalStateException("No authenticated user");
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof JwtPrincipal jp) {
      currentUser = new CurrentUser(jp.userId(), jp.username(), jp.role());
    } else if (principal instanceof Jwt jwt) {
      Long uid = jwt.getClaim("uid");
      String roleStr = jwt.getClaim("role");
      if (uid == null || roleStr == null) {
        throw new IllegalStateException("JWT missing required claims");
      }
      currentUser = new CurrentUser(uid, jwt.getSubject(), Role.valueOf(roleStr));
    } else
      throw new IllegalStateException("Unsupported principal type");

    return currentUser;
  }
}
