package com.hospital.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arranque del API Gateway.
 *
 * <p>La clase está prácticamente vacía a propósito: todo el comportamiento del
 * gateway (rutas, filtros, circuit breakers) se declara en {@code application.yml},
 * no en código. Spring Cloud Gateway construye las rutas a partir de esa
 * configuración al arrancar.
 *
 * <p>El gateway corre sobre WebFlux (no sobre Spring MVC): es un proxy reactivo y
 * no bloquea un hilo por petición mientras espera al servicio de destino.
 */
@SpringBootApplication
public class ApiGatewayApplication {

  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
