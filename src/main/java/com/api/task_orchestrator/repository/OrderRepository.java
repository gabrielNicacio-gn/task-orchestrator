package com.api.task_orchestrator.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.api.task_orchestrator.models.Order;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    long countByOperation_IdOperation(UUID operationId);

    List<Order> findAllByOperation_IdOperationAndOrderNumberIn(UUID operationId, Collection<String> orderNumbers);
}
