package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.CourseSectionDao;
import com.braculink.dao.EnrollmentDao;
import com.braculink.dto.EnrollmentRequest;
import com.braculink.dto.EnrollmentResponse;
import com.braculink.dto.RoutineEntryDto;
import com.braculink.model.Enrollment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentService {

    private final EnrollmentDao enrollmentDao;
    private final CourseSectionDao courseSectionDao;

    public EnrollmentService(EnrollmentDao enrollmentDao, CourseSectionDao courseSectionDao) {
        this.enrollmentDao = enrollmentDao;
        this.courseSectionDao = courseSectionDao;
    }

    public EnrollmentResponse enroll(Long userId, EnrollmentRequest request) {
        String courseCode = request.getCourseCode().toUpperCase();

        Long sectionId = courseSectionDao
                .findTheorySectionId(courseCode, request.getSectionName(), request.getSemesterSessionId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Section not found"));

        if (enrollmentDao.existsForUserAndCourse(userId, courseCode, request.getSemesterSessionId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Already enrolled in this course this semester");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setUserId(userId);
        enrollment.setSectionId(sectionId);
        enrollment.setSemesterSessionId(request.getSemesterSessionId());
        Long id = enrollmentDao.insert(enrollment);

        return new EnrollmentResponse(id, courseCode, request.getSectionName(), request.getSemesterSessionId());
    }

    public List<RoutineEntryDto> getMyEnrollments(Long userId) {
        Integer currentSemester = courseSectionDao.findCurrentSemesterSessionId()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No course data available yet"));
        return enrollmentDao.findByUserAndSemester(userId, currentSemester);
    }

    public void unenroll(Long id, Long userId) {
        int rows = enrollmentDao.deleteByIdAndUser(id, userId);
        if (rows == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Enrollment not found");
        }
    }
}
