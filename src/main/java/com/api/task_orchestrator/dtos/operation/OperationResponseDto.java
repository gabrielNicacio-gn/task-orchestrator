package com.api.task_orchestrator.dtos.operation;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.api.task_orchestrator.models.Operation;
import com.api.task_orchestrator.models.OperationStatus;

public record OperationResponseDto(
        UUID idOperation,
        String description,
        int orderCapacity,
        OffsetDateTime createdAt,
        OffsetDateTime deadline,
        OperationStatus status) {

    public static OperationResponseDto from(Operation operation) {
        return new OperationResponseDto(
                operation.getIdOperation(),
                operation.getDescription(),
                operation.getOrderCapacity(),
                operation.getCreatedAt(),
                operation.getDeadline(),
                operation.getStatus());
    }
}
