package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.ProposeSwapGroupRequest;
import com.braculink.dto.SwapGroupDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.SwapGroupProposalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/swap-groups")
public class SwapGroupController {

    private final SwapGroupProposalService swapGroupProposalService;

    public SwapGroupController(SwapGroupProposalService swapGroupProposalService) {
        this.swapGroupProposalService = swapGroupProposalService;
    }

    @PostMapping("/propose")
    public ResponseEntity<ApiResponse<SwapGroupDto>> propose(@Valid @RequestBody ProposeSwapGroupRequest request) {
        SwapGroupDto group = swapGroupProposalService.propose(CurrentUser.id(), request.getSwapRequestIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Swap group proposed", group));
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<SwapGroupDto>> confirm(@PathVariable Long id) {
        SwapGroupDto group = swapGroupProposalService.confirm(id, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Swap confirmed", group));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<ApiResponse<Void>> decline(@PathVariable Long id) {
        swapGroupProposalService.decline(id, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Swap group declined", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SwapGroupDto>>> getMyGroups() {
        return ResponseEntity.ok(ApiResponse.success(swapGroupProposalService.getMyGroups(CurrentUser.id())));
    }
}
