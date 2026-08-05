package com.braculink.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class ProposeSwapGroupRequest {

    @NotEmpty
    @Size(min = 2, max = 5, message = "a swap group must have between 2 and 5 members")
    private List<Long> swapRequestIds;

    public List<Long> getSwapRequestIds() {
        return swapRequestIds;
    }

    public void setSwapRequestIds(List<Long> swapRequestIds) {
        this.swapRequestIds = swapRequestIds;
    }
}
