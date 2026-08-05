package com.braculink.swap.engine;

import java.util.HashMap;
import java.util.Map;

/**
 * Disjoint-set union over section ids, used as a cheap pre-filter before the cycle search.
 *
 * <p>Keyed by a {@link Map} rather than an array because section ids are database primary keys —
 * arbitrary, sparse longs, not a dense 0..n range.
 *
 * <p>Elements are added implicitly on first use, so callers never have to pre-register them.
 * Path compression plus union by rank keeps every operation effectively constant time.
 *
 * <p><strong>This package is pure Java: no Spring, no SQL, no database access.</strong>
 */
public final class UnionFind {

    private final Map<Long, Long> parent = new HashMap<>();
    private final Map<Long, Integer> rank = new HashMap<>();

    /** Registers an element as its own singleton component, if it is not already known. */
    public void add(long element) {
        parent.putIfAbsent(element, element);
        rank.putIfAbsent(element, 0);
    }

    public boolean contains(long element) {
        return parent.containsKey(element);
    }

    /** Returns the representative of the element's component, compressing the path on the way. */
    public long find(long element) {
        add(element);
        long root = element;
        while (parent.get(root) != root) {
            root = parent.get(root);
        }
        // Second pass: point everything on the path straight at the root.
        long walk = element;
        while (parent.get(walk) != root) {
            long next = parent.get(walk);
            parent.put(walk, root);
            walk = next;
        }
        return root;
    }

    /**
     * Merges the two elements' components.
     *
     * @return true if they were in different components and have now been merged
     */
    public boolean union(long a, long b) {
        long rootA = find(a);
        long rootB = find(b);
        if (rootA == rootB) {
            return false;
        }
        int rankA = rank.get(rootA);
        int rankB = rank.get(rootB);
        if (rankA < rankB) {
            parent.put(rootA, rootB);
        } else if (rankA > rankB) {
            parent.put(rootB, rootA);
        } else {
            parent.put(rootB, rootA);
            rank.put(rootA, rankA + 1);
        }
        return true;
    }

    /**
     * Whether the two elements are in the same component.
     *
     * <p>An element that was never added is in no component at all, so this returns false rather
     * than silently creating a singleton for it — the caller is asking about reachability, and an
     * unknown section is reachable from nothing.
     */
    public boolean connected(long a, long b) {
        if (a == b) {
            return true;
        }
        if (!parent.containsKey(a) || !parent.containsKey(b)) {
            return false;
        }
        return find(a) == find(b);
    }

    /** The number of distinct components across every element added so far. */
    public int componentCount() {
        int roots = 0;
        for (Long element : parent.keySet()) {
            if (find(element) == element) {
                roots++;
            }
        }
        return roots;
    }
}
