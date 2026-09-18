package com.hospital.clinical;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Arranque del servicio de historias clínicas.
 *
 * <p>{@code @SpringBootApplication} activa la autoconfiguración y el escaneo de
 * componentes a partir de este paquete hacia abajo.
 *
 * <p>{@code @EnableFeignClients} le dice a Spring que busque interfaces anotadas con
 * {@code @FeignClient} y les genere una implementación. Sin esta anotación,
 * {@code DoctorClient} sería una interfaz sin nadie que la implemente y el arranque
 * fallaría al no encontrar el bean.
 */
@SpringBootApplication
@EnableFeignClients
public class ClinicalHistoryApplication {

  public static void main(String[] args) {
    SpringApplication.run(ClinicalHistoryApplication.class, args);
  }
}
