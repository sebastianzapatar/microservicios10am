package com.hospital.doctor.controller;

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

import com.hospital.doctor.dto.DoctorRequest;
import com.hospital.doctor.dto.DoctorResponse;
import com.hospital.doctor.service.DoctorService;

/**
 * API REST de médicos.
 *
 * <p>La clase es deliberadamente delgada: cada método declara una ruta, delega en
 * {@link DoctorService} y devuelve el resultado. No importa el repositorio, no
 * conoce la entidad {@code Doctor} y no toma ninguna decisión propia.
 *
 * <p>Solo aparecen DTO en las firmas. Ese es el objetivo: el modelo de base de datos
 * no forma parte del contrato público de la API.
 *
 * <p>Este servicio tiene dos clases de consumidores:
 * <ul>
 *   <li>el usuario final, que entra por el gateway en {@code /api/doctors};</li>
 *   <li>clinical-history-service, que envía una solicitud por RabbitMQ para validar
 *       un médico antes de guardar o actualizar una historia.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

  private final DoctorService service;

  public DoctorController(DoctorService service) {
    this.service = service;
  }

  /** Lista todos los médicos. {@code GET /api/doctors} */
  @GetMapping
  List<DoctorResponse> all() {
    return service.all();
  }

  /**
   * Un médico por id. {@code GET /api/doctors/{id}}
   *
   * <p>200 con el médico, o 404 si no existe. Ese 404 es el que
   * clinical-history-service interpreta como "el médico no existe".
   */
  @GetMapping("/{id}")
  DoctorResponse one(@PathVariable Long id) {
    return service.byId(id);
  }

  /**
   * Registra un médico nuevo. {@code POST /api/doctors}
   *
   * <p>201 con el id asignado · 400 si falta el nombre.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  DoctorResponse create(@RequestBody DoctorRequest request) {
    return service.create(request);
  }

  /**
   * Edita un médico existente. {@code PUT /api/doctors/{id}}
   *
   * <p>El id va en la URL (identifica el recurso) y el contenido en el cuerpo; por
   * eso {@link DoctorRequest} no necesita campo id.
   *
   * <p>200 con el médico actualizado · 404 si no existe · 400 si falta el nombre.
   */
  @PutMapping("/{id}")
  DoctorResponse update(@PathVariable Long id, @RequestBody DoctorRequest request) {
    return service.update(id, request);
  }
}
