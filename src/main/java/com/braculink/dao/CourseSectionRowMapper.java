package com.braculink.dao;

import com.braculink.model.CourseSection;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CourseSectionRowMapper implements RowMapper<CourseSection> {

    @Override
    public CourseSection mapRow(ResultSet rs, int rowNum) throws SQLException {
        CourseSection section = new CourseSection();
        section.setId(rs.getLong("id"));
        section.setSectionId(rs.getLong("section_id"));
        section.setCourseCode(rs.getString("course_code"));
        section.setCourseName(rs.getString("course_name"));
        section.setCourseType(rs.getString("course_type"));
        section.setSectionName(rs.getString("section_name"));
        section.setFaculties(rs.getString("faculties"));
        section.setRoomName(rs.getString("room_name"));
        section.setCapacity((Integer) rs.getObject("capacity"));
        section.setConsumedSeat((Integer) rs.getObject("consumed_seat"));
        section.setSemesterSessionId(rs.getInt("semester_session_id"));
        section.setClassSchedules(rs.getString("class_schedules"));
        section.setLabSectionId((Long) rs.getObject("lab_section_id"));
        section.setLabFaculties(rs.getString("lab_faculties"));
        section.setLabSchedules(rs.getString("lab_schedules"));
        Timestamp lastSyncedAt = rs.getTimestamp("last_synced_at");
        section.setLastSyncedAt(lastSyncedAt != null ? lastSyncedAt.toLocalDateTime() : null);
        return section;
    }
}
