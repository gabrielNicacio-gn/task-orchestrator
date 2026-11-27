package com.api.task_orchestrator.dtos.order;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.api.task_orchestrator.models.Order;
import com.api.task_orchestrator.models.OrderStatus;

public record OrderResponseDto(
        UUID id,
        String orderNumber,
        OffsetDateTime orderDate,
        OrderStatus status,
        UUID operationId,
        List<CreatedTaskDto> tasks) {

    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                order.getStatus(),
                order.getOperation().getIdOperation(),
                order.getTasks().stream()
                        .map(task -> new CreatedTaskDto(
                                task.getId(),
                                task.getTaskNumber(),
                                task.getPhase(),
                                task.getStatus()))
                        .toList());
    }
}
