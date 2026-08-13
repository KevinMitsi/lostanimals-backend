CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TABLE department (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE city (
    id UUID PRIMARY KEY,
    department_id UUID NOT NULL REFERENCES department(id),
    name VARCHAR(100) NOT NULL,
    UNIQUE (department_id, name)
);

CREATE TABLE neighborhood (
    id UUID PRIMARY KEY,
    city_id UUID NOT NULL REFERENCES city(id),
    name VARCHAR(120) NOT NULL,
    UNIQUE (city_id, name)
);

CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    document_number VARCHAR(30) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    phone VARCHAR(30) NOT NULL,
    habeas_data_accepted_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE lost_pet_report (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    pet_name VARCHAR(80) NOT NULL,
    species VARCHAR(20) NOT NULL CHECK (species IN ('DOG', 'CAT', 'BIRD', 'OTHER')),
    description VARCHAR(2000) NOT NULL,
    disappeared_at TIMESTAMPTZ NOT NULL,
    last_seen GEOGRAPHY(POINT, 4326) NOT NULL,
    neighborhood_id UUID NOT NULL REFERENCES neighborhood(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('LOST', 'REUNITED', 'CLOSED')),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE lost_pet_image (
    id UUID PRIMARY KEY,
    report_id UUID NOT NULL REFERENCES lost_pet_report(id) ON DELETE CASCADE,
    object_key VARCHAR(1024) NOT NULL UNIQUE,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    sort_order INTEGER NOT NULL CHECK (sort_order >= 0),
    UNIQUE (report_id, sort_order)
);

CREATE INDEX idx_lost_pet_report_owner_created ON lost_pet_report(owner_id, created_at DESC);
CREATE INDEX idx_lost_pet_report_search ON lost_pet_report(species, neighborhood_id, status, created_at DESC);
CREATE INDEX idx_lost_pet_report_last_seen ON lost_pet_report USING GIST(last_seen);

INSERT INTO department (id, name)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Quindío');

INSERT INTO city (id, department_id, name)
VALUES ('a2000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'Armenia');

INSERT INTO neighborhood (id, city_id, name) VALUES
    ('a3000000-0000-0000-0000-000000000001', 'a2000000-0000-0000-0000-000000000001', 'El Poblado'),
    ('a3000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000001', 'Granada'),
    ('a3000000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000001', 'Fundadores'),
    ('a3000000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000001', 'Calima'),
    ('a3000000-0000-0000-0000-000000000005', 'a2000000-0000-0000-0000-000000000001', 'San José');
