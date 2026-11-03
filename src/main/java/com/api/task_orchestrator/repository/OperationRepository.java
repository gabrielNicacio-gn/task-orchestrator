package com.api.task_orchestrator.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.task_orchestrator.models.Operation;

public interface OperationRepository extends JpaRepository<Operation, UUID> {

}
