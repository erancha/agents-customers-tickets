package com.agentscustomerstickets.identity.infra;

import com.agentscustomerstickets.identity.domain.Role;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      Function<Jwt, JwtAuthenticationToken> jwtAuthConverter) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
            .requestMatchers(HttpMethod.GET, "/", "/health").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthenticationConverter() {
              {
                setJwtGrantedAuthoritiesConverter(j -> jwtAuthConverter.apply(j).getAuthorities());
              }
            })))
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  /**
   * Converts a validated JWT into the authentication shape used by the app.
   *
   * Expected claims:
   * - uid: numeric internal user id
   * - role: enum value from {@link Role} used to build ROLE_* authorities
   *
   * The returned token exposes a {@link JwtPrincipal} via {@code getPrincipal()}
   * so controllers/services can access typed user context instead of raw claims.
   */
  Function<Jwt, JwtAuthenticationToken> jwtAuthConverter() {
    return jwt -> {
      Long uid = jwt.getClaim("uid");
      if (uid == null) {
        throw new IllegalArgumentException("JWT claim 'uid' is required");
      }

      String roleStr = jwt.getClaim("role");
      Role role = Role.valueOf(roleStr);

      // Spring Security checks roles through authorities with the ROLE_ prefix.
      Collection<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
      JwtPrincipal principal = new JwtPrincipal(uid, jwt.getSubject(), role);

      // Keep the default token behavior while exposing the typed principal.
      return new JwtAuthenticationToken(jwt, authorities, principal.username()) {
        @Override
        public Object getPrincipal() {
          return principal;
        }
      };
    };
  }

  @Bean
  JwtDecoder jwtDecoder(JwtProperties props) {
    SecretKey key = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }

  @Bean
  JwtEncoder jwtEncoder(JwtProperties props) {
    SecretKey key = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }
}
