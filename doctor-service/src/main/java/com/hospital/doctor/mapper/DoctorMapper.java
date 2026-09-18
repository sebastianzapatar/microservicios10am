package com.hospital.doctor.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.hospital.doctor.dto.DoctorRequest;
import com.hospital.doctor.dto.DoctorResponse;
import com.hospital.doctor.model.Doctor;

/**
 * Convierte entre los DTO de la API y la entidad {@link Doctor}.
 *
 * <p>Tres direcciones, una por necesidad real:
 * <ul>
 *   <li>{@link #toEntity(DoctorRequest)} — alta: construye una entidad nueva.</li>
 *   <li>{@link #applyTo(DoctorRequest, Doctor)} — edición: modifica una entidad que
 *       ya existe, en vez de crear otra.</li>
 *   <li>{@link #toResponse(Doctor)} — salida: convierte lo guardado en lo publicado.</li>
 * </ul>
 *
 * <p>La distinción entre {@code toEntity} y {@code applyTo} es lo que protege los
 * campos no editables. En una edición se parte SIEMPRE del registro guardado y solo
 * se tocan los campos que el DTO permite cambiar; el id se queda como estaba porque
 * nadie lo sobrescribe.
 */
@Component
public class DoctorMapper {

  /** Crea una entidad nueva a partir de lo que envió el cliente. Sin id: lo pone la BD. */
  public Doctor toEntity(DoctorRequest request) {
    Doctor doctor = new Doctor();
    doctor.setName(request.name());
    return doctor;
  }

  /**
   * Aplica los cambios del DTO sobre una entidad ya existente.
   *
   * <p>Recibe la entidad cargada de la base de datos y solo le cambia los campos
   * editables. Devuelve la misma instancia para poder encadenar la llamada.
   */
  public Doctor applyTo(DoctorRequest request, Doctor doctor) {
    doctor.setName(request.name());
    return doctor;
  }

  /** Convierte la entidad en la respuesta pública de la API. */
  public DoctorResponse toResponse(Doctor doctor) {
    return new DoctorResponse(doctor.getId(), doctor.getName());
  }

  /** Versión en lote de {@link #toResponse(Doctor)}, para los listados. */
  public List<DoctorResponse> toResponseList(List<Doctor> doctors) {
    return doctors.stream().map(this::toResponse).toList();
  }
}
