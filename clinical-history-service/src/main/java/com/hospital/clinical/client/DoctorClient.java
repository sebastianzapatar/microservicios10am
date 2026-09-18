package com.hospital.clinical.client;

import java.util.Optional;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente HTTP declarativo hacia doctor-service.
 *
 * <p>No se escribe la llamada HTTP: se describe con anotaciones y OpenFeign genera
 * la implementación. El {@code name} no es un host, es el nombre registrado en
 * Eureka; Spring Cloud LoadBalancer lo resuelve a una instancia concreta.
 *
 * <p>Dos comportamientos importantes, ambos configurados en {@code application.yml}:
 * <ul>
 *   <li>{@code fallback}: si doctor-service no responde, el Circuit Breaker desvía
 *       la llamada a {@link DoctorClientFallback} en vez de propagar el error.</li>
 *   <li>{@code dismiss404: true}: un 404 (el médico no existe) no se trata como
 *       fallo. Así se distingue "no existe" (culpa de quien llama) de "no responde"
 *       (culpa de la infraestructura), y consultar un id inexistente no abre el
 *       circuito.</li>
 * </ul>
 *
 * <p>Esta interfaz es la <strong>única</strong> puerta de salida hacia doctor-service:
 * ninguna otra clase del servicio conoce sus URLs.
 */
@FeignClient(name = "doctor-service", fallback = DoctorClientFallback.class)
public interface DoctorClient {

  /**
   * Busca un médico por su id.
   *
   * <p>El tipo de retorno es {@link Optional} a propósito, y no {@code DoctorResponse}
   * a secas. Feign resuelve el {@code Optional} vacío cuando la respuesta es 404,
   * <strong>sin mirar el cuerpo</strong>. Devolviendo el objeto pelado habría que
   * confiar en que el 404 llega sin cuerpo: si doctor-service adjunta un JSON de
   * error, Jackson lo deserializaría en un {@code DoctorResponse} con todo a
   * {@code null} —que no es {@code null}— y el médico inexistente pasaría por válido.
   *
   * <p>Ese fallo ya ocurrió en este proyecto. El {@code Optional} lo hace imposible.
   *
   * @return el médico, o vacío si doctor-service respondió 404 (no existe)
   */
  @GetMapping("/api/doctors/{id}")
  Optional<DoctorResponse> findById(@PathVariable("id") Long id);
}
