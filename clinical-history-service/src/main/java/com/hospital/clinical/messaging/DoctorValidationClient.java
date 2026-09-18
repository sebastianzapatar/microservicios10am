package com.hospital.clinical.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Puerta de salida de historias clínicas hacia la validación de médicos.
 *
 * <p>Implementa RPC sobre RabbitMQ: envía el id a un exchange y espera la respuesta
 * del consumidor. Spring AMQP crea un identificador de correlación y una dirección
 * temporal de respuesta ({@code amq.rabbitmq.reply-to}); por eso cada llamada recibe
 * exactamente la respuesta que le corresponde aunque haya varias simultáneas.
 *
 * <p>Esto usa mensajería, pero la operación sigue siendo síncrona desde el punto de
 * vista del usuario: la historia no puede guardarse hasta conocer el resultado. El
 * timeout configurado en {@code application.yml} impide esperar indefinidamente.
 */
@Component
public class DoctorValidationClient {

  private final RabbitTemplate rabbit;

  public DoctorValidationClient(RabbitTemplate rabbit) {
    this.rabbit = rabbit;
  }

  /**
   * @return FOUND, NOT_FOUND o UNAVAILABLE si el broker falla o nadie responde
   */
  public DoctorValidationResult validate(Long doctorId) {
    try {
      Object reply = rabbit.convertSendAndReceive(
          RabbitTopology.EXCHANGE,
          RabbitTopology.VALIDATION_ROUTING_KEY,
          doctorId.toString());

      // convertSendAndReceive devuelve null al cumplirse el reply-timeout.
      if (reply == null) {
        return DoctorValidationResult.UNAVAILABLE;
      }

      try {
        return DoctorValidationResult.valueOf(reply.toString());
      } catch (IllegalArgumentException unexpectedReply) {
        // Una respuesta desconocida es un fallo de integración, no un "no existe".
        return DoctorValidationResult.UNAVAILABLE;
      }
    } catch (AmqpException brokerFailure) {
      // Conserva el contrato HTTP de la API: la capa de servicio convertirá este
      // resultado en 503 y nunca guardará una historia sin validar.
      return DoctorValidationResult.UNAVAILABLE;
    }
  }
}
