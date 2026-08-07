package com.braculink.dao;

import com.braculink.dto.FriendSummaryDto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class FriendshipDao {

    private static final FriendSummaryRowMapper FRIEND_SUMMARY_ROW_MAPPER = new FriendSummaryRowMapper();

    private static final String INSERT_REQUEST_SQL =
            "INSERT INTO friendship (requester_id, addressee_id, status, created_at) VALUES (?, ?, 'PENDING', ?)";

    private static final String UPDATE_STATUS_SQL =
            "UPDATE friendship SET status = ? WHERE requester_id = ? AND addressee_id = ?";

    private static final String DELETE_SQL =
            "DELETE FROM friendship WHERE requester_id = ? AND addressee_id = ?";

    private static final String EXISTS_BETWEEN_SQL = "SELECT COUNT(*) FROM friendship "
            + "WHERE (requester_id = ? AND addressee_id = ?) OR (requester_id = ? AND addressee_id = ?)";

    private static final String FIND_ACCEPTED_FRIENDS_SQL = "SELECT u.id, u.full_name, u.student_id, "
            + "u.fb_profile_url, u.phone_number, u.phone_public "
            + "FROM friendship f "
            + "JOIN user u ON u.id = CASE WHEN f.requester_id = ? THEN f.addressee_id ELSE f.requester_id END "
            + "WHERE f.status = 'ACCEPTED' AND (f.requester_id = ? OR f.addressee_id = ?)";

    private static final String FIND_PENDING_INCOMING_SQL = "SELECT u.id, u.full_name, u.student_id, "
            + "u.fb_profile_url, u.phone_number, u.phone_public "
            + "FROM friendship f "
            + "JOIN user u ON u.id = f.requester_id "
            + "WHERE f.addressee_id = ? AND f.status = 'PENDING'";

    private final JdbcTemplate jdbcTemplate;

    public FriendshipDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insertRequest(Long requesterId, Long addresseeId) {
        jdbcTemplate.update(INSERT_REQUEST_SQL, requesterId, addresseeId, Timestamp.valueOf(LocalDateTime.now()));
    }

    public int updateStatus(Long requesterId, Long addresseeId, String status) {
        return jdbcTemplate.update(UPDATE_STATUS_SQL, status, requesterId, addresseeId);
    }

    public int delete(Long requesterId, Long addresseeId) {
        return jdbcTemplate.update(DELETE_SQL, requesterId, addresseeId);
    }

    public boolean existsBetween(Long a, Long b) {
        Integer count = jdbcTemplate.queryForObject(EXISTS_BETWEEN_SQL, Integer.class, a, b, b, a);
        return count != null && count > 0;
    }

    public List<FriendSummaryDto> findAcceptedFriends(Long userId) {
        return jdbcTemplate.query(FIND_ACCEPTED_FRIENDS_SQL, FRIEND_SUMMARY_ROW_MAPPER, userId, userId, userId);
    }

    public List<FriendSummaryDto> findPendingIncoming(Long userId) {
        return jdbcTemplate.query(FIND_PENDING_INCOMING_SQL, FRIEND_SUMMARY_ROW_MAPPER, userId);
    }
}
