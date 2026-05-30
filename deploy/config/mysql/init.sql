-- MySQL container init: only creates databases. Flyway handles schema + seed data.
CREATE DATABASE IF NOT EXISTS auth_system CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
