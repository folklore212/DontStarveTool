-- V11: Add preset_type to world_gen_presets for surface/caves distinction
ALTER TABLE world_gen_presets ADD COLUMN preset_type VARCHAR(16) DEFAULT 'surface' COMMENT 'surface, caves';
UPDATE world_gen_presets SET preset_type = 'surface' WHERE preset_type IS NULL;
