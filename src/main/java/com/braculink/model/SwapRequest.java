package com.braculink.model;

import java.time.LocalDateTime;

public class SwapRequest {

    private Long id;
    private Long userId;
    private String courseCode;
    private Long currentSectionId;
    private Long desiredSectionId;
    private String status;
    private boolean confirmed;
    private Long groupId;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public Long getCurrentSectionId() {
        return currentSectionId;
    }

    public void setCurrentSectionId(Long currentSectionId) {
        this.currentSectionId = currentSectionId;
    }

    public Long getDesiredSectionId() {
        return desiredSectionId;
    }

    public void setDesiredSectionId(Long desiredSectionId) {
        this.desiredSectionId = desiredSectionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
}
