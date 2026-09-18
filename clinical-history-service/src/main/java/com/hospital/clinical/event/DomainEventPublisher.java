package com.hospital.clinical.event;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/** Publica cambios de historias clínicas sin acoplar el dominio a sus consumidores. */
@Component
public class DomainEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

  private final KafkaTemplate<String, DomainEvent> kafka;
  private final String topic;

  public DomainEventPublisher(
      KafkaTemplate<String, DomainEvent> kafka,
      @Value("${hospital.kafka.topic:hospital.events}") String topic) {
    this.kafka = kafka;
    this.topic = topic;
  }

  public void publish(String type, String historyId, Map<String, Object> payload) {
    String key = "history:" + historyId;
    DomainEvent event = DomainEvent.of(type, key, payload);
    kafka.send(topic, key, event).whenComplete((result, error) -> {
      if (error != null) {
        log.error("No se pudo publicar el evento {} ({})", event.eventId(), type, error);
      } else {
        log.info("Evento {} publicado: {}", event.eventId(), type);
      }
    });
  }
}
