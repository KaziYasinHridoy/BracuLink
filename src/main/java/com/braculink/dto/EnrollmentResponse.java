package com.braculink.dto;

public class EnrollmentResponse {

    private Long id;
    private String courseCode;
    private String sectionName;
    private Integer semesterSessionId;

    public EnrollmentResponse(Long id, String courseCode, String sectionName, Integer semesterSessionId) {
        this.id = id;
        this.courseCode = courseCode;
        this.sectionName = sectionName;
        this.semesterSessionId = semesterSessionId;
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
