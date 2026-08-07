package com.braculink.dto;

import jakarta.validation.constraints.NotNull;

public class FriendRequestDto {

    @NotNull
    private Long addresseeId;

    public Long getAddresseeId() {
        return addresseeId;
    }

    public void setAddresseeId(Long addresseeId) {
        this.addresseeId = addresseeId;
    }
}
