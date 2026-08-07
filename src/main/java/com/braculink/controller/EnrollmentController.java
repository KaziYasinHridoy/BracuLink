package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.EnrollmentRequest;
import com.braculink.dto.EnrollmentResponse;
import com.braculink.dto.RoutineEntryDto;
import com.braculink.security.CurrentUser;
import com.braculink.service.EnrollmentService;
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
@RequestMapping("/api/enrollments")
@Tag(name = "Enrollments", description = "Adding a course + section builds the routine automatically")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @Operation(summary = "Enroll in a course section by course code and section name")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(@Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(CurrentUser.id(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Enrolled", response));
    }

    @GetMapping
    @Operation(summary = "List my enrollments for the current semester")
    public ResponseEntity<ApiResponse<List<RoutineEntryDto>>> getMyEnrollments() {
        List<RoutineEntryDto> entries = enrollmentService.getMyEnrollments(CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a course from my routine")
    public ResponseEntity<ApiResponse<Void>> unenroll(@PathVariable Long id) {
        enrollmentService.unenroll(id, CurrentUser.id());
        return ResponseEntity.ok(ApiResponse.success("Removed from routine", null));
    }
}
