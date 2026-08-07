package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.FriendRequestDto;
import com.braculink.dto.FriendSummaryDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.FriendshipService;
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
@RequestMapping("/api/friends")
@Tag(name = "Friends", description = "Friend requests, which gate routine and live-status visibility")
public class FriendshipController {

    private final FriendshipService friendshipService;

    public FriendshipController(FriendshipService friendshipService) {
        this.friendshipService = friendshipService;
    }

    @PostMapping("/request")
    @Operation(summary = "Send a friend request")
    public ResponseEntity<ApiResponse<Void>> sendRequest(@Valid @RequestBody FriendRequestDto request) {
        friendshipService.sendRequest(CurrentUser.id(), request.getAddresseeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Friend request sent", null));
    }

    @PostMapping("/{requesterId}/accept")
    @Operation(summary = "Accept an incoming friend request")
    public ResponseEntity<ApiResponse<Void>> accept(@PathVariable Long requesterId) {
        friendshipService.accept(requesterId, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Friend request accepted", null));
    }

    @PostMapping("/{requesterId}/decline")
    @Operation(summary = "Decline an incoming friend request")
    public ResponseEntity<ApiResponse<Void>> decline(@PathVariable Long requesterId) {
        friendshipService.decline(requesterId, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Friend request declined", null));
    }

    @DeleteMapping("/{otherUserId}")
    @Operation(summary = "Unfriend an existing friend")
    public ResponseEntity<ApiResponse<Void>> unfriend(@PathVariable Long otherUserId) {
        friendshipService.unfriend(CurrentUser.id(), otherUserId);
        return ResponseEntity.ok(ApiResponse.success("Unfriended", null));
    }

    @GetMapping
    @Operation(summary = "List my accepted friends")
    public ResponseEntity<ApiResponse<List<FriendSummaryDto>>> getFriends() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getFriends(CurrentUser.id())));
    }

    @GetMapping("/requests")
    @Operation(summary = "List friend requests I have received and not yet answered")
    public ResponseEntity<ApiResponse<List<FriendSummaryDto>>> getPendingIncoming() {
        return ResponseEntity.ok(ApiResponse.success(friendshipService.getPendingIncoming(CurrentUser.id())));
    }
}
