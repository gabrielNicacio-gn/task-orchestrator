package com.api.task_orchestrator.repository;

import java.util.UUID;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.api.task_orchestrator.models.Operation;

public interface OperationRepository extends JpaRepository<Operation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select operation from Operation operation where operation.idOperation = :idOperation")
    Optional<Operation> findByIdForUpdate(@Param("idOperation") UUID idOperation);
}
