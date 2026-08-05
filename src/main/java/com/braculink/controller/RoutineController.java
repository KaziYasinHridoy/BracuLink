package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.LiveStatusDto;
import com.braculink.dto.RoutineRowDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.RoutineService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routine")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<RoutineRowDto>>> getMyRoutine() {
        List<RoutineRowDto> routine = routineService.buildMyWeeklyRoutine(CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(routine));
    }

    @GetMapping("/me/status")
    public ResponseEntity<ApiResponse<LiveStatusDto>> getMyStatus() {
        LiveStatusDto status = routineService.getLiveStatus(CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(status));
    }
}
