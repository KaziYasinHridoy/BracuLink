package com.braculink;

import com.braculink.service.CourseSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BraculinkApplication {

    private static final Logger log = LoggerFactory.getLogger(BraculinkApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BraculinkApplication.class, args);
    }

    @Bean
    CommandLineRunner runStartupCourseSync(CourseSyncService courseSyncService) {
        return args -> {
            try {
                int count = courseSyncService.syncNow();
                log.info("Startup course section sync complete: {} rows upserted", count);
            } catch (Exception e) {
                log.warn("Startup course section sync failed: {}", e.getMessage());
            }
        };
    }

}
