package com.braculink.dto;

import jakarta.validation.constraints.NotBlank;

public class SwapRequestCreateDto {

    @NotBlank
    private String courseCode;

    @NotBlank
    private String currentSectionName;

    @NotBlank
    private String desiredSectionName;

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
}
