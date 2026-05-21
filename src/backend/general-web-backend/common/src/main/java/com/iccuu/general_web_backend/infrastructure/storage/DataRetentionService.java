package com.iccuu.general_web_backend.core.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Handles scheduled data retention operations (GDPR physical deletion, etc.).
 * Delegates to {@link TaskPersistenceService} for DB-backed persistence so
 * scheduled tasks survive application restarts.
 * <p>
 * All timestamps use UTC ({@code LocalDateTime.now(ZoneOffset.UTC)}) so that
 * scheduling is immune to DST transitions on the host machine.
 *
 * @implNote Infrastructure layer — coordinates with TaskPersistenceService
 *           and TaskPoller.
 */
@Service
public class DataRetentionService {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionService.class);

    private final TaskPersistenceService taskPersistenceService;

    public DataRetentionService(TaskPersistenceService taskPersistenceService) {
        this.taskPersistenceService = taskPersistenceService;
    }

    public void schedulePhysicalDeletion(Long userId, long delayDays) {
        LocalDateTime executeAt = LocalDateTime.now(ZoneOffset.UTC).plusDays(delayDays);
        String taskKey = "user:" + userId;
        taskPersistenceService.schedule("PHYSICAL_DELETE_USER", taskKey,
                Map.of("userId", userId), executeAt);
        log.info("Scheduled physical deletion for userId={}, executeAt={}", userId, executeAt);
    }
}
