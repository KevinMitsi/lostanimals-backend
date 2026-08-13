# Animales Perdidos Colombia

Backend para ayudar a reunir animales perdidos con sus familias después de una emergencia. La primera fase opera en Armenia, Quindío, pero el modelo geográfico permite crecer por ciudad y departamento sin cambiar el dominio.

## Arquitectura

El proyecto aplica arquitectura limpia/hexagonal. Las dependencias siempre apuntan hacia adentro:

```text
infrastructure (WebFlux, R2DBC, S3, JavaMail, Flyway)
                 ↓ implementa / invoca puertos
application (casos de uso y puertos de entrada/salida)
                 ↓
domain (reglas y modelos Java puros)
```

- `domain`: agregado `LostPetReport`, objetos de valor y reglas invariantes. No conoce Spring ni infraestructura.
- `application`: coordina el caso de uso `ReportLostPetUseCase`. Los puertos abstraen persistencia, imágenes y notificaciones.
- `infrastructure.adapter.web`: Controller GRASP; traduce HTTP al caso de uso sin contener reglas de negocio.
- `infrastructure.adapter.persistence`: PostgreSQL/PostGIS mediante R2DBC.
- `infrastructure.adapter.storage`: implementación S3 de `ImageStoragePort`. Cambiar S3 por otro proveedor solo requiere otro adaptador.
- `infrastructure.adapter.notification`: JavaMail o logging detrás de `NotificationPort`. Cambiar la librería de correo no modifica el caso de uso.
- `infrastructure.adapter.security`: BCrypt y Nimbus JWT detrás de puertos de aplicación.
- `infrastructure.adapter.cloudflare`: validación reactiva de Turnstile detrás de `BotVerificationPort`.
- `infrastructure.adapter.persistence.entity`: entidades de persistencia separadas del modelo de dominio; MapStruct realiza la conversión.
- `infrastructure.config`: composición de dependencias (el único lugar que construye el servicio de aplicación).

Las fronteras asíncronas usan `CompletionStage`, parte de Java 21. Reactor queda limitado a los adaptadores WebFlux/R2DBC.

## Requisitos y ejecución

- Java 21
- PostgreSQL con PostGIS
- Un bucket S3 y credenciales AWS disponibles en la cadena estándar del AWS SDK

Variables principales:

```text
R2DBC_URL=r2dbc:postgresql://localhost:5432/animales_perdidos
JDBC_URL=jdbc:postgresql://localhost:5432/animales_perdidos
DB_USER=postgres
DB_PASSWORD=postgres
S3_BUCKET=animales-perdidos-dev
AWS_REGION=us-east-1
EMAIL_NOTIFICATIONS_ENABLED=false
JWT_SECRET=<mínimo 32 bytes aleatorios>
CLOUDFLARE_TURNSTILE_ENABLED=true
CLOUDFLARE_TURNSTILE_SECRET=<secret del widget>
CLOUDFLARE_EXPECTED_HOSTNAME=app.example.com
```

Flyway crea el esquema, habilita PostGIS y carga Armenia junto con cinco barrios iniciales. Antes de producción se debe reemplazar esa semilla mínima por el catálogo oficial completo.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

## Autenticación y APIs

- `POST /api/v1/auth/register`: público; registra correo, contraseña, celular y cédula después de validar Turnstile.
- `POST /api/v1/auth/login`: público; devuelve un JWT Bearer.
- `POST /api/v1/auth/verify-email` y `/resend-verification`: ciclo de verificación.
- `POST /api/v1/auth/forgot-password` y `/reset-password`: recuperación segura.
- `POST /api/v1/auth/refresh` y `/logout`: rotación y revocación de sesiones.
- `POST /api/v1/lost-pet-reports`: requiere `Authorization: Bearer <jwt>`.
- OpenAPI JSON: `/v3/api-docs`.
- Swagger UI: `/swagger-ui.html`.

Las contraseñas se procesan con BCrypt de coste 12 en `boundedElastic`, de modo que el cálculo intensivo no bloquea los event loops de WebFlux. Solo el hash se almacena. Correo, celular y cédula tienen restricciones únicas en PostgreSQL; el correo se normaliza en minúsculas y también posee índice único sobre `lower(email)`.

La implementación y contratos de la primera fase están descritos en [docs/FASE_1_IDENTIDAD.md](docs/FASE_1_IDENTIDAD.md). El orden completo del producto vive en [docs/ROADMAP.md](docs/ROADMAP.md).

## Reportes de mascotas

`POST /api/v1/lost-pet-reports`, tipo `multipart/form-data`:

- encabezado `Authorization: Bearer <jwt>`; el dueño se obtiene del claim firmado `sub`;
- parte `metadata`: JSON con `petName`, `species`, `description`, `disappearedAt`, `latitude`, `longitude` y `neighborhoodId`;
- una o más partes `images` (máximo cinco).

La configuración de Turnstile, WAF, rate limiting y aislamiento del origen está en [docs/CLOUDFLARE.md](docs/CLOUDFLARE.md).

## Decisiones de integridad

- Máximo tres reportes por usuario cada 24 horas.
- Se rechaza un reporte activo con igual dueño, especie y nombre dentro de 24 horas.
- Entre una y cinco imágenes por reporte.
- Si persiste el reporte falla, el caso de uso compensa eliminando de S3 las imágenes subidas.
- Los datos de contacto están separados y no aparecen en el contrato público.

La siguiente vertical recomendada es recuperación/verificación de correo y contraseña; después, avistamientos con detección espacio-temporal y búsqueda por radio PostGIS.
