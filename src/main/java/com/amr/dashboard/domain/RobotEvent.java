package com.amr.dashboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "robot_event", indexes = {
        @Index(name = "idx_robot_event_robot_id_time", columnList = "robot_id, occurred_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RobotEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "robot_id", nullable = false, length = 50)
    private String robotId;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    public enum EventType {
        STARTED, STOPPED, LOW_BATTERY, OBSTACLE_DETECTED, GOAL_REACHED, ERROR
    }

    @Builder
    public RobotEvent(String robotId, LocalDateTime occurredAt,
                      EventType eventType, String message) {
        this.robotId = robotId;
        this.occurredAt = occurredAt;
        this.eventType = eventType;
        this.message = message;
    }
}
