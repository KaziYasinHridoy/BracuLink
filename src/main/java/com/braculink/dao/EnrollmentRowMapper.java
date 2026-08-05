package com.braculink.dao;

import com.braculink.model.Enrollment;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class EnrollmentRowMapper implements RowMapper<Enrollment> {

    @Override
    public Enrollment mapRow(ResultSet rs, int rowNum) throws SQLException {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(rs.getLong("id"));
        enrollment.setUserId(rs.getLong("user_id"));
        enrollment.setSectionId(rs.getLong("section_id"));
        enrollment.setSemesterSessionId(rs.getInt("semester_session_id"));
        return enrollment;
    }
}
