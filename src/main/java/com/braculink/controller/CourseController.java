package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.CourseSummaryDto;
import com.braculink.dto.SectionPickerDto;
import com.braculink.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Live section data synced from connect.json")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/{courseCode}/sections")
    @Operation(summary = "List a course's sections for one semester, for the section picker")
    public ResponseEntity<ApiResponse<List<SectionPickerDto>>> getSections(
            @PathVariable String courseCode,
            @RequestParam Integer semesterSessionId) {
        List<SectionPickerDto> sections = courseService.getSections(courseCode, semesterSessionId);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    @GetMapping
    @Operation(summary = "Search courses by code or name")
    public ResponseEntity<ApiResponse<List<CourseSummaryDto>>> search(@RequestParam String search) {
        List<CourseSummaryDto> courses = courseService.searchCourses(search);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }

    @GetMapping("/current-semester")
    @Operation(summary = "Get the semester session id of the most recently synced data")
    public ResponseEntity<ApiResponse<Integer>> getCurrentSemester() {
        Integer semesterSessionId = courseService.getCurrentSemesterSessionId();
        return ResponseEntity.ok(ApiResponse.success(semesterSessionId));
    }
}
