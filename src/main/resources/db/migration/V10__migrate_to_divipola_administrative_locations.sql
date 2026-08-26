ALTER TABLE lost_pet_report
    ADD COLUMN department_code VARCHAR(2),
    ADD COLUMN municipality_code VARCHAR(5),
    ADD COLUMN neighborhood VARCHAR(120);

ALTER TABLE sighting
    ADD COLUMN department_code VARCHAR(2),
    ADD COLUMN municipality_code VARCHAR(5),
    ADD COLUMN neighborhood VARCHAR(120);

DO $$
DECLARE
    unknown_cities TEXT;
BEGIN
    SELECT string_agg(DISTINCT d.name || ' / ' || c.name || ' (' || c.id || ')', ', ' ORDER BY d.name || ' / ' || c.name || ' (' || c.id || ')')
      INTO unknown_cities
      FROM (
          SELECT neighborhood_id FROM lost_pet_report
          UNION
          SELECT neighborhood_id FROM sighting
      ) publications
      JOIN neighborhood n ON n.id = publications.neighborhood_id
      JOIN city c ON c.id = n.city_id
      JOIN department d ON d.id = c.department_id
     WHERE c.id <> 'a2000000-0000-0000-0000-000000000001'::uuid;

    IF unknown_cities IS NOT NULL THEN
        RAISE EXCEPTION 'V10 cannot safely map historical cities to DIVIPOLA. Mapping required for: %', unknown_cities;
    END IF;
END $$;

UPDATE lost_pet_report report
   SET department_code = '63',
       municipality_code = '63001',
       neighborhood = btrim(regexp_replace(n.name, '\s+', ' ', 'g'))
  FROM neighborhood n
  JOIN city c ON c.id = n.city_id
 WHERE report.neighborhood_id = n.id
   AND c.id = 'a2000000-0000-0000-0000-000000000001'::uuid;

UPDATE sighting publication
   SET department_code = '63',
       municipality_code = '63001',
       neighborhood = btrim(regexp_replace(n.name, '\s+', ' ', 'g'))
  FROM neighborhood n
  JOIN city c ON c.id = n.city_id
 WHERE publication.neighborhood_id = n.id
   AND c.id = 'a2000000-0000-0000-0000-000000000001'::uuid;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM lost_pet_report
                WHERE department_code IS NULL OR municipality_code IS NULL OR neighborhood IS NULL) THEN
        RAISE EXCEPTION 'V10 backfill left lost_pet_report rows without an administrative location';
    END IF;
    IF EXISTS (SELECT 1 FROM sighting
                WHERE department_code IS NULL OR municipality_code IS NULL OR neighborhood IS NULL) THEN
        RAISE EXCEPTION 'V10 backfill left sighting rows without an administrative location';
    END IF;
END $$;

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

CREATE INDEX idx_lost_pet_report_department_code ON lost_pet_report(department_code);
CREATE INDEX idx_lost_pet_report_municipality_code ON lost_pet_report(municipality_code);
CREATE INDEX idx_lost_pet_report_location_status_created
    ON lost_pet_report(department_code, municipality_code, status, created_at DESC);
CREATE INDEX idx_lost_pet_report_neighborhood_lower ON lost_pet_report(lower(neighborhood));
CREATE INDEX idx_sighting_department_code ON sighting(department_code);
CREATE INDEX idx_sighting_municipality_code ON sighting(municipality_code);
CREATE INDEX idx_sighting_location_status_created
    ON sighting(department_code, municipality_code, status, created_at DESC);
CREATE INDEX idx_sighting_neighborhood_lower ON sighting(lower(neighborhood));

ALTER TABLE service_area ADD COLUMN municipality_code VARCHAR(5);

DO $$
DECLARE
    unknown_service_areas TEXT;
BEGIN
    SELECT string_agg(city_id::text, ', ' ORDER BY city_id::text)
      INTO unknown_service_areas
      FROM service_area
     WHERE city_id <> 'a2000000-0000-0000-0000-000000000001'::uuid;
    IF unknown_service_areas IS NOT NULL THEN
        RAISE EXCEPTION 'V10 cannot safely map service_area cities to DIVIPOLA. Mapping required for city IDs: %', unknown_service_areas;
    END IF;
END $$;

UPDATE service_area
   SET municipality_code = '63001'
 WHERE city_id = 'a2000000-0000-0000-0000-000000000001'::uuid;

ALTER TABLE service_area ALTER COLUMN municipality_code SET NOT NULL;
ALTER TABLE service_area ADD CONSTRAINT ck_service_area_municipality_code
    CHECK (municipality_code ~ '^[0-9]{5}$');
ALTER TABLE service_area ADD CONSTRAINT uk_service_area_municipality_code UNIQUE (municipality_code);

DROP INDEX IF EXISTS idx_lost_pet_report_neighborhood;
DROP INDEX IF EXISTS idx_sighting_neighborhood;
DROP INDEX IF EXISTS idx_lost_pet_report_search;

ALTER TABLE lost_pet_report DROP CONSTRAINT lost_pet_report_neighborhood_id_fkey;
ALTER TABLE sighting DROP CONSTRAINT sighting_neighborhood_id_fkey;
ALTER TABLE service_area DROP CONSTRAINT service_area_city_id_fkey;
ALTER TABLE lost_pet_report DROP COLUMN neighborhood_id;
ALTER TABLE sighting DROP COLUMN neighborhood_id;
ALTER TABLE service_area DROP CONSTRAINT service_area_pkey;
ALTER TABLE service_area DROP COLUMN city_id;
ALTER TABLE service_area ADD PRIMARY KEY (municipality_code);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
          FROM pg_constraint constraint_info
         WHERE constraint_info.contype = 'f'
           AND constraint_info.confrelid IN ('department'::regclass, 'city'::regclass, 'neighborhood'::regclass)
    ) THEN
        RAISE EXCEPTION 'V10 cannot remove the legacy geographic catalog because foreign keys still reference it';
    END IF;
END $$;

DROP TABLE neighborhood;
DROP TABLE city;
DROP TABLE department;
