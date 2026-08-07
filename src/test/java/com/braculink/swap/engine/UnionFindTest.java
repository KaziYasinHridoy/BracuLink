package com.braculink.swap.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain objects only — no Spring context, no database. */
class UnionFindTest {

    @Test
    void connected_isFalse_forElementsNeverAdded() {
        UnionFind uf = new UnionFind();

        assertFalse(uf.connected(1L, 2L));
        assertFalse(uf.contains(1L));
    }

    @Test
    void connected_isTrue_afterUnion() {
        UnionFind uf = new UnionFind();
        uf.union(1L, 2L);

        assertTrue(uf.connected(1L, 2L));
        assertTrue(uf.connected(2L, 1L));
    }

    @Test
    void connected_isTransitive_acrossChainedUnions() {
        UnionFind uf = new UnionFind();
        uf.union(1L, 2L);
        uf.union(2L, 3L);
        uf.union(3L, 4L);

        assertTrue(uf.connected(1L, 4L));
        assertEquals(1, uf.componentCount());
    }

    @Test
    void connected_isFalse_acrossSeparateComponents() {
        UnionFind uf = new UnionFind();
        uf.union(1L, 2L);
        uf.union(10L, 20L);

        assertFalse(uf.connected(1L, 10L));
        assertEquals(2, uf.componentCount());
    }

    @Test
    void union_reportsWhetherItActuallyMerged() {
        UnionFind uf = new UnionFind();

        assertTrue(uf.union(1L, 2L));
        assertFalse(uf.union(1L, 2L));
        assertFalse(uf.union(2L, 1L));
    }

    @Test
    void handlesSparseIds_sinceSectionIdsArePrimaryKeys() {
        UnionFind uf = new UnionFind();
        uf.union(9_000_000_001L, 9_000_000_500L);

        assertTrue(uf.connected(9_000_000_001L, 9_000_000_500L));
        assertFalse(uf.connected(9_000_000_001L, 9_000_000_999L));
    }

    @Test
    void connected_isTrue_forAnElementWithItself() {
        UnionFind uf = new UnionFind();
        uf.add(7L);

        assertTrue(uf.connected(7L, 7L));
        assertEquals(1, uf.componentCount());
    }
}
