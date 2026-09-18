/**
 * Conexiones salientes hacia <strong>otros microservicios</strong>.
 *
 * <p>Todo lo que este servicio necesita para hablar por HTTP con sus vecinos vive
 * aquí y solo aquí: el cliente declarativo, su plan B cuando el destino no responde
 * y los objetos que representan las respuestas ajenas.
 *
 * <p>La separación es intencionada. El paquete {@code service} contiene la lógica
 * <em>propia</em> del dominio de historias clínicas; este paquete contiene
 * <em>integración</em> con dominios de otros equipos. Son cosas distintas y cambian
 * por motivos distintos: si mañana doctor-service cambia su contrato, o se sustituye
 * OpenFeign por otro cliente, o la validación pasa a ser asíncrona por mensajería,
 * el cambio se queda contenido en esta carpeta sin tocar las reglas de negocio.
 *
 * <p>Regla práctica: si una clase sabe la URL, el nombre en Eureka o el formato JSON
 * de otro servicio, va en {@code client}. Si solo sabe de historias clínicas,
 * va en {@code service}.
 */
package com.hospital.clinical.client;
