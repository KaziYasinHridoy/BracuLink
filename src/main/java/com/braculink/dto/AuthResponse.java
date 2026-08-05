package com.braculink.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private Long userId;
    private String fullName;
    private String studentId;
    private String bracuEmail;

    public AuthResponse(String token, Long userId, String fullName, String studentId, String bracuEmail) {
        this.token = token;
        this.userId = userId;
        this.fullName = fullName;
        this.studentId = studentId;
        this.bracuEmail = bracuEmail;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getBracuEmail() {
        return bracuEmail;
    }

    public void setBracuEmail(String bracuEmail) {
        this.bracuEmail = bracuEmail;
    }
}
