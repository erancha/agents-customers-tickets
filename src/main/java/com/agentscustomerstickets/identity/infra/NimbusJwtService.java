package com.agentscustomerstickets.identity.infra;

import com.agentscustomerstickets.identity.application.JwtService;
import com.agentscustomerstickets.identity.domain.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

@Component
class NimbusJwtService implements JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties props;

  NimbusJwtService(JwtEncoder jwtEncoder, JwtProperties props) {
    this.jwtEncoder = jwtEncoder;
    this.props = props;
  }

  @Override
  /**
   * Issues a signed JWT access token for the authenticated user.
   *
   * Claims embedded in the token:
   * - sub: username (subject)
   * - uid: internal numeric user id
   * - role: role name used by security mapping
   * - scope: API scope list
   * - iss/iat/exp: issuer, issued-at, and expiration metadata
   *
   * Tamper protection:
   * - The token is JWS-signed (not encrypted) with HS256 (HMAC-SHA256).
   * - Signing uses the shared secret configured in {@link JwtProperties} via the configured {@link JwtEncoder} bean.
   * - Any payload/header change invalidates the signature and token verification fails.
   */
  public String issueAccessToken(Long userId, String username, Role role) {
    Instant now = Instant.now();
    Instant exp = now.plus(props.accessTokenMinutes(), ChronoUnit.MINUTES);

    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(props.issuer())
        .issuedAt(now)
        .expiresAt(exp)
        .subject(username)
        .claim("uid", userId)
        .claim("role", role.name())
        .claim("scope", List.of("api"))
        .build();

    // HS256 creates a signature over header+claims, allowing the server to detect
    // tampering.
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
