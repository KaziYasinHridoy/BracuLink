package com.braculink.swap.engine;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * An immutable, in-memory read view of one {@code swap_request} row, already joined to
 * {@code user} and to {@code course_section} twice — so it carries the requester's name and
 * both human-readable section names, not just ids.
 *
 * <p><strong>This package is pure Java: no Spring, no SQL, no database access.</strong> It compiles
 * against nothing but the JDK. The DAO loads these views; the engine only ever works on objects
 * already in memory.
 *
 * <p>{@code status} is carried deliberately rather than being pre-filtered away by the loading
 * query. Excluding non-PENDING requests is an invariant of {@link SwapGraph}, not of one SQL
 * {@code WHERE} clause that a future query could forget — and keeping it here is what lets that
 * exclusion be tested with plain objects and no database.
 */
public final class SwapRequestView {

    private final long requestId;
    private final long userId;
    private final String fullName;
    private final String studentId;
    private final String courseCode;
    private final long currentSectionId;
    private final String currentSectionName;
    private final long desiredSectionId;
    private final String desiredSectionName;
    private final String status;
    private final LocalDateTime createdAt;

    public SwapRequestView(long requestId, long userId, String fullName, String studentId, String courseCode,
            long currentSectionId, String currentSectionName,
            long desiredSectionId, String desiredSectionName,
            String status, LocalDateTime createdAt) {
        this.requestId = requestId;
        this.userId = userId;
        this.fullName = fullName;
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.currentSectionId = currentSectionId;
        this.currentSectionName = currentSectionName;
        this.desiredSectionId = desiredSectionId;
        this.desiredSectionName = desiredSectionName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public long getCurrentSectionId() {
        return currentSectionId;
    }

    public String getCurrentSectionName() {
        return currentSectionName;
    }

    public long getDesiredSectionId() {
        return desiredSectionId;
    }

    public String getDesiredSectionName() {
        return desiredSectionName;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SwapRequestView)) {
            return false;
        }
        return requestId == ((SwapRequestView) other).requestId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId);
    }

    @Override
    public String toString() {
        return "SwapRequestView{requestId=" + requestId
                + ", userId=" + userId
                + ", courseCode=" + courseCode
                + ", " + currentSectionName + "(" + currentSectionId + ")"
                + " -> " + desiredSectionName + "(" + desiredSectionId + ")"
                + ", status=" + status + "}";
    }
}
