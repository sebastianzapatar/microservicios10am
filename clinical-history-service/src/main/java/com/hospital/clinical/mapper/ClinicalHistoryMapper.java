package com.hospital.clinical.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hospital.clinical.dto.ClinicalHistoryRequest;
import com.hospital.clinical.dto.ClinicalHistoryResponse;
import com.hospital.clinical.dto.ClinicalHistoryUpdateRequest;
import com.hospital.clinical.model.ClinicalHistory;

/**
 * Convierte entre los DTO de la API y el documento {@link ClinicalHistory}.
 *
 * <p>Tres direcciones, una por necesidad real:
 * <ul>
 *   <li>{@link #toEntity(ClinicalHistoryRequest)} — alta: construye un documento nuevo.</li>
 *   <li>{@link #applyTo(ClinicalHistoryUpdateRequest, ClinicalHistory)} — edición:
 *       modifica un documento que ya existe.</li>
 *   <li>{@link #toResponse(ClinicalHistory)} — salida: convierte lo guardado en lo publicado.</li>
 * </ul>
 *
 * <p>Fíjese en qué <em>no</em> hace {@code applyTo}: no toca {@code clientId} ni
 * {@code createdAt}. No hace falta protegerlos con un {@code if}, porque el DTO de
 * edición no los trae. El diseño de los tipos ya impide el error.
 */
@Component
public class ClinicalHistoryMapper {

  /**
   * Crea un documento nuevo a partir de lo que envió el cliente.
   *
   * <p>El {@code id} lo asigna MongoDB al insertar y {@code createdAt} se fija solo
   * al construir la entidad, así que ninguno de los dos se copia desde el DTO.
   */
  public ClinicalHistory toEntity(ClinicalHistoryRequest request) {
    ClinicalHistory history = new ClinicalHistory();
    history.setClientId(request.clientId());
    history.setDoctorId(request.doctorId());
    history.setDiagnosis(request.diagnosis());
    history.setNotes(request.notes());
    return history;
  }

  /**
   * Aplica los cambios del DTO sobre un documento ya existente y lo marca como editado.
   *
   * <p>Recibe el documento cargado de MongoDB y solo le cambia los tres campos
   * editables. Devuelve la misma instancia para poder encadenar la llamada.
   */
  public ClinicalHistory applyTo(ClinicalHistoryUpdateRequest request, ClinicalHistory history) {
    history.setDoctorId(request.doctorId());
    history.setDiagnosis(request.diagnosis());
    history.setNotes(request.notes());
    history.markUpdated();
    return history;
  }

  /** Convierte el documento en la respuesta pública de la API. */
  public ClinicalHistoryResponse toResponse(ClinicalHistory history) {
    return new ClinicalHistoryResponse(
        history.getId(),
        history.getClientId(),
        history.getDoctorId(),
        history.getDiagnosis(),
        history.getNotes(),
        history.getCreatedAt(),
        history.getUpdatedAt());
  }

  /** Versión en lote de {@link #toResponse(ClinicalHistory)}, para los listados. */
  public List<ClinicalHistoryResponse> toResponseList(List<ClinicalHistory> histories) {
    return histories.stream().map(this::toResponse).toList();
  }
}
