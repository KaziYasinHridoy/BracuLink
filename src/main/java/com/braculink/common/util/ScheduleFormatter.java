package com.braculink.common.util;

import com.braculink.model.ClassSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ScheduleFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<DayOfWeek> WEEK_ORDER = List.of(
            DayOfWeek.SATURDAY, DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

    private ScheduleFormatter() {
    }

    /**
     * Formats slots like "Sat/Thu 11:00–12:20", grouping days that share the same time range.
     */
    public static String format(List<ClassSlot> slots) {
        if (slots == null || slots.isEmpty()) {
            return null;
        }

        Map<TimeRange, List<DayOfWeek>> byTimeRange = new LinkedHashMap<>();
        for (ClassSlot slot : slots) {
            TimeRange range = new TimeRange(slot.getStartTime(), slot.getEndTime());
            byTimeRange.computeIfAbsent(range, r -> new ArrayList<>()).add(slot.getDay());
        }

        return byTimeRange.entrySet().stream()
                .map(entry -> formatGroup(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(", "));
    }

    private static String formatGroup(TimeRange range, List<DayOfWeek> days) {
        String dayList = days.stream()
                .sorted(Comparator.comparingInt(WEEK_ORDER::indexOf))
                .map(day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .collect(Collectors.joining("/"));
        return dayList + " " + range.start().format(TIME_FORMAT) + "–" + range.end().format(TIME_FORMAT);
    }

    private record TimeRange(LocalTime start, LocalTime end) {
    }
}
