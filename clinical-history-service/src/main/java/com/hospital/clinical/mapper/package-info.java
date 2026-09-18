/**
 * Traducción entre los DTO de la API y los documentos de MongoDB.
 *
 * <p>Este paquete existe para que la conversión tenga <strong>un solo sitio</strong>.
 * Sin él, el "copiar campo a campo" acaba repetido en cada método del servicio, y
 * basta olvidar una línea en uno de ellos para que aparezca un bug silencioso: un
 * campo que no se guarda o uno que no se devuelve.
 *
 * <p>Los mappers son componentes de Spring y se inyectan como cualquier otra
 * dependencia. Están escritos a mano porque el mapeo es trivial y así se ve lo que
 * ocurre; en un proyecto grande se generarían con MapStruct, que produce exactamente
 * este mismo código en tiempo de compilación.
 *
 * <p>Regla: el mapper <strong>solo traduce</strong>. No consulta la base de datos, no
 * valida y no llama a otros servicios. Cualquier condición que aparezca aquí es una
 * señal de que esa lógica pertenece a la capa de servicio.
 */
package com.hospital.clinical.mapper;
