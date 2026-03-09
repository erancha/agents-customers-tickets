package com.agentscustomerstickets.admin.events.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configures the admin websocket endpoint and in-memory topic broker.
 * Registered by Spring as messaging configuration and wires the inbound auth interceptor used for STOMP frames.
 */
@Configuration
@EnableWebSocketMessageBroker
class AdminEventsWebSocketConfig implements WebSocketMessageBrokerConfigurer {

   private final AdminEventsWebSocketAuthInterceptor authInterceptor;

   /**
    * Interceptor bean is injected by Spring and attached to inbound STOMP traffic.
    */
   AdminEventsWebSocketConfig(AdminEventsWebSocketAuthInterceptor authInterceptor) {
      this.authInterceptor = authInterceptor;
   }

   @Override
   public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
      registry.enableSimpleBroker("/topic");
      registry.setApplicationDestinationPrefixes("/app");
   }

   @Override
   public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
      registry.addEndpoint("/ws/admin-events").setAllowedOriginPatterns("*");
   }

   @Override
   public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
      registration.interceptors(authInterceptor);
   }
}
