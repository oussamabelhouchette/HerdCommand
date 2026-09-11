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

    @Test
    void farmMembershipFoundationMigrationIsApplied() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(installed_rank) FROM flyway_schema_history",
                Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(6);

        Integer farmStatus = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE lower(table_name) = 'farm' AND lower(column_name) = 'status'
                """,
                Integer.class);
        assertThat(farmStatus).isEqualTo(1);

        Integer memberships = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'farm_membership'
                """,
                Integer.class);
        assertThat(memberships).isEqualTo(1);

        Integer subscriptions = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'farm_subscription'
                """,
                Integer.class);
        assertThat(subscriptions).isEqualTo(1);

        Integer sequences = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.sequences
                WHERE lower(sequence_name) = 'farm_code_seq'
                """,
                Integer.class);
        assertThat(sequences).isEqualTo(1);

        String harriStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM farm WHERE code = 'HARRI'",
                String.class);
        assertThat(harriStatus).isEqualTo("ACTIVE");
    }

    @Test
    void featureCatalogMigrationIsApplied() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(installed_rank) FROM flyway_schema_history",
                Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(7);

        Integer catalogs = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'feature_catalog'
                """,
                Integer.class);
        assertThat(catalogs).isEqualTo(1);

        Integer assignments = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'farm_feature'
                """,
                Integer.class);
        assertThat(assignments).isEqualTo(1);

        Integer farmFlags = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE lower(table_name) = 'farm'
                  AND lower(column_name) IN ('animal_management_enabled', 'animalmanagementenabled')
                """,
                Integer.class);
        assertThat(farmFlags).isZero();
    }

    @Test
    void farmOnboardingIdempotencyMigrationIsApplied() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(installed_rank) FROM flyway_schema_history",
                Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(8);

        Integer requests = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'farm_onboarding_request'
                """,
                Integer.class);
        assertThat(requests).isEqualTo(1);

        Integer compensations = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'farm_onboarding_compensation'
                """,
                Integer.class);
        assertThat(compensations).isEqualTo(1);
    }

    @Test
    void farmMembershipDisplayNameMigrationIsApplied() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(installed_rank) FROM flyway_schema_history",
                Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(9);

        Integer displayName = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.columns
                WHERE lower(table_name) = 'farm_membership' AND lower(column_name) = 'display_name'
                """,
                Integer.class);
        assertThat(displayName).isEqualTo(1);
    }

    @Test
    void animalTableMigrationIsApplied() {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT MAX(installed_rank) FROM flyway_schema_history",
                Integer.class);
        assertThat(version).isGreaterThanOrEqualTo(10);

        Integer animals = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE lower(table_name) = 'animal'
                """,
                Integer.class);
        assertThat(animals).isEqualTo(1);
    }
}
