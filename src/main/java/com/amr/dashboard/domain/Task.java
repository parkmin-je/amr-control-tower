package com.amr.dashboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "task", indexes = {
        @Index(name = "idx_task_robot_status", columnList = "robot_id, status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "robot_id", nullable = false, length = 50)
    private String robotId;

    @Column(nullable = false, length = 100)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.QUEUED;

    private Double targetX;
    private Double targetY;
    private Double targetTheta;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private Integer priority = 3;  // 1(최고) ~ 5(최저)

    @Column(nullable = false, length = 50)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(length = 255)
    private String errorMessage;

    @Builder
    public Task(String robotId, String title, TaskType type,
                Double targetX, Double targetY, Double targetTheta,
                String description, Integer priority, String createdBy) {
        this.robotId = robotId;
        this.title = title;
        this.type = type;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetTheta = targetTheta;
        this.description = description;
        this.priority = priority != null ? priority : 3;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.status = TaskStatus.QUEUED;
    }

    public void start() {
        this.status = TaskStatus.EXECUTING;
        this.startedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String error) {
        this.status = TaskStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = error;
    }

    public void cancel() {
        this.status = TaskStatus.CANCELLED;
        this.completedAt = LocalDateTime.now();
    }

    public void pause() {
        this.status = TaskStatus.PAUSED;
    }
}
