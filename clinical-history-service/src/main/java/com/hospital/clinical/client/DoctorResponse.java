package com.hospital.clinical.client;

/**
 * Lo que devuelve doctor-service en {@code GET /api/doctors/{id}}.
 *
 * <p>Es un <em>record</em>: solo transporta datos, así que no necesita getters ni
 * constructor escritos a mano. Jackson lo construye directamente desde el JSON.
 *
 * <p>Solo se declaran los campos que a este servicio le interesan; si doctor-service
 * añadiera más, Jackson los ignora sin romper nada.
 *
 * <p>Vive en {@code client} y no en {@code model} a propósito: no es una entidad de
 * este servicio, es la forma que tiene el dato en <em>otro</em> servicio.
 */
public record DoctorResponse(Long id, String name) {

  /**
   * Nombre ficticio con el que el fallback marca "no pude hablar con doctor-service".
   * Se usa un marcador en vez de una excepción porque el fallback debe devolver
   * un objeto del mismo tipo que el método original.
   */
  static final String UNAVAILABLE = "UNAVAILABLE";

  /**
   * Respuesta que entrega el fallback cuando doctor-service no responde.
   * Package-private: solo {@link DoctorClientFallback} debe poder fabricarla.
   */
  static DoctorResponse unavailable(Long id) {
    return new DoctorResponse(id, UNAVAILABLE);
  }

  /**
   * {@code true} si este objeto vino del fallback y no de doctor-service.
   *
   * <p>Es público porque lo consulta la capa de servicio para decidir si responder
   * 503. Es el único detalle del fallback que se expone fuera de este paquete.
   */
  public boolean isUnavailable() {
    return UNAVAILABLE.equals(name);
  }
}
