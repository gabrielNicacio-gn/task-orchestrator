package com.api.task_orchestrator.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.api.task_orchestrator.dtos.order.NewOrdersRequestDto;
import com.api.task_orchestrator.dtos.order.OrderResponseDto;
import com.api.task_orchestrator.dtos.operation.NewOperationRequestDto;
import com.api.task_orchestrator.dtos.operation.OperationResponseDto;
import com.api.task_orchestrator.services.OrderServices;
import com.api.task_orchestrator.services.OperationServices;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/operation")
public class OperationController {

    private final OperationServices operationServices;
    private final OrderServices orderServices;

    public OperationController(OperationServices operationServices, OrderServices orderServices) {
        this.operationServices = operationServices;
        this.orderServices = orderServices;
    }

    @PostMapping
    public ResponseEntity<OperationResponseDto> createOperation(@RequestBody NewOperationRequestDto request) {
        OperationResponseDto result = operationServices.createNewOperation(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idOperation}")
                .buildAndExpand(result.idOperation())
                .toUri();
        return ResponseEntity.created(uri).body(result);
    }

    @PostMapping("/{operationId}/orders")
    public ResponseEntity<List<OrderResponseDto>> createOrders(
            @PathVariable UUID operationId,
            @RequestBody NewOrdersRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderServices.createOrders(operationId, request));
    }
}
