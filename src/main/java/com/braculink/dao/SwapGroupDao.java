package com.braculink.dao;

import com.braculink.model.SwapGroup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class SwapGroupDao {

    private static final SwapGroupRowMapper ROW_MAPPER = new SwapGroupRowMapper();

    private static final String COLUMNS = "id, course_code, status, created_at";

    private static final String INSERT_SQL =
            "INSERT INTO swap_group (course_code, status, created_at) VALUES (?, ?, ?)";

    private static final String FIND_BY_ID_SQL = "SELECT " + COLUMNS + " FROM swap_group WHERE id = ?";

    private static final String UPDATE_STATUS_SQL = "UPDATE swap_group SET status = ? WHERE id = ? AND status = ?";

    private static final String FIND_IDS_BY_STATUS_OLDER_THAN_SQL =
            "SELECT id FROM swap_group WHERE status = ? AND created_at < ? ORDER BY created_at";

    // A user's groups are reached through their own swap request's group_id. Releasing a group
    // nulls that link, so cancelled and expired groups drop out of this list by construction.
    private static final String FIND_FOR_USER_SQL = "SELECT DISTINCT g.id, g.course_code, g.status, g.created_at "
            + "FROM swap_group g "
            + "JOIN swap_request sr ON sr.group_id = g.id "
            + "WHERE sr.user_id = ? "
            + "ORDER BY g.created_at DESC";

    private final JdbcTemplate jdbcTemplate;

    public SwapGroupDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(SwapGroup group) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, group.getCourseCode());
            ps.setString(2, group.getStatus());
            ps.setTimestamp(3, Timestamp.valueOf(group.getCreatedAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public Optional<SwapGroup> findById(Long id) {
        List<SwapGroup> results = jdbcTemplate.query(FIND_BY_ID_SQL, ROW_MAPPER, id);
        return results.stream().findFirst();
    }

    /**
     * Moves a group between states, but only from the state the caller expects.
     *
     * <p>The {@code AND status = ?} is what makes this safe to call concurrently: two requests
     * racing to cancel the same group will see one row updated and one row not, so the loser can
     * tell it lost.
     */
    public int updateStatus(Long id, String newStatus, String expectedCurrentStatus) {
        return jdbcTemplate.update(UPDATE_STATUS_SQL, newStatus, id, expectedCurrentStatus);
    }

    public List<Long> findIdsByStatusOlderThan(String status, LocalDateTime cutoff) {
        return jdbcTemplate.query(FIND_IDS_BY_STATUS_OLDER_THAN_SQL,
                (rs, rowNum) -> rs.getLong("id"), status, Timestamp.valueOf(cutoff));
    }

    public List<SwapGroup> findForUser(Long userId) {
        return jdbcTemplate.query(FIND_FOR_USER_SQL, ROW_MAPPER, userId);
    }
}
