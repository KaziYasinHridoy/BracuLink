package com.braculink.swap.engine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The directed swap graph for a single course.
 *
 * <p>A <strong>node</strong> is a section id. An <strong>edge</strong> is one PENDING swap request,
 * directed {@code currentSectionId -> desiredSectionId} ("I have this, I want that"). A swap group
 * is therefore a cycle in this graph.
 *
 * <p>One graph is built per {@code courseCode}, which is what makes the same-course rule structural
 * rather than a validation somebody has to remember: a cross-course swap is not a state this
 * algorithm can reach.
 *
 * <p><strong>This package is pure Java: no Spring, no SQL, no database access.</strong>
 *
 * <p>The admission rules in {@link #build} follow one principle: <em>skip what is legitimate data
 * variance; throw on what can only be a caller bug.</em>
 */
public final class SwapGraph {

    private static final String PENDING = "PENDING";

    private final String courseCode;
    private final List<SwapRequestView> edges;
    private final Map<Long, List<SwapRequestView>> outgoing;

    private SwapGraph(String courseCode, List<SwapRequestView> edges, Map<Long, List<SwapRequestView>> outgoing) {
        this.courseCode = courseCode;
        this.edges = edges;
        this.outgoing = outgoing;
    }

    /**
     * Builds the graph for one course from an in-memory list of swap request views.
     *
     * <p>Requests whose status is not PENDING are skipped — RESERVED requests are already committed
     * to a proposed group, and MATCHED/CANCELLED/EXPIRED ones are terminal, so none of them may be
     * offered to anyone. Self-loops are skipped too, so a stale row can never become a degenerate
     * one-person "cycle".
     *
     * @throws IllegalArgumentException if any view belongs to a different course, or is null
     */
    public static SwapGraph build(String courseCode, List<SwapRequestView> requests) {
        Objects.requireNonNull(courseCode, "courseCode");
        Objects.requireNonNull(requests, "requests");

        List<SwapRequestView> admitted = new ArrayList<>();
        for (SwapRequestView view : requests) {
            if (view == null) {
                throw new IllegalArgumentException("requests must not contain null elements");
            }
            // Checked for every input, including non-PENDING ones: the loading query filters by
            // course_code, so a foreign-course row can only be a caller bug. Dropping it silently
            // would hide that bug and quietly weaken the one rule this project treats as absolute.
            if (!courseCode.equals(view.getCourseCode())) {
                throw new IllegalArgumentException("Swap request " + view.getRequestId() + " belongs to course "
                        + view.getCourseCode() + ", not " + courseCode);
            }
            if (!PENDING.equalsIgnoreCase(view.getStatus())) {
                continue;
            }
            if (view.getCurrentSectionId() == view.getDesiredSectionId()) {
                continue;
            }
            admitted.add(view);
        }

        // Deterministic output regardless of input order — not inherited from the loading query's
        // ORDER BY. Oldest request first, so the student who has waited longest is suggested first.
        admitted.sort(Comparator
                .comparing(SwapRequestView::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparingLong(SwapRequestView::getRequestId));

        Map<Long, List<SwapRequestView>> outgoing = new LinkedHashMap<>();
        for (SwapRequestView view : admitted) {
            outgoing.computeIfAbsent(view.getCurrentSectionId(), key -> new ArrayList<>()).add(view);
        }

        return new SwapGraph(courseCode, List.copyOf(admitted), outgoing);
    }

    public String courseCode() {
        return courseCode;
    }

    /** Every section id touched by at least one admitted edge, as either endpoint. */
    public Set<Long> nodes() {
        Set<Long> nodes = new LinkedHashSet<>();
        for (SwapRequestView edge : edges) {
            nodes.add(edge.getCurrentSectionId());
            nodes.add(edge.getDesiredSectionId());
        }
        return Collections.unmodifiableSet(nodes);
    }

    /** All admitted (PENDING) edges, oldest first. */
    public List<SwapRequestView> edges() {
        return edges;
    }

    /** The requests leaving a section — students who currently hold it and want out. */
    public List<SwapRequestView> edgesFrom(long sectionId) {
        List<SwapRequestView> found = outgoing.get(sectionId);
        return found == null ? List.of() : Collections.unmodifiableList(found);
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean containsNode(long sectionId) {
        return nodes().contains(sectionId);
    }
}
