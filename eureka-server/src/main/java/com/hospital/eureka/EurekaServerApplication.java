package com.hospital.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor de descubrimiento (service registry).
 *
 * <p>Es el "directorio telefónico" del sistema: cada servicio se anuncia al arrancar
 * y renueva su presencia con heartbeats periódicos. Si deja de renovarla, Eureka lo
 * da de baja y el gateway deja de enviarle tráfico.
 *
 * <p>Gracias a esto nadie necesita conocer IPs ni puertos de los demás: el gateway
 * pide "doctor-service" y Eureka responde dónde está ahora mismo.
 *
 * <p>{@code @EnableEurekaServer} convierte esta aplicación Spring Boot corriente en
 * el registro. El panel web queda en {@code http://localhost:8762} (8761 dentro de
 * la red de Docker).
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
