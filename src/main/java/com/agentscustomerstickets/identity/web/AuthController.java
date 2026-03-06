package com.agentscustomerstickets.identity.web;

import com.agentscustomerstickets.identity.application.IdentityService;
import com.agentscustomerstickets.identity.application.JwtService;
import com.agentscustomerstickets.identity.infra.UserEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Validated
class AuthController {

  private final IdentityService identityService;
  private final JwtService jwtService;

  @Value("${security.jwt.accessTokenMinutes:60}")
  private long accessTokenMinutes;

  AuthController(IdentityService identityService, JwtService jwtService) {
    this.identityService = identityService;
    this.jwtService = jwtService;
  }

  record TokenRequest(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(max = 200) String password) {
  }

  record TokenResponse(
      String access_token,
      String token_type,
      long expires_in,
      Instant issued_at) {
  }

  @PostMapping("/token")
  ResponseEntity<TokenResponse> token(@Valid @RequestBody TokenRequest req) {
    UserEntity u = identityService.authenticate(req.username(), req.password());
    String token = jwtService.issueAccessToken(u.getId(), u.getUsername(), u.getRole());
    return ResponseEntity.ok(new TokenResponse(token, "Bearer", accessTokenMinutes * 60, Instant.now()));
  }
}
