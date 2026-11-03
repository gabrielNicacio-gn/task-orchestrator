package com.api.task_orchestrator.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.api.task_orchestrator.dtos.operation.NewOperationRequestDto;
import com.api.task_orchestrator.dtos.operation.OperationResponseDto;
import com.api.task_orchestrator.services.OperationServices;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static org.springframework.http.ResponseEntity.created;

@RestController
@RequestMapping("/api/operation")
public class OperationController {

    private final OperationServices operationServices;

    public OperationController(OperationServices operationServices) {
        this.operationServices = operationServices;
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OperationResponseDto> createOperation(@RequestBody NewOperationRequestDto request) {
        OperationResponseDto result = operationServices.createNewOperation(request);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{idOperation}")
                .buildAndExpand(result.idOperation())
                .toUri();
        return ResponseEntity.created(uri).body(result);
    }
}
