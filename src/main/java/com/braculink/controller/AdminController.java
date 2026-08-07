package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.service.CourseSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Manual triggers for the background course sync")
public class AdminController {

    private final CourseSyncService courseSyncService;

    public AdminController(CourseSyncService courseSyncService) {
        this.courseSyncService = courseSyncService;
    }

    @PostMapping("/sync-now")
    @Operation(summary = "Force an immediate sync against connect.json")
    public ResponseEntity<ApiResponse<Integer>> syncNow() {
        int count = courseSyncService.syncNow();
        return ResponseEntity.ok(ApiResponse.success("Course sections synced", count));
    }
}
