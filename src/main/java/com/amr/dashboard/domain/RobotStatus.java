package com.amr.dashboard.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "robot_status", indexes = {
        @Index(name = "idx_robot_status_robot_id_time", columnList = "robot_id, recorded_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RobotStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "robot_id", nullable = false, length = 50)
    private String robotId;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "pos_x")
    private Double posX;

    @Column(name = "pos_y")
    private Double posY;

    @Column(name = "linear_vel")
    private Double linearVel;

    @Column(name = "angular_vel")
    private Double angularVel;

    @Column(name = "battery")
    private Integer battery;

    @Builder
    public RobotStatus(String robotId, LocalDateTime recordedAt,
                       Double posX, Double posY,
                       Double linearVel, Double angularVel,
                       Integer battery) {
        this.robotId = robotId;
        this.recordedAt = recordedAt;
        this.posX = posX;
        this.posY = posY;
        this.linearVel = linearVel;
        this.angularVel = angularVel;
        this.battery = battery;
    }
}
