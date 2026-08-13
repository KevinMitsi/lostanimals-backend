# Fase 2 — Gestión integral de reportes

## Flujo de imágenes privadas

1. El frontend calcula SHA-256 del archivo y lo codifica en Base64.
2. Solicita `POST /api/v1/lost-pet-reports/image-uploads` con nombre, MIME, bytes y checksum.
3. La API devuelve una URL S3 `PUT` válida por 10 minutos y los encabezados firmados obligatorios.
4. El frontend hace `PUT` del archivo directamente a S3 usando exactamente esos encabezados.
5. Al crear el reporte o agregar una imagen, envía el `objectKey` de `staging`.
6. La API comprueba propietario, tamaño, checksum almacenado por S3 y firma binaria JPEG/PNG.
7. La imagen se decodifica con límite de 40 megapíxeles/12.000 px, se reduce a máximo 2.048 px y se reescribe sin EXIF/GPS.
8. Se almacena una copia privada sanitizada y se elimina el original de `staging`.
9. Las consultas entregan URLs `GET` firmadas por 15 minutos; nunca URLs públicas permanentes.

El checksum forma parte de la firma `PUT` y de la clave, por lo que el contenido no puede sustituirse después de ser aprobado. Los objetos S3 permanecen privados.

## API

| Método | Ruta | Acceso | Uso |
|---|---|---|---|
| `POST` | `/api/v1/lost-pet-reports/image-uploads` | JWT | Preparar carga directa |
| `POST` | `/api/v1/lost-pet-reports` | JWT | Crear reporte con 1–5 claves staging |
| `GET` | `/api/v1/lost-pet-reports` | Público | Búsqueda paginada |
| `GET` | `/api/v1/lost-pet-reports/{id}` | Público | Detalle con coordenadas aproximadas |
| `GET` | `/api/v1/lost-pet-reports/mine` | JWT | Reportes propios con ubicación exacta |
| `PUT` | `/api/v1/lost-pet-reports/{id}` | JWT/propietario | Editar reporte activo |
| `PATCH` | `/api/v1/lost-pet-reports/{id}/status` | JWT/propietario | Reunido, cerrado o reapertura ≤30 días |
| `POST` | `/api/v1/lost-pet-reports/{id}/images` | JWT/propietario | Agregar imagen sanitizada |
| `DELETE` | `/api/v1/lost-pet-reports/{id}/images/{imageId}` | JWT/propietario | Eliminar conservando mínimo una |
| `PUT` | `/api/v1/lost-pet-reports/{id}/images/{imageId}/primary` | JWT/propietario | Elegir portada |

La paginación usa cursor estable `(created_at,id)` y pide como máximo 50 elementos. Las coordenadas públicas se redondean a tres decimales; la ubicación exacta solo se devuelve al propietario autenticado.

## Configuración del bucket S3

- Activar **Block Public Access** en las cuatro opciones.
- No agregar ACL públicas ni políticas `Principal: *` para lectura.
- La identidad del backend necesita únicamente `s3:PutObject`, `s3:GetObject`, `s3:HeadObject` y `s3:DeleteObject` sobre `lost-pet-reports/*` y `sightings/*`.
- Crear reglas lifecycle que eliminen `lost-pet-reports/staging/` y `sightings/staging/` después de un día para limpiar cargas abandonadas.
- Configurar CORS sustituyendo el origen por el frontend real:

```json
[
  {
    "AllowedOrigins": ["https://app.example.com"],
    "AllowedMethods": ["PUT", "GET", "HEAD"],
    "AllowedHeaders": ["content-type", "x-amz-checksum-sha256"],
    "ExposeHeaders": ["ETag", "x-amz-checksum-sha256"],
    "MaxAgeSeconds": 600
  }
]
```

CORS no concede acceso al bucket: la autorización sigue estando en la firma temporal de cada URL.

## Integridad y concurrencia

- `version` aplica optimistic locking a toda modificación del agregado.
- La imagen principal está protegida por un índice único parcial por reporte.
- `object_key` es único, por lo que una imagen sanitizada no puede asociarse dos veces.
- Actualizar reporte e imágenes ocurre dentro de una transacción R2DBC.
- Si falla persistencia tras sanitizar, la aplicación intenta eliminar los objetos finales compensatorios.
