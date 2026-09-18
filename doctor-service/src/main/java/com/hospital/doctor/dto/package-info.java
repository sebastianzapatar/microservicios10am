/**
 * DTO (<em>Data Transfer Objects</em>): la forma que tienen los datos <strong>en la
 * API</strong>, separada de la forma que tienen en la base de datos.
 *
 * <p>Antes el controlador recibía y devolvía la entidad {@code Doctor} directamente.
 * Eso ataba el contrato público al esquema de PostgreSQL: renombrar una columna
 * rompía a todos los clientes, y cualquier campo nuevo de la entidad quedaba expuesto
 * sin querer. Con DTO, el contrato se declara aquí de forma explícita y la entidad
 * queda libre de evolucionar.
 *
 * <p>Hay dos tipos por operación, y no es redundancia:
 * <ul>
 *   <li><b>Request</b> — lo que el cliente <em>puede</em> enviar. No incluye {@code id}
 *       porque el identificador lo asigna la base de datos, no quien llama.</li>
 *   <li><b>Response</b> — lo que el servicio devuelve. Incluye {@code id} porque el
 *       cliente lo necesita para operaciones posteriores.</li>
 * </ul>
 *
 * <p>Todos son {@code record}: inmutables, sin getters ni {@code equals} escritos a
 * mano, y Jackson los construye y serializa directamente. Un DTO no tiene lógica;
 * si la tuviera, estaría en el sitio equivocado.
 */
package com.hospital.doctor.dto;
