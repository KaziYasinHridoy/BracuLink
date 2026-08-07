package com.braculink.dto;

/**
 * One member of a swap group, as seen by the other members.
 *
 * <p>Carries the member's {@link PublicProfileDto} because that is the whole coordination channel —
 * there is no in-app chat, so people reach each other through the Facebook link (always shown when
 * filled in) or the phone number (only when they made it public).
 */
public class SwapGroupMemberDto {

    private Long userId;
    private PublicProfileDto profile;
    private String fromSection;
    private String toSection;
    private boolean confirmed;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PublicProfileDto getProfile() {
        return profile;
    }

    public void setProfile(PublicProfileDto profile) {
        this.profile = profile;
    }

    public String getFromSection() {
        return fromSection;
    }

    public void setFromSection(String fromSection) {
        this.fromSection = fromSection;
    }

    public String getToSection() {
        return toSection;
    }

    public void setToSection(String toSection) {
        this.toSection = toSection;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
}
