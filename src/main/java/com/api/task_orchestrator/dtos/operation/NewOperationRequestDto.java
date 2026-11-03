package com.api.task_orchestrator.dtos.operation;

import java.time.OffsetDateTime;

public record NewOperationRequestDto(
        String description,
        int orderCapacity,
        OffsetDateTime deadline) { }
