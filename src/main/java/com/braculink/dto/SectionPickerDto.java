package com.braculink.dto;

public class SectionPickerDto {

    private String sectionName;
    private String theoryFaculty;
    private String theoryTiming;
    private boolean hasLab;
    private String labFaculty;
    private String labTiming;
    private Integer availableSeats;
    private String roomName;

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getTheoryFaculty() {
        return theoryFaculty;
    }

    public void setTheoryFaculty(String theoryFaculty) {
        this.theoryFaculty = theoryFaculty;
    }

    public String getTheoryTiming() {
        return theoryTiming;
    }

    public void setTheoryTiming(String theoryTiming) {
        this.theoryTiming = theoryTiming;
    }

    public boolean isHasLab() {
        return hasLab;
    }

    public void setHasLab(boolean hasLab) {
        this.hasLab = hasLab;
    }

    public String getLabFaculty() {
        return labFaculty;
    }

    public void setLabFaculty(String labFaculty) {
        this.labFaculty = labFaculty;
    }

    public String getLabTiming() {
        return labTiming;
    }

    public void setLabTiming(String labTiming) {
        this.labTiming = labTiming;
    }

    public Integer getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
}
