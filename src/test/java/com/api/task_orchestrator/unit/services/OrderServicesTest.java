package com.api.task_orchestrator.unit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.api.task_orchestrator.dtos.order.NewOrderRequestDto;
import com.api.task_orchestrator.dtos.order.NewOrdersRequestDto;
import com.api.task_orchestrator.dtos.order.OrderResponseDto;
import com.api.task_orchestrator.models.Operation;
import com.api.task_orchestrator.models.OperationStatus;
import com.api.task_orchestrator.models.Order;
import com.api.task_orchestrator.models.OrderStatus;
import com.api.task_orchestrator.models.TaskPhase;
import com.api.task_orchestrator.models.TaskPriority;
import com.api.task_orchestrator.models.TaskStatus;
import com.api.task_orchestrator.repository.OperationRepository;
import com.api.task_orchestrator.repository.OrderRepository;
import com.api.task_orchestrator.services.OrderServices;

class OrderServicesTest {

    private static final UUID OPERATION_ID = UUID.fromString("a17740b8-ad93-48d6-807d-78149e4391ef");
    private static final OffsetDateTime ORDER_DATE = OffsetDateTime.parse("2035-06-01T12:00:00Z");

    private final OperationRepository operationRepository = mock(OperationRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderServices orderServices = new OrderServices(operationRepository, orderRepository);

    @Test
    void createsPendingOrdersWithAvailablePickingTasksAndKeepsCapacityOpen() {
        Operation operation = givenOperation(OperationStatus.CREATED, 3);
        NewOrderRequestDto firstOrder = newOrder("ORD-002");
        NewOrderRequestDto secondOrder = newOrder("ORD-001");
        when(orderRepository.findAllByOperation_IdOperationAndOrderNumberIn(
                OPERATION_ID, Set.of("ORD-001", "ORD-002"))).thenReturn(List.of());

        List<OrderResponseDto> response = orderServices.createOrders(
                OPERATION_ID, batch(firstOrder, secondOrder));

        ArgumentCaptor<Iterable<Order>> ordersCaptor = ArgumentCaptor.captor();
        verify(orderRepository).saveAll(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).satisfiesExactly(
                order -> assertCreatedOrder(order, firstOrder, operation),
                order -> assertCreatedOrder(order, secondOrder, operation));
        assertThat(response).extracting(OrderResponseDto::orderNumber).containsExactly("ORD-002", "ORD-001");
        assertThat(response).allSatisfy(order -> {
            assertThat(order.operationId()).isEqualTo(OPERATION_ID);
            assertThat(order.orderDate()).isEqualTo(ORDER_DATE);
            assertThat(order.status()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.tasks()).singleElement().satisfies(task -> {
                assertThat(task.phase()).isEqualTo(TaskPhase.PICKING);
                assertThat(task.status()).isEqualTo(TaskStatus.AVAILABLE);
            });
        });
        assertThat(operation.getStatus()).isEqualTo(OperationStatus.ACCEPTING_ORDERS);
        verify(operationRepository).save(operation);
    }

    @Test
    void countsOnlyNewOrdersTowardCapacityAndReturnsMixedBatchInRequestOrder() {
        Operation operation = givenOperation(OperationStatus.ACCEPTING_ORDERS, 2);
        Order existingOrder = existingOrder(operation);
        NewOrderRequestDto newOrder = newOrder("ORD-002");
        when(orderRepository.findAllByOperation_IdOperationAndOrderNumberIn(
                OPERATION_ID, Set.of("ORD-001", "ORD-002"))).thenReturn(List.of(existingOrder));
        when(orderRepository.countByOperation_IdOperation(OPERATION_ID)).thenReturn(1L);

        List<OrderResponseDto> response = orderServices.createOrders(
                OPERATION_ID, batch(newOrder, newOrder("ORD-001")));

        ArgumentCaptor<Iterable<Order>> ordersCaptor = ArgumentCaptor.captor();
        verify(orderRepository).saveAll(ordersCaptor.capture());
        assertThat(ordersCaptor.getValue()).singleElement()
                .satisfies(order -> assertCreatedOrder(order, newOrder, operation));
        assertThat(response).extracting(OrderResponseDto::orderNumber).containsExactly("ORD-002", "ORD-001");
        assertThat(response.get(1)).isEqualTo(OrderResponseDto.from(existingOrder));
        assertThat(operation.getStatus()).isEqualTo(OperationStatus.IN_PROGRESS);
        verify(operationRepository).save(operation);
    }

    @ParameterizedTest
    @EnumSource(value = OperationStatus.class, names = { "COMPLETED", "CANCELLED" })
    void returnsExistingOrderForSameInstantEvenAfterOperationCloses(OperationStatus status) {
        Operation operation = givenOperation(status, 1);
        Order existingOrder = existingOrder(operation);
        when(orderRepository.findAllByOperation_IdOperationAndOrderNumberIn(
                OPERATION_ID, Set.of("ORD-001"))).thenReturn(List.of(existingOrder));
        when(orderRepository.countByOperation_IdOperation(OPERATION_ID)).thenReturn(1L);
        OffsetDateTime sameInstant = ORDER_DATE.withOffsetSameInstant(ZoneOffset.ofHours(-3));

        List<OrderResponseDto> response = orderServices.createOrders(
                OPERATION_ID, batch(new NewOrderRequestDto("ORD-001", sameInstant)));

        assertThat(response).containsExactly(OrderResponseDto.from(existingOrder));
        assertThat(operation.getStatus()).isEqualTo(status);
        verifyNoWrites();
    }

    @ParameterizedTest
    @MethodSource("emptyBatches")
    void rejectsMissingOrEmptyBatchBeforeRepositoryAccess(NewOrdersRequestDto request) {
        assertRejected(request, HttpStatus.BAD_REQUEST);

        verifyNoInteractions(operationRepository, orderRepository);
    }

    static Stream<NewOrdersRequestDto> emptyBatches() {
        return Stream.of(null, new NewOrdersRequestDto(null), new NewOrdersRequestDto(List.of()));
    }

    @ParameterizedTest
    @MethodSource("invalidBatches")
    void rejectsInvalidOrdersAndDuplicateNumbers(NewOrdersRequestDto request) {
        givenOperation(OperationStatus.CREATED, 2);

        assertRejected(request, HttpStatus.BAD_REQUEST);

        verifyNoInteractions(orderRepository);
        verify(operationRepository, never()).save(any());
    }

    static Stream<NewOrdersRequestDto> invalidBatches() {
        return Stream.of(
                new NewOrdersRequestDto(Collections.singletonList(null)),
                batch(newOrder(null)),
                batch(newOrder(" \t")),
                batch(new NewOrderRequestDto("ORD-001", null)),
                batch(newOrder("ORD-001"), newOrder("ORD-001")));
    }

    @Test
    void rejectsOrdersForMissingOperation() {
        when(operationRepository.findByIdForUpdate(OPERATION_ID)).thenReturn(Optional.empty());

        assertRejected(batch(newOrder("ORD-001")), HttpStatus.NOT_FOUND);

        verifyNoInteractions(orderRepository);
        verify(operationRepository, never()).save(any());
    }

    @Test
    void rejectsWholeBatchWhenExistingOrderHasDifferentDate() {
        Operation operation = givenOperation(OperationStatus.ACCEPTING_ORDERS, 3);
        when(orderRepository.findAllByOperation_IdOperationAndOrderNumberIn(
                OPERATION_ID, Set.of("ORD-001", "ORD-002"))).thenReturn(List.of(existingOrder(operation)));

        assertRejected(batch(newOrder("ORD-002"), new NewOrderRequestDto("ORD-001", ORDER_DATE.plusDays(1))),
                HttpStatus.CONFLICT);

        assertThat(operation.getStatus()).isEqualTo(OperationStatus.ACCEPTING_ORDERS);
        verifyNoWrites();
    }

    @ParameterizedTest
    @EnumSource(value = OperationStatus.class, names = { "COMPLETED", "CANCELLED" })
    void rejectsNewOrdersAfterOperationCloses(OperationStatus status) {
        Operation operation = givenOperation(status, 2);

        assertRejected(batch(newOrder("ORD-001")), HttpStatus.CONFLICT);

        assertThat(operation.getStatus()).isEqualTo(status);
        verifyNoWrites();
    }

    @Test
    void rejectsWholeBatchWhenOnlyPartOfItFits() {
        Operation operation = givenOperation(OperationStatus.ACCEPTING_ORDERS, 2);
        when(orderRepository.countByOperation_IdOperation(OPERATION_ID)).thenReturn(1L);

        assertRejected(batch(newOrder("ORD-001"), newOrder("ORD-002")), HttpStatus.CONFLICT);

        assertThat(operation.getStatus()).isEqualTo(OperationStatus.ACCEPTING_ORDERS);
        verifyNoWrites();
    }

    private Operation givenOperation(OperationStatus status, int capacity) {
        Operation operation = new Operation();
        operation.setIdOperation(OPERATION_ID);
        operation.setStatus(status);
        operation.setOrderCapacity(capacity);
        when(operationRepository.findByIdForUpdate(OPERATION_ID)).thenReturn(Optional.of(operation));
        return operation;
    }

    private static Order existingOrder(Operation operation) {
        Order order = new Order();
        order.setId(UUID.fromString("17609bfe-ef6a-4e42-9c14-07dbb78a98fb"));
        order.setOrderNumber("ORD-001");
        order.setOrderDate(ORDER_DATE);
        order.setStatus(OrderStatus.PENDING);
        order.setOperation(operation);
        return order;
    }

    private static NewOrderRequestDto newOrder(String number) {
        return new NewOrderRequestDto(number, ORDER_DATE);
    }

    private static NewOrdersRequestDto batch(NewOrderRequestDto... orders) {
        return new NewOrdersRequestDto(List.of(orders));
    }

    private static void assertCreatedOrder(Order order, NewOrderRequestDto request, Operation operation) {
        assertThat(order.getOrderNumber()).isEqualTo(request.orderNumber());
        assertThat(order.getOrderDate()).isEqualTo(request.orderDate());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getOperation()).isSameAs(operation);
        assertThat(order.getTasks()).singleElement().satisfies(task -> {
            assertThat(task.getOrder()).isSameAs(order);
            assertThat(task.getPhase()).isEqualTo(TaskPhase.PICKING);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.AVAILABLE);
            assertThat(task.getPriority()).isEqualTo(TaskPriority.NORMAL);
            assertThat(task.getTaskNumber()).startsWith("T-");
        });
    }

    private void assertRejected(NewOrdersRequestDto request, HttpStatus status) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> orderServices.createOrders(OPERATION_ID, request));
        assertThat(exception.getStatusCode()).isEqualTo(status);
    }

    private void verifyNoWrites() {
        verify(orderRepository, never()).saveAll(any());
        verify(operationRepository, never()).save(any());
    }
}
