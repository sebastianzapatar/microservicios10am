// Descripción de cada rama del repositorio. El frontend es el mismo en las tres;
// solo cambia qué arquitectura se anuncia y qué panel de mensajería se muestra.

export const ARQUITECTURAS = {
  rest: {
    rama: 'main',
    badge: 'REST síncrono',
    titulo: 'Los servicios se llaman entre sí por HTTP, y esperan la respuesta.',
    lead:
      'El gateway enruta cada petición con Eureka y la protege con un circuit breaker. ' +
      'Al abrir una historia clínica, el servicio de historias llama a médicos con OpenFeign: ' +
      'si médicos no responde, la historia se rechaza con 503 en vez de guardarse sin validar.',
    flujo: [
      { id: 'ui', nombre: 'Frontend React', detalle: 'nginx · :3000' },
      { enlace: 'HTTP' },
      { id: 'gw', nombre: 'API Gateway', detalle: 'Resilience4j · :8080' },
      { enlace: 'lb:// Eureka' },
      { id: 'hist', nombre: 'Historias clínicas', detalle: 'MongoDB' },
      { enlace: 'OpenFeign · HTTP' },
      { id: 'doc', nombre: 'Médicos', detalle: 'PostgreSQL', destacado: true },
    ],
  },
  rabbitmq: {
    rama: 'rabbit',
    badge: 'RabbitMQ',
    titulo: 'La validación del médico viaja por RabbitMQ con request/reply.',
    lead:
      'Historias clínicas ya no llama a médicos por HTTP: publica una solicitud en el exchange ' +
      'hospital.doctors, la cola doctor.validation la entrega a doctor-service y la respuesta ' +
      'vuelve por reply-to. Si nadie contesta en 3 segundos, la API responde 503.',
    flujo: [
      { id: 'ui', nombre: 'Frontend React', detalle: 'nginx · :3000' },
      { enlace: 'HTTP' },
      { id: 'gw', nombre: 'API Gateway', detalle: 'Resilience4j · :8080' },
      { enlace: 'lb:// Eureka' },
      { id: 'hist', nombre: 'Historias clínicas', detalle: 'MongoDB' },
      { enlace: 'AMQP request' },
      { id: 'broker', nombre: 'RabbitMQ', detalle: 'doctor.validation', destacado: true },
      { enlace: 'reply-to' },
      { id: 'doc', nombre: 'Médicos', detalle: 'PostgreSQL' },
    ],
  },
  kafka: {
    rama: 'kafka',
    badge: 'Apache Kafka',
    titulo: 'Cada cambio se publica como evento en Kafka.',
    lead:
      'Médicos, clientes e historias publican sus altas y ediciones en el tópico hospital.events. ' +
      'audit-service los consume de forma independiente y construye una vista de auditoría: ' +
      'quien escribe no sabe quién escucha, ni necesita esperarlo.',
    flujo: [
      { id: 'ui', nombre: 'Frontend React', detalle: 'nginx · :3000' },
      { enlace: 'HTTP' },
      { id: 'gw', nombre: 'API Gateway', detalle: 'Resilience4j · :8080' },
      { enlace: 'lb:// Eureka' },
      { id: 'svc', nombre: '3 servicios de dominio', detalle: 'Médicos · Clientes · Historias' },
      { enlace: 'produce' },
      { id: 'broker', nombre: 'Kafka', detalle: 'hospital.events', destacado: true },
      { enlace: 'consume' },
      { id: 'audit', nombre: 'audit-service', detalle: 'GET /api/events' },
    ],
  },
}
