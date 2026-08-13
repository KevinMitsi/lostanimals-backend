# Fase 4 — Búsqueda geoespacial

## Alcance terminado

Los listados públicos y propios de reportes y avistamientos ahora permiten combinar:

- especie y estado;
- departamento, ciudad y barrio;
- fecha inicial y final;
- centro geográfico y radio;
- paginación estable mediante cursor.

La búsqueda radial usa `ST_DWithin` sobre columnas `GEOGRAPHY(POINT, 4326)`, por lo que el radio se expresa directamente en metros. Se acepta entre 100 y 50.000 metros. `latitude`, `longitude` y `radiusMeters` son un conjunto indivisible: deben enviarse los tres o ninguno.

En reportes, `from` y `to` filtran `disappeared_at`; en avistamientos filtran `observed_at`. Los filtros territoriales se resuelven mediante las relaciones barrio → ciudad → departamento y pueden combinarse entre sí.

## Privacidad de ubicación

Las búsquedas públicas no solo muestran coordenadas redondeadas a tres decimales: también calculan la pertenencia al radio usando esa ubicación aproximada. Esto evita recuperar la ubicación exacta mediante consultas repetidas de triangulación.

Para conservar el índice GiST, PostgreSQL primero aplica un prefiltro sobre la geografía exacta con 100 metros de tolerancia y después verifica el punto redondeado. Las rutas `/mine` usan la coordenada exacta tanto para filtrar como para responder, porque el usuario consulta sus propios registros.

## API de búsqueda

Las rutas existentes aceptan los nuevos query parameters:

- `GET /api/v1/lost-pet-reports`
- `GET /api/v1/lost-pet-reports/mine`
- `GET /api/v1/sightings`
- `GET /api/v1/sightings/mine`

Ejemplo:

```text
GET /api/v1/sightings?species=DOG&departmentId=a1000000-0000-0000-0000-000000000001&cityId=a2000000-0000-0000-0000-000000000001&latitude=4.5339&longitude=-75.6811&radiusMeters=5000&from=2026-08-01T00:00:00Z&limit=20
```

El cursor representa `(created_at,id)`, está codificado en Base64 URL-safe y es opaco para el cliente. Un cursor mal formado produce una validación de negocio; el límite máximo es 50.

## Catálogo geográfico público

El frontend puede construir filtros dependientes sin conocer IDs previamente:

| Método | Ruta | Resultado |
|---|---|---|
| `GET` | `/api/v1/geography/departments` | Departamentos disponibles |
| `GET` | `/api/v1/geography/cities?departmentId={id}` | Ciudades del departamento |
| `GET` | `/api/v1/geography/neighborhoods?cityId={id}` | Barrios de la ciudad |

El catálogo posee puerto de salida, caso de uso y adaptador R2DBC propios. Los DTOs de respuesta están separados y MapStruct realiza la traducción web.

## Arquitectura y consultas

- `GeoSearchArea` protege los límites del radio sin depender de Spring o PostGIS.
- `SearchCriteriaPolicy` valida combinaciones, fechas y cursores dentro de aplicación.
- Los repositorios reciben criterios ya válidos y construyen las consultas PostGIS.
- Flyway V6 agrega índices temporales, territoriales y de claves foráneas; los índices GiST geográficos existentes se conservan.
- Los controllers no contienen lógica de negocio ni construyen criterios: delegan en casos de uso, mappers, el resolvedor de identidad y la fábrica de respuestas HTTP.
- ArchUnit impide que clases `*Controller` dependan de servicios de aplicación, puertos de salida o dominio.

## Pruebas

La suite verifica límites del objeto geográfico, traducción de filtros a criterios de repositorio, privacidad público/propietario, combinaciones incompletas, catálogo geográfico, cursor y reglas de arquitectura. Las dependencias inyectadas se simulan con Mockito; no fue necesario utilizar `spy`.
