/**
 * Integración de historias clínicas con otros dominios mediante RabbitMQ.
 *
 * <p>La lógica clínica solo conoce un resultado de validación. Los nombres del
 * exchange, la cola, la routing key y el detalle RPC quedan aislados aquí.
 */
package com.hospital.clinical.messaging;
