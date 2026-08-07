package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.SwapRequestDao;
import com.braculink.dao.UserDao;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

/**
 * Proves the propose transaction actually rolls back.
 *
 * <p>Kept apart from {@link SwapGroupLifecycleIntegrationTest} because it needs a spy on the DAO,
 * and that test should run against the real one.
 *
 * <p><strong>Why a spy is necessary here.</strong> {@code propose} re-validates that every request
 * is PENDING <em>before</em> it inserts the group row, so an ordinary overlapping proposal is
 * rejected at that earlier check and no group is ever created — nothing to roll back. The
 * affected-row-count guard only fires when a request is taken in the narrow window <em>between</em>
 * that check and the reserving UPDATE. Reproducing that window for real needs two threads
 * interleaving inside one transaction; stubbing the row count reproduces its exact effect
 * deterministically, which is what lets us assert the rollback.
 */
@SpringBootTest
@ActiveProfiles("test")
class SwapGroupProposalRollbackTest {

    private static final String COURSE = "CSE370";

    @MockitoBean
    private CourseSyncService courseSyncService;

    @MockitoSpyBean
    private SwapRequestDao swapRequestDao;

    @Autowired
    private SwapGroupProposalService proposalService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long alice;
    private Long bob;
    private Long aliceRequest;
    private Long bobRequest;

    @BeforeEach
    void resetAndSeed() {
        jdbcTemplate.execute("DELETE FROM notification");
        jdbcTemplate.execute("DELETE FROM swap_request");
        jdbcTemplate.execute("DELETE FROM swap_group");
        jdbcTemplate.execute("DELETE FROM course_section");
        jdbcTemplate.execute("DELETE FROM user");

        alice = createUser("Alice", "20101001");
        bob = createUser("Bob", "20101002");
        Long sectionA = createSection("01", 9001L);
        Long sectionB = createSection("02", 9002L);

        aliceRequest = createRequest(alice, sectionA, sectionB);
        bobRequest = createRequest(bob, sectionB, sectionA);
    }

    @Test
    void aShortReserveCountRollsBackTheGroupInsert() {
        // Stand in for "somebody reserved one of these a millisecond ago": the UPDATE matches fewer
        // rows than the group has members.
        doReturn(1).when(swapRequestDao).reserveAll(anyList(), anyLong());

        ApiException rejected = assertThrows(ApiException.class,
                () -> proposalService.propose(alice, List.of(aliceRequest, bobRequest)));
        assertEquals(HttpStatus.CONFLICT, rejected.getStatus());
        assertEquals("This option is no longer available", rejected.getMessage());

        // The group row was inserted before the guard fired. If @Transactional were not in effect,
        // it would still be sitting here as an orphan.
        assertEquals(0L, countGroups(), "the swap_group insert must have been rolled back");

        // And nobody is left stranded out of the pool.
        for (Long id : List.of(aliceRequest, bobRequest)) {
            SwapRequest request = swapRequestDao.findById(id).orElseThrow();
            assertEquals("PENDING", request.getStatus());
            assertNull(request.getGroupId());
        }
        assertEquals(0L, countNotifications(), "a rolled-back proposal must not notify anyone");
    }

    @Test
    void theGroupInsertSurvivesWhenTheCountMatches() {
        // The mirror image, so the test above cannot pass merely because propose always fails.
        assertEquals("PROPOSED", proposalService.propose(alice, List.of(aliceRequest, bobRequest)).getStatus());
        assertEquals(1L, countGroups());
    }

    private long countGroups() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM swap_group", Long.class);
        return count == null ? 0 : count;
    }

    private long countNotifications() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notification", Long.class);
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

    private Long createSection(String sectionName, Long externalId) {
        jdbcTemplate.update("INSERT INTO course_section "
                + "(section_id, course_code, course_name, course_type, section_name, capacity, consumed_seat, "
                + "semester_session_id) VALUES (?, ?, ?, 'THEORY', ?, 30, 10, 700)",
                externalId, COURSE, "Database Systems", sectionName);
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
