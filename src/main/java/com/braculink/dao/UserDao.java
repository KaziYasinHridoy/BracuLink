package com.braculink.dao;

import com.braculink.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Optional;

@Repository
public class UserDao {

    private static final UserRowMapper ROW_MAPPER = new UserRowMapper();

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long save(User user) {
        String sql = "INSERT INTO user "
                + "(student_id, full_name, bracu_email, password_hash, phone_number, phone_public, fb_profile_url, email_verified, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getStudentId());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getBracuEmail());
            ps.setString(4, user.getPasswordHash());
            ps.setString(5, user.getPhoneNumber());
            ps.setBoolean(6, user.isPhonePublic());
            ps.setString(7, user.getFbProfileUrl());
            ps.setBoolean(8, user.isEmailVerified());
            ps.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<User> findByBracuEmail(String bracuEmail) {
        String sql = "SELECT * FROM user WHERE bracu_email = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, ROW_MAPPER, bracuEmail));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByStudentId(String studentId) {
        String sql = "SELECT * FROM user WHERE student_id = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, ROW_MAPPER, studentId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void markVerified(Long id) {
        String sql = "UPDATE user SET email_verified = TRUE WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
