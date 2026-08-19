# Contrato backend para el mapa de avistamientos

El mapa consume `GET /api/v1/sightings` con `status=ACTIVE`, `latitude`, `longitude`, `radiusMeters` y `limit`. El contrato ya entrega por cada punto el identificador, coordenadas públicas, imágenes con URL firmada, `createdAt` y demás datos del detalle.

## Garantías

- `latitude`, `longitude` y `radiusMeters` son un conjunto indivisible validado en aplicación.
- El radio válido es de 100 a 50.000 metros y el límite máximo es 50 elementos.
- PostGIS usa `GEOGRAPHY(POINT,4326)`, `ST_DWithin` e índice GiST.
- La búsqueda pública filtra y responde usando una coordenada aproximada a tres decimales. El prefiltro exacto sólo conserva el uso del índice y no revela el punto real.
- La ruta autenticada `/mine` conserva precisión completa exclusivamente para el reportante.
- Las fotos permanecen privadas; la API emite URLs firmadas con expiración de 15 minutos.
- La creación valida latitud/longitud tanto en la capa web como en el objeto de dominio y persiste el punto con SRID 4326.
- Las restricciones y claves foráneas de PostgreSQL mantienen integridad de avistamientos, barrios e imágenes.

El frontend debe navegar al detalle con el `id` recibido y no intentar inferir ni almacenar una ubicación más precisa a partir del listado público.
