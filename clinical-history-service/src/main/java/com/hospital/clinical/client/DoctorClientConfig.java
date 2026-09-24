package com.hospital.clinical.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;

/**
 * Reintentos de {@link DoctorClient}: <strong>cuántas veces</strong> se repite una
 * llamada fallida y <strong>cada cuánto</strong>.
 *
 * <p>Los números no están aquí sino en {@code application.yml}, bloque
 * {@code doctor-client.retry}, para poder cambiarlos sin recompilar.
 *
 * <p>No lleva {@code @Configuration} a propósito. Si la llevara, el escaneo de
 * componentes la cargaría en el contexto principal y aplicaría a <em>todos</em> los
 * clientes Feign del servicio. Así solo la usa quien la pide en
 * {@code @FeignClient(configuration = DoctorClientConfig.class)}.
 *
 * <p>Relación con el Circuit Breaker: los reintentos ocurren <em>dentro</em> de una
 * sola llamada protegida por el circuito. Con 3 intentos fallidos el circuito
 * registra <strong>un</strong> fallo, no tres, y solo después actúa el fallback.
 */
public class DoctorClientConfig {

  /**
   * Política de reintentos de Feign.
   *
   * <p>{@code Retryer.Default} espera {@code period} antes del segundo intento y
   * multiplica la espera por 1,5 en cada intento siguiente, sin pasar de
   * {@code maxPeriod}. Con los valores del yml (500 ms, 2 s, 3 intentos):
   * <pre>
   *   intento 1 ──falla──▶ espera 500 ms ──▶ intento 2 ──falla──▶ espera 750 ms ──▶ intento 3
   * </pre>
   *
   * <p>{@code maxAttempts} cuenta el intento original: 3 = 1 llamada + 2 reintentos.
   * Con 1 no se reintenta nunca ({@code Retryer.NEVER_RETRY} hace lo mismo).
   */
  @Bean
  Retryer doctorRetryer(
      @Value("${doctor-client.retry.max-attempts}") int maxAttempts,
      @Value("${doctor-client.retry.period}") long periodMs,
      @Value("${doctor-client.retry.max-period}") long maxPeriodMs) {
    return new Retryer.Default(periodMs, maxPeriodMs, maxAttempts);
  }

  /**
   * Decide qué respuestas de error merecen reintento.
   *
   * <p>Por defecto Feign solo reintenta errores de red (conexión rechazada, timeout
   * de lectura). Un 5xx —por ejemplo el 503 que da el balanceador cuando
   * doctor-service no tiene instancias en Eureka— se lanzaría sin reintentar.
   * Aquí se convierte en {@link RetryableException} para que también se repita.
   *
   * <p>Los 4xx no se reintentan: repetir una petición mal formada da el mismo error.
   * El 404 ni siquiera llega aquí, lo absorbe {@code dismiss404}.
   */
  @Bean
  ErrorDecoder doctorErrorDecoder() {
    ErrorDecoder porDefecto = new ErrorDecoder.Default();

    return (methodKey, response) -> {
      if (response.status() >= 500) {
        return new RetryableException(
            response.status(),
            "doctor-service respondió " + response.status(),
            response.request().httpMethod(),
            (Long) null,   // sin Retry-After: se usa la espera del Retryer
            response.request());
      }
      return porDefecto.decode(methodKey, response);
    };
  }
}
