package com.braculink.dao;

import com.braculink.dto.FriendSummaryDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FriendSummaryRowMapper implements RowMapper<FriendSummaryDto> {

    @Override
    public FriendSummaryDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        FriendSummaryDto dto = new FriendSummaryDto();
        dto.setUserId(rs.getLong("id"));
        dto.setFullName(rs.getString("full_name"));
        dto.setStudentId(rs.getString("student_id"));
        dto.setFbProfileUrl(rs.getString("fb_profile_url"));
        boolean phonePublic = rs.getBoolean("phone_public");
        dto.setPhoneNumber(phonePublic ? rs.getString("phone_number") : null);
        return dto;
    }
}
