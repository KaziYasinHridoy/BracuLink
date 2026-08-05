package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.SwapRequestCreateDto;
import com.braculink.dto.SwapRequestResponse;
import com.braculink.security.CurrentUser;
import com.braculink.service.SwapRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/swap-requests")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SwapRequestResponse>> create(@Valid @RequestBody SwapRequestCreateDto request) {
        SwapRequestResponse response = swapRequestService.create(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Swap request created", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<SwapRequestResponse>>> getMyRequests() {
        return ResponseEntity.ok(ApiResponse.success(swapRequestService.getMyRequests(CurrentUser.id())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        swapRequestService.cancel(id, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Swap request cancelled", null));
    }
}
