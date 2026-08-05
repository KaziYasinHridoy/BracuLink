package com.braculink.dto;

public class SignupResponse {

    private Long userId;
    private String bracuEmail;

    public SignupResponse(Long userId, String bracuEmail) {
        this.userId = userId;
        this.bracuEmail = bracuEmail;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBracuEmail() {
        return bracuEmail;
    }

    public void setBracuEmail(String bracuEmail) {
        this.bracuEmail = bracuEmail;
    }
}
