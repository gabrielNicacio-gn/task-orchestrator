package com.api.task_orchestrator.dtos.operation;

import com.api.task_orchestrator.models.Operation;

import java.time.OffsetDateTime;

public record NewOperationRequestDto(
        String description,
        int orderCapacity,
        OffsetDateTime deadline) { }
