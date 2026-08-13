# Fase 3 — Avistamientos

## Alcance terminado

La vertical permite preparar y sanitizar imágenes privadas, publicar avistamientos, consultar el listado público o los propios, editar, cerrar y administrar entre una y cinco imágenes. Todos los comandos de modificación exigen JWT y comprueban que el usuario autenticado sea el reportante.

La lógica permanece aislada mediante `CreateSightingUseCase`, `ManageSightingUseCase`, `QuerySightingsUseCase`, `PrepareSightingImageUploadUseCase`, `SightingRepository` e `ImageStoragePort`. WebFlux, R2DBC, PostGIS y S3 son adaptadores sustituibles.

## Posibles duplicados

Antes de procesar imágenes o crear el registro se consulta PostGIS buscando un avistamiento `ACTIVE`:

- de la misma especie;
- observado dentro de una ventana de ±2 horas;
- ubicado a una distancia máxima de 50 metros.

Si existe, `POST /api/v1/sightings` responde `200` con `created=false` y los datos mínimos del candidato. No crea el avistamiento ni toca S3. El usuario puede revisar la advertencia y reenviar la solicitud con `confirmPossibleDuplicate=true`; en ese caso se publica y responde `201`.

## API

| Método | Ruta | Acceso | Uso |
|---|---|---|---|
| `POST` | `/api/v1/sightings/image-uploads` | JWT | Preparar carga directa |
| `POST` | `/api/v1/sightings` | JWT | Publicar o advertir posible duplicado |
| `GET` | `/api/v1/sightings` | Público | Buscar con filtros y cursor |
| `GET` | `/api/v1/sightings/{id}` | Público | Detalle con ubicación aproximada |
| `GET` | `/api/v1/sightings/mine` | JWT | Propios con ubicación exacta |
| `PUT` | `/api/v1/sightings/{id}` | JWT/propietario | Editar mientras esté activo |
| `PATCH` | `/api/v1/sightings/{id}/close` | JWT/propietario | Cerrar definitivamente |
| `POST` | `/api/v1/sightings/{id}/images` | JWT/propietario | Agregar imagen sanitizada |
| `DELETE` | `/api/v1/sightings/{id}/images/{imageId}` | JWT/propietario | Eliminar conservando al menos una |
| `PUT` | `/api/v1/sightings/{id}/images/{imageId}/primary` | JWT/propietario | Elegir portada |

Los filtros disponibles son especie, barrio y estado. La paginación usa el cursor estable `(created_at,id)`, acepta como máximo 50 elementos y la ubicación pública se redondea a tres decimales. Las imágenes se sirven mediante URL firmada válida por 15 minutos.

## Persistencia e imágenes

Flyway V5 crea `sighting` y `sighting_image`, índices de cursor y reportante, un índice GiST parcial para ubicaciones activas y un índice para la detección temporal. El índice único parcial garantiza una sola imagen principal; `object_key` es único y `version` implementa optimistic locking. La actualización del agregado y sus imágenes es transaccional.

S3 usa prefijos independientes:

- `sightings/staging/users/{usuario}/...` para cargas pendientes;
- `sightings/users/{usuario}/...` para copias sanitizadas privadas.

Aplican las mismas garantías de Fase 2: checksum SHA-256 firmado, verificación binaria JPEG/PNG, máximo 8 MB, defensa contra bombas de imagen, reducción a 2.048 px, eliminación de EXIF/GPS y compensación si falla persistencia. La regla lifecycle del bucket debe borrar también `sightings/staging/` después de un día.

## Pruebas

La suite cubre invariantes y ciclo de vida del agregado, advertencia y confirmación de duplicado, propiedad de comandos, privacidad de coordenadas, URLs firmadas y preparación de cargas. Las dependencias inyectadas se simulan con Mockito; no se usaron `spy` porque ningún escenario necesitó observar parcialmente una implementación real.
