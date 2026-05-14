-- V9: Templates system + Steam Workshop cache

-- 1. Templates table (both server templates and world gen presets)
CREATE TABLE IF NOT EXISTS templates (
    id              BIGINT PRIMARY KEY,
    author_id       BIGINT NOT NULL,
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    template_type   VARCHAR(32) NOT NULL COMMENT 'server_template, world_gen',
    category        VARCHAR(32) COMMENT 'survival, pvp, caves, modpack, endless, custom',
    game_mode       VARCHAR(32) DEFAULT 'survival',
    max_players     INT DEFAULT 6,
    tags            VARCHAR(512),
    cover_image     VARCHAR(512) COMMENT 'Cover/banner image URL',
    config_json     JSON COMMENT 'Full DST cluster config JSON for server templates; world gen overrides for world_gen type',
    mod_list        JSON COMMENT 'List of workshop mod IDs',
    version         INT DEFAULT 1,
    download_count  INT DEFAULT 0,
    rating_avg      DECIMAL(3,2) DEFAULT 0.00,
    rating_count    INT DEFAULT 0,
    status          VARCHAR(16) DEFAULT 'published' COMMENT 'published, draft, archived',
    verified        TINYINT DEFAULT 0,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted_at      BIGINT DEFAULT 0,
    INDEX idx_template_type (template_type, deleted_at),
    INDEX idx_template_category (category),
    INDEX idx_template_author (author_id),
    INDEX idx_template_downloads (download_count),
    INDEX idx_template_rating (rating_avg),
    INDEX idx_template_status (status, deleted_at)
);

-- 2. World gen preset settings (rich details for each preset)
CREATE TABLE IF NOT EXISTS world_gen_presets (
    id              BIGINT PRIMARY KEY,
    template_id     BIGINT COMMENT 'FK to templates.id when this is a standalone world_gen preset',
    name            VARCHAR(128) NOT NULL,
    description     TEXT,
    preview_image   VARCHAR(512) COMMENT 'Preview image showing world gen style',
    -- World gen parameters (DST leveldataoverride fields)
    world_size      VARCHAR(16) DEFAULT 'default' COMMENT 'small, medium, default, large, huge',
    branching       VARCHAR(16) DEFAULT 'default' COMMENT 'never, least, default, most, random',
    loop_mode       VARCHAR(16) DEFAULT 'default' COMMENT 'never, default, always',
    season_start    VARCHAR(16) DEFAULT 'default' COMMENT 'default, winter, spring, summer, autumn',
    day_mode        VARCHAR(16) DEFAULT 'default' COMMENT 'default, longday, longdusk, longnight, onlyday, onlydusk, onlynight',
    autumn_length   VARCHAR(16) DEFAULT 'default' COMMENT 'noseason, veryshortseason, shortseason, default, longseason, verylongseason, random',
    winter_length   VARCHAR(16) DEFAULT 'default',
    spring_length   VARCHAR(16) DEFAULT 'default',
    summer_length   VARCHAR(16) DEFAULT 'default',
    resource_variety VARCHAR(16) DEFAULT 'default' COMMENT 'classic, default, highlyrandom',
    -- Extra settings as JSON for future extensibility
    extra_settings  JSON COMMENT 'Additional world gen overrides (boons, traps, mobs, etc.)',
    sort_order      INT DEFAULT 0,
    created_at      DATETIME,
    updated_at      DATETIME,
    deleted_at      BIGINT DEFAULT 0,
    INDEX idx_wgp_template (template_id)
);

-- 3. Binding table: server templates can include multiple world gen presets
CREATE TABLE IF NOT EXISTS template_world_gen_bindings (
    id                  BIGINT PRIMARY KEY,
    server_template_id  BIGINT NOT NULL COMMENT 'FK to templates.id (server_template type)',
    world_gen_preset_id BIGINT NOT NULL COMMENT 'FK to world_gen_presets.id',
    shard_type          VARCHAR(16) DEFAULT 'master' COMMENT 'master, caves',
    sort_order          INT DEFAULT 0,
    created_at          DATETIME,
    UNIQUE KEY uk_binding (server_template_id, world_gen_preset_id, shard_type),
    INDEX idx_binding_template (server_template_id),
    INDEX idx_binding_preset (world_gen_preset_id)
);

-- 4. Steam Workshop cache table
CREATE TABLE IF NOT EXISTS steam_workshop_cache (
    id              BIGINT PRIMARY KEY,
    workshop_id     VARCHAR(32) NOT NULL,
    title           VARCHAR(256),
    description     TEXT,
    preview_url     VARCHAR(512),
    author_name     VARCHAR(128),
    subscriptions   INT DEFAULT 0,
    favorited       INT DEFAULT 0,
    tags            VARCHAR(512),
    last_updated    DATETIME COMMENT 'Last Steam data refresh',
    created_at      DATETIME,
    UNIQUE KEY uk_workshop_id (workshop_id),
    INDEX idx_subscriptions (subscriptions DESC)
);
