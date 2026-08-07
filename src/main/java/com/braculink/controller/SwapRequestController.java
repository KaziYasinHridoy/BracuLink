package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.SwapRequestCreateDto;
import com.braculink.dto.SwapRequestResponse;
import com.braculink.dto.SwapSuggestionDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.SwapRequestService;
import com.braculink.service.SwapSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Swap Requests", description = "\"I have this section, I want that one\" — same course only")
public class SwapRequestController {

    private final SwapRequestService swapRequestService;
    private final SwapSuggestionService swapSuggestionService;

    public SwapRequestController(SwapRequestService swapRequestService,
            SwapSuggestionService swapSuggestionService) {
        this.swapRequestService = swapRequestService;
        this.swapSuggestionService = swapSuggestionService;
    }

    @PostMapping
    @Operation(summary = "Create a swap request for one of my enrolled sections")
    public ResponseEntity<ApiResponse<SwapRequestResponse>> create(@Valid @RequestBody SwapRequestCreateDto request) {
        SwapRequestResponse response = swapRequestService.create(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Swap request created", response));
    }

    @GetMapping("/me")
    @Operation(summary = "List my swap requests")
    public ResponseEntity<ApiResponse<List<SwapRequestResponse>>> getMyRequests() {
        return ResponseEntity.ok(ApiResponse.success(swapRequestService.getMyRequests(CurrentUser.id())));
    }

    @GetMapping("/{id}/suggestions")
    @Operation(summary = "Find viable swap groups (sizes 2-5) for one of my pending requests")
    public ResponseEntity<ApiResponse<List<SwapSuggestionDto>>> getSuggestions(@PathVariable Long id) {
        List<SwapSuggestionDto> suggestions = swapSuggestionService.getSuggestions(id, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel my own pending swap request")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        swapRequestService.cancel(id, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Swap request cancelled", null));
    }
}
