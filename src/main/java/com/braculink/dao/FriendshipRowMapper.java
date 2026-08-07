package com.braculink.dao;

import com.braculink.model.Friendship;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FriendshipRowMapper implements RowMapper<Friendship> {

    @Override
    public Friendship mapRow(ResultSet rs, int rowNum) throws SQLException {
        Friendship friendship = new Friendship();
        friendship.setRequesterId(rs.getLong("requester_id"));
        friendship.setAddresseeId(rs.getLong("addressee_id"));
        friendship.setStatus(rs.getString("status"));
        friendship.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return friendship;
    }
}
