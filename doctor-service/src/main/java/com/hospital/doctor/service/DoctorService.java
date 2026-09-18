package com.hospital.doctor.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.hospital.doctor.dto.DoctorRequest;
import com.hospital.doctor.dto.DoctorResponse;
import com.hospital.doctor.mapper.DoctorMapper;
import com.hospital.doctor.model.Doctor;
import com.hospital.doctor.repository.DoctorRepository;

/**
 * Reglas de negocio de los médicos.
 *
 * <p>Es la única clase que habla con {@link DoctorRepository} y la única que ve la
 * entidad {@link Doctor}. Hacia arriba solo entrega DTO; hacia abajo solo maneja
 * entidades. Esa es la frontera: <strong>el modelo de base de datos no sale de aquí</strong>.
 *
 * <p>Por eso los métodos públicos reciben {@link DoctorRequest} y devuelven
 * {@link DoctorResponse}: el controlador no llega a tocar una entidad ni por
 * accidente, y si mañana cambia el mapeo JPA, nada fuera de este paquete se entera.
 *
 * <p>Los errores se señalan con {@link ResponseStatusException} para que cada caso
 * llegue al cliente con su código correcto. El <strong>404</strong> de
 * {@link #byId(Long)} es especialmente importante: es la señal que usa
 * clinical-history-service para distinguir "este médico no existe" de
 * "doctor-service no responde".
 */
@Service
public class DoctorService {

  private final DoctorRepository repository;
  private final DoctorMapper mapper;

  /** Inyección por constructor: las dependencias quedan explícitas y son finales. */
  public DoctorService(DoctorRepository repository, DoctorMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  // --- Lectura --------------------------------------------------------------

  /** Todos los médicos registrados, ya convertidos a DTO. */
  public List<DoctorResponse> all() {
    return mapper.toResponseList(repository.findAll());
  }

  /** Un médico por su id. Lanza 404 si no existe. */
  public DoctorResponse byId(Long id) {
    return mapper.toResponse(findEntity(id));
  }

  // --- Escritura ------------------------------------------------------------

  /**
   * Registra un médico nuevo.
   *
   * <p>El id lo asigna PostgreSQL. {@link DoctorRequest} ni siquiera tiene campo
   * para el id, así que no hay nada que ignorar ni que validar al respecto.
   */
  public DoctorResponse create(DoctorRequest request) {
    requireName(request.name());

    Doctor nuevo = mapper.toEntity(request);

    return mapper.toResponse(repository.save(nuevo));
  }

  /**
   * Edita un médico existente.
   *
   * <p>Se carga el registro guardado y se le aplican encima los campos editables.
   * Ese orden —cargar, aplicar, guardar— es lo que garantiza que nadie pueda cambiar
   * el id ni cualquier campo futuro que no esté en el DTO de entrada.
   */
  public DoctorResponse update(Long id, DoctorRequest request) {
    Doctor almacenado = findEntity(id);   // 404 si no existe
    requireName(request.name());          // 400 si el nombre no sirve

    mapper.applyTo(request, almacenado);

    return mapper.toResponse(repository.save(almacenado));
  }

  // --- Interno --------------------------------------------------------------

  /**
   * Carga la entidad o lanza 404.
   *
   * <p>Es privado a propósito: la entidad es un detalle interno y no debe salir de
   * esta clase. Lo reutilizan {@link #byId(Long)} y {@link #update(Long, DoctorRequest)}.
   */
  private Doctor findEntity(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "No existe el médico " + id));
  }

  /**
   * Un médico sin nombre no tiene sentido en el dominio: se rechaza con 400.
   *
   * <p>La validación vive aquí, y no en el controlador, porque es una regla del
   * dominio: seguiría siendo cierta si mañana los médicos se dieran de alta desde
   * una cola de mensajes en vez de por HTTP.
   */
  private void requireName(String name) {
    if (name == null || name.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El nombre del médico es obligatorio");
    }
  }
}
