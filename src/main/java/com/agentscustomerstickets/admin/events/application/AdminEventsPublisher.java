package com.agentscustomerstickets.admin.events.application;

import com.agentscustomerstickets.users.api.User;
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

   static final String ADMIN_EVENTS_TOPIC = "/topic/admin/events";

   private final SimpMessagingTemplate messagingTemplate;

   /**
    * {@code SimpMessagingTemplate} is provided by Spring messaging infrastructure.
    */
   AdminEventsPublisher(SimpMessagingTemplate messagingTemplate) {
      this.messagingTemplate = messagingTemplate;
   }

   public void publishAgentCreated(User user) {
      AgentCreatedPayload payload = new AgentCreatedPayload(
            user.id(), user.username(), user.fullName(), user.email());
      messagingTemplate.convertAndSend(ADMIN_EVENTS_TOPIC,
            AdminEventMessage.of(AdminEventType.AGENT_CREATED, payload));
   }

   public void publishCustomerCreated(User user) {
      CustomerCreatedPayload payload = new CustomerCreatedPayload(
            user.id(), user.username(), user.fullName(), user.email(), user.agentId());
      messagingTemplate.convertAndSend(ADMIN_EVENTS_TOPIC,
            AdminEventMessage.of(AdminEventType.CUSTOMER_CREATED, payload));
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
}
