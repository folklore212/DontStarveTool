-- V13: Add author_id to world_gen_presets for ownership tracking
ALTER TABLE world_gen_presets ADD COLUMN author_id BIGINT AFTER template_id;
UPDATE world_gen_presets SET author_id = 0 WHERE author_id IS NULL;
