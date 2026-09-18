package com.hospital.audit;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
public class AuditServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditServiceApplication.class, args);
  }

  /** Declara el tópico de forma idempotente al arrancar el consumidor. */
  @Bean
  NewTopic hospitalEventsTopic() {
    return TopicBuilder.name("hospital.events").partitions(3).replicas(1).build();
  }
}
