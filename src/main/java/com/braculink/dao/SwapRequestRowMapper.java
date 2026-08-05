package com.braculink.dao;

import com.braculink.model.SwapRequest;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class SwapRequestRowMapper implements RowMapper<SwapRequest> {

    @Override
    public SwapRequest mapRow(ResultSet rs, int rowNum) throws SQLException {
        SwapRequest request = new SwapRequest();
        request.setId(rs.getLong("id"));
        request.setUserId(rs.getLong("user_id"));
        request.setCourseCode(rs.getString("course_code"));
        request.setCurrentSectionId(rs.getLong("current_section_id"));
        request.setDesiredSectionId(rs.getLong("desired_section_id"));
        request.setStatus(rs.getString("status"));
        request.setConfirmed(rs.getBoolean("confirmed"));
        request.setGroupId((Long) rs.getObject("group_id"));
        request.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp respondedAt = rs.getTimestamp("responded_at");
        request.setRespondedAt(respondedAt != null ? respondedAt.toLocalDateTime() : null);
        return request;
    }
}
