# Microservicios de hospital con RabbitMQ

Solución con APIs HTTP detrás de un gateway y mensajería AMQP entre dominios. El gateway resuelve `lb://nombre-servicio` mediante Eureka y protege cada ruta con Circuit Breaker (Resilience4j). Para validar al médico, historias clínicas ya no llama por HTTP: publica una solicitud en RabbitMQ y `doctor-service` responde mediante el patrón request/reply.

## Stack

| Componente | Versión |
|---|---|
| Spring Boot | 4.0.8 (Spring Framework 7.0.9) |
| Spring Cloud | 2025.1.3 (Gateway 5.0.3 · Netflix/Eureka 5.0.2) |
| Spring AMQP | 4.0.5 |
| RabbitMQ | 4 (imagen `rabbitmq:4-management-alpine`) |
| Java (toolchain) | 25 |
| Gradle | 9.7.1 |
| Go | 1.26 · Gin 1.12.0 · GORM 1.31.2 |
| Bases de datos | PostgreSQL 18 · MongoDB 8.0 · MySQL 8.4 |

## Ejecutar

Requisitos: Docker Desktop. Desde esta carpeta:

```bash
docker compose up --build --wait
```

El proyecto Compose se llama `hospital-microservices` (fijado con `name:` en `compose.yaml`, no depende del nombre de la carpeta). Toda la API entra por `http://localhost:8080`; Eureka está en `http://localhost:8762` y RabbitMQ Management en `http://localhost:15672` con usuario y contraseña `hospital`.

`--wait` bloquea hasta que los diez contenedores estén *healthy*: tres motores de datos, RabbitMQ, Eureka, tres servicios, el gateway y el frontend. Sin esperar, las primeras llamadas pueden responder `503` mientras todavía se registran los servicios o se conecta el consumidor AMQP.

Para apagar todo conservando los datos: `docker compose down`. Para borrarlos también: `docker compose down -v`.

## Panel web (frontend)

`http://localhost:3000` sirve un panel en React (Vite + nginx, carpeta `frontend/`) para demostrar la arquitectura en vivo:

- **RabbitMQ:** consumidores y mensajes de la cola `doctor.validation`, solicitudes publicadas por intervalo, la topología exchange → cola → consumidor y el resultado de cada validación request/reply (`FOUND`, `NOT_FOUND` o timeout), leídos de la API de RabbitMQ Management cada 2 s.
- **Estado de la plataforma:** servicios registrados en Eureka y estado de cada circuit breaker del gateway.
- **Probar el flujo:** formularios para registrar médicos y pacientes y abrir historias, una *demo rápida* que hace las tres operaciones seguidas y un botón que envía un médico inexistente para ver el `NOT_FOUND` → `400`.

El navegador solo habla con el puerto 3000: nginx reenvía `/api` al gateway y consulta Eureka y RabbitMQ Management dentro de la red de Compose. El mismo frontend existe en las ramas `main` y `kafka`; la variable `ARQUITECTURA` del servicio `frontend` en `compose.yaml` decide qué panel se muestra.

Para desarrollarlo sin Docker (con el resto del stack levantado): `cd frontend && npm install && VITE_ARQUITECTURA=rabbitmq npm run dev`.

## Endpoints

| Servicio | Base por gateway | Persistencia |
|---|---|---|
| Médicos | `/api/doctors` | PostgreSQL |
| Historias | `/api/histories` | MongoDB |
| Clientes | `/api/clients` | MySQL |

### Médicos

| Operación | Endpoint |
|---|---|
| Listar | `GET /api/doctors` |
| Consultar uno | `GET /api/doctors/{id}` |
| Registrar | `POST /api/doctors` |
| **Editar** | `PUT /api/doctors/{id}` |

`400` si falta el nombre, `404` si el médico no existe. Ese `404` no es cosmético: es la señal que usa clinical-history-service para saber que un médico no existe.

### Historias clínicas

| Operación | Endpoint |
|---|---|
| Abrir una historia | `POST /api/histories` |
| **Actualizar diagnóstico y notas** | `PUT /api/histories/{id}` |
| Consultar una historia | `GET /api/histories/{id}` |
| Historial de un paciente | `GET /api/histories/client/{clientId}` |

Tanto al crear como al actualizar se valida el médico contra doctor-service. El `PUT` es la operación que usa el médico en una consulta de control: toma `doctorId`, `diagnosis` y `notes`, conserva `clientId` y `createdAt`, y fija `updatedAt`.

| Situación | Respuesta |
|---|---|
| Actualización correcta | `200` |
| La historia no existe | `404` |
| Falta `doctorId` o el médico no existe | `400` |
| doctor-service no responde | `503` |

Distinguir `400` de `503` es deliberado: `NOT_FOUND` es una respuesta válida del dominio, mientras que `UNAVAILABLE` significa que RabbitMQ falló o nadie respondió antes del timeout de 3 segundos.

## Cómo funciona RabbitMQ aquí

La validación conserva el comportamiento inmediato de la API, pero cambia el transporte de HTTP a AMQP:

1. `clinical-history-service` publica el ID en el exchange directo `hospital.doctors` con la routing key `doctor.validation.request`.
2. El binding lo dirige a la cola durable `doctor.validation`.
3. `doctor-service` consume el mensaje con `@RabbitListener`, consulta PostgreSQL y retorna `FOUND` o `NOT_FOUND`.
4. Spring AMQP envía esa respuesta a `amq.rabbitmq.reply-to` y usa el correlation id para entregarla a la llamada correcta.
5. Si no hay respuesta en 3 segundos, Historias devuelve `503`. La cola tiene TTL de 5 segundos para no procesar solicitudes RPC ya vencidas.

Es importante no confundir transporte con modelo temporal: este flujo usa RabbitMQ, pero sigue siendo RPC porque la historia debe esperar la validación antes de guardarse. Un flujo completamente asíncrono tendría que responder `202 Accepted` y completar la operación después, aceptando consistencia eventual.

| Aspecto | OpenFeign (antes) | RabbitMQ (ahora) |
|---|---|---|
| Transporte | HTTP request/response | AMQP mediante broker |
| Destino | Nombre Eureka + ruta HTTP | Exchange + routing key + cola |
| Acoplamiento | Verbo, ruta y DTO remoto | Contrato del mensaje y destino lógico |
| Picos de carga | Cada conexión espera | La cola amortigua mientras el mensaje sea vigente |
| Escalado | LoadBalancer elige instancia | RabbitMQ reparte entre consumidores |
| Fallo de validación | Circuit breaker y fallback | Reply timeout, TTL y `503` |

## Organización del código (servicios Java)

```
com.hospital.<dominio>/
├── controller/   traduce HTTP <-> dominio; solo maneja DTO
├── dto/          contrato público de la API (records inmutables)
├── mapper/       única traducción DTO <-> entidad
├── service/      reglas de negocio; único que ve la entidad
├── repository/   acceso a datos
├── model/        entidad de base de datos
└── messaging/    productores, consumidores y topología RabbitMQ
```

Tres decisiones deliberadas:

- **Las entidades no salen del paquete `service`.** Los controladores reciben y devuelven DTO, así que el esquema de la base de datos no forma parte del contrato público y puede cambiar sin romper clientes.
- **Crear y editar usan DTO distintos.** `ClinicalHistoryRequest` lleva `clientId`; `ClinicalHistoryUpdateRequest` no. La regla "el paciente de una historia no cambia" queda expresada en los tipos, no en una validación que se pueda olvidar. Lo mismo con `id`: ningún DTO de entrada lo tiene, así que no se puede inyectar desde fuera.
- **`messaging/` está separado de `service/`.** Exchange, cola, routing key y detalles AMQP viven aparte de las reglas propias del dominio, porque cambian por motivos distintos.

El controlador nunca importa el repositorio: pide operaciones de negocio a la capa de servicio.

Ejemplo de uso, primero cree el médico y cliente, y luego la historia:

```bash
curl -X POST http://localhost:8080/api/doctors -H 'Content-Type: application/json' -d '{"name":"Dra. Ana Pérez"}'
curl -X POST http://localhost:8080/api/clients -H 'Content-Type: application/json' -d '{"name":"Carlos Ruiz"}'
curl -X POST http://localhost:8080/api/histories -H 'Content-Type: application/json' -d '{"clientId":"1","doctorId":1,"diagnosis":"Control general","notes":"Sin novedades"}'
curl http://localhost:8080/api/histories/client/1
```

Para editar después, con el id que devolvió cada `POST`:

```bash
curl -X PUT http://localhost:8080/api/doctors/1 -H 'Content-Type: application/json' \
     -d '{"name":"Dra. Ana Pérez Gómez"}'

curl -X PUT http://localhost:8080/api/histories/<id> -H 'Content-Type: application/json' \
     -d '{"doctorId":1,"diagnosis":"Faringitis aguda","notes":"Antibiótico 7 días"}'
```

Cuando un destino HTTP no responde, el gateway devuelve `503` desde `/fallback/...`. Para la validación AMQP, Historias espera como máximo 3 segundos y también rechaza con `503` en vez de guardar sin validar.

## Demostrar RabbitMQ y la resiliencia

```bash
docker compose stop doctor-service
curl -i -X POST http://localhost:8080/api/histories \
  -H 'Content-Type: application/json' \
  -d '{"clientId":"1","doctorId":1,"diagnosis":"Control","notes":"Demo"}'
# Tras 3 s: 503 porque no llegó la respuesta AMQP.

curl -s http://localhost:8080/api/clients  # las demás rutas siguen bien
docker compose start doctor-service        # vuelve a consumir validaciones
```

El patrón AMQP usa timeout y TTL; los circuit breakers siguen existiendo, pero protegen las rutas HTTP del gateway. Son mecanismos distintos y la presentación los muestra por separado.

## Presentación

`presentacion/index.html` es un deck de reveal.js; se abre directamente en el navegador, sin servidor.
