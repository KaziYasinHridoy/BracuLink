package com.braculink;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.braculink.service.CourseSyncService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the real hand-written {@code schema.sql} loads on H2, so the integration tests are running
 * against the DDL that actually ships rather than a test-only fork of it.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaOnH2Test {

    /** Stops the course sync scheduler making a real network call during tests. */
    @MockitoBean
    private CourseSyncService courseSyncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void theRealSchemaLoadsOnH2() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_type = 'BASE TABLE' AND UPPER(table_schema) = 'PUBLIC'",
                String.class);

        // The seven domain tables from CLAUDE.md, plus otp for email verification.
        List<String> expected = List.of("course_section", "user", "enrollment",
                "swap_request", "swap_group", "notification", "friendship", "otp");

        for (String table : expected) {
            assertTrue(tables.stream().anyMatch(name -> name.equalsIgnoreCase(table)),
                    "missing table " + table + ", got " + tables);
        }
        assertEquals(expected.size(), tables.size(), "unexpected tables: " + tables);
    }

    @Test
    void theSwapRequestConstraintsSurvivedTheTranslation() {
        // The propose flow leans on these, so it matters that H2 kept them rather than silently
        // dropping the MySQL-specific syntax they are declared with.
        Integer foreignKeys = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_name = 'swap_request' AND constraint_type = 'FOREIGN KEY'",
                Integer.class);
        assertEquals(4, foreignKeys, "swap_request should keep all four foreign keys");

        Integer uniqueOnSection = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints "
                        + "WHERE table_name = 'course_section' AND constraint_type = 'UNIQUE'",
                Integer.class);
        assertEquals(1, uniqueOnSection, "course_section should keep uq_section_semester");
    }
}
