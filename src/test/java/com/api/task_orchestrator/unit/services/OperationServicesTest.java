package com.api.task_orchestrator.unit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import com.api.task_orchestrator.services.OperationServices;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.api.task_orchestrator.dtos.operation.NewOperationRequestDto;
import com.api.task_orchestrator.dtos.operation.OperationResponseDto;
import com.api.task_orchestrator.models.Operation;
import com.api.task_orchestrator.repository.OperationRepository;

class OperationServicesTest {

    private final OperationRepository operationRepository = mock(OperationRepository.class);
    private final OperationServices operationServices = new OperationServices(operationRepository);

    @Test
    void createsOperationFromRequestAndReturnsItsData() {
        OffsetDateTime deadline = OffsetDateTime.parse("2035-06-01T12:00:00Z");
        NewOperationRequestDto request = new NewOperationRequestDto("Fulfillment", 100, deadline);
        when(operationRepository.save(any(Operation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OperationResponseDto response = operationServices.createNewOperation(request);

        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepository).save(operationCaptor.capture());
        Operation savedOperation = operationCaptor.getValue();
        assertEquals("Fulfillment", savedOperation.getDescription());
        assertEquals(100, savedOperation.getOrderCapacity());
        assertEquals(deadline, savedOperation.getDeadline());
        assertEquals("Fulfillment", response.description());
        assertEquals(100, response.orderCapacity());
        assertEquals(deadline, response.deadline());
    }
}
