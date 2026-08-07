package com.braculink.dao;

import com.braculink.model.SwapGroup;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SwapGroupRowMapper implements RowMapper<SwapGroup> {

    @Override
    public SwapGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
        SwapGroup group = new SwapGroup();
        group.setId(rs.getLong("id"));
        group.setCourseCode(rs.getString("course_code"));
        group.setStatus(rs.getString("status"));
        group.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return group;
    }
}
