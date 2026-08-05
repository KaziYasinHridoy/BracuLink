package com.braculink.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SwapGroupDto {

    private Long groupId;
    private String courseCode;
    private String status;
    private int groupSize;
    private boolean allConfirmed;
    private LocalDateTime createdAt;
    private List<SwapGroupMemberDto> members;

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public boolean isAllConfirmed() {
        return allConfirmed;
    }

    public void setAllConfirmed(boolean allConfirmed) {
        this.allConfirmed = allConfirmed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<SwapGroupMemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<SwapGroupMemberDto> members) {
        this.members = members;
    }
}
