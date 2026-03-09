package com.agentscustomerstickets.admin.events.web;

import com.agentscustomerstickets.users.api.Role;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP {@code CONNECT} frames with Bearer JWT and enforces admin role.
 * Also enforces admin role on inbound {@code SUBSCRIBE} and {@code SEND} frames.
 *
 * Declared as a bean and injected into {@code AdminEventsWebSocketConfig}
 * where it is registered on the inbound websocket channel.
 */
@Component
class AdminEventsWebSocketAuthInterceptor implements ChannelInterceptor {

   private static final Logger log = LoggerFactory.getLogger(AdminEventsWebSocketAuthInterceptor.class);
   private static final String ADMIN_ROLE = "ROLE_" + Role.ADMIN.name();

   private final JwtDecoder jwtDecoder;
   private final Function<Jwt, JwtAuthenticationToken> jwtAuthConverter;

   /**
    * Uses existing security beans from the HTTP resource-server setup.
    */
   AdminEventsWebSocketAuthInterceptor(
         JwtDecoder jwtDecoder,
         Function<Jwt, JwtAuthenticationToken> jwtAuthConverter) {
      this.jwtDecoder = jwtDecoder;
      this.jwtAuthConverter = jwtAuthConverter;
   }

   @Override
   public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
      StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
      if (accessor == null || accessor.getCommand() == null) {
         return message;
      }

      try {
         if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Authentication authentication = authenticate(accessor);
            assertAdmin(authentication);
            accessor.setUser(authentication);
            return message;
         }

         if (StompCommand.SUBSCRIBE.equals(accessor.getCommand()) || StompCommand.SEND.equals(accessor.getCommand())) {
            Principal user = accessor.getUser();
            if (!(user instanceof Authentication authentication)) {
               throw new InsufficientAuthenticationException("Authentication is required");
            }
            assertAdmin(authentication);
         }
      } catch (InsufficientAuthenticationException | AccessDeniedException ex) {
         log.info("WebSocket access denied: command={} destination={} user={} reason={}",
               accessor.getCommand(), accessor.getDestination(), principalName(accessor.getUser()), ex.getMessage());
         throw ex;
      }

      return message;
   }

   private static String principalName(Principal principal) {
      if (principal == null) {
         return "anonymous";
      }
      return principal.getName();
   }

   private Authentication authenticate(StompHeaderAccessor accessor) {
      String authorization = firstNativeHeader(accessor, "Authorization");
      if (authorization == null) {
         authorization = firstNativeHeader(accessor, "authorization");
      }
      if (authorization == null || !authorization.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
         throw new InsufficientAuthenticationException("Missing Bearer token in STOMP CONNECT headers");
      }

      String token = authorization.substring("Bearer ".length()).trim();
      if (token.isEmpty()) {
         throw new InsufficientAuthenticationException("Empty Bearer token");
      }

      try {
         Jwt jwt = jwtDecoder.decode(token);
         return jwtAuthConverter.apply(jwt);
      } catch (JwtException ex) {
         throw new InsufficientAuthenticationException("Invalid JWT token", ex);
      }
   }

   private static String firstNativeHeader(StompHeaderAccessor accessor, String name) {
      List<String> values = accessor.getNativeHeader(name);
      if (values == null || values.isEmpty()) {
         return null;
      }
      return values.getFirst();
   }

   private static void assertAdmin(Authentication authentication) {
      boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ADMIN_ROLE::equals);
      if (!isAdmin) {
         throw new AccessDeniedException("Admin role required for admin events websocket");
      }
   }
}
