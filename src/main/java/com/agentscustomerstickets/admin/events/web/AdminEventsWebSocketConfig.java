package com.agentscustomerstickets.admin.events.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
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

   private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AdminEventsWebSocketConfig.class);

   private final AdminEventsWebSocketAuthInterceptor authInterceptor;
   private final boolean relayEnabled;
   private final String relayHost;
   private final int relayPort;
   private final String relayClientLogin;
   private final String relayClientPasscode;
   private final String relaySystemLogin;
   private final String relaySystemPasscode;

   /**
    * Interceptor bean is injected by Spring and attached to inbound STOMP traffic.
    */
   AdminEventsWebSocketConfig(
         AdminEventsWebSocketAuthInterceptor authInterceptor,
         @Value("${websocket.broker.relay.enabled:false}") boolean relayEnabled,
         @Value("${websocket.broker.relay.host:localhost}") String relayHost,
         @Value("${websocket.broker.relay.port:61613}") int relayPort,
         @Value("${websocket.broker.relay.client-login:guest}") String relayClientLogin,
         @Value("${websocket.broker.relay.client-passcode:guest}") String relayClientPasscode,
         @Value("${websocket.broker.relay.system-login:guest}") String relaySystemLogin,
         @Value("${websocket.broker.relay.system-passcode:guest}") String relaySystemPasscode) {
      this.authInterceptor = authInterceptor;
      this.relayEnabled = relayEnabled;
      this.relayHost = relayHost;
      this.relayPort = relayPort;
      this.relayClientLogin = relayClientLogin;
      this.relayClientPasscode = relayClientPasscode;
      this.relaySystemLogin = relaySystemLogin;
      this.relaySystemPasscode = relaySystemPasscode;
   }

   @Override
   public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
      if (relayEnabled) {
         log.info("Admin websocket broker mode: relay ({}:{})", relayHost, relayPort);
         registry.enableStompBrokerRelay("/topic")
               .setRelayHost(relayHost)
               .setRelayPort(relayPort)
               .setClientLogin(relayClientLogin)
               .setClientPasscode(relayClientPasscode)
               .setSystemLogin(relaySystemLogin)
               .setSystemPasscode(relaySystemPasscode)
               .setSystemHeartbeatSendInterval(10000)
               .setSystemHeartbeatReceiveInterval(10000);
      } else {
         log.info("Admin websocket broker mode: simple (in-memory)");
         // STOMP heartbeat policy for the in-memory broker.
         // Format: setHeartbeatValue([serverOutgoingMs, serverIncomingMs]).
         // - serverOutgoingMs=10000: when the connection is idle, Spring's STOMP broker sends heartbeat bytes to the client about every 10s.
         // - serverIncomingMs=10000: the broker requests/monitors client heartbeats at ~10s.
         //   If negotiated and then missing for long enough, the session is considered dead.
         // Heartbeats are protocol-level frames sent by STOMP infrastructure (not app code),
         // and periodic traffic helps intermediaries like nginx avoid idle timeout disconnects.
         registry.enableSimpleBroker("/topic")
               .setHeartbeatValue(new long[] { 10000, 10000 })
               .setTaskScheduler(heartbeatTaskScheduler());
      }
      registry.setApplicationDestinationPrefixes("/app");
   }

   @Bean
   @NonNull
   TaskScheduler heartbeatTaskScheduler() {
      // Scheduler used by the simple broker to emit heartbeat frames on a timer.
      ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
      scheduler.setPoolSize(1);
      scheduler.setThreadNamePrefix("ws-heartbeat-");
      scheduler.initialize();
      return scheduler;
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
