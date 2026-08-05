package com.braculink.dao;

import com.braculink.model.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {

    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setStudentId(rs.getString("student_id"));
        user.setFullName(rs.getString("full_name"));
        user.setBracuEmail(rs.getString("bracu_email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setPhonePublic(rs.getBoolean("phone_public"));
        user.setFbProfileUrl(rs.getString("fb_profile_url"));
        user.setEmailVerified(rs.getBoolean("email_verified"));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    }
}
