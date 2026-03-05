package com.customersupporthub.identity.infra;

import com.customersupporthub.identity.application.JwtService;
import com.customersupporthub.identity.domain.Role;
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

  public NimbusJwtService(JwtEncoder jwtEncoder, JwtProperties props) {
    this.jwtEncoder = jwtEncoder;
    this.props = props;
  }

  @Override
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

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}
