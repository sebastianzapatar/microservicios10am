package com.hospital.clinical.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.clinical.dto.ClinicalHistoryRequest;
import com.hospital.clinical.dto.ClinicalHistoryResponse;
import com.hospital.clinical.dto.ClinicalHistoryUpdateRequest;
import com.hospital.clinical.mapper.ClinicalHistoryMapper;
import com.hospital.clinical.messaging.DoctorValidationClient;
import com.hospital.clinical.messaging.DoctorValidationResult;
import com.hospital.clinical.model.ClinicalHistory;
import com.hospital.clinical.repository.ClinicalHistoryRepository;

/**
 * Reglas de negocio de las historias clínicas.
 *
 * <p>Aquí vive la regla central del dominio: <strong>una historia clínica siempre
 * debe estar respaldada por un médico que exista</strong>. Como los médicos viven
 * en otro servicio y en otra base de datos, no se puede usar una clave foránea:
 * hay que preguntárselo a doctor-service por RabbitMQ antes de escribir en MongoDB.
 *
 * <p>Esta clase es la frontera del servicio:
 * <ul>
 *   <li>Hacia arriba entrega DTO, nunca documentos: el modelo de MongoDB no sale de aquí.</li>
 *   <li>Hacia los lados usa {@link DoctorValidationClient}, sin conocer exchanges,
 *       colas, routing keys ni detalles del protocolo AMQP.</li>
 * </ul>
 *
 * <p>Decide; no sabe ni cómo se guarda ni cómo se viaja por la red.
 */
@Service
public class ClinicalHistoryService {

  private final ClinicalHistoryRepository repository;
  private final ClinicalHistoryMapper mapper;

  /** Puerta de salida hacia doctor-service (paquete {@code client}). */
  private final DoctorValidationClient doctors;

  /** Inyección por constructor: las dependencias quedan explícitas y son finales. */
  public ClinicalHistoryService(
      ClinicalHistoryRepository repository,
      ClinicalHistoryMapper mapper,
      DoctorValidationClient doctors) {
    this.repository = repository;
    this.mapper = mapper;
    this.doctors = doctors;
  }

  // --- Escritura ------------------------------------------------------------

  /**
   * Abre una historia clínica nueva.
   *
   * <p>Se valida el médico ANTES de guardar: si doctor-service no confirma, se
   * rechaza la operación en vez de dejar en MongoDB una historia sin respaldo.
   */
  public ClinicalHistoryResponse create(ClinicalHistoryRequest request) {
    requireExistingDoctor(request.doctorId());

    ClinicalHistory nueva = mapper.toEntity(request);

    return mapper.toResponse(repository.save(nueva));
  }

  /**
   * Actualiza el diagnóstico y las notas de una historia existente.
   *
   * <p>Es la operación que usa el médico para registrar la evolución del paciente en
   * consultas posteriores. Se valida el médico igual que al crear, porque quien firma
   * el cambio también tiene que existir.
   *
   * <p>El orden —cargar, validar, aplicar, guardar— es intencionado: nada se escribe
   * hasta que las dos comprobaciones han pasado.
   */
  public ClinicalHistoryResponse update(String id, ClinicalHistoryUpdateRequest request) {
    ClinicalHistory almacenada = findEntity(id);   // 404 si no existe
    requireExistingDoctor(request.doctorId());     // 400/503 si el médico no sirve

    mapper.applyTo(request, almacenada);

    return mapper.toResponse(repository.save(almacenada));
  }

  // --- Lectura --------------------------------------------------------------

  /** Una historia por su id. Lanza 404 si no está. */
  public ClinicalHistoryResponse byId(String id) {
    return mapper.toResponse(findEntity(id));
  }

  /** Todas las historias de un paciente, en el orden en que las devuelve MongoDB. */
  public List<ClinicalHistoryResponse> byClient(String clientId) {
    return mapper.toResponseList(repository.findByClientId(clientId));
  }

  // --- Interno --------------------------------------------------------------

  /**
   * Carga el documento o lanza 404.
   *
   * <p>Es privado a propósito: el documento es un detalle interno y no debe salir de
   * esta clase. Lo reutilizan {@link #byId(String)} y
   * {@link #update(String, ClinicalHistoryUpdateRequest)}.
   */
  private ClinicalHistory findEntity(String id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No existe la historia clínica " + id));
  }

  // --- Validación contra otro servicio --------------------------------------

  /**
   * Comprueba contra doctor-service que el médico indicado existe y está accesible.
   *
   * <p>Distingue tres situaciones, porque al cliente de la API le importan distinto:
   * <ul>
   *   <li><b>400</b> no se envió un id o doctor-service respondió {@code NOT_FOUND}.
   *       Es un error de quien llama.</li>
   *   <li><b>503</b> RabbitMQ falló o se agotó el tiempo sin respuesta. No es culpa
   *       de quien llama: puede reintentar más tarde.</li>
   * </ul>
   *
   * <p>Separar ambos casos no es cosmética: {@code NOT_FOUND} es una respuesta válida
   * del dominio; {@code UNAVAILABLE} señala un problema temporal de infraestructura.
   */
  private void requireExistingDoctor(Long doctorId) {
    if (doctorId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el campo doctorId");
    }

    DoctorValidationResult result = doctors.validate(doctorId);

    switch (result) {
      case FOUND -> { /* Validación terminada: la escritura puede continuar. */ }
      case NOT_FOUND -> throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El médico " + doctorId + " no existe");
      case UNAVAILABLE -> throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "No fue posible validar el médico " + doctorId + " mediante RabbitMQ");
    }
  }
}
