package com.api.task_orchestrator.dtos.order;

import java.util.List;

public record NewOrdersRequestDto(List<NewOrderRequestDto> orders) {
}
