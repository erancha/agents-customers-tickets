package com.agentscustomerstickets.admin.events.application;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes CDC messages from Kafka when running with profile {@code cdc}
 * and forwards each consumed record to admin websocket subscribers.
 */
@Component
@Profile("cdc")
class AdminKafkaCdcConsumer {

   private static final Logger log = LoggerFactory.getLogger(AdminKafkaCdcConsumer.class);

   private final AdminEventsPublisher adminEventsPublisher;

   AdminKafkaCdcConsumer(AdminEventsPublisher adminEventsPublisher) {
      this.adminEventsPublisher = adminEventsPublisher;
   }

   @KafkaListener(id = "admin-cdc-consumer", topicPattern = "${admin.events.kafka.topic-pattern:my.agentscustomerstickets_db.*}")
   void consume(ConsumerRecord<String, String> record) {
      log.debug("Consumed CDC Kafka message: topic={} partition={} offset={}",
            record.topic(), record.partition(), record.offset());
      adminEventsPublisher.publishConsumedCdcKafkaMessage(
            record.topic(),
            record.key(),
            record.value());
   }
}
