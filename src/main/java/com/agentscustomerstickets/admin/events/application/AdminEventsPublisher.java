package com.agentscustomerstickets.admin.events.application;

import com.agentscustomerstickets.users.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes admin-facing realtime events to the websocket topic.
 *
 * Injected into application services that emit admin notifications
 * (currently {@code AgentsService} and {@code CustomerService}).
 */
@Service
public class AdminEventsPublisher {

      private static final Logger log = LoggerFactory.getLogger(AdminEventsPublisher.class);

      static final String ADMIN_EVENTS_TOPIC = "/topic/admin.events";

      private final SimpMessagingTemplate messagingTemplate;
      private final boolean cdcProfileActive;

      /**
       * {@code SimpMessagingTemplate} is provided by Spring messaging infrastructure.
       */
      AdminEventsPublisher(SimpMessagingTemplate messagingTemplate, Environment environment) {
            this.messagingTemplate = messagingTemplate;
            this.cdcProfileActive = environment.acceptsProfiles(Profiles.of("cdc"));
      }

      public void publishAgentCreated(User user) {
            if (cdcProfileActive) {
                  log.debug("Skipping direct AGENT_CREATED websocket publish because cdc profile is active");
                  return;
            }
            AgentCreatedPayload payload = new AgentCreatedPayload(
                        user.id(), user.username(), user.fullName(), user.email());
            messagingTemplate.convertAndSend(ADMIN_EVENTS_TOPIC,
                        AdminEventMessage.of(AdminEventType.AGENT_CREATED, payload));
      }

      public void publishCustomerCreated(User user) {
            if (cdcProfileActive) {
                  log.debug("Skipping direct CUSTOMER_CREATED websocket publish because cdc profile is active");
                  return;
            }
            CustomerCreatedPayload payload = new CustomerCreatedPayload(
                        user.id(), user.username(), user.fullName(), user.email(), user.agentId());
            messagingTemplate.convertAndSend(ADMIN_EVENTS_TOPIC,
                        AdminEventMessage.of(AdminEventType.CUSTOMER_CREATED, payload));
      }

      public void publishConsumedCdcKafkaMessage(String topic, String key, String payload) {
            CdcKafkaMessagePayload messagePayload = new CdcKafkaMessagePayload(topic, key, payload);
            messagingTemplate.convertAndSend(ADMIN_EVENTS_TOPIC,
                        AdminEventMessage.of(AdminEventType.CDC_KAFKA_MESSAGE_CONSUMED, messagePayload));
      }

      private record AgentCreatedPayload(Long id, String username, String fullName, String email) {
      }

      private record CustomerCreatedPayload(
                  Long id,
                  String username,
                  String fullName,
                  String email,
                  Long agentId) {
      }

      private record CdcKafkaMessagePayload(String topic, String key, String payload) {
      }
}
