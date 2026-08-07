package com.braculink.swap.engine;

import com.braculink.dto.SwapMemberDto;
import com.braculink.dto.SwapSuggestionDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final long SEC_D = 104L;
    private static final long SEC_E = 105L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 1, 1, 9, 0);

    private final CycleMatchingService engine = new CycleMatchingService();

    // ---------------------------------------------------------------- size 2

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

    // ---------------------------------------------------------- sizes 3, 4, 5

    @Test
    void findCandidates_findsCleanThreeCycle() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapRequestView second = pending(2, 20, SEC_B, SEC_C);
        SwapRequestView third = pending(3, 30, SEC_C, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, second, third));
        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(1, suggestions.size());
        assertEquals(3, suggestions.get(0).getGroupSize());
        assertEquals(List.of(10L, 20L, 30L), userIds(suggestions.get(0)));

        // Each member hands on the section the next one is waiting for.
        assertEquals("S101", suggestions.get(0).getMembers().get(0).getFromSection());
        assertEquals("S102", suggestions.get(0).getMembers().get(0).getToSection());
        assertEquals("S102", suggestions.get(0).getMembers().get(1).getFromSection());
        assertEquals("S103", suggestions.get(0).getMembers().get(1).getToSection());
        assertEquals("S103", suggestions.get(0).getMembers().get(2).getFromSection());
        assertEquals("S101", suggestions.get(0).getMembers().get(2).getToSection());
    }

    @Test
    void findCandidates_findsCleanFourCycle() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_C),
                pending(3, 30, SEC_C, SEC_D),
                pending(4, 40, SEC_D, SEC_A)));

        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(1, suggestions.size());
        assertEquals(4, suggestions.get(0).getGroupSize());
        assertEquals(List.of(10L, 20L, 30L, 40L), userIds(suggestions.get(0)));
    }

    @Test
    void findCandidates_findsCleanFiveCycle() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_C),
                pending(3, 30, SEC_C, SEC_D),
                pending(4, 40, SEC_D, SEC_E),
                pending(5, 50, SEC_E, SEC_A)));

        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(1, suggestions.size());
        assertEquals(5, suggestions.get(0).getGroupSize());
        assertEquals(List.of(10L, 20L, 30L, 40L, 50L), userIds(suggestions.get(0)));
    }

    @Test
    void findCandidates_respectsTheDepthCap() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // A genuine 5-cycle, which a cap of 4 must refuse to assemble.
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_C),
                pending(3, 30, SEC_C, SEC_D),
                pending(4, 40, SEC_D, SEC_E),
                pending(5, 50, SEC_E, SEC_A)));

        assertTrue(engine.findCandidates(graph, me, 4).isEmpty());
        assertEquals(1, engine.findCandidates(graph, me, 5).size());
    }

    // ------------------------------------------------- all sizes, and ordering

    @Test
    void findCandidates_returnsAllSizes_smallestFirst() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_A),   // size 2
                pending(3, 30, SEC_B, SEC_C),   // \_ size 3 with 40
                pending(4, 40, SEC_C, SEC_A),   // /
                pending(5, 50, SEC_C, SEC_D),   // \_ size 4 with 30 and 60
                pending(6, 60, SEC_D, SEC_A))); // /

        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(List.of(2, 3, 4), suggestions.stream()
                .map(SwapSuggestionDto::getGroupSize)
                .collect(Collectors.toList()));
        assertEquals(List.of(10L, 20L), userIds(suggestions.get(0)));
        assertEquals(List.of(10L, 30L, 40L), userIds(suggestions.get(1)));
        assertEquals(List.of(10L, 30L, 50L, 60L), userIds(suggestions.get(2)));
    }

    @Test
    void findCandidates_ordersSameSizeGroupsByOldestWaitingPartner() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Two independent size-2 partners. Ids ascend but timestamps do not, so a result ordered
        // by timestamp is distinguishable from one that merely kept insertion order.
        SwapRequestView newer = req(2, 20, SEC_B, SEC_A, "PENDING", BASE.plusHours(5));
        SwapRequestView older = req(3, 30, SEC_B, SEC_A, "PENDING", BASE.plusHours(1));

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, newer, older));
        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(2, suggestions.size());
        assertEquals(List.of(10L, 30L), userIds(suggestions.get(0)));
        assertEquals(List.of(10L, 20L), userIds(suggestions.get(1)));
    }

    @Test
    void findCandidates_ordersSameSizeGroupsByOldestPartner_evenWhenDiscoveredOutOfOrder() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Two size-3 groups. The graph hands out edges leaving B oldest-first, so the B->D route is
        // explored first — yet the B->C route contains the longest-waiting partner overall and must
        // come back first. Only a sort over whole candidates, not edge order, can produce that.
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                req(2, 20, SEC_B, SEC_C, "PENDING", BASE.plusHours(10)),
                req(3, 21, SEC_C, SEC_A, "PENDING", BASE.plusHours(1)),
                req(4, 30, SEC_B, SEC_D, "PENDING", BASE.plusHours(2)),
                req(5, 31, SEC_D, SEC_A, "PENDING", BASE.plusHours(20))));

        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(2, suggestions.size());
        assertEquals(List.of(10L, 20L, 21L), userIds(suggestions.get(0)));
        assertEquals(List.of(10L, 30L, 31L), userIds(suggestions.get(1)));
    }

    @Test
    void findCandidates_canPlaceOneStudentInSeveralDifferentCycles() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapRequestView shared = pending(2, 20, SEC_B, SEC_C);
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                shared,
                pending(3, 30, SEC_C, SEC_A),   // closes a 3-cycle with `shared`
                pending(4, 40, SEC_C, SEC_D),   // \_ closes a 4-cycle with `shared`
                pending(5, 50, SEC_D, SEC_A))); // /

        List<SwapSuggestionDto> suggestions = engine.findCandidates(graph, me, 5);

        assertEquals(2, suggestions.size());
        assertEquals(List.of(10L, 20L, 30L), userIds(suggestions.get(0)));
        assertEquals(List.of(10L, 20L, 40L, 50L), userIds(suggestions.get(1)));

        // Student 20 is offered in both groups — the reservation step, not the search, is what
        // eventually stops them being committed to two groups at once.
        assertTrue(suggestions.stream().allMatch(s -> userIds(s).contains(20L)));
    }

    // ------------------------------------------------------- nothing to return

    @Test
    void findCandidates_returnsEmpty_whenGraphHasNoCycle() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Partner edges reach my section, so the components do connect and the DFS really runs —
        // but no directed path leads from the section I want back to the one I hold.
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_C),
                pending(3, 30, SEC_A, SEC_C)));

        assertTrue(engine.mightFormCycle(graph, me));
        assertTrue(engine.findCandidates(graph, me, 5).isEmpty());
    }

    @Test
    void findCandidates_shortCircuits_whenWantAndHaveAreInSeparateComponents() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Nobody's swap touches my section at all: SEC_B's component is {B, C}, SEC_A is in none.
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_C),
                pending(3, 30, SEC_C, SEC_B)));

        // The pre-filter itself says no, so the DFS is never entered.
        assertFalse(engine.mightFormCycle(graph, me));
        assertTrue(engine.findCandidates(graph, me, 5).isEmpty());
    }

    @Test
    void mightFormCycle_ignoresMyOwnEdge_soThePreFilterIsNotVacuous() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapGraph graph = SwapGraph.build(COURSE, List.of(me));

        // My own request is itself an A -> B edge. Were it counted, A and B would be connected by
        // definition and this pre-filter could never rule anything out.
        assertFalse(engine.mightFormCycle(graph, me));
    }

    @Test
    void findCandidates_returnsEmpty_whenNobodyWantsMySection() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        SwapRequestView hasWhatIWant = pending(2, 20, SEC_B, SEC_C);
        SwapRequestView wantsMySection = pending(3, 30, SEC_C, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, hasWhatIWant, wantsMySection));

        // No direct swap-back exists, so a size-2 search finds nothing...
        assertTrue(engine.findCandidates(graph, me, 2).isEmpty());
        // ...though these three do form a 3-cycle once larger groups are allowed.
        assertEquals(1, engine.findCandidates(graph, me, 3).size());
    }

    // ------------------------------------------------------------ exclusions

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

        assertTrue(engine.findCandidates(graph, me, 5).isEmpty());
    }

    @Test
    void findCandidates_neverSuggestsAnotherRequestFromTheSameUser() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Same user, different request id — a perfect mirror of myself.
        SwapRequestView mineToo = pending(2, 10, SEC_B, SEC_A);

        SwapGraph graph = SwapGraph.build(COURSE, List.of(me, mineToo));

        assertTrue(engine.findCandidates(graph, me, 5).isEmpty());
    }

    @Test
    void findCandidates_neverPlacesTheSameStudentTwiceInOneGroup() {
        SwapRequestView me = pending(1, 10, SEC_A, SEC_B);
        // Student 20 holds both of the middle legs, so the only route home would use them twice.
        SwapGraph graph = SwapGraph.build(COURSE, List.of(
                me,
                pending(2, 20, SEC_B, SEC_C),
                pending(3, 20, SEC_C, SEC_A)));

        assertTrue(engine.findCandidates(graph, me, 5).isEmpty());
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

    // ---------------------------------------------------------------- helpers

    private static List<Long> userIds(SwapSuggestionDto suggestion) {
        return suggestion.getMembers().stream()
                .map(SwapMemberDto::getUserId)
                .collect(Collectors.toList());
    }

    private static SwapRequestView req(long id, long userId, long from, long to, String status) {
        return req(id, userId, from, to, status, BASE.plusMinutes(id));
    }

    private static SwapRequestView req(long id, long userId, long from, long to, String status,
            LocalDateTime createdAt) {
        return new SwapRequestView(id, userId,
                "Student " + userId, "2000" + userId,
                COURSE,
                from, "S" + from,
                to, "S" + to,
                status, createdAt);
    }

    private static SwapRequestView pending(long id, long userId, long from, long to) {
        return req(id, userId, from, to, "PENDING");
    }
}
