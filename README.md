# Microservicios de hospital

Solución con descubrimiento de servicios Eureka y comunicación HTTP. El gateway resuelve `lb://nombre-servicio` mediante Eureka y aplica Circuit Breaker (Resilience4j) a cada ruta. Historia clínica también llama por HTTP al servicio de médicos mediante OpenFeign, con fallback/circuit breaker.

## Stack

| Componente | Versión |
|---|---|
| Spring Boot | 4.0.8 (Spring Framework 7.0.9) |
| Spring Cloud | 2025.1.3 (Gateway 5.0.3 · Netflix/Eureka 5.0.2 · OpenFeign 5.0.3) |
| Java (toolchain) | 25 |
| Gradle | 9.7.1 |
| Go | 1.26 · Gin 1.12.0 · GORM 1.31.2 |
| Bases de datos | PostgreSQL 18 · MongoDB 8.0 · MySQL 8.4 |

## Ejecutar

Requisitos: Docker Desktop. Desde esta carpeta:

```bash
docker compose up --build --wait
```

El proyecto Compose se llama `hospital-microservices` (fijado con `name:` en `compose.yaml`, no depende del nombre de la carpeta). Eureka queda disponible en `http://localhost:8762` y toda la API entra por `http://localhost:8080`. Internamente los servicios siguen usando su puerto estándar `8761`.

`--wait` bloquea hasta que los ocho contenedores estén *healthy*: los tres motores de datos, Eureka, los tres servicios y el gateway. Sin esperar, las primeras llamadas pueden responder `503` mientras el gateway todavía no recibe el registro de Eureka.

Para apagar todo conservando los datos: `docker compose down`. Para borrarlos también: `docker compose down -v`.

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

Distinguir `400` de `503` es deliberado: con `dismiss404: true` en el cliente Feign, un médico inexistente no se trata como fallo de infraestructura y por tanto **no abre el circuito**.

## Organización del código (servicios Java)

```
com.hospital.<dominio>/
├── controller/   traduce HTTP <-> dominio; solo maneja DTO
├── dto/          contrato público de la API (records inmutables)
├── mapper/       única traducción DTO <-> entidad
├── service/      reglas de negocio; único que ve la entidad
├── repository/   acceso a datos
├── model/        entidad de base de datos
└── client/       conexiones salientes a OTROS microservicios
```

Tres decisiones deliberadas:

- **Las entidades no salen del paquete `service`.** Los controladores reciben y devuelven DTO, así que el esquema de la base de datos no forma parte del contrato público y puede cambiar sin romper clientes.
- **Crear y editar usan DTO distintos.** `ClinicalHistoryRequest` lleva `clientId`; `ClinicalHistoryUpdateRequest` no. La regla "el paciente de una historia no cambia" queda expresada en los tipos, no en una validación que se pueda olvidar. Lo mismo con `id`: ningún DTO de entrada lo tiene, así que no se puede inyectar desde fuera.
- **`client/` está separado de `service/`.** Lo que sabe de otros servicios (URLs, nombres en Eureka, formato JSON ajeno) vive aparte de las reglas propias del dominio, porque cambian por motivos distintos.

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

Cuando un destino no responda, el gateway devuelve `503` desde `/fallback/...`; el cliente OpenFeign de historias también corta llamadas fallidas al servicio de médicos y usa su fallback, de modo que la historia se rechaza con `503` en vez de guardarse sin validar.

## Demostrar la resiliencia

```bash
docker compose stop doctor-service
for i in $(seq 1 6); do curl -s http://localhost:8080/api/doctors; echo; done   # 503 del fallback
curl -s http://localhost:8080/actuator/circuitbreakers                          # doctorGateway: OPEN
curl -s http://localhost:8080/api/clients                                       # las demás rutas siguen bien
docker compose start doctor-service                                             # el circuito vuelve solo a CLOSED
```

El circuito pasa a `HALF_OPEN` solo tras `waitDurationInOpenState` (10 s) y vuelve a `CLOSED` sin necesidad de tráfico, porque `automaticTransitionFromOpenToHalfOpenEnabled` está activo.

## Presentación

`presentacion/index.html` es un deck de reveal.js; se abre directamente en el navegador, sin servidor.
