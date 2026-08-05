package com.braculink.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SignupRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String studentId;

    @NotBlank
    private String bracuEmail;

    @NotBlank
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
