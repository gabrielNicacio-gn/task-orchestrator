package com.api.task_orchestrator.models;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "operation")
public class Operation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID idOperation;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "order_capacity", nullable = false)
    private int orderCapacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deadline", nullable = false)
    private OffsetDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private OperationStatus status;

    @OneToMany(mappedBy = "operation", cascade = CascadeType.PERSIST)
    private List<Order> orders = new ArrayList<>();

    @PrePersist
    private void applyDefaults() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
        if (status == null)
            status = OperationStatus.CREATED;
        if (description == null)
            description = "";
    }
}
