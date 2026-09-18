package com.hospital.doctor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Arranque del servicio de médicos.
 *
 * <p>No hace falta {@code @EnableEurekaClient}: basta con tener el starter de
 * eureka-client en el classpath y la URL en {@code application.yml} para que
 * Spring Cloud registre el servicio automáticamente.
 */
@SpringBootApplication
public class DoctorServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DoctorServiceApplication.class, args);
  }
}
