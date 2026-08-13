# Fase 5 — Contacto seguro y MVP Armenia

## Alcance terminado

El MVP permite que la comunidad de Armenia publique mascotas perdidas y avistamientos, solicite contacto con consentimiento y converse dentro de la plataforma. No se entregan automáticamente correo, teléfono ni cédula entre usuarios.

El flujo es:

1. Un usuario autenticado solicita contacto sobre un reporte `LOST` o un avistamiento `ACTIVE`.
2. El creador consulta sus solicitudes recibidas y acepta o rechaza.
3. Aceptar actualiza la solicitud y crea la conversación con sus dos participantes en una transacción.
4. Los participantes almacenan y consultan mensajes por polling con cursor.
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
| `GET` | `/api/v1/conversations/{id}/messages?after={cursor}` | Polling de mensajes |
| `POST` | `/api/v1/conversations/{id}/messages` | Enviar mensaje almacenado |
| `PATCH` | `/api/v1/conversations/{id}/close` | Cerrar conversación |
| `PATCH` | `/api/v1/conversations/{id}/block` | Bloquear y cerrar |
| `POST` | `/api/v1/conversations/{id}/reports` | Denunciar conversación |

El polling devuelve hasta 100 mensajes en orden ascendente. El cursor opaco representa `(created_at,id)`. WebSocket podrá implementarse después como otro adaptador sobre los mismos casos de uso y mensajes, sin modificar el dominio.

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

## Armenia y expansión futura

`service_area` contiene las ciudades habilitadas. Flyway V7 habilita únicamente Armenia. Crear o editar reportes y avistamientos consulta `ServiceAreaRepository`; una zona deshabilitada se rechaza antes de procesar imágenes.

El catálogo público solo muestra departamentos, ciudades y barrios habilitados. El administrador puede preparar nuevas ciudades en el catálogo y activarlas mediante:

```text
PUT /api/v1/admin/service-areas/{cityId}
```

Así la expansión territorial es configuración y datos, no un cambio en los casos de publicación.

## Persistencia y privacidad

Flyway V7 agrega roles, áreas de servicio, solicitudes, conversaciones, participantes, mensajes, bloqueos, denuncias y revisiones de reencuentro, con índices de polling, colas y unicidad.

Las respuestas de conversaciones solo incluyen UUID y nombre visible de participantes. Teléfono, correo, documento y hashes nunca forman parte de esos DTOs. El teléfono del propietario solo aparece en la cola protegida de verificación de reencuentros.

## Arquitectura y pruebas

Los controllers se limitan a invocar casos de uso, mappers y colaboradores web. Las reglas viven en agregados y servicios de aplicación; R2DBC y Spring Security son adaptadores. ArchUnit mantiene la prohibición de dependencias desde controllers hacia dominio, servicios concretos o puertos de salida.

Las pruebas usan Mockito para dependencias inyectadas y cubren consentimiento, propiedad, bloqueo, mensajería, transición moderada a `REUNITED`, roles y límites territoriales. No se necesitó `spy`.
