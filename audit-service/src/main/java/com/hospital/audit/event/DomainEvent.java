package com.hospital.audit.event;

import java.time.Instant;
import java.util.Map;

/** Representación neutral de un evento recibido desde cualquier dominio. */
public record DomainEvent(
    String eventId,
    String eventType,
    String aggregateId,
    Instant occurredAt,
    Map<String, Object> payload) {
}
