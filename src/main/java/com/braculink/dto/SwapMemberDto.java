package com.braculink.dto;

/**
 * One member of a suggested swap group.
 *
 * <p>Serialized only, never deserialized — no setters, no no-arg constructor.
 * Deliberately annotation-free: {@code com.braculink.swap.engine} imports this type,
 * and that package must compile against nothing but the JDK. Do not add Jackson
 * (or any other) annotations here.
 *
 * <p>{@code fromSection} and {@code toSection} are human-readable section names
 * ("15", "15B"), never internal section ids.
 *
 * <p>{@code requestId} is that member's underlying {@code swap_request} id — the client needs it
 * to build the {@code swapRequestIds} list for {@code POST /api/swap-groups/propose}.
 */
public class SwapMemberDto {

    private final Long requestId;
    private final Long userId;
    private final String fullName;
    private final String studentId;
    private final String fromSection;
    private final String toSection;

    public SwapMemberDto(Long requestId, Long userId, String fullName, String studentId, String fromSection,
            String toSection) {
        this.requestId = requestId;
        this.userId = userId;
        this.fullName = fullName;
        this.studentId = studentId;
        this.fromSection = fromSection;
        this.toSection = toSection;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFromSection() {
        return fromSection;
    }

    public String getToSection() {
        return toSection;
    }
}
