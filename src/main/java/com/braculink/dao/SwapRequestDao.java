package com.braculink.dao;

import com.braculink.dto.SwapGroupMemberDto;
import com.braculink.dto.SwapRequestResponse;
import com.braculink.model.SwapRequest;
import com.braculink.swap.engine.SwapRequestView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class SwapRequestDao {

    private static final SwapRequestRowMapper ROW_MAPPER = new SwapRequestRowMapper();

    private static final SwapRequestViewRowMapper VIEW_ROW_MAPPER = new SwapRequestViewRowMapper();

    private static final String COLUMNS = "id, user_id, course_code, current_section_id, desired_section_id, "
            + "status, confirmed, group_id, created_at, responded_at";

    private static final String INSERT_SQL = "INSERT INTO swap_request "
            + "(user_id, course_code, current_section_id, desired_section_id, status, confirmed, created_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String FIND_BY_ID_SQL = "SELECT " + COLUMNS + " FROM swap_request WHERE id = ?";

    private static final String FIND_BY_USER_SQL = "SELECT sr.id, sr.course_code, "
            + "cur.section_name AS current_section_name, des.section_name AS desired_section_name, "
            + "sr.status, sr.created_at "
            + "FROM swap_request sr "
            + "JOIN course_section cur ON cur.id = sr.current_section_id "
            + "JOIN course_section des ON des.id = sr.desired_section_id "
            + "WHERE sr.user_id = ? "
            + "ORDER BY sr.created_at DESC";

    // Feeds the matching engine. One JOIN instead of an N+1: the engine is pure in-memory Java and
    // cannot look anything up, so the names it needs for a suggestion payload must arrive with the
    // rows. RESERVED is loaded alongside PENDING so SwapGraph's status filter stays load-bearing in
    // production rather than being dead code only the tests exercise.
    private static final String FIND_ACTIVE_VIEWS_BY_COURSE_SQL = "SELECT sr.id, sr.user_id, "
            + "u.full_name, u.student_id, sr.course_code, "
            + "sr.current_section_id, cur.section_name AS current_section_name, "
            + "sr.desired_section_id, des.section_name AS desired_section_name, "
            + "sr.status, sr.created_at "
            + "FROM swap_request sr "
            + "JOIN user u ON u.id = sr.user_id "
            + "JOIN course_section cur ON cur.id = sr.current_section_id "
            + "JOIN course_section des ON des.id = sr.desired_section_id "
            + "WHERE sr.course_code = ? AND sr.status IN ('PENDING', 'RESERVED') "
            + "ORDER BY sr.created_at, sr.id";

    private static final String UPDATE_STATUS_SQL = "UPDATE swap_request SET status = ? WHERE id = ?";

    private static final String CANCEL_SQL = "UPDATE swap_request SET status = 'CANCELLED', responded_at = ? "
            + "WHERE id = ? AND user_id = ? AND status = 'PENDING'";

    private static final String EXISTS_ACTIVE_FOR_USER_AND_COURSE_SQL = "SELECT COUNT(*) FROM swap_request "
            + "WHERE user_id = ? AND course_code = ? AND status IN ('PENDING', 'RESERVED')";

    private static final String FIND_BY_GROUP_SQL = "SELECT " + COLUMNS + " FROM swap_request "
            + "WHERE group_id = ? ORDER BY id";

    // The concurrency guard for the propose flow. The AND status = 'PENDING' means a request that
    // someone else reserved a moment ago simply will not match, so the affected-row count comes
    // back short and the caller rolls the whole proposal back.
    private static final String RESERVE_SQL = "UPDATE swap_request "
            + "SET status = 'RESERVED', group_id = ?, confirmed = FALSE, responded_at = NULL "
            + "WHERE status = 'PENDING' AND id IN ";

    private static final String CONFIRM_MEMBER_SQL = "UPDATE swap_request "
            + "SET confirmed = TRUE, responded_at = ? "
            + "WHERE group_id = ? AND user_id = ? AND status = 'RESERVED' AND confirmed = FALSE";

    private static final String COUNT_UNCONFIRMED_IN_GROUP_SQL = "SELECT COUNT(*) FROM swap_request "
            + "WHERE group_id = ? AND confirmed = FALSE";

    private static final String MARK_MATCHED_BY_GROUP_SQL = "UPDATE swap_request "
            + "SET status = 'MATCHED', responded_at = ? WHERE group_id = ? AND status = 'RESERVED'";

    // Releasing clears group_id, so a released request is indistinguishable from one that never
    // joined a group — which is exactly what "back in the pool" means.
    private static final String RELEASE_GROUP_SQL = "UPDATE swap_request "
            + "SET status = 'PENDING', group_id = NULL, confirmed = FALSE, responded_at = NULL "
            + "WHERE group_id = ? AND status = 'RESERVED'";

    private static final String FIND_MEMBERS_BY_GROUPS_SQL = "SELECT sr.group_id, sr.user_id, sr.confirmed, "
            + "u.full_name, u.student_id, u.fb_profile_url, u.phone_number, u.phone_public, "
            + "cur.section_name AS current_section_name, des.section_name AS desired_section_name "
            + "FROM swap_request sr "
            + "JOIN user u ON u.id = sr.user_id "
            + "JOIN course_section cur ON cur.id = sr.current_section_id "
            + "JOIN course_section des ON des.id = sr.desired_section_id "
            + "WHERE sr.group_id IN ";

    private static final SwapGroupMemberExtractor MEMBER_EXTRACTOR = new SwapGroupMemberExtractor();

    private final JdbcTemplate jdbcTemplate;

    public SwapRequestDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(SwapRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, request.getUserId());
            ps.setString(2, request.getCourseCode());
            ps.setLong(3, request.getCurrentSectionId());
            ps.setLong(4, request.getDesiredSectionId());
            ps.setString(5, request.getStatus());
            ps.setBoolean(6, request.isConfirmed());
            ps.setTimestamp(7, Timestamp.valueOf(request.getCreatedAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<SwapRequest> findById(Long id) {
        List<SwapRequest> results = jdbcTemplate.query(FIND_BY_ID_SQL, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    public List<SwapRequestResponse> findByUser(Long userId) {
        return jdbcTemplate.query(FIND_BY_USER_SQL, (rs, rowNum) -> new SwapRequestResponse(
                rs.getLong("id"),
                rs.getString("course_code"),
                rs.getString("current_section_name"),
                rs.getString("desired_section_name"),
                rs.getString("status"),
                rs.getTimestamp("created_at").toLocalDateTime()
        ), userId);
    }

    public List<SwapRequestView> findActiveViewsByCourse(String courseCode) {
        return jdbcTemplate.query(FIND_ACTIVE_VIEWS_BY_COURSE_SQL, VIEW_ROW_MAPPER, courseCode);
    }

    public int updateStatus(Long id, String status) {
        return jdbcTemplate.update(UPDATE_STATUS_SQL, status, id);
    }

    public int cancel(Long id, Long userId) {
        return jdbcTemplate.update(CANCEL_SQL, Timestamp.valueOf(LocalDateTime.now()), id, userId);
    }

    public boolean existsActiveForUserAndCourse(Long userId, String courseCode) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_ACTIVE_FOR_USER_AND_COURSE_SQL, Integer.class,
                userId, courseCode);
        return count != null && count > 0;
    }

    public List<SwapRequest> findAllByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        String sql = "SELECT " + COLUMNS + " FROM swap_request WHERE id IN " + placeholders(ids.size())
                + " ORDER BY id";
        return jdbcTemplate.query(sql, ROW_MAPPER, ids.toArray());
    }

    public List<SwapRequest> findByGroup(Long groupId) {
        return jdbcTemplate.query(FIND_BY_GROUP_SQL, ROW_MAPPER, groupId);
    }

    /**
     * Reserves every listed request for a group, but only those still PENDING.
     *
     * @return how many rows were actually reserved. The caller must compare this against the
     *         intended group size and roll back if it falls short — that comparison is the whole
     *         concurrency guard against two students proposing overlapping groups at once.
     */
    public int reserveAll(List<Long> ids, Long groupId) {
        if (ids.isEmpty()) {
            return 0;
        }
        Object[] args = new Object[ids.size() + 1];
        args[0] = groupId;
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = ids.get(i);
        }
        return jdbcTemplate.update(RESERVE_SQL + placeholders(ids.size()), args);
    }

    public int confirmMember(Long groupId, Long userId) {
        return jdbcTemplate.update(CONFIRM_MEMBER_SQL, Timestamp.valueOf(LocalDateTime.now()), groupId, userId);
    }

    public int countUnconfirmedInGroup(Long groupId) {
        Integer count = jdbcTemplate.queryForObject(COUNT_UNCONFIRMED_IN_GROUP_SQL, Integer.class, groupId);
        return count == null ? 0 : count;
    }

    public int markMatchedByGroup(Long groupId) {
        return jdbcTemplate.update(MARK_MATCHED_BY_GROUP_SQL, Timestamp.valueOf(LocalDateTime.now()), groupId);
    }

    public int releaseGroup(Long groupId) {
        return jdbcTemplate.update(RELEASE_GROUP_SQL, groupId);
    }

    public Map<Long, List<SwapGroupMemberDto>> findMembersByGroupIds(List<Long> groupIds) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }
        String sql = FIND_MEMBERS_BY_GROUPS_SQL + placeholders(groupIds.size()) + " ORDER BY sr.group_id, sr.id";
        Map<Long, List<SwapGroupMemberDto>> members =
                jdbcTemplate.query(sql, MEMBER_EXTRACTOR, groupIds.toArray());
        return members == null ? Map.of() : members;
    }

    /**
     * Builds an {@code (?, ?, ?)} list for an IN clause.
     *
     * <p>Only the <em>count</em> of placeholders varies — no caller-supplied value is ever
     * concatenated into the SQL. Every actual value still travels as a bound parameter, so this
     * stays within the project's rule that user input never reaches the statement text.
     */
    private static String placeholders(int count) {
        StringBuilder sql = new StringBuilder("(");
        for (int i = 0; i < count; i++) {
            sql.append(i == 0 ? "?" : ", ?");
        }
        return sql.append(')').toString();
    }
}
