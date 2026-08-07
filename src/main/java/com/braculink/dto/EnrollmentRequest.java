package com.braculink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class EnrollmentRequest {

    @NotBlank
    private String courseCode;

    @NotBlank
    private String sectionName;

    @NotNull
    private Integer semesterSessionId;

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public Integer getSemesterSessionId() {
        return semesterSessionId;
    }

    public void setSemesterSessionId(Integer semesterSessionId) {
        this.semesterSessionId = semesterSessionId;
    }
}
