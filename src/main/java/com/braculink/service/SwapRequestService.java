package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.CourseSectionDao;
import com.braculink.dao.SwapRequestDao;
import com.braculink.dto.SwapRequestCreateDto;
import com.braculink.dto.SwapRequestResponse;
import com.braculink.model.SwapRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SwapRequestService {

    private final SwapRequestDao swapRequestDao;
    private final CourseSectionDao courseSectionDao;

    public SwapRequestService(SwapRequestDao swapRequestDao, CourseSectionDao courseSectionDao) {
        this.swapRequestDao = swapRequestDao;
        this.courseSectionDao = courseSectionDao;
    }

    public SwapRequestResponse create(Long userId, SwapRequestCreateDto request) {
        String courseCode = request.getCourseCode().toUpperCase();

        if (request.getCurrentSectionName().equalsIgnoreCase(request.getDesiredSectionName())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current and desired sections must be different");
        }

        Integer semesterSessionId = courseSectionDao.findCurrentSemesterSessionId()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No course data available yet"));

        Long currentSectionId = courseSectionDao
                .findTheorySectionId(courseCode, request.getCurrentSectionName(), semesterSessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Current section not found"));

        Long desiredSectionId = courseSectionDao
                .findTheorySectionId(courseCode, request.getDesiredSectionName(), semesterSessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Desired section not found"));

        if (swapRequestDao.existsActiveForUserAndCourse(userId, courseCode)) {
            throw new ApiException(HttpStatus.CONFLICT, "You already have an active swap request for this course");
        }

        SwapRequest swapRequest = new SwapRequest();
        swapRequest.setUserId(userId);
        swapRequest.setCourseCode(courseCode);
        swapRequest.setCurrentSectionId(currentSectionId);
        swapRequest.setDesiredSectionId(desiredSectionId);
        swapRequest.setStatus("PENDING");
        swapRequest.setConfirmed(false);
        LocalDateTime createdAt = LocalDateTime.now();
        swapRequest.setCreatedAt(createdAt);

        Long id = swapRequestDao.insert(swapRequest);

        return new SwapRequestResponse(id, courseCode, request.getCurrentSectionName(),
                request.getDesiredSectionName(), "PENDING", createdAt);
    }

    public List<SwapRequestResponse> getMyRequests(Long userId) {
        return swapRequestDao.findByUser(userId);
    }

    public void cancel(Long id, Long userId) {
        int rows = swapRequestDao.cancel(id, userId);
        if (rows == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Pending swap request not found");
        }
    }
}
