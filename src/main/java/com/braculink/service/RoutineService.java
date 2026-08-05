package com.braculink.service;

import com.braculink.common.ApiException;
import com.braculink.dao.CourseSectionDao;
import com.braculink.dao.EnrollmentDao;
import com.braculink.dto.LiveStatusDto;
import com.braculink.dto.RoutineEntryDto;
import com.braculink.dto.RoutineRowDto;
import com.braculink.model.ClassSlot;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class RoutineService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId DHAKA = ZoneId.of("Asia/Dhaka");

    private final EnrollmentDao enrollmentDao;
    private final CourseSectionDao courseSectionDao;

    public RoutineService(EnrollmentDao enrollmentDao, CourseSectionDao courseSectionDao) {
        this.enrollmentDao = enrollmentDao;
        this.courseSectionDao = courseSectionDao;
    }

    public List<RoutineRowDto> buildWeeklyRoutine(Long userId, Integer semesterSessionId) {
        List<Block> blocks = collectBlocks(userId, semesterSessionId);

        Map<TimeRange, Map<DayOfWeek, String>> rows =
                new TreeMap<>(Comparator.comparing(TimeRange::start).thenComparing(TimeRange::end));
        for (Block block : blocks) {
            TimeRange range = new TimeRange(block.start(), block.end());
            Map<DayOfWeek, String> cells = rows.computeIfAbsent(range, r -> new EnumMap<>(DayOfWeek.class));
            cells.merge(block.day(), block.label(), (existing, added) -> existing + ", " + added);
        }

        return rows.entrySet().stream()
                .map(entry -> new RoutineRowDto(formatTimeLabel(entry.getKey()), entry.getValue()))
                .toList();
    }

    public List<RoutineRowDto> buildMyWeeklyRoutine(Long userId) {
        return buildWeeklyRoutine(userId, currentSemesterSessionId());
    }

    public LiveStatusDto getLiveStatus(Long userId) {
        List<Block> blocks = collectBlocks(userId, currentSemesterSessionId());

        ZonedDateTime now = ZonedDateTime.now(DHAKA);
        DayOfWeek today = now.getDayOfWeek();
        LocalTime nowTime = now.toLocalTime();

        for (Block block : blocks) {
            if (block.day() == today && !nowTime.isBefore(block.start()) && nowTime.isBefore(block.end())) {
                return LiveStatusDto.busy(block.courseCode(), block.end().format(TIME_FORMAT));
            }
        }
        return LiveStatusDto.free();
    }

    private Integer currentSemesterSessionId() {
        return courseSectionDao.findCurrentSemesterSessionId()
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No course data available yet"));
    }

    private List<Block> collectBlocks(Long userId, Integer semesterSessionId) {
        List<RoutineEntryDto> entries = enrollmentDao.findByUserAndSemester(userId, semesterSessionId);

        List<Block> blocks = new ArrayList<>();
        for (RoutineEntryDto entry : entries) {
            if (entry.getClassSchedules() != null) {
                for (ClassSlot slot : entry.getClassSchedules()) {
                    blocks.add(new Block(slot.getDay(), slot.getStartTime(), slot.getEndTime(),
                            entry.getCourseCode(),
                            formatCell(entry.getCourseCode(), entry.getSectionName(), entry.getFaculties(), entry.getRoomName())));
                }
            }
            if (entry.isHasLab() && entry.getLabSchedules() != null) {
                String labCourseCode = entry.getCourseCode() + "L";
                for (ClassSlot slot : entry.getLabSchedules()) {
                    blocks.add(new Block(slot.getDay(), slot.getStartTime(), slot.getEndTime(),
                            labCourseCode,
                            formatCell(labCourseCode, entry.getSectionName(), entry.getLabFaculties(), entry.getLabRoomName())));
                }
            }
        }
        return blocks;
    }

    private static String formatCell(String courseCode, String sectionName, String faculty, String room) {
        return courseCode + "-" + sectionName + " -" + nvl(faculty) + "-" + nvl(room);
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static String formatTimeLabel(TimeRange range) {
        return range.start().format(TIME_FORMAT) + "–" + range.end().format(TIME_FORMAT);
    }

    private record Block(DayOfWeek day, LocalTime start, LocalTime end, String courseCode, String label) {
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
