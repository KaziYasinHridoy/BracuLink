package com.braculink.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Repository
public class OtpDao {

    private final JdbcTemplate jdbcTemplate;

    public OtpDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Stores the code for this email, replacing any earlier one (email is the primary key). */
    public void save(String email, String code, LocalDateTime expiresAt) {
        String sql = "INSERT INTO otp (email, code, expires_at) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE code = VALUES(code), expires_at = VALUES(expires_at)";
        jdbcTemplate.update(sql, email, code, Timestamp.valueOf(expiresAt));
    }

    /** True only if a row exists for this email whose code matches and has not expired. */
    public boolean isValid(String email, String code) {
        String sql = "SELECT COUNT(*) FROM otp WHERE email = ? AND code = ? AND expires_at > ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, code, Timestamp.valueOf(LocalDateTime.now()));
        return count != null && count > 0;
    }

    public void deleteByEmail(String email) {
        String sql = "DELETE FROM otp WHERE email = ?";
        jdbcTemplate.update(sql, email);
    }
}
