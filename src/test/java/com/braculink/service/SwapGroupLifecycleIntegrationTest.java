package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.SwapGroupDao;
import com.braculink.dao.SwapRequestDao;
import com.braculink.dao.UserDao;
import com.braculink.dto.SwapGroupDto;
import com.braculink.dto.SwapGroupMemberDto;
import com.braculink.model.SwapGroup;
import com.braculink.model.SwapRequest;
import com.braculink.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the swap group lifecycle against a real database, through the real service, DAOs and
 * transactions — the paths that unit tests cannot reach.
 *
 * <p>Runs on H2 in MySQL mode, loading the same hand-written {@code schema.sql} that ships. No
 * {@code @Transactional} on this class: rolling the test back around the service would hide whether
 * the service's own transactions actually commit, which is the thing under test. Each test starts
 * from a wiped database instead.
 */
@SpringBootTest
@ActiveProfiles("test")
class SwapGroupLifecycleIntegrationTest {

    private static final String COURSE = "CSE370";

    /** Stops the course sync scheduler making a real network call during tests. */
    @MockitoBean
    private CourseSyncService courseSyncService;

    @Autowired
    private SwapGroupProposalService proposalService;

    @Autowired
    private SwapRequestDao swapRequestDao;

    @Autowired
    private SwapGroupDao swapGroupDao;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long alice;
    private Long bob;
    private Long carol;
    private Long dave;
    private Long erin;

    private Long sectionA;
    private Long sectionB;
    private Long sectionC;
    private Long sectionD;

    @BeforeEach
    void resetAndSeed() {
        jdbcTemplate.execute("DELETE FROM notification");
        jdbcTemplate.execute("DELETE FROM swap_request");
        jdbcTemplate.execute("DELETE FROM swap_group");
        jdbcTemplate.execute("DELETE FROM enrollment");
        jdbcTemplate.execute("DELETE FROM friendship");
        jdbcTemplate.execute("DELETE FROM course_section");
        jdbcTemplate.execute("DELETE FROM user");

        alice = createUser("Alice", "20101001");
        bob = createUser("Bob", "20101002");
        carol = createUser("Carol", "20101003");
        dave = createUser("Dave", "20101004");
        erin = createUser("Erin", "20101005");

        sectionA = createSection("01");
        sectionB = createSection("02");
        sectionC = createSection("03");
        sectionD = createSection("04");
    }

    // ------------------------------------------------- propose -> confirm -> confirm

    @Test
    void threeCycleReachesConfirmedAndMatched() {
        // A holds 01 and wants 02, B holds 02 and wants 03, C holds 03 and wants 01.
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionA);

        SwapGroupDto proposed = proposalService.propose(alice,
                List.of(aliceRequest, bobRequest, carolRequest));

        assertEquals("PROPOSED", proposed.getStatus());
        assertEquals(3, proposed.getGroupSize());
        assertFalse(proposed.isAllConfirmed());

        // Proposing is itself an agreement, so Alice is already confirmed and the others are not.
        assertTrue(confirmedFor(proposed, alice));
        assertFalse(confirmedFor(proposed, bob));
        assertFalse(confirmedFor(proposed, carol));

        for (Long id : List.of(aliceRequest, bobRequest, carolRequest)) {
            SwapRequest request = require(id);
            assertEquals("RESERVED", request.getStatus());
            assertEquals(proposed.getGroupId(), request.getGroupId());
        }

        // Bob confirms — still short of the full set, so nothing is finalised yet.
        SwapGroupDto afterBob = proposalService.confirm(proposed.getGroupId(), bob);
        assertEquals("PROPOSED", afterBob.getStatus());
        assertFalse(afterBob.isAllConfirmed());
        assertEquals("RESERVED", require(aliceRequest).getStatus());

        // Carol is the last one, which tips the whole group over.
        SwapGroupDto afterCarol = proposalService.confirm(proposed.getGroupId(), carol);
        assertEquals("CONFIRMED", afterCarol.getStatus());
        assertTrue(afterCarol.isAllConfirmed());

        assertEquals("CONFIRMED", swapGroupDao.findById(proposed.getGroupId()).orElseThrow().getStatus());
        for (Long id : List.of(aliceRequest, bobRequest, carolRequest)) {
            SwapRequest request = require(id);
            assertEquals("MATCHED", request.getStatus());
            assertTrue(request.isConfirmed());
            assertEquals(proposed.getGroupId(), request.getGroupId());
        }
    }

    @Test
    void everyOtherMemberIsNotifiedOfTheProposal() {
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionA);

        proposalService.propose(alice, List.of(aliceRequest, bobRequest, carolRequest));

        assertEquals(0, notificationCount(alice, "SWAP_PROPOSED"), "the proposer notifies themselves of nothing");
        assertEquals(1, notificationCount(bob, "SWAP_PROPOSED"));
        assertEquals(1, notificationCount(carol, "SWAP_PROPOSED"));
    }

    // ----------------------------------------------------------------- the race

    @Test
    void anOverlappingProposeIsRejectedAndTheFirstGroupIsUntouched() {
        // Bob's single request sits in two genuinely different cycles, which is exactly the
        // situation roadmap step 4 describes:
        //   group X = Alice(01->02), Bob(02->03), Carol(03->01)
        //   group Y = Dave (04->02), Bob(02->03), Erin (03->04)
        // Carol and Erin both hold section 03, which is ordinary — sections hold many students.
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionA);
        Long daveRequest = createRequest(dave, sectionD, sectionB);
        Long erinRequest = createRequest(erin, sectionC, sectionD);

        SwapGroupDto firstGroup = proposalService.propose(alice,
                List.of(aliceRequest, bobRequest, carolRequest));
        assertEquals("PROPOSED", firstGroup.getStatus());
        Long firstGroupId = firstGroup.getGroupId();

        long groupsBefore = countGroups();

        // Dave proposes his own perfectly valid cycle — but Bob is spoken for.
        ApiException rejected = assertThrows(ApiException.class,
                () -> proposalService.propose(dave, List.of(daveRequest, bobRequest, erinRequest)));
        assertEquals(HttpStatus.CONFLICT, rejected.getStatus());
        assertEquals("This option is no longer available", rejected.getMessage());

        // The failed proposal must leave nothing behind — no half-formed group row.
        assertEquals(groupsBefore, countGroups(), "the rejected propose should not have created a group");

        // And the first group is entirely unaffected.
        assertEquals("PROPOSED", swapGroupDao.findById(firstGroupId).orElseThrow().getStatus());
        for (Long id : List.of(aliceRequest, bobRequest, carolRequest)) {
            assertEquals("RESERVED", require(id).getStatus());
            assertEquals(firstGroupId, require(id).getGroupId());
        }

        // Dave and Erin were never reserved, so they stay available for other suggestions.
        for (Long id : List.of(daveRequest, erinRequest)) {
            assertEquals("PENDING", require(id).getStatus());
            assertNull(require(id).getGroupId());
        }
    }

    @Test
    void reserveAllReportsAShortCountWhenARequestWasTakenFirst() {
        // Drives the row-count concurrency guard directly at the DAO, which is the only way to
        // observe it: going through propose() twice trips the earlier re-validation check instead,
        // so the guard itself would never be reached.
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionA);
        Long carolRequest = createRequest(carol, sectionC, sectionA);

        SwapGroup group = new SwapGroup();
        group.setCourseCode(COURSE);
        group.setStatus("PROPOSED");
        group.setCreatedAt(LocalDateTime.now());
        Long firstGroupId = swapGroupDao.insert(group);

        assertEquals(2, swapRequestDao.reserveAll(List.of(aliceRequest, bobRequest), firstGroupId));

        SwapGroup second = new SwapGroup();
        second.setCourseCode(COURSE);
        second.setStatus("PROPOSED");
        second.setCreatedAt(LocalDateTime.now());
        Long secondGroupId = swapGroupDao.insert(second);

        // Bob is already RESERVED, so only Carol matches. A short count is exactly what makes
        // propose() roll the whole thing back.
        assertEquals(1, swapRequestDao.reserveAll(List.of(bobRequest, carolRequest), secondGroupId));

        // Critically, Bob still belongs to the first group — the losing update did not steal him.
        assertEquals(firstGroupId, require(bobRequest).getGroupId());
    }

    // -------------------------------------------------------------------- decline

    @Test
    void declineReleasesEveryoneBackToPending() {
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionA);

        SwapGroupDto proposed = proposalService.propose(alice,
                List.of(aliceRequest, bobRequest, carolRequest));
        Long groupId = proposed.getGroupId();

        proposalService.decline(groupId, carol);

        assertEquals("CANCELLED", swapGroupDao.findById(groupId).orElseThrow().getStatus());

        for (Long id : List.of(aliceRequest, bobRequest, carolRequest)) {
            SwapRequest request = require(id);
            assertEquals("PENDING", request.getStatus(), "request " + id + " should be back in the pool");
            assertNull(request.getGroupId(), "request " + id + " should no longer point at the group");
            assertFalse(request.isConfirmed(), "request " + id + " should have its confirmation reset");
        }

        // Released members are eligible for fresh proposals immediately.
        SwapGroupDto reproposed = proposalService.propose(bob,
                List.of(aliceRequest, bobRequest, carolRequest));
        assertEquals("PROPOSED", reproposed.getStatus());
        assertTrue(confirmedFor(reproposed, bob), "the new proposer is the one now pre-confirmed");
        assertFalse(confirmedFor(reproposed, alice));
    }

    @Test
    void declineNotifiesTheOtherMembersOnly() {
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionA);

        SwapGroupDto proposed = proposalService.propose(alice,
                List.of(aliceRequest, bobRequest, carolRequest));
        proposalService.decline(proposed.getGroupId(), carol);

        assertEquals(1, notificationCount(alice, "SWAP_DECLINED"));
        assertEquals(1, notificationCount(bob, "SWAP_DECLINED"));
        assertEquals(0, notificationCount(carol, "SWAP_DECLINED"), "the decliner already knows");
    }

    // ------------------------------------------------------------------ rejections

    @Test
    void proposingASetThatIsNotACycleIsRejectedAndReservesNobody() {
        // A wants 02, B wants 03 — nobody hands 01 back, so this chain never closes.
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);

        ApiException rejected = assertThrows(ApiException.class,
                () -> proposalService.propose(alice, List.of(aliceRequest, bobRequest)));
        assertEquals(HttpStatus.BAD_REQUEST, rejected.getStatus());

        assertEquals(0, countGroups());
        assertEquals("PENDING", require(aliceRequest).getStatus());
        assertEquals("PENDING", require(bobRequest).getStatus());
    }

    @Test
    void proposingAGroupYouAreNotInIsForbidden() {
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionB);

        ApiException rejected = assertThrows(ApiException.class,
                () -> proposalService.propose(alice, List.of(bobRequest, carolRequest)));
        assertEquals(HttpStatus.FORBIDDEN, rejected.getStatus());

        assertEquals(0, countGroups());
        assertEquals("PENDING", require(bobRequest).getStatus());
    }

    @Test
    void expiredProposalsAreReleasedByTheSameSweep() {
        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionA);

        SwapGroupDto proposed = proposalService.propose(alice, List.of(aliceRequest, bobRequest));
        Long groupId = proposed.getGroupId();

        // Backdate the group past the 24h timeout.
        jdbcTemplate.update("UPDATE swap_group SET created_at = ? WHERE id = ?",
                LocalDateTime.now().minusHours(SwapGroupProposalService.PROPOSAL_TIMEOUT_HOURS + 1), groupId);

        List<Long> expired = proposalService.findExpiredProposalIds();
        assertTrue(expired.contains(groupId));

        proposalService.releaseExpiredGroup(groupId);

        assertEquals("CANCELLED", swapGroupDao.findById(groupId).orElseThrow().getStatus());
        assertEquals("PENDING", require(aliceRequest).getStatus());
        assertNull(require(aliceRequest).getGroupId());
        assertEquals("PENDING", require(bobRequest).getStatus());
    }

    @Test
    void myGroupsCarriesContactDetailsWithPhoneGating() {
        jdbcTemplate.update("UPDATE user SET phone_number = ?, phone_public = TRUE, fb_profile_url = ? WHERE id = ?",
                "01700000002", "https://fb.com/bob", bob);
        jdbcTemplate.update("UPDATE user SET phone_number = ?, phone_public = FALSE WHERE id = ?",
                "01700000003", carol);

        Long aliceRequest = createRequest(alice, sectionA, sectionB);
        Long bobRequest = createRequest(bob, sectionB, sectionC);
        Long carolRequest = createRequest(carol, sectionC, sectionA);
        proposalService.propose(alice, List.of(aliceRequest, bobRequest, carolRequest));

        List<SwapGroupDto> groups = proposalService.getMyGroups(alice);
        assertEquals(1, groups.size());

        SwapGroupMemberDto bobMember = memberFor(groups.get(0), bob);
        assertEquals("Bob", bobMember.getProfile().getFullName());
        assertEquals("https://fb.com/bob", bobMember.getProfile().getFbProfileUrl());
        assertEquals("01700000002", bobMember.getProfile().getPhoneNumber(), "phone_public is set, so it shows");
        assertEquals("02", bobMember.getFromSection());
        assertEquals("03", bobMember.getToSection());

        SwapGroupMemberDto carolMember = memberFor(groups.get(0), carol);
        assertNull(carolMember.getProfile().getPhoneNumber(), "phone_public is false, so the phone is withheld");
    }

    // --------------------------------------------------------------------- helpers

    private SwapRequest require(Long id) {
        return swapRequestDao.findById(id).orElseThrow();
    }

    private boolean confirmedFor(SwapGroupDto group, Long userId) {
        return memberFor(group, userId).isConfirmed();
    }

    private SwapGroupMemberDto memberFor(SwapGroupDto group, Long userId) {
        SwapGroupMemberDto member = group.getMembers().stream()
                .filter(m -> m.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        assertNotNull(member, "user " + userId + " should be a member of group " + group.getGroupId());
        return member;
    }

    private long countGroups() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM swap_group", Long.class);
        return count == null ? 0 : count;
    }

    private int notificationCount(Long userId, String type) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notification WHERE user_id = ? AND type = ?",
                Integer.class, userId, type);
        return count == null ? 0 : count;
    }

    private Long createUser(String name, String studentId) {
        User user = new User();
        user.setStudentId(studentId);
        user.setFullName(name);
        user.setBracuEmail(name.toLowerCase() + "@g.bracu.ac.bd");
        user.setPasswordHash("hash");
        user.setPhonePublic(false);
        user.setEmailVerified(true);
        user.setCreatedAt(LocalDateTime.now());
        return userDao.save(user);
    }

    private Long createSection(String sectionName) {
        jdbcTemplate.update("INSERT INTO course_section "
                + "(section_id, course_code, course_name, course_type, section_name, capacity, consumed_seat, "
                + "semester_session_id) VALUES (?, ?, ?, 'THEORY', ?, 30, 10, 700)",
                Long.parseLong(sectionName) + 9000, COURSE, "Database Systems", sectionName);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM course_section WHERE course_code = ? AND section_name = ?",
                Long.class, COURSE, sectionName);
    }

    private Long createRequest(Long userId, Long currentSectionId, Long desiredSectionId) {
        SwapRequest request = new SwapRequest();
        request.setUserId(userId);
        request.setCourseCode(COURSE);
        request.setCurrentSectionId(currentSectionId);
        request.setDesiredSectionId(desiredSectionId);
        request.setStatus("PENDING");
        request.setConfirmed(false);
        request.setCreatedAt(LocalDateTime.now());
        return swapRequestDao.insert(request);
    }
}
