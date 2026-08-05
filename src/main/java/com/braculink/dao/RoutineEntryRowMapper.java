package com.braculink.dao;

import com.braculink.dto.RoutineEntryDto;
import com.braculink.model.ClassSlot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class RoutineEntryRowMapper implements RowMapper<RoutineEntryDto> {

    private final ObjectMapper objectMapper;

    public RoutineEntryRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public RoutineEntryDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        RoutineEntryDto dto = new RoutineEntryDto();
        dto.setEnrollmentId(rs.getLong("enrollment_id"));
        dto.setCourseCode(rs.getString("course_code"));
        dto.setCourseName(rs.getString("course_name"));
        dto.setSectionName(rs.getString("section_name"));
        dto.setFaculties(rs.getString("faculties"));
        dto.setRoomName(rs.getString("room_name"));
        dto.setClassSchedules(readSchedules(rs.getString("class_schedules")));

        boolean hasLab = rs.getObject("lab_section_id") != null;
        dto.setHasLab(hasLab);
        dto.setLabFaculties(hasLab ? rs.getString("lab_faculties") : null);
        dto.setLabRoomName(hasLab ? rs.getString("lab_room_name") : null);
        dto.setLabSchedules(hasLab ? readSchedules(rs.getString("lab_schedules")) : null);

        dto.setSemesterSessionId(rs.getInt("semester_session_id"));
        return dto;
    }

    private List<ClassSlot> readSchedules(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<ClassSlot>>() {
            });
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to parse schedule JSON: " + json, e);
        }
    }
}
