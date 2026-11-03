package com.api.task_orchestrator.services;

import com.api.task_orchestrator.dtos.operation.OperationResponseDto;
import org.springframework.stereotype.Service;

import com.api.task_orchestrator.dtos.operation.NewOperationRequestDto;
import com.api.task_orchestrator.models.Operation;
import com.api.task_orchestrator.repository.OperationRepository;

@Service
public class OperationServices {

    private final OperationRepository operationRepository;

    public OperationServices(OperationRepository operationRepository) {
        this.operationRepository = operationRepository;
    }

    public OperationResponseDto createNewOperation(NewOperationRequestDto entry) {
        Operation operation = new Operation();
        operation.setDescription(entry.description());
        operation.setOrderCapacity(entry.orderCapacity());
        operation.setDeadline(entry.deadline());

        Operation operationCreated = operationRepository.save(operation);
        return OperationResponseDto.from(operationCreated);
    }
}
