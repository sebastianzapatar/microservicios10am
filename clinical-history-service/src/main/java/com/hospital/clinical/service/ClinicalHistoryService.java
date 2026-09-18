package com.hospital.clinical.service;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.clinical.client.DoctorClient;
import com.hospital.clinical.client.DoctorResponse;
import com.hospital.clinical.dto.ClinicalHistoryRequest;
import com.hospital.clinical.dto.ClinicalHistoryResponse;
import com.hospital.clinical.dto.ClinicalHistoryUpdateRequest;
import com.hospital.clinical.event.DomainEventPublisher;
import com.hospital.clinical.mapper.ClinicalHistoryMapper;
import com.hospital.clinical.model.ClinicalHistory;
import com.hospital.clinical.repository.ClinicalHistoryRepository;

/**
 * Reglas de negocio de las historias clínicas.
 *
 * <p>Aquí vive la regla central del dominio: <strong>una historia clínica siempre
 * debe estar respaldada por un médico que exista</strong>. Como los médicos viven
 * en otro servicio y en otra base de datos, no se puede usar una clave foránea:
 * hay que preguntárselo a doctor-service por HTTP antes de escribir en MongoDB.
 *
 * <p>Esta clase es la frontera del servicio:
 * <ul>
 *   <li>Hacia arriba entrega DTO, nunca documentos: el modelo de MongoDB no sale de aquí.</li>
 *   <li>Hacia los lados usa {@link DoctorClient}, sin saber que por debajo hay HTTP,
 *       Eureka ni un circuit breaker.</li>
 * </ul>
 *
 * <p>Decide; no sabe ni cómo se guarda ni cómo se viaja por la red.
 */
@Service
public class ClinicalHistoryService {

  private final ClinicalHistoryRepository repository;
  private final ClinicalHistoryMapper mapper;

  /** Puerta de salida hacia doctor-service (paquete {@code client}). */
  private final DoctorClient doctors;
  private final DomainEventPublisher events;

  /** Inyección por constructor: las dependencias quedan explícitas y son finales. */
  public ClinicalHistoryService(
      ClinicalHistoryRepository repository,
      ClinicalHistoryMapper mapper,
      DoctorClient doctors,
      DomainEventPublisher events) {
    this.repository = repository;
    this.mapper = mapper;
    this.doctors = doctors;
    this.events = events;
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

    ClinicalHistory guardada = repository.save(nueva);
    publish("clinical-history.created", guardada);
    return mapper.toResponse(guardada);
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

    ClinicalHistory guardada = repository.save(almacenada);
    publish("clinical-history.updated", guardada);
    return mapper.toResponse(guardada);
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
   *   <li><b>400</b> no se envió un id de médico, o el médico no existe (el 404 de
   *       doctor-service lo convierte en {@code null} la opción {@code dismiss404}).
   *       Es un error de quien llama.</li>
   *   <li><b>503</b> doctor-service no respondió y actuó el fallback. No es culpa de
   *       quien llama: puede reintentar más tarde.</li>
   * </ul>
   *
   * <p>Separar ambos casos no es cosmética: si el 404 se tratara como fallo, consultar
   * médicos inexistentes acabaría abriendo el circuito y dejando fuera de servicio una
   * ruta que funciona perfectamente.
   */
  private void requireExistingDoctor(Long doctorId) {
    if (doctorId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Falta el campo doctorId");
    }

    Optional<DoctorResponse> doctor = doctors.findById(doctorId);

    // Optional vacío = doctor-service respondió 404: el médico no existe.
    if (doctor.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El médico " + doctorId + " no existe");
    }
    // Con contenido pero marcado UNAVAILABLE = vino del fallback, no de doctor-service.
    if (doctor.get().isUnavailable()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "doctor-service no está disponible: no se puede validar el médico " + doctorId);
    }
  }

  private void publish(String type, ClinicalHistory history) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("id", history.getId());
    payload.put("clientId", history.getClientId());
    payload.put("doctorId", history.getDoctorId());
    payload.put("diagnosis", history.getDiagnosis());
    payload.put("notes", history.getNotes());
    events.publish(
        type,
        history.getId(),
        payload);
  }
}
