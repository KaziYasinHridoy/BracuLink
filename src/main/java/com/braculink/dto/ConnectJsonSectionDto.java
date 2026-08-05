package com.braculink.dto;

import com.braculink.model.ClassSlot;

import java.util.List;

public class ConnectJsonSectionDto {

    private Long sectionId;
    private String courseCode;
    private String courseName;
    private String courseType;
    private String sectionName;
    private String faculties;
    private String roomName;
    private Integer capacity;
    private Integer consumedSeat;
    private Integer semesterSessionId;
    private SectionScheduleDto sectionSchedule;
    private Long labSectionId;
    private String labFaculties;
    private String labRoomName;
    private List<ClassSlot> labSchedules;

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
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

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Integer getConsumedSeat() {
        return consumedSeat;
    }

    public void setConsumedSeat(Integer consumedSeat) {
        this.consumedSeat = consumedSeat;
    }

    public Integer getSemesterSessionId() {
        return semesterSessionId;
    }

    public void setSemesterSessionId(Integer semesterSessionId) {
        this.semesterSessionId = semesterSessionId;
    }

    public SectionScheduleDto getSectionSchedule() {
        return sectionSchedule;
    }

    public void setSectionSchedule(SectionScheduleDto sectionSchedule) {
        this.sectionSchedule = sectionSchedule;
    }

    public Long getLabSectionId() {
        return labSectionId;
    }

    public void setLabSectionId(Long labSectionId) {
        this.labSectionId = labSectionId;
    }

    public String getLabFaculties() {
        return labFaculties;
    }

    public void setLabFaculties(String labFaculties) {
        this.labFaculties = labFaculties;
    }

    public String getLabRoomName() {
        return labRoomName;
    }

    public void setLabRoomName(String labRoomName) {
        this.labRoomName = labRoomName;
    }

    public List<ClassSlot> getLabSchedules() {
        return labSchedules;
    }

    public void setLabSchedules(List<ClassSlot> labSchedules) {
        this.labSchedules = labSchedules;
    }
}
