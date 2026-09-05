package com.herdcommand.api.persistence;

import com.herdcommand.api.support.TestJwtConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestJwtConfig.class)
class FlywayMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void configurationFoundationMigrationIsApplied() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(installed_rank) FROM flyway_schema_history",
                Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(1);

        Integer rows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM herdcommand_schema_info WHERE module = 'configuration-foundation'",
                Integer.class);
        assertThat(rows).isEqualTo(1);
    }
}
