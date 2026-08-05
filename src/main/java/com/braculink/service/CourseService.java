package com.braculink.service;

import com.braculink.dao.CourseSectionDao;
import com.braculink.dto.CourseSummaryDto;
import com.braculink.dto.SectionPickerDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private final CourseSectionDao courseSectionDao;

    public CourseService(CourseSectionDao courseSectionDao) {
        this.courseSectionDao = courseSectionDao;
    }

    public List<SectionPickerDto> getSections(String courseCode, Integer semesterSessionId) {
        return courseSectionDao.findSectionsForPicker(courseCode.toUpperCase(), semesterSessionId);
    }

    public List<CourseSummaryDto> searchCourses(String search) {
        return courseSectionDao.searchCourses(search);
    }
}
