package com.hospital.doctor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Médico del hospital.
 *
 * <p>A diferencia de la historia clínica, esto sí es una entidad relacional: los
 * datos de un médico son pocos, fijos y bien estructurados, así que se guardan en
 * PostgreSQL con JPA (Hibernate).
 *
 * <p>La tabla {@code doctors} la crea Hibernate al arrancar, porque en
 * {@code application.yml} está {@code ddl-auto: update}.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

  /**
   * Clave primaria. {@code IDENTITY} deja que PostgreSQL genere el número con su
   * propia secuencia, así que no hay que asignarlo al crear el médico.
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Nombre del médico. {@code nullable = false} se traduce en un NOT NULL en la tabla. */
  @Column(nullable = false)
  private String name;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  // No hay setId: el identificador lo asigna la base de datos, no quien llama a la API.
}
