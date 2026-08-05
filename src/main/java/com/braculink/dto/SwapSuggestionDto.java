package com.braculink.dto;

import java.util.List;

/**
 * One viable swap group the matching engine found.
 *
 * <p>The requesting student is always {@code members[0]} and their swap partner follows,
 * so the UI can render "you -&gt; them" without reordering.
 *
 * <p>Serialized only, never deserialized — no setters, no no-arg constructor.
 * Deliberately annotation-free: {@code com.braculink.swap.engine} imports this type,
 * and that package must compile against nothing but the JDK. Do not add Jackson
 * (or any other) annotations here.
 */
public class SwapSuggestionDto {

    private final int groupSize;
    private final List<SwapMemberDto> members;

    public SwapSuggestionDto(List<SwapMemberDto> members) {
        this.members = List.copyOf(members);
        this.groupSize = this.members.size();
    }

    public int getGroupSize() {
        return groupSize;
    }

    public List<SwapMemberDto> getMembers() {
        return members;
    }
}
