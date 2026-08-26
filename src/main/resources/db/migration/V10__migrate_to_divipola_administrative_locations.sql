-- 1. Agregar las nuevas columnas a las tablas principales
ALTER TABLE lost_pet_report
    ADD COLUMN IF NOT EXISTS department_code VARCHAR(2),
    ADD COLUMN IF NOT EXISTS municipality_code VARCHAR(5),
    ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(120);

ALTER TABLE sighting
    ADD COLUMN IF NOT EXISTS department_code VARCHAR(2),
    ADD COLUMN IF NOT EXISTS municipality_code VARCHAR(5),
    ADD COLUMN IF NOT EXISTS neighborhood VARCHAR(120);

ALTER TABLE service_area
    ADD COLUMN IF NOT EXISTS municipality_code VARCHAR(5);

-- 2. Migrar los datos existentes del catálogo viejo al formato DIVIPOLA (Armenia - Quindío: '63', '63001')
UPDATE lost_pet_report report
SET department_code = '63',
    municipality_code = '63001',
    neighborhood = btrim(regexp_replace(n.name, '\s+', ' ', 'g'))
    FROM neighborhood n
WHERE report.neighborhood_id = n.id;

UPDATE sighting publication
SET department_code = '63',
    municipality_code = '63001',
    neighborhood = btrim(regexp_replace(n.name, '\s+', ' ', 'g'))
    FROM neighborhood n
WHERE publication.neighborhood_id = n.id;

UPDATE service_area
SET municipality_code = '63001'
WHERE city_id IS NOT NULL;

-- 3. Aplicar restricciones NOT NULL y Validaciones CHECK a los nuevos campos
ALTER TABLE lost_pet_report
    ALTER COLUMN department_code SET NOT NULL,
ALTER COLUMN municipality_code SET NOT NULL,
    ALTER COLUMN neighborhood SET NOT NULL,
    ADD CONSTRAINT ck_lost_pet_report_department_code CHECK (department_code ~ '^[0-9]{2}$'),
    ADD CONSTRAINT ck_lost_pet_report_municipality_code CHECK (municipality_code ~ '^[0-9]{5}$'),
    ADD CONSTRAINT ck_lost_pet_report_municipality_department CHECK (left(municipality_code, 2) = department_code),
    ADD CONSTRAINT ck_lost_pet_report_neighborhood CHECK (btrim(neighborhood) <> '');

ALTER TABLE sighting
    ALTER COLUMN department_code SET NOT NULL,
ALTER COLUMN municipality_code SET NOT NULL,
    ALTER COLUMN neighborhood SET NOT NULL,
    ADD CONSTRAINT ck_sighting_department_code CHECK (department_code ~ '^[0-9]{2}$'),
    ADD CONSTRAINT ck_sighting_municipality_code CHECK (municipality_code ~ '^[0-9]{5}$'),
    ADD CONSTRAINT ck_sighting_municipality_department CHECK (left(municipality_code, 2) = department_code),
    ADD CONSTRAINT ck_sighting_neighborhood CHECK (btrim(neighborhood) <> '');

ALTER TABLE service_area
    ALTER COLUMN municipality_code SET NOT NULL,
    ADD CONSTRAINT ck_service_area_municipality_code CHECK (municipality_code ~ '^[0-9]{5}$'),
    ADD CONSTRAINT uk_service_area_municipality_code UNIQUE (municipality_code);

-- 4. Crear los nuevos índices de búsqueda optimizados
CREATE INDEX IF NOT EXISTS idx_lost_pet_report_department_code ON lost_pet_report(department_code);
CREATE INDEX IF NOT EXISTS idx_lost_pet_report_municipality_code ON lost_pet_report(municipality_code);
CREATE INDEX IF NOT EXISTS idx_lost_pet_report_location_status_created
    ON lost_pet_report(department_code, municipality_code, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_lost_pet_report_neighborhood_lower ON lost_pet_report(lower(neighborhood));

CREATE INDEX IF NOT EXISTS idx_sighting_department_code ON sighting(department_code);
CREATE INDEX IF NOT EXISTS idx_sighting_municipality_code ON sighting(municipality_code);
CREATE INDEX IF NOT EXISTS idx_sighting_location_status_created
    ON sighting(department_code, municipality_code, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sighting_neighborhood_lower ON sighting(lower(neighborhood));

-- 5. Eliminar índices obsoletos
DROP INDEX IF EXISTS idx_lost_pet_report_neighborhood;
DROP INDEX IF EXISTS idx_sighting_neighborhood;
DROP INDEX IF EXISTS idx_lost_pet_report_search;
DROP INDEX IF EXISTS idx_city_department;
DROP INDEX IF EXISTS idx_neighborhood_city;

-- 6. Eliminar Foreign Keys viejas de las tablas de negocio y catálogo
ALTER TABLE lost_pet_report DROP CONSTRAINT IF EXISTS lost_pet_report_neighborhood_id_fkey;
ALTER TABLE sighting DROP CONSTRAINT IF EXISTS sighting_neighborhood_id_fkey;
ALTER TABLE service_area DROP CONSTRAINT IF EXISTS service_area_city_id_fkey;
ALTER TABLE neighborhood DROP CONSTRAINT IF EXISTS neighborhood_city_id_fkey;
ALTER TABLE city DROP CONSTRAINT IF EXISTS city_department_id_fkey;

-- 7. Eliminar columnas viejas y actualizar Primary Key de service_area
ALTER TABLE lost_pet_report DROP COLUMN IF EXISTS neighborhood_id;
ALTER TABLE sighting DROP COLUMN IF EXISTS neighborhood_id;
ALTER TABLE service_area DROP CONSTRAINT IF EXISTS service_area_pkey;
ALTER TABLE service_area DROP COLUMN IF EXISTS city_id;
ALTER TABLE service_area ADD PRIMARY KEY (municipality_code);

-- 8. Eliminar las tablas legacy en cascada
DROP TABLE IF EXISTS neighborhood CASCADE;
DROP TABLE IF EXISTS city CASCADE;
DROP TABLE IF EXISTS department CASCADE;