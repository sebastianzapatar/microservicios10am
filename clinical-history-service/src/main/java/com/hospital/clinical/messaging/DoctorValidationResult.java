package com.hospital.clinical.messaging;

/**
 * Los tres resultados posibles al validar un médico por RabbitMQ.
 *
 * <p>Se usa un enum en vez de {@code boolean}: {@code false} solo podría expresar
 * que el médico no existe, pero no que nadie respondió. Distinguir ambos casos
 * permite devolver 400 para un dato inválido y 503 para un fallo temporal.
 */
public enum DoctorValidationResult {
  FOUND,
  NOT_FOUND,
  UNAVAILABLE
}
