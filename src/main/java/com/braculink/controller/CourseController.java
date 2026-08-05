package com.braculink.controller;

import com.braculink.common.ApiResponse;
import com.braculink.dto.CourseSummaryDto;
import com.braculink.dto.SectionPickerDto;
import com.braculink.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/{courseCode}/sections")
    public ResponseEntity<ApiResponse<List<SectionPickerDto>>> getSections(
            @PathVariable String courseCode,
            @RequestParam Integer semesterSessionId) {
        List<SectionPickerDto> sections = courseService.getSections(courseCode, semesterSessionId);
        return ResponseEntity.ok(ApiResponse.success(sections));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseSummaryDto>>> search(@RequestParam String search) {
        List<CourseSummaryDto> courses = courseService.searchCourses(search);
        return ResponseEntity.ok(ApiResponse.success(courses));
    }
}
