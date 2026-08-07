package com.braculink.dao;

import com.braculink.common.util.ScheduleFormatter;
import com.braculink.dto.SectionPickerDto;
import com.braculink.model.ClassSlot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SectionPickerRowMapper implements RowMapper<SectionPickerDto> {

    private final ObjectMapper objectMapper;

    public SectionPickerRowMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public SectionPickerDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        SectionPickerDto dto = new SectionPickerDto();
        dto.setSectionName(rs.getString("section_name"));
        dto.setTheoryFaculty(rs.getString("faculties"));
        dto.setTheoryTiming(ScheduleFormatter.format(readSchedules(rs.getString("class_schedules"))));

        boolean hasLab = rs.getObject("lab_section_id") != null;
        dto.setHasLab(hasLab);
        dto.setLabFaculty(hasLab ? rs.getString("lab_faculties") : null);
        dto.setLabTiming(hasLab ? ScheduleFormatter.format(readSchedules(rs.getString("lab_schedules"))) : null);

        dto.setAvailableSeats(rs.getInt("available_seats"));
        dto.setRoomName(rs.getString("room_name"));
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
