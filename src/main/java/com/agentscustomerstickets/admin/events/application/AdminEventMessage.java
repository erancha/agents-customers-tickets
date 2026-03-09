package com.agentscustomerstickets.admin.events.application;

import java.time.Instant;

record AdminEventMessage(String type, Instant at, Object payload) {
   static AdminEventMessage of(AdminEventType type, Object payload) {
      return new AdminEventMessage(type.name(), Instant.now(), payload);
   }
}
