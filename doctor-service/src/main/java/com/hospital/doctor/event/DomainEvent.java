package com.hospital.doctor.event;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Contrato JSON común de los eventos publicados en hospital.events. */
public record DomainEvent(
    String eventId,
    String eventType,
    String aggregateId,
    Instant occurredAt,
    Map<String, Object> payload) {

  public static DomainEvent of(
      String eventType, String aggregateId, Map<String, Object> payload) {
    return new DomainEvent(
        UUID.randomUUID().toString(), eventType, aggregateId, Instant.now(), payload);
  }
}
