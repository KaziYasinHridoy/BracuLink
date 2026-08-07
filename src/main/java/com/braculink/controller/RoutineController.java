package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.LiveStatusDto;
import com.braculink.dto.RoutineRowDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routine")
@Tag(name = "Routine", description = "The caller's own weekly grid, auto-built from their enrollments")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my weekly routine for the current semester")
    public ResponseEntity<ApiResponse<List<RoutineRowDto>>> getMyRoutine() {
        List<RoutineRowDto> routine = routineService.buildWeeklyRoutineForCurrentSemester(CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(routine));
    }

    @GetMapping("/me/status")
    @Operation(summary = "Get my live free/busy status, computed in Asia/Dhaka")
    public ResponseEntity<ApiResponse<LiveStatusDto>> getMyStatus() {
        LiveStatusDto status = routineService.getLiveStatus(CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
