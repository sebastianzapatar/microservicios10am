package com.hospital.clinical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arranque del servicio de historias clínicas.
 *
 * <p>{@code @SpringBootApplication} activa la autoconfiguración y el escaneo de
 * componentes a partir de este paquete hacia abajo.
 *
 * <p>La conexión con RabbitMQ se configura automáticamente al incluir el starter
 * AMQP. La topología concreta (exchange, cola y binding) se declara en
 * {@code RabbitTopology}, dentro del paquete {@code messaging}.
 */
@SpringBootApplication
public class ClinicalHistoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClinicalHistoryApplication.class, args);
  }
}
