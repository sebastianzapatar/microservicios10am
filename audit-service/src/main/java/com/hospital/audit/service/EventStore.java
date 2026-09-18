package com.hospital.audit.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.hospital.audit.event.DomainEvent;

/** Vista de auditoría acotada y segura para acceso concurrente. */
@Service
public class EventStore {

  private final ConcurrentLinkedDeque<DomainEvent> events = new ConcurrentLinkedDeque<>();
  private final ConcurrentHashMap<String, DomainEvent> byId = new ConcurrentHashMap<>();
  private final int maxEvents;

  public EventStore(@Value("${hospital.audit.max-events:1000}") int maxEvents) {
    this.maxEvents = maxEvents;
  }

  @KafkaListener(topics = "${hospital.kafka.topic:hospital.events}")
  public void receive(DomainEvent event) {
    // eventId hace idempotente la vista ante una posible reentrega de Kafka.
    if (byId.putIfAbsent(event.eventId(), event) != null) {
      return;
    }
    events.addFirst(event);
    while (events.size() > maxEvents) {
      DomainEvent removed = events.pollLast();
      if (removed != null) {
        byId.remove(removed.eventId(), removed);
      }
    }
  }

  public List<DomainEvent> all(String eventType) {
    if (eventType == null || eventType.isBlank()) {
      return new ArrayList<>(events);
    }
    return events.stream().filter(event -> event.eventType().equals(eventType)).toList();
  }

  public Optional<DomainEvent> byId(String id) {
    return Optional.ofNullable(byId.get(id));
  }
}
