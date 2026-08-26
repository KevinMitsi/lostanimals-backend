# Fase 4 — Búsqueda geoespacial

## Alcance terminado

Los listados públicos y propios de reportes y avistamientos ahora permiten combinar:

- especie y estado;
- departamento y municipio por código DIVIPOLA, y barrio textual;
- fecha inicial y final;
- centro geográfico y radio;
- paginación estable mediante cursor.

La búsqueda radial usa `ST_DWithin` sobre columnas `GEOGRAPHY(POINT, 4326)`, por lo que el radio se expresa directamente en metros. Se acepta entre 100 y 50.000 metros. `latitude`, `longitude` y `radiusMeters` son un conjunto indivisible: deben enviarse los tres o ninguno.

En reportes, `from` y `to` filtran `disappeared_at`; en avistamientos filtran `observed_at`. Los filtros territoriales consultan directamente `department_code`, `municipality_code` y `lower(neighborhood)`. Se permite departamento sin municipio o municipio sin departamento; cuando ambos aparecen deben ser consistentes.

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
GET /api/v1/sightings?species=DOG&departmentCode=63&municipalityCode=63001&neighborhood=Granada&latitude=4.5339&longitude=-75.6811&radiusMeters=5000&from=2026-08-01T00:00:00Z&limit=20
```

El cursor representa `(created_at,id)`, está codificado en Base64 URL-safe y es opaco para el cliente. Un cursor mal formado produce una validación de negocio; el límite máximo es 50.

## Catálogo geográfico

Las rutas locales `/api/v1/geography/**` fueron retiradas. El frontend consulta directamente `https://www.datos.gov.co/api/v3/views/gdxc-w37w/query.json`, usando `cod_dpto`, `dpto`, `cod_mpio` y `nom_mpio`. El backend persiste solamente los códigos y el barrio libre normalizado.

## Arquitectura y consultas

- `GeoSearchArea` protege los límites del radio sin depender de Spring o PostGIS.
- `SearchCriteriaPolicy` valida combinaciones, fechas y cursores dentro de aplicación.
- Los repositorios reciben criterios ya válidos y construyen las consultas PostGIS.
- Flyway V10 migra los UUID históricos, agrega índices DIVIPOLA y de barrio, y conserva los índices GiST geográficos.
- Los controllers no contienen lógica de negocio ni construyen criterios: delegan en casos de uso, mappers, el resolvedor de identidad y la fábrica de respuestas HTTP.
- ArchUnit impide que clases `*Controller` dependan de servicios de aplicación, puertos de salida o dominio.

## Pruebas

La suite verifica límites del objeto geográfico, traducción de filtros DIVIPOLA a criterios de repositorio, privacidad público/propietario, combinaciones incompletas, cursor y reglas de arquitectura.
