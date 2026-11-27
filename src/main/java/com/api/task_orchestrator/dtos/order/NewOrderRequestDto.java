package com.api.task_orchestrator.dtos.order;

import java.time.OffsetDateTime;

public record NewOrderRequestDto(
        String orderNumber,
        OffsetDateTime orderDate) {
}
