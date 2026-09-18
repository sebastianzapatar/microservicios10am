/**
 * DTO (<em>Data Transfer Objects</em>): la forma que tienen los datos <strong>en la
 * API</strong>, separada de la forma que tienen en MongoDB.
 *
 * <p>Antes el controlador recibía y devolvía el documento {@code ClinicalHistory}
 * directamente, y eso traía dos problemas concretos:
 * <ul>
 *   <li>El cliente podía enviar {@code createdAt} o {@code id} en el cuerpo, aunque
 *       el servicio los ignorase. El contrato no decía la verdad sobre lo aceptado.</li>
 *   <li>Crear y actualizar parecían admitir los mismos campos, cuando no es cierto:
 *       el paciente se fija al abrir la historia y no se cambia después.</li>
 * </ul>
 *
 * <p>Por eso hay <strong>dos DTO de entrada distintos</strong> y no uno reutilizado:
 * {@code ClinicalHistoryRequest} incluye {@code clientId} y
 * {@code ClinicalHistoryUpdateRequest} no. La diferencia entre crear y editar queda
 * escrita en los tipos, no escondida en un comentario.
 *
 * <p>Todos son {@code record}: inmutables, sin código repetitivo, y Jackson los
 * construye y serializa directamente. Un DTO no tiene lógica.
 */
package com.hospital.clinical.dto;
