package com.braculink.dto;

import java.time.LocalDateTime;

public class UserProfileDto {

    private Long id;
    private String studentId;
    private String fullName;
    private String bracuEmail;
    private String phoneNumber;
    private boolean phonePublic;
    private String fbProfileUrl;
    private boolean emailVerified;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBracuEmail() {
        return bracuEmail;
    }

    public void setBracuEmail(String bracuEmail) {
        this.bracuEmail = bracuEmail;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isPhonePublic() {
        return phonePublic;
    }

    public void setPhonePublic(boolean phonePublic) {
        this.phonePublic = phonePublic;
    }

    public String getFbProfileUrl() {
        return fbProfileUrl;
    }

    public void setFbProfileUrl(String fbProfileUrl) {
        this.fbProfileUrl = fbProfileUrl;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
