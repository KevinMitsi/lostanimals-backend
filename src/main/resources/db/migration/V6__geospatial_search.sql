CREATE INDEX idx_lost_pet_report_disappeared_at ON lost_pet_report(disappeared_at DESC);
CREATE INDEX idx_lost_pet_report_neighborhood ON lost_pet_report(neighborhood_id);
CREATE INDEX idx_sighting_observed_at ON sighting(observed_at DESC);
CREATE INDEX idx_sighting_neighborhood ON sighting(neighborhood_id);
CREATE INDEX idx_city_department ON city(department_id);
CREATE INDEX idx_neighborhood_city ON neighborhood(city_id);
