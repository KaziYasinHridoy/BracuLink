package com.braculink.dao;

import com.braculink.swap.engine.SwapRequestView;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SwapRequestViewRowMapper implements RowMapper<SwapRequestView> {

    @Override
    public SwapRequestView mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new SwapRequestView(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("full_name"),
                rs.getString("student_id"),
                rs.getString("course_code"),
                rs.getLong("current_section_id"),
                rs.getString("current_section_name"),
                rs.getLong("desired_section_id"),
                rs.getString("desired_section_name"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
