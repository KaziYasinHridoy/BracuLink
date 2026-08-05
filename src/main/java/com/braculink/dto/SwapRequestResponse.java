package com.braculink.dto;

import java.time.LocalDateTime;

public class SwapRequestResponse {

    private Long id;
    private String courseCode;
    private String currentSectionName;
    private String desiredSectionName;
    private String status;
    private LocalDateTime createdAt;

    public SwapRequestResponse() {
    }

    public SwapRequestResponse(Long id, String courseCode, String currentSectionName, String desiredSectionName,
            String status, LocalDateTime createdAt) {
        this.id = id;
        this.courseCode = courseCode;
        this.currentSectionName = currentSectionName;
        this.desiredSectionName = desiredSectionName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCurrentSectionName() {
        return currentSectionName;
    }

    public void setCurrentSectionName(String currentSectionName) {
        this.currentSectionName = currentSectionName;
    }

    public String getDesiredSectionName() {
        return desiredSectionName;
    }

    public void setDesiredSectionName(String desiredSectionName) {
        this.desiredSectionName = desiredSectionName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
