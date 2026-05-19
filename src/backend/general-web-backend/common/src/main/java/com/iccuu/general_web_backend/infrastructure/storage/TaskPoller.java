package com.iccuu.general_web_backend.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Polls the scheduled_tasks table for due tasks and executes them.
 * This replaces in-memory TaskScheduler scheduling with DB-backed persistence
 * so that scheduled tasks survive application restarts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskPoller {

    private static final int BATCH_SIZE = 20;

    private final TaskPersistenceService taskPersistenceService;
    private final PhysicalDeleteExecutor physicalDeleteExecutor;

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void pollAndExecute() {
        List<ScheduledTask> dueTasks = taskPersistenceService.fetchDueTasks(BATCH_SIZE);
        if (dueTasks.isEmpty()) return;

        log.info("TaskPoller: found {} due tasks", dueTasks.size());
        for (ScheduledTask task : dueTasks) {
            if (!taskPersistenceService.markRunning(task.getId())) {
                continue; // another instance grabbed it
            }
            try {
                executeTask(task);
                taskPersistenceService.markCompleted(task.getId());
            } catch (Exception e) {
                log.error("TaskPoller: task {} failed", task.getId(), e);
                taskPersistenceService.markFailed(task.getId(), e.getMessage());
            }
        }
    }

    private void executeTask(ScheduledTask task) {
        String taskType = task.getTaskType();
        Map<String, Object> payload = taskPersistenceService.parsePayload(task);

        if ("PHYSICAL_DELETE_USER".equals(taskType)) {
            Long userId = Long.valueOf(payload.get("userId").toString());
            physicalDeleteExecutor.executeCascadeDelete(userId);
            // Delete the scheduled_tasks row LAST — if executeCascadeDelete
            // partially fails and rolls back, the task remains for retry.
            jdbcDeleteTaskRow(task.getTaskKey());
        } else {
            throw new IllegalArgumentException("Unknown task type: " + taskType);
        }
    }

    private void jdbcDeleteTaskRow(String taskKey) {
        // Use raw JDBC to avoid MyBatis-Plus mapper dependency in infrastructure layer
        taskPersistenceService.deleteByTaskKey(taskKey);
    }
}
