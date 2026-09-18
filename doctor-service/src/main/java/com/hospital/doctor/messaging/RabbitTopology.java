package com.hospital.doctor.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología compartida por nombre con clinical-history-service.
 *
 * <p>El exchange directo entrega una solicitud solo a la cola cuya binding key
 * coincide exactamente con la routing key. Esto hace explícito el destino lógico
 * sin acoplar al productor a una URL o instancia de doctor-service.
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
