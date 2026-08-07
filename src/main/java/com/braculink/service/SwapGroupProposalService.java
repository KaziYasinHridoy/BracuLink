package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.SwapGroupDao;
import com.braculink.dao.SwapRequestDao;
import com.braculink.dto.SwapGroupDto;
import com.braculink.dto.SwapGroupMemberDto;
import com.braculink.model.SwapGroup;
import com.braculink.model.SwapRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the propose -&gt; invite -&gt; confirm lifecycle of a swap group.
 *
 * <p>This is the one place in the project where transactions genuinely matter. Proposing a group
 * writes to two tables and must be all-or-nothing: a {@code swap_group} row with nobody reserved
 * against it would be an orphan, and requests reserved against a group that failed to insert would
 * be stuck out of the pool forever.
 */
@Service
public class SwapGroupProposalService {

    private static final Logger log = LoggerFactory.getLogger(SwapGroupProposalService.class);

    private static final String PENDING = "PENDING";
    private static final String RESERVED = "RESERVED";
    private static final String MATCHED = "MATCHED";

    private static final String PROPOSED = "PROPOSED";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String CANCELLED = "CANCELLED";

    private static final String UNAVAILABLE_MESSAGE = "This option is no longer available";

    /** How long a proposal may sit unconfirmed before everyone is released back into the pool. */
    public static final int PROPOSAL_TIMEOUT_HOURS = 24;

    private final SwapRequestDao swapRequestDao;
    private final SwapGroupDao swapGroupDao;
    private final NotificationService notificationService;

    public SwapGroupProposalService(SwapRequestDao swapRequestDao, SwapGroupDao swapGroupDao,
            NotificationService notificationService) {
        this.swapRequestDao = swapRequestDao;
        this.swapGroupDao = swapGroupDao;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------ propose

    /**
     * Turns one chosen suggestion into a PROPOSED group, reserving exactly its members.
     *
     * <p>Everything below happens in one transaction. The reservation is what stops a student being
     * proposed into two groups at once, so it has to land atomically with the group row itself.
     */
    @Transactional
    public SwapGroupDto propose(Long proposerId, List<Long> swapRequestIds) {
        List<Long> ids = distinctIds(swapRequestIds);

        List<SwapRequest> requests = swapRequestDao.findAllByIds(ids);
        if (requests.size() != ids.size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "One or more swap requests could not be found");
        }

        // Re-validate rather than trusting the suggestion the client is holding: it may have been
        // generated minutes ago, and anybody in it could have been reserved or cancelled since.
        for (SwapRequest request : requests) {
            if (!PENDING.equals(request.getStatus())) {
                throw new ApiException(HttpStatus.CONFLICT, UNAVAILABLE_MESSAGE);
            }
        }

        SwapRequest mine = requests.stream()
                .filter(request -> request.getUserId().equals(proposerId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "You can only propose a group that includes your own swap request"));

        requireDistinctMembers(requests);
        String courseCode = requireSingleCourse(requests);
        requireSingleCycle(requests);

        SwapGroup group = new SwapGroup();
        group.setCourseCode(courseCode);
        group.setStatus(PROPOSED);
        group.setCreatedAt(LocalDateTime.now());
        Long groupId = swapGroupDao.insert(group);

        int reserved = swapRequestDao.reserveAll(ids, groupId);
        if (reserved != ids.size()) {
            // Somebody else reserved one of these between our check above and this update. Throwing
            // rolls back the group row too, so nothing half-formed survives.
            log.info("Propose lost the race for group {}: reserved {} of {} requests",
                    groupId, reserved, ids.size());
            throw new ApiException(HttpStatus.CONFLICT, UNAVAILABLE_MESSAGE);
        }

        // The proposer has, by the act of proposing, already agreed to this swap.
        swapRequestDao.confirmMember(groupId, proposerId);

        for (SwapRequest request : requests) {
            if (!request.getUserId().equals(proposerId)) {
                notify(request.getUserId(), "SWAP_PROPOSED", groupId, courseCode, requests.size());
            }
        }

        log.info("User {} proposed swap group {} for {} with {} members",
                proposerId, groupId, courseCode, requests.size());

        return getGroup(groupId, proposerId);
    }

    // ------------------------------------------------------------------ confirm

    @Transactional
    public SwapGroupDto confirm(Long groupId, Long userId) {
        SwapGroup group = requireGroup(groupId);
        if (!PROPOSED.equals(group.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This group is no longer awaiting confirmation");
        }

        int updated = swapRequestDao.confirmMember(groupId, userId);
        if (updated == 0) {
            // Either they aren't in this group, or they already confirmed. Both are dead ends, and
            // distinguishing them would leak which groups exist.
            throw new ApiException(HttpStatus.CONFLICT,
                    "You are not an unconfirmed member of this group");
        }

        if (swapRequestDao.countUnconfirmedInGroup(groupId) == 0) {
            swapGroupDao.updateStatus(groupId, CONFIRMED, PROPOSED);
            swapRequestDao.markMatchedByGroup(groupId);
            for (SwapRequest request : swapRequestDao.findByGroup(groupId)) {
                notify(request.getUserId(), "SWAP_CONFIRMED", groupId, group.getCourseCode(), 0);
            }
            log.info("Swap group {} fully confirmed and matched", groupId);
        }

        return getGroup(groupId, userId);
    }

    // ------------------------------------------------------------------ decline

    @Transactional
    public void decline(Long groupId, Long userId) {
        SwapGroup group = requireGroup(groupId);
        if (!PROPOSED.equals(group.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This group is no longer awaiting confirmation");
        }
        List<SwapRequest> members = swapRequestDao.findByGroup(groupId);
        if (members.stream().noneMatch(request -> request.getUserId().equals(userId))) {
            // Reported as missing rather than forbidden, so a non-member cannot probe which
            // group ids exist.
            throw new ApiException(HttpStatus.NOT_FOUND, "Swap group not found");
        }

        List<Long> otherMembers = members.stream()
                .map(SwapRequest::getUserId)
                .filter(memberId -> !memberId.equals(userId))
                .toList();

        release(groupId, group.getCourseCode(), otherMembers, "SWAP_DECLINED");
        log.info("User {} declined swap group {}; members released back to the pool", userId, groupId);
    }

    // ------------------------------------------------------------------- expiry

    /** Groups that have sat unconfirmed past the timeout. Read-only, so the scheduler can loop it. */
    public List<Long> findExpiredProposalIds() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(PROPOSAL_TIMEOUT_HOURS);
        return swapGroupDao.findIdsByStatusOlderThan(PROPOSED, cutoff);
    }

    /**
     * Releases one timed-out proposal. Called per group from the scheduler rather than looped
     * internally, so each release is its own transaction and one bad group cannot strand the rest.
     */
    @Transactional
    public void releaseExpiredGroup(Long groupId) {
        SwapGroup group = swapGroupDao.findById(groupId).orElse(null);
        if (group == null || !PROPOSED.equals(group.getStatus())) {
            return;
        }
        List<Long> members = swapRequestDao.findByGroup(groupId).stream()
                .map(SwapRequest::getUserId)
                .toList();
        release(groupId, group.getCourseCode(), members, "SWAP_EXPIRED");
        log.info("Swap group {} expired after {}h; members released", groupId, PROPOSAL_TIMEOUT_HOURS);
    }

    // --------------------------------------------------------------------- read

    public List<SwapGroupDto> getMyGroups(Long userId) {
        List<SwapGroup> groups = swapGroupDao.findForUser(userId);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<Long> groupIds = groups.stream().map(SwapGroup::getId).toList();
        Map<Long, List<SwapGroupMemberDto>> membersByGroup = swapRequestDao.findMembersByGroupIds(groupIds);

        List<SwapGroupDto> result = new ArrayList<>(groups.size());
        for (SwapGroup group : groups) {
            result.add(toDto(group, membersByGroup.getOrDefault(group.getId(), List.of())));
        }
        return result;
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Cancels the group and puts every member's request back in the pool.
     *
     * <p>Roadmap step 5: the group is CANCELLED, each request returns to PENDING with its
     * {@code group_id} cleared and {@code confirmed} reset, and they become eligible for fresh
     * suggestions — including ones they were not part of before.
     */
    private void release(Long groupId, String courseCode, List<Long> notifyUserIds, String notificationType) {
        int cancelled = swapGroupDao.updateStatus(groupId, CANCELLED, PROPOSED);
        if (cancelled == 0) {
            // Another request cancelled or confirmed it first; leave their outcome alone.
            throw new ApiException(HttpStatus.CONFLICT, "This group is no longer awaiting confirmation");
        }
        swapRequestDao.releaseGroup(groupId);
        for (Long memberId : notifyUserIds) {
            notify(memberId, notificationType, groupId, courseCode, 0);
        }
    }

    private SwapGroupDto getGroup(Long groupId, Long userId) {
        return getMyGroups(userId).stream()
                .filter(dto -> dto.getGroupId().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Swap group not found"));
    }

    private SwapGroup requireGroup(Long groupId) {
        return swapGroupDao.findById(groupId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Swap group not found"));
    }

    private SwapGroupDto toDto(SwapGroup group, List<SwapGroupMemberDto> members) {
        SwapGroupDto dto = new SwapGroupDto();
        dto.setGroupId(group.getId());
        dto.setCourseCode(group.getCourseCode());
        dto.setStatus(group.getStatus());
        dto.setCreatedAt(group.getCreatedAt());
        dto.setGroupSize(members.size());
        dto.setAllConfirmed(!members.isEmpty() && members.stream().allMatch(SwapGroupMemberDto::isConfirmed));
        dto.setMembers(members);
        return dto;
    }

    private static List<Long> distinctIds(List<Long> swapRequestIds) {
        List<Long> ids = new ArrayList<>(swapRequestIds.size());
        Set<Long> seen = new HashSet<>();
        for (Long id : swapRequestIds) {
            if (id == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Swap request ids must not be null");
            }
            if (!seen.add(id)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Duplicate swap request in the proposed group");
            }
            ids.add(id);
        }
        return ids;
    }

    private static void requireDistinctMembers(List<SwapRequest> requests) {
        Set<Long> userIds = new HashSet<>();
        for (SwapRequest request : requests) {
            if (!userIds.add(request.getUserId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "A student cannot appear twice in the same swap group");
            }
        }
    }

    /** The absolute rule: a swap only ever exchanges sections within one course. */
    private static String requireSingleCourse(List<SwapRequest> requests) {
        String courseCode = requests.get(0).getCourseCode();
        for (SwapRequest request : requests) {
            if (!courseCode.equals(request.getCourseCode())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "All swap requests in a group must be for the same course");
            }
        }
        return courseCode;
    }

    /**
     * Rejects any set of requests that is not one closed trading cycle.
     *
     * <p>Without this a client could post any collection of ids it liked and reserve those students
     * into a group whose sections do not actually chain — nobody could ever complete it, and the
     * members would sit out of the pool until the 24h timeout. Every section given up must be
     * exactly the section somebody else is waiting for, and following the chain must visit the
     * whole group before returning to the start.
     */
    private static void requireSingleCycle(List<SwapRequest> requests) {
        Map<Long, SwapRequest> bySectionHeld = new HashMap<>();
        for (SwapRequest request : requests) {
            if (bySectionHeld.put(request.getCurrentSectionId(), request) != null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "These swap requests do not form a valid swap cycle");
            }
        }

        SwapRequest start = requests.get(0);
        SwapRequest step = start;
        int visited = 0;
        while (visited < requests.size()) {
            SwapRequest next = bySectionHeld.get(step.getDesiredSectionId());
            if (next == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "These swap requests do not form a valid swap cycle");
            }
            visited++;
            step = next;
            if (step == start) {
                break;
            }
        }
        if (step != start || visited != requests.size()) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "These swap requests do not form a valid swap cycle");
        }
    }

    private void notify(Long userId, String type, Long groupId, String courseCode, int groupSize) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("groupId", groupId);
        payload.put("courseCode", courseCode);
        if (groupSize > 0) {
            payload.put("groupSize", groupSize);
        }
        notificationService.notify(userId, type, payload);
    }
}
