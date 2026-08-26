# Fase 5 — Contacto seguro y cobertura nacional

## Alcance terminado

El MVP permite que la comunidad colombiana publique mascotas perdidas y avistamientos, solicite contacto con consentimiento y converse dentro de la plataforma. No se entregan automáticamente correo, teléfono ni cédula entre usuarios.

El flujo es:

1. Un usuario autenticado solicita contacto sobre un reporte `LOST` o un avistamiento `ACTIVE`.
2. El creador consulta sus solicitudes recibidas y acepta o rechaza.
3. Aceptar actualiza la solicitud y crea la conversación con sus dos participantes en una transacción.
4. Los participantes envían y reciben mensajes por WebSocket; el historial persistido se consulta por HTTP con cursor.
5. Cualquiera puede cerrar la conversación, bloquear al otro participante o denunciarla.
6. Los moderadores revisan denuncias en una cola independiente.

Una solicitud no puede dirigirse a una publicación propia ni crearse si existe bloqueo en cualquier dirección. Solo el destinatario puede aceptar/rechazar y solo el solicitante puede cancelar. Un usuario ajeno recibe `404` al intentar consultar una conversación, evitando confirmar su existencia.

## APIs de usuarios

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/contact-requests` | Solicitar contacto |
| `GET` | `/api/v1/contact-requests/received` | Solicitudes recibidas |
| `GET` | `/api/v1/contact-requests/sent` | Solicitudes enviadas |
| `PATCH` | `/api/v1/contact-requests/{id}/accept` | Aceptar y abrir conversación |
| `PATCH` | `/api/v1/contact-requests/{id}/reject` | Rechazar |
| `PATCH` | `/api/v1/contact-requests/{id}/cancel` | Cancelar una solicitud propia |
| `GET` | `/api/v1/conversations` | Conversaciones propias |
| `GET` | `/api/v1/conversations/{id}/messages?after={cursor}` | Historial y recuperación de mensajes |
| `WS` | `/ws/conversations/{id}` | Enviar y recibir mensajes en tiempo real |
| `PATCH` | `/api/v1/conversations/{id}/close` | Cerrar conversación |
| `PATCH` | `/api/v1/conversations/{id}/block` | Bloquear y cerrar |
| `POST` | `/api/v1/conversations/{id}/reports` | Denunciar conversación |

La consulta histórica devuelve hasta 100 mensajes en orden ascendente. `nextAfter` siempre conserva el último checkpoint conocido —incluso cuando todavía no hay mensajes nuevos— y representa `(created_at,id)` de forma opaca. El contrato WebSocket, la autenticación y la estrategia de reconexión están documentados en [WEBSOCKET_MENSAJERIA.md](WEBSOCKET_MENSAJERIA.md).

## Reencuentro verificado

El propietario ya no puede asignar `REUNITED`. Su DTO de estados solo acepta `CLOSED` y `LOST`; el servicio también rechaza `REUNITED` como defensa adicional.

Cuando tiene físicamente la mascota solicita revisión mediante:

```text
POST /api/v1/lost-pet-reports/{reportId}/reunion-review
```

Un moderador o administrador consulta la cola, ve el nombre y teléfono del propietario exclusivamente en el endpoint protegido, lo llama y decide. Aprobar la revisión y cambiar el reporte a `REUNITED` sucede en una transacción. Desde ese momento no aparece en la búsqueda pública predeterminada y no puede ser reabierto por el propietario.

## Controllers separados y roles

- `ModeratorController`: revisiones de reencuentro y denuncias de conversación.
- `AdminController`: áreas habilitadas, roles y también revisiones de reencuentro.
- `/api/v1/moderator/**`: requiere `SCOPE_MODERATOR` o `SCOPE_ADMIN`.
- `/api/v1/admin/**`: requiere `SCOPE_ADMIN`.

El claim JWT `scope` se genera desde el rol persistido `USER`, `MODERATOR` o `ADMIN`. Después de cambiar un rol, el usuario debe volver a autenticarse para obtener el nuevo claim. Para crear el primer administrador, después de registrar y verificar una cuenta, ejecutar una única vez con acceso restringido a PostgreSQL:

```sql
UPDATE app_user SET role='ADMIN' WHERE lower(email)=lower('administrador@dominio.co');
```

No se incluyen credenciales administrativas predeterminadas ni contraseñas en Flyway.

## Cobertura nacional y excepciones

`service_area` contiene configuraciones explícitas por `municipality_code`. Una municipalidad sin fila está habilitada por defecto; una fila con `enabled=false` bloquea la creación o edición antes de procesar imágenes. Flyway V10 migra la configuración de Armenia a `63001`.

El backend no lista municipios ni barrios. El frontend resuelve los nombres con el dataset DIVIPOLA oficial y el administrador persiste únicamente excepciones mediante:

```text
PUT /api/v1/admin/service-areas/{municipalityCode}
```

`GET /api/v1/admin/service-areas` devuelve solo las configuraciones persistidas, no los 1.122 municipios del catálogo oficial.

## Persistencia y privacidad

Flyway V7 agrega roles, áreas de servicio, solicitudes, conversaciones, participantes, mensajes, bloqueos, denuncias y revisiones de reencuentro, con índices de polling, colas y unicidad.

Las respuestas de conversaciones solo incluyen UUID y nombre visible de participantes. Teléfono, correo, documento y hashes nunca forman parte de esos DTOs. El teléfono del propietario solo aparece en la cola protegida de verificación de reencuentros.

## Arquitectura y pruebas

Los controllers se limitan a invocar casos de uso, mappers y colaboradores web. Las reglas viven en agregados y servicios de aplicación; R2DBC y Spring Security son adaptadores. ArchUnit mantiene la prohibición de dependencias desde controllers hacia dominio, servicios concretos o puertos de salida.

Las pruebas usan Mockito para dependencias inyectadas y cubren consentimiento, propiedad, bloqueo, mensajería, transición moderada a `REUNITED`, roles y límites territoriales. No se necesitó `spy`.
