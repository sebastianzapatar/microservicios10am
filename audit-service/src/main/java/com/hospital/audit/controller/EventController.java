package com.hospital.audit.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.audit.event.DomainEvent;
import com.hospital.audit.service.EventStore;

@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventStore store;

  public EventController(EventStore store) {
    this.store = store;
  }

  /** Eventos más recientes primero; eventType permite filtrar por tipo exacto. */
  @GetMapping
  List<DomainEvent> all(@RequestParam(required = false) String eventType) {
    return store.all(eventType);
  }

  @GetMapping("/{id}")
  DomainEvent byId(@PathVariable String id) {
    return store.byId(id).orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND, "No existe el evento " + id));
  }
}
