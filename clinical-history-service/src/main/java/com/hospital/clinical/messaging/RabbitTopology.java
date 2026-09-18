package com.hospital.clinical.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declara la topología AMQP usada para validar médicos.
 *
 * <p>Los dos microservicios declaran los mismos nombres. RabbitMQ trata esta
 * operación como idempotente: si exchange, cola y binding ya existen con los
 * mismos atributos, no crea duplicados.
 *
 * <p>La cola es durable para sobrevivir a un reinicio del broker. El TTL evita
 * que queden solicitudes RPC viejas esperando cuando doctor-service está caído:
 * después de cinco segundos ya no le sirven al cliente que dejó de esperar.
 */
@Configuration
public class RabbitTopology {

  public static final String EXCHANGE = "hospital.doctors";
  public static final String VALIDATION_QUEUE = "doctor.validation";
  public static final String VALIDATION_ROUTING_KEY = "doctor.validation.request";

  @Bean
  DirectExchange doctorExchange() {
    return new DirectExchange(EXCHANGE, true, false);
  }

  @Bean
  Queue doctorValidationQueue() {
    return QueueBuilder.durable(VALIDATION_QUEUE)
        .ttl(5_000)
        .build();
  }

  @Bean
  Binding doctorValidationBinding(Queue doctorValidationQueue, DirectExchange doctorExchange) {
    return BindingBuilder.bind(doctorValidationQueue)
        .to(doctorExchange)
        .with(VALIDATION_ROUTING_KEY);
  }
}
