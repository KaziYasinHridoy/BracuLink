package com.braculink.service;

import com.braculink.client.ConnectJsonClient;
import com.braculink.dao.CourseSectionDao;
import com.braculink.dto.ConnectJsonSectionDto;
import com.braculink.model.CourseSection;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseSyncService {

    private final ConnectJsonClient connectJsonClient;
    private final CourseSectionDao courseSectionDao;

    public CourseSyncService(ConnectJsonClient connectJsonClient, CourseSectionDao courseSectionDao) {
        this.connectJsonClient = connectJsonClient;
        this.courseSectionDao = courseSectionDao;
    }

    public int syncNow() {
        List<ConnectJsonSectionDto> sections = connectJsonClient.fetchSections();
        LocalDateTime syncedAt = LocalDateTime.now();
        List<CourseSection> rows = sections.stream()
                .map(dto -> toCourseSection(dto, syncedAt))
                .toList();
        courseSectionDao.upsertAll(rows);
        return rows.size();
    }

    private CourseSection toCourseSection(ConnectJsonSectionDto dto, LocalDateTime syncedAt) {
        CourseSection section = new CourseSection();
        section.setSectionId(dto.getSectionId());
        section.setCourseCode(dto.getCourseCode());
        section.setCourseName(dto.getCourseName());
        section.setCourseType(dto.getCourseType());
        section.setSectionName(dto.getSectionName());
        section.setFaculties(dto.getFaculties());
        section.setRoomName(dto.getRoomName());
        section.setCapacity(dto.getCapacity());
        section.setConsumedSeat(dto.getConsumedSeat());
        section.setSemesterSessionId(dto.getSemesterSessionId());
        section.setClassSchedules(dto.getSectionSchedule() != null ? dto.getSectionSchedule().getClassSchedules() : null);
        section.setLabSectionId(dto.getLabSectionId());
        section.setLabFaculties(dto.getLabFaculties());
        section.setLabSchedules(dto.getLabSchedules());
        section.setLastSyncedAt(syncedAt);
        return section;
    }
}
