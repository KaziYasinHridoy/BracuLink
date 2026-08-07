package com.braculink.dao;

import com.braculink.model.Notification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public class NotificationDao {

    private static final NotificationRowMapper ROW_MAPPER = new NotificationRowMapper();

    private static final String INSERT_SQL =
            "INSERT INTO notification (user_id, type, payload, is_read, created_at) VALUES (?, ?, ?, ?, ?)";

    private static final String FIND_BY_USER_SQL = "SELECT id, user_id, type, payload, is_read, created_at "
            + "FROM notification WHERE user_id = ? ORDER BY created_at DESC";

    // Scoped by user_id so a request can never mark someone else's notification read.
    private static final String MARK_READ_SQL =
            "UPDATE notification SET is_read = TRUE WHERE id = ? AND user_id = ?";

    private final JdbcTemplate jdbcTemplate;

    public NotificationDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(Notification notification) {
        jdbcTemplate.update(INSERT_SQL,
                notification.getUserId(),
                notification.getType(),
                notification.getPayload(),
                notification.isRead(),
                Timestamp.valueOf(notification.getCreatedAt()));
    }

    public List<Notification> findByUser(Long userId) {
        return jdbcTemplate.query(FIND_BY_USER_SQL, ROW_MAPPER, userId);
    }

    public int markRead(Long id, Long userId) {
        return jdbcTemplate.update(MARK_READ_SQL, id, userId);
    }
}
