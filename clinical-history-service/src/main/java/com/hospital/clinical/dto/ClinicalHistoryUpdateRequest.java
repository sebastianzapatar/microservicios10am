package com.hospital.clinical.dto;

/**
 * Datos para <strong>actualizar</strong> una historia clínica.
 * Se usa en {@code PUT /api/histories/{id}}.
 *
 * <p>Es lo que envía el médico en una consulta de control. Contiene exactamente los
 * tres campos editables, y esa es toda su razón de ser: un cliente no puede reasignar
 * la historia a otro paciente ni falsear la fecha de apertura, porque
 * <strong>esos campos no existen en este tipo</strong>.
 *
 * <p>Compárese con {@link ClinicalHistoryRequest}: la ausencia de {@code clientId}
 * aquí es la regla de negocio "el paciente de una historia no cambia", expresada en
 * el sistema de tipos en lugar de en una validación.
 *
 * <p>{@code doctorId} sí viaja: identifica al médico que firma esta actualización, y
 * se valida contra doctor-service igual que al crear.
 *
 * @param doctorId  médico que registra el cambio
 * @param diagnosis diagnóstico actualizado
 * @param notes     observaciones actualizadas
 */
public record ClinicalHistoryUpdateRequest(
    Long doctorId,
    String diagnosis,
    String notes) {
}
