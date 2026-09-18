package com.hospital.clinical.dto;

import java.time.Instant;

/**
 * Historia clínica tal y como la devuelve la API.
 *
 * <p>Es la única representación que sale de este servicio: el documento
 * {@code ClinicalHistory} nunca cruza la frontera del paquete {@code service}.
 *
 * <p>Aquí sí aparecen los campos que genera el servidor —{@code id},
 * {@code createdAt} y {@code updatedAt}— porque al cliente le sirven para leer,
 * aunque no pueda escribirlos. Esa asimetría entre lo que se acepta y lo que se
 * devuelve es justamente lo que un único modelo compartido no puede expresar.
 *
 * <p>{@code updatedAt} llega como {@code null} mientras nadie haya editado la
 * historia: distingue "recién abierta" de "revisada en una consulta posterior".
 *
 * @param id        identificador que asignó MongoDB
 * @param clientId  paciente dueño de la historia
 * @param doctorId  médico responsable de la última versión
 * @param diagnosis diagnóstico
 * @param notes     observaciones
 * @param createdAt cuándo se abrió la historia
 * @param updatedAt cuándo se actualizó por última vez, o {@code null} si nunca
 */
public record ClinicalHistoryResponse(
    String id,
    String clientId,
    Long doctorId,
    String diagnosis,
    String notes,
    Instant createdAt,
    Instant updatedAt) {
}
