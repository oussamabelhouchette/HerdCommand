package com.herdcommand.api.domain.farm;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class FarmCodeGenerator {

    public static final String PREFIX = "FARM-TN-";

    private final JdbcTemplate jdbcTemplate;

    public FarmCodeGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextCode() {
        Long sequence = jdbcTemplate.queryForObject("SELECT nextval('farm_code_seq')", Long.class);
        if (sequence == null) {
            throw new IllegalStateException("farm_code_seq returned null");
        }
        return PREFIX + String.format("%04d", sequence);
    }
}
