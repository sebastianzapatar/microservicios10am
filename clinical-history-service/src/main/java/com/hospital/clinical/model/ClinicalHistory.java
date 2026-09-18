package com.hospital.clinical.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Historia clínica de un paciente.
 *
 * <p>Se guarda como documento en MongoDB, no como fila en una tabla: cada historia
 * es un texto libre que puede crecer o cambiar de forma con el tiempo, y eso encaja
 * mejor con un modelo documental que con un esquema relacional rígido.
 *
 * <p>La colección se llama {@code clinical_histories} (ver {@code @Document}).
 */
@Document("clinical_histories")
public class ClinicalHistory {

  /** Identificador que asigna MongoDB al insertar (un ObjectId en hexadecimal). */
  @Id
  private String id;

  /** Paciente dueño de la historia. Vive en client-service (MySQL), aquí solo se referencia. */
  private String clientId;

  /** Médico responsable. Se valida contra doctor-service antes de guardar. */
  private Long doctorId;

  private String diagnosis;

  private String notes;

  /** Momento de creación. Se fija una sola vez y nunca se modifica. */
  private Instant createdAt = Instant.now();

  /** Momento de la última actualización. Queda en {@code null} mientras nadie la edite. */
  private Instant updatedAt;

  // --- Lectura --------------------------------------------------------------

  public String getId() {
    return id;
  }

  public String getClientId() {
    return clientId;
  }

  public Long getDoctorId() {
    return doctorId;
  }

  public String getDiagnosis() {
    return diagnosis;
  }

  public String getNotes() {
    return notes;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  // --- Escritura ------------------------------------------------------------
  // No hay setters para id, clientId ni createdAt: son datos que no deben cambiar
  // una vez creada la historia. El paciente y la fecha de apertura son inmutables.

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public void setDoctorId(Long doctorId) {
    this.doctorId = doctorId;
  }

  public void setDiagnosis(String diagnosis) {
    this.diagnosis = diagnosis;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  /** Marca la historia como editada justo ahora. La invoca el servicio al actualizar. */
  public void markUpdated() {
    this.updatedAt = Instant.now();
  }
}
