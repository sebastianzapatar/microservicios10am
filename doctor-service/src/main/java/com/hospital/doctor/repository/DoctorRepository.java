package com.hospital.doctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hospital.doctor.model.Doctor;

/**
 * Acceso a la tabla {@code doctors}.
 *
 * <p>Interfaz vacía a propósito: {@link JpaRepository} ya aporta save, findById,
 * findAll, existsById, deleteById... y Spring Data genera la implementación al
 * arrancar. Solo habría que escribir métodos aquí para consultas propias.
 *
 * <p>Los dos tipos genéricos son la entidad ({@code Doctor}) y el tipo de su
 * clave primaria ({@code Long}).
 */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
}
