package com.iccuu.general_web_backend.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class FlywayMigrationIntegrationTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("auth_system").withUsername("test").withPassword("test")
            .withCommand("--log_bin_trust_function_creators=1");

    @Test
    void allMigrationsApplyCleanly() {
        Flyway flyway = Flyway.configure()
                .dataSource(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        assertTrue(flyway.info().applied().length > 0,
                "at least 1 migration should apply, got " + flyway.info().applied().length);
    }
}
