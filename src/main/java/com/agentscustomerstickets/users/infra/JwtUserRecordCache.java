package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.shared.error.ResourceNotFoundException;
import com.agentscustomerstickets.users.application.CurrentUserRecordProvider;
import com.agentscustomerstickets.users.api.User;
import com.agentscustomerstickets.users.api.UserDirectory;
import java.time.Instant;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Local in-memory cache that maps the current bearer JWT token value to a resolved User record.
 *
 * The cache is scoped to this application instance and is intended to avoid repeated user lookups during authenticated request flows that
 * need the full user record derived from the current JWT. Entries are retained only until the JWT expiration time.
 */
@Component
public class JwtUserRecordCache implements CurrentUserRecordProvider {

  private static final Logger log = LoggerFactory.getLogger(JwtUserRecordCache.class);

  private final UserDirectory userDirectory;
  private final boolean localJWTCacheEnabled;
  private final Map<String, CachedUserRecord> cache = new ConcurrentHashMap<>();

  JwtUserRecordCache(UserDirectory userDirectory, @Value("${users.local-jwt-cache.enabled:false}") boolean localJWTCacheEnabled) {
    this.userDirectory = userDirectory;
    this.localJWTCacheEnabled = localJWTCacheEnabled;
    log.info("Local JWT cache enabled={}", localJWTCacheEnabled);
  }

  /**
   * Resolves the current authenticated user's full record using the JWT from the security context. If the current token has already been
   * resolved and its cached entry has not expired, the cached User is returned. Otherwise the user is loaded from UserDirectory using the
   * JWT's uid claim and then cached until the token expires.
   *
   * @return the full authenticated user record for the current JWT
   * @throws IllegalStateException     if there is no JWT-authenticated user in the security context or if the JWT is missing the uid claim
   * @throws ResourceNotFoundException if the JWT refers to a user record that does not exist
   */
  @Override
  public @NonNull User getCurrentUserRecord() {
    JwtAuthenticationToken authentication = currentJwtAuthentication();
    Jwt jwt = authentication.getToken();
    Long userId = jwt.getClaim("uid");
    if (userId == null) {
      throw new IllegalStateException("JWT claim 'uid' is required");
    }

    if (!localJWTCacheEnabled) {
      return requireUser(userId);
    }

    String tokenValue = jwt.getTokenValue();
    Instant now = Instant.now();

    CachedUserRecord cached = cache.get(tokenValue);
    if (cached != null) {
      if (!cached.isExpiredAt(now)) {
        return cached.user();
      }
      cache.remove(tokenValue, cached);
    }

    User user = requireUser(userId);
    put(tokenValue, jwt.getExpiresAt(), user);
    return user;
  }

  private @NonNull User requireUser(@NonNull Long userId) {
    return Objects.requireNonNull(userDirectory.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")));
  }

  private void put(String tokenValue, Instant expiresAt, @NonNull User user) {
    if (expiresAt != null && expiresAt.isAfter(Instant.now())) {
      cache.put(tokenValue, new CachedUserRecord(user, expiresAt));
    }
  }

  private JwtAuthenticationToken currentJwtAuthentication() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
      return jwtAuthentication;
    }
    throw new IllegalStateException("No JWT-authenticated user");
  }

  private record CachedUserRecord(@NonNull User user, Instant expiresAt) {
    private boolean isExpiredAt(Instant instant) {
      return !expiresAt.isAfter(instant);
    }
  }
}
