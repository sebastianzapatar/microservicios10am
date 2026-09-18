package com.hospital.gateway;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Respuesta de cortesía cuando un servicio de destino no está disponible.
 *
 * <p>Cada ruta del gateway declara un {@code fallbackUri} del tipo
 * {@code forward:/fallback/doctors}. Cuando el Circuit Breaker de esa ruta corta la
 * llamada —porque el destino falla o porque el circuito ya está abierto— el gateway
 * reenvía la petición aquí <em>internamente</em>, sin salir a la red.
 *
 * <p>El resultado es un <strong>503 inmediato y con un mensaje claro</strong> en vez
 * de una petición colgada o un error genérico. Esa es la diferencia entre degradar
 * el servicio y caerse.
 *
 * <p>{@code @RequestMapping} sin método responde a GET, POST, PUT... porque el
 * fallback tiene que cubrir cualquier verbo que se estuviera enrutando.
 */
@RestController
public class FallbackController {

  @RequestMapping("/fallback/{service}")
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  Map<String, String> fallback(@PathVariable String service) {
    return Map.of("message", service + " service is temporarily unavailable");
  }
}
