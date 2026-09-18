package com.hospital.clinical.dto;

/**
 * Datos para <strong>abrir</strong> una historia clínica.
 * Se usa en {@code POST /api/histories}.
 *
 * <p>Incluye {@code clientId} porque al abrir la historia hay que decir de qué
 * paciente es. A partir de ahí ese dato queda fijo: el DTO de edición
 * ({@link ClinicalHistoryUpdateRequest}) ya no lo lleva.
 *
 * <p>No tiene {@code id}, {@code createdAt} ni {@code updatedAt}: son datos que
 * genera el servidor, no el cliente.
 *
 * @param clientId  paciente dueño de la historia (vive en client-service)
 * @param doctorId  médico responsable; se valida contra doctor-service antes de guardar
 * @param diagnosis diagnóstico
 * @param notes     observaciones del médico
 */
public record ClinicalHistoryRequest(
    String clientId,
    Long doctorId,
    String diagnosis,
    String notes) {
}
