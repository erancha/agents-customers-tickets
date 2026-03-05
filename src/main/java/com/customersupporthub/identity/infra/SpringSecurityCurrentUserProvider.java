package com.customersupporthub.identity.infra;

import com.customersupporthub.identity.api.CurrentUser;
import com.customersupporthub.identity.api.CurrentUserProvider;
import com.customersupporthub.identity.domain.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
class SpringSecurityCurrentUserProvider implements CurrentUserProvider {

  @Override
  public CurrentUser get() {
    CurrentUser currentUser = null;

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
    } else throw new IllegalStateException("Unsupported principal type");

    return currentUser;
  }
}
