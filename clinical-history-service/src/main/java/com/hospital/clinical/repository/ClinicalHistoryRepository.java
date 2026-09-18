package com.hospital.clinical.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.hospital.clinical.model.ClinicalHistory;

/**
 * Acceso a la colección de historias clínicas en MongoDB.
 *
 * <p>No hay implementación: Spring Data genera la clase en tiempo de arranque.
 * De {@link MongoRepository} se heredan save, findById, findAll, deleteById, etc.
 *
 * <p>{@code findByClientId} es una <em>query derivada</em>: Spring Data lee el nombre
 * del método y construye sola la consulta {@code { clientId: ? }}.
 */
public interface ClinicalHistoryRepository extends MongoRepository<ClinicalHistory, String> {

  List<ClinicalHistory> findByClientId(String clientId);
}
