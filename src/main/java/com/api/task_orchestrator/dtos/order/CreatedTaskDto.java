package com.api.task_orchestrator.dtos.order;

import java.util.UUID;

import com.api.task_orchestrator.models.TaskPhase;
import com.api.task_orchestrator.models.TaskStatus;

public record CreatedTaskDto(
        UUID id,
        String taskNumber,
        TaskPhase phase,
        TaskStatus status) {
}
