package com.braculink.dto;

import java.time.DayOfWeek;
import java.util.Map;

public class RoutineRowDto {

    private String timeLabel;
    private Map<DayOfWeek, String> cells;

    public RoutineRowDto(String timeLabel, Map<DayOfWeek, String> cells) {
        this.timeLabel = timeLabel;
        this.cells = cells;
    }

    public String getTimeLabel() {
        return timeLabel;
    }

    public void setTimeLabel(String timeLabel) {
        this.timeLabel = timeLabel;
    }

    public Map<DayOfWeek, String> getCells() {
        return cells;
    }

    public void setCells(Map<DayOfWeek, String> cells) {
        this.cells = cells;
    }
}
