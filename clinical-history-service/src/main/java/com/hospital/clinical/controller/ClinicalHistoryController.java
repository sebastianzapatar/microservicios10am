package com.hospital.clinical.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.clinical.dto.ClinicalHistoryRequest;
import com.hospital.clinical.dto.ClinicalHistoryResponse;
import com.hospital.clinical.dto.ClinicalHistoryUpdateRequest;
import com.hospital.clinical.service.ClinicalHistoryService;

/**
 * API REST de historias clínicas.
 *
 * <p>La clase es deliberadamente delgada: cada método declara una ruta, delega en
 * {@link ClinicalHistoryService} y devuelve el resultado. No valida reglas de negocio,
 * no habla con MongoDB y no conoce el documento {@code ClinicalHistory}.
 *
 * <p>Solo aparecen DTO en las firmas, y son distintos según la operación: crear pide
 * {@link ClinicalHistoryRequest} (con paciente) y editar pide
 * {@link ClinicalHistoryUpdateRequest} (sin paciente). La diferencia está en el tipo,
 * así que el compilador la vigila.
 *
 * <p>Las rutas son las mismas que expone el gateway, porque este reenvía la URL tal
 * cual: lo que aquí es {@code /api/histories} el usuario lo ve en
 * {@code http://localhost:8080/api/histories}.
 */
@RestController
@RequestMapping("/api/histories")
public class ClinicalHistoryController {

  private final ClinicalHistoryService service;

  public ClinicalHistoryController(ClinicalHistoryService service) {
    this.service = service;
  }

  /**
   * Abre una historia clínica. {@code POST /api/histories}
   *
   * <p>201 si se creó · 400 si el médico no existe · 503 si doctor-service no responde.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  ClinicalHistoryResponse create(@RequestBody ClinicalHistoryRequest request) {
    return service.create(request);
  }

  /**
   * Actualiza el diagnóstico y las notas de una historia. {@code PUT /api/histories/{id}}
   *
   * <p>Es lo que hace el médico en una consulta de control. El paciente y la fecha de
   * apertura no se pueden cambiar: no forman parte del DTO de entrada.
   *
   * <p>200 si se actualizó · 404 si la historia no existe · 400 si el médico no existe
   * · 503 si doctor-service no responde.
   */
  @PutMapping("/{id}")
  ClinicalHistoryResponse update(
      @PathVariable String id, @RequestBody ClinicalHistoryUpdateRequest request) {
    return service.update(id, request);
  }

  /** Una historia concreta. {@code GET /api/histories/{id}} — 404 si no existe. */
  @GetMapping("/{id}")
  ClinicalHistoryResponse byId(@PathVariable String id) {
    return service.byId(id);
  }

  /**
   * Historial completo de un paciente. {@code GET /api/histories/client/{clientId}}
   *
   * <p>Esta ruta no choca con {@code /{id}}: Spring prefiere siempre el segmento
   * literal ("client") antes que la variable de ruta.
   */
  @GetMapping("/client/{clientId}")
  List<ClinicalHistoryResponse> byClient(@PathVariable String clientId) {
    return service.byClient(clientId);
  }
}
