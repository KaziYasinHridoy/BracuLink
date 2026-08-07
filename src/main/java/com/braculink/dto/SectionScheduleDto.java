package com.braculink.dto;

import com.braculink.model.ClassSlot;

import java.util.List;

public class SectionScheduleDto {

    private List<ClassSlot> classSchedules;

    public List<ClassSlot> getClassSchedules() {
        return classSchedules;
    }

    public void setClassSchedules(List<ClassSlot> classSchedules) {
        this.classSchedules = classSchedules;
    }
}
