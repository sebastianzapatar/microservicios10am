package com.hospital.doctor.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.hospital.doctor.service.DoctorService;

/**
 * Consumidor de solicitudes de validación de médicos.
 *
 * <p>{@code @RabbitListener} mantiene un consumidor conectado a la cola. Cada
 * mensaje contiene el id en texto plano; el valor retornado se publica en la
 * dirección {@code reply-to} del mensaje original conservando su correlation id.
 * Spring AMQP hace esa parte del patrón RPC sin código manual.
 */
@Component
public class DoctorValidationListener {

  private static final String FOUND = "FOUND";
  private static final String NOT_FOUND = "NOT_FOUND";

  private final DoctorService doctors;

  public DoctorValidationListener(DoctorService doctors) {
    this.doctors = doctors;
  }

  @RabbitListener(queues = RabbitTopology.VALIDATION_QUEUE)
  public String validate(String rawDoctorId) {
    try {
      Long doctorId = Long.valueOf(rawDoctorId);
      return doctors.exists(doctorId) ? FOUND : NOT_FOUND;
    } catch (NumberFormatException invalidMessage) {
      // El contrato actual solo envía números. Una carga inválida no se reencola:
      // se contesta NOT_FOUND y se evita un ciclo infinito de reintentos.
      return NOT_FOUND;
    }
  }
}
