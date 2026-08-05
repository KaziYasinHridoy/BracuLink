package com.braculink.dto;

import com.braculink.model.ClassSlot;

import java.util.List;

public class RoutineEntryDto {

    private Long enrollmentId;
    private String courseCode;
    private String courseName;
    private String sectionName;
    private String faculties;
    private String roomName;
    private List<ClassSlot> classSchedules;
    private boolean hasLab;
    private String labFaculties;
    private List<ClassSlot> labSchedules;
    private Integer semesterSessionId;

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getFaculties() {
        return faculties;
    }

    public void setFaculties(String faculties) {
        this.faculties = faculties;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public List<ClassSlot> getClassSchedules() {
        return classSchedules;
    }

    public void setClassSchedules(List<ClassSlot> classSchedules) {
        this.classSchedules = classSchedules;
    }

    public boolean isHasLab() {
        return hasLab;
    }

    public void setHasLab(boolean hasLab) {
        this.hasLab = hasLab;
    }

    public String getLabFaculties() {
        return labFaculties;
    }

    public void setLabFaculties(String labFaculties) {
        this.labFaculties = labFaculties;
    }

    public List<ClassSlot> getLabSchedules() {
        return labSchedules;
    }

    public void setLabSchedules(List<ClassSlot> labSchedules) {
        this.labSchedules = labSchedules;
    }

    public Integer getSemesterSessionId() {
        return semesterSessionId;
    }

    public void setSemesterSessionId(Integer semesterSessionId) {
        this.semesterSessionId = semesterSessionId;
    }
}
