package com.braculink.scheduler;

import com.braculink.service.CourseSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CourseSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CourseSyncScheduler.class);

    private final CourseSyncService courseSyncService;

    public CourseSyncScheduler(CourseSyncService courseSyncService) {
        this.courseSyncService = courseSyncService;
    }

    @Scheduled(fixedRate = 600000)
    public void syncCourseSections() {
        try {
            int count = courseSyncService.syncNow();
            log.info("Course section sync complete: {} rows upserted", count);
        } catch (Exception e) {
            log.warn("Course section sync failed: {}", e.getMessage());
        }
    }
}
