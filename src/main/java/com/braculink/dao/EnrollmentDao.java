package com.braculink.dao;

import com.braculink.dto.RoutineEntryDto;
import com.braculink.model.Enrollment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class EnrollmentDao {

    private static final String INSERT_SQL =
            "INSERT INTO enrollment (user_id, section_id, semester_session_id) VALUES (?, ?, ?)";

    private static final String FIND_BY_USER_AND_SEMESTER_SQL = "SELECT e.id AS enrollment_id, e.semester_session_id, "
            + "cs.course_code, cs.course_name, cs.section_name, cs.faculties, cs.room_name, "
            + "cs.class_schedules, cs.lab_section_id, cs.lab_faculties, cs.lab_room_name, cs.lab_schedules "
            + "FROM enrollment e "
            + "JOIN course_section cs ON cs.id = e.section_id "
            + "WHERE e.user_id = ? AND e.semester_session_id = ? "
            + "ORDER BY cs.course_code";

    private static final String DELETE_BY_ID_AND_USER_SQL = "DELETE FROM enrollment WHERE id = ? AND user_id = ?";

    private static final String EXISTS_FOR_USER_AND_COURSE_SQL = "SELECT COUNT(*) FROM enrollment e "
            + "JOIN course_section cs ON cs.id = e.section_id "
            + "WHERE e.user_id = ? AND cs.course_code = ? AND e.semester_session_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EnrollmentDao(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Long insert(Enrollment enrollment) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, enrollment.getUserId());
            ps.setLong(2, enrollment.getSectionId());
            ps.setInt(3, enrollment.getSemesterSessionId());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public List<RoutineEntryDto> findByUserAndSemester(Long userId, Integer semesterSessionId) {
        return jdbcTemplate.query(FIND_BY_USER_AND_SEMESTER_SQL, new RoutineEntryRowMapper(objectMapper),
                userId, semesterSessionId);
    }

    public int deleteByIdAndUser(Long id, Long userId) {
        return jdbcTemplate.update(DELETE_BY_ID_AND_USER_SQL, id, userId);
    }

    public boolean existsForUserAndCourse(Long userId, String courseCode, Integer semesterSessionId) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_FOR_USER_AND_COURSE_SQL, Integer.class,
                userId, courseCode, semesterSessionId);
        return count != null && count > 0;
    }
}
