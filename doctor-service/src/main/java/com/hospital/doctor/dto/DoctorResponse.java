package com.hospital.doctor.dto;

/**
 * Datos de un médico tal y como los devuelve la API.
 *
 * <p>Es la única representación de un médico que sale de este servicio. La entidad
 * {@code Doctor} nunca cruza la frontera del paquete {@code service}.
 *
 * <p>Hoy coincide campo a campo con la entidad, y eso está bien: el valor no está en
 * que sean distintos, sino en que <em>puedan</em> serlo. El día que la tabla gane
 * columnas internas (fecha de alta, estado, auditoría), este record decide qué se
 * publica y qué no, sin tocar el modelo.
 *
 * <p>Este DTO pertenece solo a la API HTTP pública. La validación entre servicios
 * usa un contrato mínimo por RabbitMQ ({@code FOUND}/{@code NOT_FOUND}), por lo que
 * añadir aquí nuevos datos no rompe a clinical-history-service.
 *
 * @param id   identificador asignado por PostgreSQL
 * @param name nombre completo del médico
 */
public record DoctorResponse(Long id, String name) {
}
