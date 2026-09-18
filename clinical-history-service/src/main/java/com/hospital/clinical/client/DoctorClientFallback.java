package com.hospital.clinical.client;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Plan B de {@link DoctorClient}.
 *
 * <p>El Circuit Breaker invoca esta clase cuando la llamada a doctor-service falla
 * o cuando el circuito ya está abierto. Gracias a ella, historias clínicas responde
 * en milisegundos en vez de quedarse esperando un servicio caído.
 *
 * <p>Devuelve un {@code Optional} <strong>con contenido</strong>, no vacío: vacío
 * significa "el médico no existe" y aquí no se sabe tal cosa. Lo que se sabe es que
 * no se pudo preguntar, y eso se comunica con el marcador
 * {@link DoctorResponse#isUnavailable()}.
 *
 * <p>No lanza una excepción: quien decide qué hacer con el fallo es la capa de
 * servicio, no el cliente HTTP. Aquí solo se constata que la llamada no se completó.
 */
@Component
public class DoctorClientFallback implements DoctorClient {

  @Override
  public Optional<DoctorResponse> findById(Long id) {
    return Optional.of(DoctorResponse.unavailable(id));
  }
}
