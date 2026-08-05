package com.braculink.dto;

import jakarta.validation.constraints.NotBlank;

public class VerifyRequest {

    @NotBlank
    private String bracuEmail;

    @NotBlank
    private String otp;

    public String getBracuEmail() {
        return bracuEmail;
    }

    public void setBracuEmail(String bracuEmail) {
        this.bracuEmail = bracuEmail;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
