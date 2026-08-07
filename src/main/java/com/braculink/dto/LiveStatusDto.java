package com.braculink.dto;

public class LiveStatusDto {

    private String status;
    private String courseCode;
    private String until;

    public static LiveStatusDto free() {
        LiveStatusDto dto = new LiveStatusDto();
        dto.status = "FREE";
        return dto;
    }

    public static LiveStatusDto busy(String courseCode, String until) {
        LiveStatusDto dto = new LiveStatusDto();
        dto.status = "BUSY";
        dto.courseCode = courseCode;
        dto.until = until;
        return dto;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getUntil() {
        return until;
    }

    public void setUntil(String until) {
        this.until = until;
    }
}
