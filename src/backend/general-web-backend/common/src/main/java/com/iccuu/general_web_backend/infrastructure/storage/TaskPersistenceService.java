package com.iccuu.general_web_backend.core.storage;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Persists scheduled tasks to the database so they survive application restarts.
 * All timestamps use UTC (via {@code LocalDateTime.now(ZoneOffset.UTC)}) so
 * scheduling is immune to DST transitions.
 *
 * @implNote Infrastructure layer — direct Mapper access is intentional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskPersistenceService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ScheduledTaskMapper taskMapper;

    /**
     * Schedule a new task. Dedup by {@code taskKey} is intentional for
     * idempotency — calling forget-me twice for the same user will not
     * create duplicate physical-deletion tasks.
     *
     * @implNote Infrastructure layer — dedup by task_key is intentional for idempotency.
     */
    public void schedule(String taskType, String taskKey, Map<String, Object> payload, LocalDateTime executeAt) {
        ScheduledTask existing = taskMapper.selectOne(
                new LambdaQueryWrapper<ScheduledTask>().eq(ScheduledTask::getTaskKey, taskKey));
        if (existing != null) {
            log.debug("Task already scheduled: taskKey={}", taskKey);
            return;
        }

        ScheduledTask task = new ScheduledTask();
        task.setTaskType(taskType);
        task.setTaskKey(taskKey);
        task.setExecuteAt(executeAt);
        task.setStatus(0); // PENDING
        task.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        try {
            task.setPayloadJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payload for taskKey={}", taskKey, e);
            throw new RuntimeException("Task payload serialization failed", e);
        }
        taskMapper.insert(task);
        log.info("Scheduled task persisted: type={}, taskKey={}, executeAt={}", taskType, taskKey, executeAt);
    }

    public List<ScheduledTask> fetchDueTasks(int limit) {
        return taskMapper.selectList(
                new LambdaQueryWrapper<ScheduledTask>()
                        .eq(ScheduledTask::getStatus, 0) // PENDING
                        .le(ScheduledTask::getExecuteAt, LocalDateTime.now(ZoneOffset.UTC))
                        .last("LIMIT " + limit));
    }

    public boolean markRunning(Long taskId) {
        return taskMapper.update(null,
                new LambdaUpdateWrapper<ScheduledTask>()
                        .eq(ScheduledTask::getId, taskId)
                        .eq(ScheduledTask::getStatus, 0)
                        .set(ScheduledTask::getStatus, 1)) > 0; // RUNNING
    }

    public void markCompleted(Long taskId) {
        taskMapper.update(null,
                new LambdaUpdateWrapper<ScheduledTask>()
                        .eq(ScheduledTask::getId, taskId)
                        .set(ScheduledTask::getStatus, 2) // COMPLETED
                        .set(ScheduledTask::getExecutedAt, LocalDateTime.now(ZoneOffset.UTC)));
    }

    public void markFailed(Long taskId, String error) {
        taskMapper.update(null,
                new LambdaUpdateWrapper<ScheduledTask>()
                        .eq(ScheduledTask::getId, taskId)
                        .set(ScheduledTask::getStatus, 3) // FAILED
                        .set(ScheduledTask::getExecutedAt, LocalDateTime.now(ZoneOffset.UTC))
                        .set(ScheduledTask::getErrorMessage,
                                error.length() > 500 ? error.substring(0, 500) : error));
    }

    public void deleteByTaskKey(String taskKey) {
        taskMapper.delete(new LambdaQueryWrapper<ScheduledTask>().eq(ScheduledTask::getTaskKey, taskKey));
    }

    /**
     * Parse the JSON payload of a scheduled task.
     *
     * @throws RuntimeException if the payload cannot be deserialized, so the
     *         caller's error handler marks the task as FAILED rather than
     *         silently proceeding with an empty payload and hitting a
     *         NullPointerException downstream.
     */
    public Map<String, Object> parsePayload(ScheduledTask task) {
        try {
            return objectMapper.readValue(task.getPayloadJson(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Task payload deserialization failed for taskId=" + task.getId(), e);
        }
    }
}
