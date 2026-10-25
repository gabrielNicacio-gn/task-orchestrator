package com.api.task_orchestrator.models;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "task", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tarefa_pedido_fase", columnNames = { "pedido_id", "fase" })
})
public class Tasks {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID idTask;

    @Column(name = "numero_tarefa", nullable = false, unique = true, length = 64)
    private String taskNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "fase", nullable = false, length = 24)
    private TaskPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade", nullable = false, length = 24)
    private TaskPriority priority;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "prazo")
    private OffsetDateTime deadline;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depende_de_tarefa_id")
    private Tasks dependsOn;

    @PrePersist
    private void applyDefaults() {
        if (createdAt == null)
            createdAt = OffsetDateTime.now();
        if (status == null)
            status = TaskStatus.PENDENTE;
        if (priority == null)
            priority = TaskPriority.NORMAL;
    }
}
