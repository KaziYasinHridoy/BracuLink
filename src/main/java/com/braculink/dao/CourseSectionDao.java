package com.braculink.dao;

import com.braculink.dto.CourseSummaryDto;
import com.braculink.dto.SectionPickerDto;
import com.braculink.model.ClassSlot;
import com.braculink.model.CourseSection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class CourseSectionDao {

    private static final String FIND_SECTIONS_FOR_PICKER_SQL = "SELECT section_name, faculties, class_schedules, "
            + "lab_section_id, lab_faculties, lab_schedules, (capacity - consumed_seat) AS available_seats, room_name "
            + "FROM course_section "
            + "WHERE course_code = ? AND semester_session_id = ? AND course_type = 'THEORY' "
            + "ORDER BY section_name";

    private static final String SEARCH_COURSES_SQL = "SELECT DISTINCT course_code, course_name "
            + "FROM course_section "
            + "WHERE course_code LIKE ? OR course_name LIKE ? "
            + "ORDER BY course_code";

    private static final String UPSERT_SQL = "INSERT INTO course_section "
            + "(section_id, course_code, course_name, course_type, section_name, faculties, room_name, "
            + "capacity, consumed_seat, semester_session_id, class_schedules, lab_section_id, lab_faculties, "
            + "lab_schedules, last_synced_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE "
            + "course_code = VALUES(course_code), "
            + "course_name = VALUES(course_name), "
            + "course_type = VALUES(course_type), "
            + "section_name = VALUES(section_name), "
            + "faculties = VALUES(faculties), "
            + "room_name = VALUES(room_name), "
            + "capacity = VALUES(capacity), "
            + "consumed_seat = VALUES(consumed_seat), "
            + "class_schedules = VALUES(class_schedules), "
            + "lab_section_id = VALUES(lab_section_id), "
            + "lab_faculties = VALUES(lab_faculties), "
            + "lab_schedules = VALUES(lab_schedules), "
            + "last_synced_at = VALUES(last_synced_at)";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CourseSectionDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void upsertAll(List<CourseSection> sections) {
        List<Object[]> batchArgs = sections.stream()
                .map(this::toRow)
                .toList();
        jdbcTemplate.batchUpdate(UPSERT_SQL, batchArgs);
    }

    public List<SectionPickerDto> findSectionsForPicker(String courseCode, Integer semesterSessionId) {
        return jdbcTemplate.query(FIND_SECTIONS_FOR_PICKER_SQL, new SectionPickerRowMapper(objectMapper),
                courseCode, semesterSessionId);
    }

    public List<CourseSummaryDto> searchCourses(String search) {
        String pattern = "%" + search + "%";
        return jdbcTemplate.query(SEARCH_COURSES_SQL,
                (rs, rowNum) -> new CourseSummaryDto(rs.getString("course_code"), rs.getString("course_name")),
                pattern, pattern);
    }

    private Object[] toRow(CourseSection section) {
        return new Object[]{
                section.getSectionId(),
                section.getCourseCode(),
                section.getCourseName(),
                section.getCourseType(),
                section.getSectionName(),
                section.getFaculties(),
                section.getRoomName(),
                section.getCapacity(),
                section.getConsumedSeat(),
                section.getSemesterSessionId(),
                writeJson(section.getClassSchedules()),
                section.getLabSectionId(),
                section.getLabFaculties(),
                writeJson(section.getLabSchedules()),
                Timestamp.valueOf(section.getLastSyncedAt())
        };
    }

    private String writeJson(List<ClassSlot> schedules) {
        if (schedules == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(schedules);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize schedule JSON", e);
        }
    }
}
