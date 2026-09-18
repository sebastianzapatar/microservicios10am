package com.hospital.doctor.dto;

/**
 * Datos que el cliente envía para crear o editar un médico.
 *
 * <p>Se usa en {@code POST /api/doctors} y en {@code PUT /api/doctors/{id}}: en
 * ambos casos el cliente aporta exactamente lo mismo, el nombre. En el PUT, el id
 * viaja en la URL, que es donde corresponde identificar el recurso.
 *
 * <p><strong>No tiene campo {@code id} a propósito.</strong> Si lo tuviera, un
 * cliente podría intentar fijar o cambiar el identificador de un médico; al no
 * existir en el DTO, esa posibilidad desaparece del contrato en vez de tener que
 * ignorarse por código.
 *
 * <p>La obligatoriedad del nombre se comprueba en la capa de servicio, que es donde
 * viven las reglas del dominio.
 *
 * @param name nombre completo del médico
 */
public record DoctorRequest(String name) {
}
