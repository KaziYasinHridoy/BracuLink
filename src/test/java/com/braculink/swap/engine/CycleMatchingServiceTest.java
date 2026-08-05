package com.braculink.swap.engine;

import com.braculink.dto.SwapMemberDto;
import com.braculink.dto.SwapSuggestionDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Plain objects only — no Spring context, no database, no mocks. The whole point of keeping
 * {@code com.braculink.swap.engine} free of Spring and SQL is that these tests run with MySQL
 * stopped.
 */
class CycleMatchingServiceTest {

    private static final String COURSE = "CSE370";
    private static final long SEC_A = 101L;
    private static final long SEC_B = 102L;
    private static final long SEC_C = 103L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0);

    private final CycleMatchingService engine = new CycleMatchingService();

    @Test
    void findCandidates_findsPartner_whenCleanTwoCycleExists() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapRequestView partner = pending(2, 20, SEC_B, SEC_A);
        // Has what I want, but wants somewhere else — only half the mirror.
        SwapRequestView decoyWrongWant = pending(3, 30, SEC_B, SEC_C);
        // Wants my section, but doesn't hold the one I want — the other half.
        SwapRequestView decoyWrongHave = pending(4, 40, SEC_C, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, partner, decoyWrongWant, decoyWrongHave));
        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 2);

        assertEquals(1, suggestions.size());

        SwapSuggestionDto suggestion = suggestions.get(0);
        assertEquals(2, suggestion.getGroupSize());
        assertEquals(2, suggestion.getMembers().size());

        SwapMemberDto first = suggestion.getMembers().get(0);
        assertEquals(10L, first.getUserId());
        assertEquals("Student 10", first.getFullName());
        assertEquals("200010", first.getStudentId());
        assertEquals("S101", first.getFromSection());
        assertEquals("S102", first.getToSection());

        SwapMemberDto second = suggestion.getMembers().get(1);
        assertEquals(20L, second.getUserId());
        assertEquals("Student 20", second.getFullName());
        assertEquals("200020", second.getStudentId());
        assertEquals("S102", second.getFromSection());
        assertEquals("S101", second.getToSection());
    }

    @Test
    void findCandidates_returnsEmpty_whenNobodyWantsMySection() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapRequestView hasWhatIWant = pending(2, 20, SEC_B, SEC_C);
        SwapRequestView wantsMySection = pending(3, 30, SEC_C, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, hasWhatIWant, wantsMySection));

        // Note this graph does contain a 3-cycle (A->B, B->C, C->A). Asserting empty pins the
        // size-2-only contract as deliberate. Roadmap phase 9 updates this test, not deletes it.
        assertTrue(engine.findCandidates(graph, me, 2).isEmpty());
    }

    @Test
    void findCandidates_excludesReservedAndMatchedRequests() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Both are otherwise perfect mirrors, so status is the only thing that can exclude them.
        SwapRequestView reserved = req(2, 20, SEC_B, SEC_A, "RESERVED");
        SwapRequestView matched = req(3, 30, SEC_B, SEC_A, "MATCHED");

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, reserved, matched));

        // Asserted on the graph itself, so a failure localises to build() rather than to the search.
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.edgesFrom(SEC_B).isEmpty());

        assertTrue(engine.findCandidates(graph, me, 2).isEmpty());
    }

    @Test
    void findCandidates_neverSuggestsAnotherRequestFromTheSameUser() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Same user, different request id — a perfect mirror of myself.
        SwapRequestView mineToo = pending(2, 10, SEC_B, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, mineToo));

        assertTrue(engine.findCandidates(graph, me, 2).isEmpty());
    }

    @Test
    void findCandidates_rejectsMaxSizeBelowTwo() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapGraph graph = SwapGraph.build(COURSE, List.of(me));

        assertThrows(IllegalArgumentException.class, () -> engine.findCandidates(graph, me, 1));
    }

    @Test
    void build_rejectsEdgeFromAnotherCourse() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapRequestView otherCourse = new SwapRequestView(2, 20, "Student 20", "200020", "CSE220",
                SEC_B, "S102", SEC_A, "S101", "PENDING", BASE);

        assertThrows(IllegalArgumentException.class, () -> SwapGraph.build(COURSE, List.of(me, otherCourse)));
    }

    @Test
    void build_skipsSelfLoop() {
        SwapRequestView selfLoop = pending(1, 10, SEC_A, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(selfLoop));

        assertEquals(0, graph.edgeCount());
        assertTrue(graph.edgesFrom(SEC_A).isEmpty());
    }

    private static SwapRequestView req(long id, long userId, long from, long to, String status) {
        return new SwapRequestView(id, userId,
                "Student " + userId, "2000" + userId,
                COURSE,
                from, "S" + from,
                to, "S" + to,
                status, BASE.plusMinutes(id));
    }

    private static SwapRequestView pending(long id, long userId, long from, long to) {
        return req(id, userId, from, to, "PENDING");
    }
}
