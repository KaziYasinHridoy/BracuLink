package com.braculink.swap.engine;

import com.braculink.dto.SwapMemberDto;
import com.braculink.dto.SwapSuggestionDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Finds viable swap groups — directed cycles in a {@link SwapGraph} — for one student's request.
 *
 * <p>The caller's own request is the edge {@code myHave -> myWant}. A group of size N is that edge
 * plus N-1 other students' edges forming a path from {@code myWant} back to {@code myHave}. So a
 * size-2 group is the direct swap-back, and larger groups are longer trading cycles.
 *
 * <p>Every viable size from 2 up to {@code maxSize} is returned, not just the smallest — the
 * student picks which one to act on. Results are ordered smallest group first, then by whichever
 * group contains the longest-waiting partner.
 *
 * <p><strong>This package is pure Java: no Spring, no SQL, no database access.</strong> This class
 * carries no annotations and holds no state, so it is trivially thread-safe and is exposed as a
 * singleton bean from {@code com.braculink.swap.SwapEngineConfig}, outside this package.
 */
public class CycleMatchingService {

    private static final String PENDING = "PENDING";

    /** The largest group this engine will assemble. Larger requests are clamped to this. */
    public static final int MAX_SUPPORTED_GROUP_SIZE = 5;

    /** Used when a caller does not specify a cap. */
    public static final int DEFAULT_MAX_GROUP_SIZE = MAX_SUPPORTED_GROUP_SIZE;

    /** Searches every group size from 2 up to {@link #DEFAULT_MAX_GROUP_SIZE}. */
    public List<SwapSuggestionDto> findCandidates(SwapGraph graph, SwapRequestView myRequest) {
        return findCandidates(graph, myRequest, DEFAULT_MAX_GROUP_SIZE);
    }

    /**
     * Returns every viable group the caller could swap through, caller always first in each group.
     *
     * @param graph     the graph for the caller's course
     * @param myRequest the caller's own request
     * @param maxSize   largest group size to search for; must be at least 2, and is clamped down to
     *                  {@link #MAX_SUPPORTED_GROUP_SIZE}
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
        if (!mightFormCycle(graph, myRequest)) {
            return List.of();
        }

        int cap = Math.min(maxSize, MAX_SUPPORTED_GROUP_SIZE);

        List<List<SwapRequestView>> paths = new ArrayList<>();
        Set<Long> usedUserIds = new HashSet<>();
        usedUserIds.add(myRequest.getUserId());
        Set<Long> visitedSections = new HashSet<>();
        visitedSections.add(myRequest.getCurrentSectionId());
        visitedSections.add(myRequest.getDesiredSectionId());

        extend(graph, myRequest, myRequest.getDesiredSectionId(), cap,
                new ArrayList<>(), usedUserIds, visitedSections, paths);

        // Smallest group first — a 2-way swap is the easiest to actually pull off — then the group
        // whose longest-waiting member has waited longest, then by request id for full determinism.
        paths.sort(Comparator
                .comparingInt(List<SwapRequestView>::size)
                .thenComparing(CycleMatchingService::oldestCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(CycleMatchingService::requestIdSignature));

        List<SwapSuggestionDto> suggestions = new ArrayList<>(paths.size());
        for (List<SwapRequestView> path : paths) {
            suggestions.add(toSuggestion(myRequest, path));
        }
        return List.copyOf(suggestions);
    }

    /**
     * The Union-Find pre-filter: whether a cycle through this request could exist at all.
     *
     * <p>Treating the graph as undirected, a cycle back to {@code myHave} is only possible if
     * {@code myWant} and {@code myHave} sit in the same connected component. If they do not, there
     * is nothing to search and the DFS is skipped entirely.
     *
     * <p>Crucially the components are computed over <em>other students'</em> edges only. The
     * caller's own request is itself an edge {@code myHave -> myWant}, so including it would join
     * those two sections by definition and make this check always say yes — a pre-filter that never
     * filters. Excluding the caller asks the question that actually matters: can I get from the
     * section I want back to the section I hold, using swaps other people are offering?
     *
     * <p>This is conservative in the safe direction: it never rules out a group that exists, since
     * every edge of a real cycle belongs to somebody else and is therefore in the structure.
     */
    public boolean mightFormCycle(SwapGraph graph, SwapRequestView myRequest) {
        Objects.requireNonNull(graph, "graph");
        Objects.requireNonNull(myRequest, "myRequest");

        UnionFind components = new UnionFind();
        for (SwapRequestView edge : graph.edges()) {
            if (edge.getUserId() == myRequest.getUserId()) {
                continue;
            }
            components.union(edge.getCurrentSectionId(), edge.getDesiredSectionId());
        }
        return components.connected(myRequest.getDesiredSectionId(), myRequest.getCurrentSectionId());
    }

    /**
     * Depth-bounded DFS extending a partial path, looking for a way back to the caller's section.
     *
     * <p>A group of {@code maxSize} students uses the caller's edge plus {@code maxSize - 1} others,
     * so the path may never exceed that many edges.
     */
    private void extend(SwapGraph graph, SwapRequestView myRequest, long fromSection, int maxSize,
            List<SwapRequestView> path, Set<Long> usedUserIds, Set<Long> visitedSections,
            List<List<SwapRequestView>> found) {

        int maxPartnerEdges = maxSize - 1;

        for (SwapRequestView edge : graph.edgesFrom(fromSection)) {
            // Skips the caller's own edges too, since their user id seeds the set. No student may
            // appear twice in one group — they cannot swap with themselves.
            if (usedUserIds.contains(edge.getUserId())) {
                continue;
            }

            if (edge.getDesiredSectionId() == myRequest.getCurrentSectionId()) {
                path.add(edge);
                found.add(List.copyOf(path));
                path.remove(path.size() - 1);
                continue;
            }

            // Adding this edge would use the last slot without closing the loop — dead end.
            if (path.size() + 1 >= maxPartnerEdges) {
                continue;
            }
            // Revisiting a section would embed a smaller cycle inside this one; each member of a
            // trading cycle gives up and receives exactly one distinct section.
            if (visitedSections.contains(edge.getDesiredSectionId())) {
                continue;
            }

            path.add(edge);
            usedUserIds.add(edge.getUserId());
            visitedSections.add(edge.getDesiredSectionId());

            extend(graph, myRequest, edge.getDesiredSectionId(), maxSize,
                    path, usedUserIds, visitedSections, found);

            visitedSections.remove(edge.getDesiredSectionId());
            usedUserIds.remove(edge.getUserId());
            path.remove(path.size() - 1);
        }
    }

    private static SwapSuggestionDto toSuggestion(SwapRequestView myRequest, List<SwapRequestView> path) {
        List<SwapMemberDto> members = new ArrayList<>(path.size() + 1);
        members.add(toMember(myRequest));
        for (SwapRequestView edge : path) {
            members.add(toMember(edge));
        }
        return new SwapSuggestionDto(members);
    }

    private static SwapMemberDto toMember(SwapRequestView view) {
        return new SwapMemberDto(view.getUserId(), view.getFullName(), view.getStudentId(),
                view.getCurrentSectionName(), view.getDesiredSectionName());
    }

    /**
     * The oldest partner request in a group. Computed over partners only — the caller is in every
     * group, so including them would flatten the comparison whenever their own request is the
     * oldest.
     */
    private static LocalDateTime oldestCreatedAt(List<SwapRequestView> path) {
        LocalDateTime oldest = null;
        for (SwapRequestView edge : path) {
            LocalDateTime createdAt = edge.getCreatedAt();
            if (createdAt != null && (oldest == null || createdAt.isBefore(oldest))) {
                oldest = createdAt;
            }
        }
        return oldest;
    }

    /** Final tiebreak, so two otherwise-equal groups always come back in a stable order. */
    private static String requestIdSignature(List<SwapRequestView> path) {
        StringBuilder signature = new StringBuilder();
        for (SwapRequestView edge : path) {
            signature.append(String.format("%020d", edge.getRequestId())).append(',');
        }
        return signature.toString();
    }
}
