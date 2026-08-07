package com.braculink.dao;

import com.braculink.dto.PublicProfileDto;
import com.braculink.dto.SwapGroupMemberDto;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Buckets one flat result set of group members into {@code groupId -> members}.
 *
 * <p>A {@link ResultSetExtractor} rather than a {@code RowMapper} so every group's members come
 * back from a single query — the alternative is one members query per group, which is an N+1.
 *
 * <p>Applies the same privacy rule as the rest of the project: the phone number is dropped unless
 * the member set {@code phone_public}, while the Facebook link is shown whenever it is filled in.
 */
public class SwapGroupMemberExtractor implements ResultSetExtractor<Map<Long, List<SwapGroupMemberDto>>> {

    @Override
    public Map<Long, List<SwapGroupMemberDto>> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<Long, List<SwapGroupMemberDto>> byGroup = new LinkedHashMap<>();
        while (rs.next()) {
            PublicProfileDto profile = new PublicProfileDto();
            profile.setFullName(rs.getString("full_name"));
            profile.setStudentId(rs.getString("student_id"));
            profile.setFbProfileUrl(rs.getString("fb_profile_url"));
            profile.setPhoneNumber(rs.getBoolean("phone_public") ? rs.getString("phone_number") : null);

            SwapGroupMemberDto member = new SwapGroupMemberDto();
            member.setUserId(rs.getLong("user_id"));
            member.setProfile(profile);
            member.setFromSection(rs.getString("current_section_name"));
            member.setToSection(rs.getString("desired_section_name"));
            member.setConfirmed(rs.getBoolean("confirmed"));

            byGroup.computeIfAbsent(rs.getLong("group_id"), key -> new ArrayList<>()).add(member);
        }
        return byGroup;
    }
}
