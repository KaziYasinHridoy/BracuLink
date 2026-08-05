package com.braculink.swap.engine;

import com.braculink.dto.SwapMemberDto;
import com.braculink.dto.SwapSuggestionDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Finds viable swap groups — cycles in a {@link SwapGraph} — for one student's request.
 *
 * <p><strong>This package is pure Java: no Spring, no SQL, no database access.</strong> This class
 * carries no annotations and holds no state, so it is trivially thread-safe and is exposed as a
 * singleton bean from {@code com.braculink.swap.SwapEngineConfig}, outside this package.
 *
 * <p><strong>Size 2 only, for now.</strong> {@code maxSize} is honoured up to 2; values above 2
 * currently behave as 2. Sizes 3–5 arrive with the bounded-DFS and Union-Find work in roadmap
 * phase 9, at which point this class widens rather than gets rewritten.
 */
public class CycleMatchingService {

    private static final String PENDING = "PENDING";

    /** The largest group this engine can currently assemble. */
    public static final int MAX_SUPPORTED_GROUP_SIZE = 2;

    /**
     * Returns every viable group the caller could swap through, caller always first in each group.
     *
     * <p>A size-2 group is the direct swap-back: somebody who <em>has</em> the section I want and
     * <em>wants</em> the section I have.
     *
     * @param graph     the graph for the caller's course
     * @param myRequest the caller's own request, which must be one of the graph's edges
     * @param maxSize   largest group size to search for; must be at least 2
     * @throws IllegalArgumentException if {@code maxSize < 2}, or the request's course does not
     *                                  match the graph's
     */
    public List<SwapSuggestionDto> findCandidates(SwapGraph graph, SwapRequestView myRequest, int maxSize) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(myRequest, "myRequest");

        if (maxSize < 2) {
            throw new IllegalArgumentException("maxSize must be at least 2, got " + maxSize);
        }
        if (!graph.courseCode().equals(myRequest.getCourseCode())) {
            throw new IllegalArgumentException("Swap request " + myRequest.getRequestId() + " belongs to course "
                    + myRequest.getCourseCode() + ", not " + graph.courseCode());
        }
        // Belt-and-braces behind the service layer, which already rejects non-pending requests.
        if (!PENDING.equalsIgnoreCase(myRequest.getStatus())) {
            return List.of();
        }

        List<SwapSuggestionDto> suggestions = new ArrayList<>();

        // Everyone holding the section I want. edgesFrom has already dropped every non-PENDING
        // request, so the status invariant lives in exactly one place — the graph.
        for (SwapRequestView candidate : graph.edgesFrom(myRequest.getDesiredSectionId())) {
            if (candidate.getDesiredSectionId() != myRequest.getCurrentSectionId()) {
                continue;
            }
            if (candidate.getRequestId() == myRequest.getRequestId()) {
                continue;
            }
            // Two requests from the same person can never form a group. The write path already
            // prevents this today, but that is a service-layer guard the engine must not silently
            // depend on — and for size >= 3 this widens to "all userIds in the group are distinct".
            if (candidate.getUserId() == myRequest.getUserId()) {
                continue;
            }
            suggestions.add(new SwapSuggestionDto(List.of(toMember(myRequest), toMember(candidate))));
        }

        return List.copyOf(suggestions);
    }

    private static SwapMemberDto toMember(SwapRequestView view) {
        return new SwapMemberDto(view.getUserId(), view.getFullName(), view.getStudentId(),
                view.getCurrentSectionName(), view.getDesiredSectionName());
    }
}
