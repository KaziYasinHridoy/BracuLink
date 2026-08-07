package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.LiveStatusDto;
import com.braculink.dto.PublicProfileDto;
import com.braculink.dto.RoutineRowDto;
import com.braculink.dto.UpdateProfileRequest;
import com.braculink.dto.UserProfileDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Profile, and friend-gated views of another student's routine")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my own profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getMe() {
        return ResponseEntity.ok(ApiResponse.success(userService.getMe(CurrentUser.id())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my phone, phone visibility, and Facebook link")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateMe(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateMe(CurrentUser.id(), request)));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get another student's public profile (FB link always shown if set; phone gated)")
    public ResponseEntity<ApiResponse<PublicProfileDto>> getPublicProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getPublicProfile(id)));
    }

    @GetMapping("/{id}/routine")
    @Operation(summary = "Get a friend's weekly routine — 403 unless we are accepted friends")
    public ResponseEntity<ApiResponse<List<RoutineRowDto>>> getUserRoutine(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getRoutineIfFriends(CurrentUser.id(), id)));
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Get a friend's live free/busy status — 403 unless we are accepted friends")
    public ResponseEntity<ApiResponse<LiveStatusDto>> getUserStatus(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getStatusIfFriends(CurrentUser.id(), id)));
    }
}
