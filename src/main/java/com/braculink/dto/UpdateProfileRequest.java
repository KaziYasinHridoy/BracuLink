package com.braculink.dto;

public class UpdateProfileRequest {

    private String phoneNumber;
    private boolean phonePublic;
    private String fbProfileUrl;

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
}
