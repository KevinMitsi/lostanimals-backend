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
```

Flyway crea el esquema, habilita PostGIS y carga Armenia junto con cinco barrios iniciales. Antes de producción se debe reemplazar esa semilla mínima por el catálogo oficial completo.

```powershell
.\gradlew.bat test
.\gradlew.bat bootRun
```

## Endpoint inicial

`POST /api/v1/lost-pet-reports`, tipo `multipart/form-data`:

- encabezado temporal `X-Owner-Id`: UUID del usuario autenticado;
- parte `metadata`: JSON con `petName`, `species`, `description`, `disappearedAt`, `latitude`, `longitude` y `neighborhoodId`;
- una o más partes `images` (máximo cinco).

`X-Owner-Id` es únicamente un contrato provisional de esta primera vertical. Al implementar autenticación, el Controller debe obtener el usuario del principal JWT y nunca confiar en un identificador enviado por el cliente.

## Decisiones de integridad

- Máximo tres reportes por usuario cada 24 horas.
- Se rechaza un reporte activo con igual dueño, especie y nombre dentro de 24 horas.
- Entre una y cinco imágenes por reporte.
- Si persiste el reporte falla, el caso de uso compensa eliminando de S3 las imágenes subidas.
- Los datos de contacto están separados y no aparecen en el contrato público.

La siguiente vertical recomendada es registro/autenticación y consentimiento de tratamiento de datos; después, avistamientos con detección espacio-temporal y búsqueda por radio PostGIS.
