package com.agentscustomerstickets.users.infra;

import com.agentscustomerstickets.users.api.Role;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
class SecurityConfig {

  private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

  /**
   * Configures stateless API security for the application.
   *
   * Access rules:
   * - allow token issuance at {@code POST /api/auth/token}
   * - allow health/status endpoints ({@code GET /}, {@code GET /health}, {@code /actuator/**})
   * - require JWT authentication for every other request
   * The provided converter maps validated JWT claims into the application's
   * {@link JwtAuthenticationToken} shape.
   */
  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      Function<Jwt, JwtAuthenticationToken> jwtAuthConverter) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/auth/token").permitAll()
            .requestMatchers("/ws/admin-events/**").permitAll()
            .requestMatchers("/internal/users/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/", "/health").permitAll()
            .requestMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()) // require JWT authentication for every other request
        .oauth2ResourceServer(oauth -> oauth
            .jwt(jwt -> jwt.jwtAuthenticationConverter(new JwtAuthenticationConverter() {
              {
                setJwtGrantedAuthoritiesConverter(j -> jwtAuthConverter.apply(j).getAuthorities());
              }
            })))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(authenticationFailedEntryPoint())
            .accessDeniedHandler(accessDeniedHandler()))
        .addFilterAfter(securityFlowLoggingFilter(), AuthorizationFilter.class)
        .httpBasic(Customizer.withDefaults());

    return http.build();
  }

  @Bean
  AuthenticationEntryPoint authenticationFailedEntryPoint() {
    return (request, response, authException) -> {
      log.info("Authentication failed: method={} path={} reason={}",
          request.getMethod(), request.getRequestURI(), authException.getMessage());
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    };
  }

  @Bean
  AccessDeniedHandler accessDeniedHandler() {
    AccessDeniedHandlerImpl delegate = new AccessDeniedHandlerImpl();
    return (request, response, accessDeniedException) -> {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      log.info("Authorization denied: method={} path={} user={} authorities={} reason={}",
          request.getMethod(), request.getRequestURI(), principalName(auth), authorityNames(auth),
          accessDeniedException.getMessage());
      delegate.handle(request, response, accessDeniedException);
    };
  }

  @Bean
  OncePerRequestFilter securityFlowLoggingFilter() {
    return new OncePerRequestFilter() {
      @Override
      protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain) throws ServletException, IOException {
        Authentication before = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Security flow start: method={} path={} user={} authorities={}",
            request.getMethod(), request.getRequestURI(), principalName(before), authorityNames(before));

        filterChain.doFilter(request, response);

        Authentication after = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Security flow end: method={} path={} status={} user={} authorities={}",
            request.getMethod(), request.getRequestURI(), response.getStatus(), principalName(after),
            authorityNames(after));
      }
    };
  }

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
  @Bean
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
      log.debug("JWT authenticated: subject={} uid={} role={}", jwt.getSubject(), uid, role);

      // Keep the default token behavior while exposing the typed principal.
      return new JwtAuthenticationToken(jwt, authorities, principal.username()) {
        @Override
        public Object getPrincipal() {
          return principal;
        }
      };
    };
  }

  private static String principalName(Authentication auth) {
    if (auth == null) {
      return "anonymous";
    }
    return auth.getName();
  }

  private static String authorityNames(Authentication auth) {
    if (auth == null || auth.getAuthorities() == null) {
      return "[]";
    }
    return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList().toString();
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
