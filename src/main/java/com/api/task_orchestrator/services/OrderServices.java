package com.api.task_orchestrator.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.api.task_orchestrator.dtos.order.NewOrderRequestDto;
import com.api.task_orchestrator.dtos.order.NewOrdersRequestDto;
import com.api.task_orchestrator.dtos.order.OrderResponseDto;
import com.api.task_orchestrator.models.Operation;
import com.api.task_orchestrator.models.OperationStatus;
import com.api.task_orchestrator.models.Order;
import com.api.task_orchestrator.models.OrderStatus;
import com.api.task_orchestrator.models.Task;
import com.api.task_orchestrator.models.TaskPhase;
import com.api.task_orchestrator.models.TaskPriority;
import com.api.task_orchestrator.models.TaskStatus;
import com.api.task_orchestrator.repository.OperationRepository;
import com.api.task_orchestrator.repository.OrderRepository;

@Service
public class OrderServices {
    private final OperationRepository operationRepository;
    private final OrderRepository orderRepository;

    public OrderServices(OperationRepository operationRepository, OrderRepository orderRepository) {
        this.operationRepository = operationRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public List<OrderResponseDto> createOrders(UUID operationId, NewOrdersRequestDto request) {
        if (request == null || request.orders() == null || request.orders().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The order batch must not be empty");
        }

        Operation operation = operationRepository.findByIdForUpdate(operationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operation not found"));

        List<NewOrderRequestDto> requestedOrders = request.orders();
        Set<String> requestedNumbers = createRequestedNumbers(requestedOrders);

        Map<String, Order> ordersByNumber = new HashMap<>();
        orderRepository.findAllByOperation_IdOperationAndOrderNumberIn(operationId, requestedNumbers)
                .forEach(order -> ordersByNumber.put(order.getOrderNumber(), order));

        List<NewOrderRequestDto> ordersToCreate = new ArrayList<>();
        for (NewOrderRequestDto requestedOrder : requestedOrders) {
            Order existingOrder = ordersByNumber.get(requestedOrder.orderNumber());
            if (existingOrder == null) {
                ordersToCreate.add(requestedOrder);
            }
            else if (!existingOrder.getOrderDate().toInstant().equals(requestedOrder.orderDate().toInstant())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An order with this number already exists in the operation with different data");
            }
        }

        if (!ordersToCreate.isEmpty()
                && (operation.getStatus() == OperationStatus.COMPLETED
                        || operation.getStatus() == OperationStatus.CANCELLED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Orders cannot be added to a completed or cancelled operation");
        }

        long currentOrderCount = orderRepository.countByOperation_IdOperation(operationId);
        if (currentOrderCount + ordersToCreate.size() > operation.getOrderCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "The order batch exceeds the operation capacity");
        }

        List<Order> createdOrders = new ArrayList<>();
        for (NewOrderRequestDto requestedOrder : ordersToCreate) {
            Order order = new Order();
            order.setOrderNumber(requestedOrder.orderNumber());
            order.setOrderDate(requestedOrder.orderDate());
            order.setStatus(OrderStatus.PENDING);
            order.setOperation(operation);

            Task pickingTask = new Task();
            pickingTask.setTaskNumber("T-" + UUID.randomUUID());
            pickingTask.setPhase(TaskPhase.PICKING);
            pickingTask.setStatus(TaskStatus.AVAILABLE);
            pickingTask.setPriority(TaskPriority.NORMAL);
            pickingTask.setOrder(order);
            order.getTasks().add(pickingTask);

            createdOrders.add(order);
        }

        if (!createdOrders.isEmpty()) {
            orderRepository.saveAll(createdOrders);
            createdOrders.forEach(order -> ordersByNumber.put(order.getOrderNumber(), order));

            long totalOrderCount = currentOrderCount + createdOrders.size();
            operation.setStatus(totalOrderCount == operation.getOrderCapacity()
                    ? OperationStatus.IN_PROGRESS
                    : OperationStatus.ACCEPTING_ORDERS);
            operationRepository.save(operation);
        }

        return requestedOrders.stream()
                .map(order -> OrderResponseDto.from(ordersByNumber.get(order.orderNumber())))
                .toList();
    }

    private static @NonNull Set<String> createRequestedNumbers(List<NewOrderRequestDto> requestedOrders) {
        Set<String> requestedNumbers = new HashSet<>();
        for (NewOrderRequestDto requestedOrder : requestedOrders) {
            if (requestedOrder == null || requestedOrder.orderNumber() == null
                    || requestedOrder.orderNumber().isBlank() || requestedOrder.orderDate() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Each order must have an order number and an order date");
            }
            if (!requestedNumbers.add(requestedOrder.orderNumber())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "The batch contains a duplicate order number");
            }
        }
        return requestedNumbers;
    }
}
